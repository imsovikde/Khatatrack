package com.example.util

import android.content.Context
import com.example.data.model.Contact
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ParsedTransactionResult(
    val intent: String = Transaction.TYPE_YOU_GAVE, // YOU_GAVE or YOU_GOT
    val amount: Double? = null,
    val contactName: String? = null,
    val paymentMode: String = "Cash",
    val note: String? = null,
    val collectionDueDate: Long? = null,
    val referenceNumber: String? = null,
    val parsedSource: String = "LOCAL" // LOCAL or GEMINI
)

object IntelligentParser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Main Entry point: Parses input text. If Gemini BYOK enhancement is enabled, tries Gemini
     * with a 3-second hard timeout. If offline, error, timeout, or disabled, falls back silently to Local NLP.
     */
    suspend fun parseInputText(
        context: Context,
        rawText: String,
        activeContacts: List<Contact> = emptyList()
    ): ParsedTransactionResult = withContext(Dispatchers.IO) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return@withContext ParsedTransactionResult()
        }

        val isAiParsingEnabled = AiConfigManager.getUseAiParsing(context)
        val apiKey = AiConfigManager.getCustomApiKey(context).ifBlank { AiConfigManager.getEffectiveGeminiKey() }

        if (isAiParsingEnabled && apiKey.isNotBlank()) {
            try {
                // Hard 3-second timeout requirement for optional Gemini call
                val geminiRes = withTimeout(3000L) {
                    parseWithGeminiApi(apiKey, trimmed)
                }
                if (geminiRes != null) {
                    // Match contact name from Gemini result against local contacts if needed
                    val matchedContactName = geminiRes.contactName?.let { cName ->
                        matchContactFuzzy(cName, activeContacts) ?: cName
                    }
                    return@withContext geminiRes.copy(
                        contactName = matchedContactName,
                        parsedSource = "GEMINI"
                    )
                }
            } catch (e: Exception) {
                // Silent fallback to local parser
                e.printStackTrace()
            }
        }

        // Mandatory Baseline Local NLP Pipeline
        return@withContext parseInputTextLocal(trimmed, activeContacts)
    }

    /**
     * Local 5-Stage NLP Pipeline (Q.3)
     */
    fun parseInputTextLocal(
        rawText: String,
        activeContacts: List<Contact> = emptyList()
    ): ParsedTransactionResult {
        val lowerText = rawText.lowercase()

        // 1. Intent Classifier
        val gaveKeywords = listOf("gave", "paid", "lent", "spent", "sent", "transfer to", "diya", "deya")
        val gotKeywords = listOf("got", "received", "took", "borrowed", "collected", "liya", "leya")

        var intent = Transaction.TYPE_YOU_GAVE
        val containsGot = gotKeywords.any { lowerText.contains(it) }
        val containsGave = gaveKeywords.any { lowerText.contains(it) }

        if (containsGot && !containsGave) {
            intent = Transaction.TYPE_YOU_GOT
        } else if (containsGave) {
            intent = Transaction.TYPE_YOU_GAVE
        }

        // 2. Amount Extractor
        var amount: Double? = null
        val amountRegex = Regex("(?:₹|rs\\.?|rupees?)?\\s*(\\d+(?:\\.\\d{1,2})?|\\d+k)", RegexOption.IGNORE_CASE)
        val matchResult = amountRegex.find(rawText)
        if (matchResult != null) {
            val amountStr = matchResult.groupValues[1].lowercase()
            amount = if (amountStr.endsWith("k")) {
                val numPart = amountStr.dropLast(1).toDoubleOrNull()
                if (numPart != null) numPart * 1000.0 else null
            } else {
                amountStr.toDoubleOrNull()
            }
        }

        // 3. Contact Matcher
        var matchedContact: String? = null
        var maxConfidence = 0.65 // Require high similarity threshold to prevent false matches

        for (contact in activeContacts) {
            val cName = contact.name.trim()
            val cNameLower = cName.lowercase()
            if (cNameLower.isNotEmpty()) {
                if (lowerText.contains(cNameLower)) {
                    val score = 1.0
                    if (score > maxConfidence) {
                        maxConfidence = score
                        matchedContact = cName
                    }
                } else {
                    val tokens = cNameLower.split("\\s+".toRegex()).filter { it.length >= 2 }
                    for (tok in tokens) {
                        if (lowerText.contains(tok)) {
                            val score = 0.9
                            if (score > maxConfidence) {
                                maxConfidence = score
                                matchedContact = cName
                            }
                        }
                    }
                    val wordsInText = lowerText.split("\\s+".toRegex()).filter { it.length >= 2 }
                    for (word in wordsInText) {
                        for (tok in tokens) {
                            val sim = calculateSimilarity(tok, word)
                            if (sim >= 0.75 && sim > maxConfidence) {
                                maxConfidence = sim
                                matchedContact = cName
                            }
                        }
                    }
                }
            }
        }

        // Only accept matched contact if confidence > 0.85
        if (maxConfidence < 0.85) {
            matchedContact = null
        }

        // Offline Fallback Name Extraction for New Contacts
        if (matchedContact == null) {
            val namePatterns = listOf(
                Regex("(?:gave|paid|lent|sent|diya|deya)\\s+([a-zA-Z]{2,20})", RegexOption.IGNORE_CASE),
                Regex("(?:got|received|took|borrowed|liya|leya|from)\\s+([a-zA-Z]{2,20})", RegexOption.IGNORE_CASE),
                Regex("(?:to|for)\\s+([a-zA-Z]{2,20})", RegexOption.IGNORE_CASE)
            )
            val ignoreWords = setOf("cash", "upi", "bank", "card", "cheque", "check", "today", "tomorrow", "for", "the", "and", "via", "due", "rs", "rupees", "rupee", "k", "with", "other")
            for (pat in namePatterns) {
                val match = pat.find(rawText)
                if (match != null) {
                    val candidate = match.groupValues[1].lowercase()
                    if (!ignoreWords.contains(candidate) && candidate.length >= 2) {
                        matchedContact = candidate.replaceFirstChar { it.uppercase() }
                        break
                    }
                }
            }
        }

        // 4. Payment Mode Resolver & Reference Number
        var paymentMode = "Cash"
        var refNumber: String? = null

        if (lowerText.contains("upi") || lowerText.contains("gpay") || lowerText.contains("phonepe") || lowerText.contains("paytm")) {
            paymentMode = "UPI"
        } else if (lowerText.contains("bank") || lowerText.contains("neft") || lowerText.contains("imps") || lowerText.contains("rtgs")) {
            paymentMode = "Bank Transfer"
        } else if (lowerText.contains("cheque") || lowerText.contains("check")) {
            paymentMode = "Cheque"
        } else if (lowerText.contains("card") || lowerText.contains("debit") || lowerText.contains("credit")) {
            paymentMode = "Card"
        }

        // Reference number extraction (e.g., UTR 123456)
        val refRegex = Regex("(?:utr|ref|cheque\\s*no\\.?|tx\\s*id)\\s*:?\\s*([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE)
        val refMatch = refRegex.find(rawText)
        if (refMatch != null) {
            refNumber = refMatch.groupValues[1]
        }

        // 5. Relative Date Parser for collectionDueDate
        var dueDateMillis: Long? = null
        val cal = Calendar.getInstance()

        if (lowerText.contains("due today") || lowerText.contains("today")) {
            dueDateMillis = cal.timeInMillis
        } else if (lowerText.contains("tomorrow") || lowerText.contains("kal")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            dueDateMillis = cal.timeInMillis
        } else if (lowerText.contains("next friday")) {
            var daysUntilFriday = (Calendar.FRIDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (daysUntilFriday == 0) daysUntilFriday = 7
            cal.add(Calendar.DAY_OF_YEAR, daysUntilFriday)
            dueDateMillis = cal.timeInMillis
        } else if (lowerText.contains("next week")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            dueDateMillis = cal.timeInMillis
        } else if (lowerText.contains("next month")) {
            cal.add(Calendar.MONTH, 1)
            dueDateMillis = cal.timeInMillis
        } else {
            val inDaysRegex = Regex("in\\s*(\\d+)\\s*days?", RegexOption.IGNORE_CASE)
            val daysMatch = inDaysRegex.find(rawText)
            if (daysMatch != null) {
                val days = daysMatch.groupValues[1].toIntOrNull() ?: 0
                if (days > 0) {
                    cal.add(Calendar.DAY_OF_YEAR, days)
                    dueDateMillis = cal.timeInMillis
                }
            }
        }

        // Note is the raw text or cleaned context
        val note = rawText

        return ParsedTransactionResult(
            intent = intent,
            amount = amount,
            contactName = matchedContact,
            paymentMode = paymentMode,
            note = note,
            collectionDueDate = dueDateMillis,
            referenceNumber = refNumber,
            parsedSource = "LOCAL"
        )
    }

    /**
     * Gemini API Free Tier Structured Extraction (Q.4)
     */
    private fun parseWithGeminiApi(apiKey: String, text: String): ParsedTransactionResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are a strict financial transaction parser. Parse the user's spoken/typed text into JSON with keys:
            {
              "intent": "YOU_GAVE" or "YOU_GOT",
              "amount": number or null,
              "contactName": string or null,
              "paymentMode": "Cash" | "UPI" | "Bank Transfer" | "Cheque" | "Card" | "Other",
              "note": string,
              "dueDateDaysFromNow": number or null,
              "referenceNumber": string or null
            }
            Respond with valid JSON only. Do not wrap in markdown or backticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", "$systemInstruction\n\nInput text: $text")))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: return null

        if (!response.isSuccessful) return null

        val jsonObj = JSONObject(bodyStr)
        val candidates = jsonObj.optJSONArray("candidates")
        val firstCand = candidates?.optJSONObject(0)
        val contentObj = firstCand?.optJSONObject("content")
        val parts = contentObj?.optJSONArray("parts")
        val rawJsonText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: return null

        // Clean markdown backticks if present
        val cleanJson = rawJsonText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val resultJson = JSONObject(cleanJson)

        val intent = resultJson.optString("intent", Transaction.TYPE_YOU_GAVE)
        val amount = if (resultJson.has("amount") && !resultJson.isNull("amount")) resultJson.optDouble("amount") else null
        val contactName = if (resultJson.has("contactName") && !resultJson.isNull("contactName")) resultJson.optString("contactName") else null
        val paymentMode = resultJson.optString("paymentMode", "Cash")
        val note = resultJson.optString("note", text)
        val daysFromNow = if (resultJson.has("dueDateDaysFromNow") && !resultJson.isNull("dueDateDaysFromNow")) resultJson.optInt("dueDateDaysFromNow") else null
        val refNumber = if (resultJson.has("referenceNumber") && !resultJson.isNull("referenceNumber")) resultJson.optString("referenceNumber") else null

        var dueDateMillis: Long? = null
        if (daysFromNow != null && daysFromNow >= 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
            dueDateMillis = cal.timeInMillis
        }

        return ParsedTransactionResult(
            intent = if (intent == "YOU_GOT") Transaction.TYPE_YOU_GOT else Transaction.TYPE_YOU_GAVE,
            amount = amount,
            contactName = contactName,
            paymentMode = paymentMode,
            note = note,
            collectionDueDate = dueDateMillis,
            referenceNumber = refNumber,
            parsedSource = "GEMINI"
        )
    }

    private fun matchContactFuzzy(targetName: String, contacts: List<Contact>): String? {
        if (contacts.isEmpty()) return targetName
        var bestMatch: String? = null
        var maxScore = 0.0
        for (c in contacts) {
            val score = calculateSimilarity(targetName.lowercase(), c.name.lowercase())
            if (score > maxScore) {
                maxScore = score
                bestMatch = c.name
            }
        }
        return if (maxScore >= 0.75) bestMatch else targetName
    }

    /**
     * Levenshtein + Token overlap similarity calculation
     */
    fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        if (str2.contains(str1)) return 0.9

        val levDist = computeLevenshteinDistance(str1, str2)
        val maxLen = Math.max(str1.length, str2.length)
        val levSim = 1.0 - (levDist.toDouble() / maxLen.toDouble())

        return levSim.coerceIn(0.0, 1.0)
    }

    private fun computeLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = Math.min(
                    dp[i - 1][j] + 1,
                    Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
