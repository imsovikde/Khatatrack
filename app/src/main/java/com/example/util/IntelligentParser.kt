package com.example.util

import android.content.Context
import com.example.data.model.Contact
import com.example.data.model.IncomeExpenseEntry
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

data class ParsedIncomeExpenseResult(
    val type: String = IncomeExpenseEntry.TYPE_EXPENSE, // INCOME or EXPENSE
    val amount: Double? = null,
    val categoryTag: String = "General",
    val paymentMode: String = "Cash",
    val note: String? = null,
    val parsedSource: String = "LOCAL"
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
        val provider = AiConfigManager.getProvider(context)
        val apiKey = AiConfigManager.getCustomApiKey(context).ifBlank { AiConfigManager.getEffectiveGeminiKey() }
        val customEndpoint = AiConfigManager.getCustomEndpoint(context)
        val customModel = AiConfigManager.getCustomModel(context)

        if (isAiParsingEnabled && apiKey.isNotBlank()) {
            try {
                val aiResult = withTimeout(4000L) {
                    if (provider == AiProvider.GEMINI || (customEndpoint.isBlank() && provider == AiProvider.GEMINI)) {
                        parseWithGeminiApi(apiKey, trimmed)
                    } else {
                        parseWithOpenAiCompatibleApi(provider, customEndpoint.ifBlank { null }, apiKey, customModel.ifBlank { null }, trimmed)
                    }
                }
                if (aiResult != null) {
                    val matchedContactName = aiResult.contactName?.let { cName ->
                        matchContactFuzzy(cName, activeContacts) ?: cName
                    }
                    return@withContext aiResult.copy(
                        contactName = matchedContactName,
                        parsedSource = provider.displayName
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
        val gaveKeywords = listOf(
            "gave", "paid", "lent", "spent", "sent", "transfer to", "diya", "deya",
            "purchased", "purchase", "bought", "buy", "ordered", "order", "shopping",
            "debited", "deducted", "withdrawn", "withdraw", "emi", "bill", "invoice",
            "kharch", "kharcha", "de diya", "fee", "payment"
        )
        val gotKeywords = listOf(
            "got", "received", "took", "borrowed", "collected", "liya", "leya",
            "earned", "earn", "credited", "salary", "income", "revenue", "bonus",
            "cashback", "refund", "dividend", "profit", "gain", "interest",
            "payment received", "mila", "mili", "aaya", "aai"
        )

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
     * Parse voice/typed text for personal income/expense (not ledger transactions).
     * Non-suspend version for direct UI thread use (local only).
     */
    fun parseForIncomeExpense(rawText: String): ParsedIncomeExpenseResult {
        val lower = rawText.lowercase()

        // Determine type — explicit keyword-based detection
        val expenseKeywords = listOf(
            "spent", "paid", "bought", "purchased", "purchase", "ordered", "shopping",
            "debited", "bill", "fee", "emi", "expense", "kharch", "kharcha"
        )
        val incomeKeywords = listOf(
            "received", "earned", "got", "credited", "salary", "income", "revenue",
            "bonus", "cashback", "refund", "dividend", "mila", "aaya"
        )
        val isExpense = expenseKeywords.any { lower.contains(it) }
        val isIncome = incomeKeywords.any { lower.contains(it) }
        // Income keyword wins over expense if both detected (avoid "got salary" → EXPENSE)
        val type = when {
            isIncome && !isExpense -> IncomeExpenseEntry.TYPE_INCOME
            isIncome && isExpense -> IncomeExpenseEntry.TYPE_INCOME // income words take priority
            else -> IncomeExpenseEntry.TYPE_EXPENSE
        }

        // Amount
        val amountRegex = Regex("(?:₹|rs\\.?|rupees?)?\\s*(\\d+(?:\\.\\d{1,2})?|\\d+k)", RegexOption.IGNORE_CASE)
        var amount: Double? = null
        amountRegex.find(rawText)?.let {
            val s = it.groupValues[1].lowercase()
            amount = if (s.endsWith("k")) s.dropLast(1).toDoubleOrNull()?.times(1000) else s.toDoubleOrNull()
        }

        // Category from keywords
        val category = when {
            lower.contains("food") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") || lower.contains("restaurant") -> "Food"
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("taxi") || lower.contains("auto") || lower.contains("travel") || lower.contains("uber") || lower.contains("ola") -> "Transport"
            lower.contains("salary") || lower.contains("bonus") || lower.contains("earning") || lower.contains("paycheck") -> "Salary"
            lower.contains("shopping") || lower.contains("cloth") || lower.contains("shoe") || lower.contains("amazon") || lower.contains("flipkart") -> "Shopping"
            lower.contains("medical") || lower.contains("doctor") || lower.contains("medicine") || lower.contains("hospital") || lower.contains("pharmacy") -> "Health"
            lower.contains("rent") || lower.contains("electricity") || lower.contains("water") || lower.contains("internet") -> "Bills"
            lower.contains("emi") || lower.contains("loan") -> "Loan/EMI"
            lower.contains("refund") || lower.contains("cashback") -> "Refund"
            lower.contains("invest") || lower.contains("mutual fund") || lower.contains("stock") || lower.contains("sip") -> "Investment"
            lower.contains("freelance") || lower.contains("client") || lower.contains("project") -> "Freelance"
            else -> if (type == IncomeExpenseEntry.TYPE_INCOME) "Income" else "General"
        }

        // Payment mode
        val paymentMode = when {
            lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") -> "UPI"
            lower.contains("card") || lower.contains("debit") || lower.contains("credit") -> "Card"
            lower.contains("bank") || lower.contains("neft") || lower.contains("imps") -> "Bank Transfer"
            lower.contains("cheque") || lower.contains("check") -> "Cheque"
            else -> "Cash"
        }

        return ParsedIncomeExpenseResult(
            type = type,
            amount = amount,
            categoryTag = category,
            paymentMode = paymentMode,
            note = rawText,
            parsedSource = "LOCAL"
        )
    }

    /**
     * Gemini API Structured Extraction
     */
    private fun parseWithGeminiApi(apiKey: String, text: String): ParsedTransactionResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

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

        return parseTransactionJson(rawJsonText, text)
    }

    /**
     * OpenAI-compatible API call (works for OpenAI, Nvidia NIM, local LLMs, Anthropic-compatible)
     */
    private fun parseWithOpenAiCompatibleApi(
        provider: AiProvider,
        customEndpoint: String?,
        apiKey: String,
        modelOverride: String?,
        text: String
    ): ParsedTransactionResult? {
        val endpoint = when {
            !customEndpoint.isNullOrBlank() -> customEndpoint
            provider == AiProvider.NVIDIA_NIM -> "https://integrate.api.nvidia.com/v1/chat/completions"
            provider == AiProvider.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
            else -> "https://api.openai.com/v1/chat/completions"
        }
        val model = when {
            !modelOverride.isNullOrBlank() -> modelOverride
            provider == AiProvider.NVIDIA_NIM -> "nvidia/llama-3.1-nemotron-70b-instruct"
            provider == AiProvider.ANTHROPIC -> "claude-3-haiku-20240307"
            else -> "gpt-4o-mini"
        }

        val systemPrompt = """
            You are a strict financial transaction parser. Parse the user's spoken/typed text into JSON with keys:
            {"intent":"YOU_GAVE" or "YOU_GOT","amount":number or null,"contactName":string or null,"paymentMode":"Cash"|"UPI"|"Bank Transfer"|"Cheque"|"Card"|"Other","note":string,"dueDateDaysFromNow":number or null,"referenceNumber":string or null}
            Respond with valid JSON only.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", text) })
            })
            put("temperature", 0.1)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) return null

        val bodyStr = response.body?.string() ?: return null
        val jsonObj = JSONObject(bodyStr)
        val choices = jsonObj.optJSONArray("choices") ?: return null
        val messageContent = choices.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim() ?: return null

        return parseTransactionJson(messageContent, text)
    }

    private fun parseTransactionJson(rawJsonText: String, fallbackText: String): ParsedTransactionResult? {
        return try {
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
            val note = resultJson.optString("note", fallbackText)
            val daysFromNow = if (resultJson.has("dueDateDaysFromNow") && !resultJson.isNull("dueDateDaysFromNow")) resultJson.optInt("dueDateDaysFromNow") else null
            val refNumber = if (resultJson.has("referenceNumber") && !resultJson.isNull("referenceNumber")) resultJson.optString("referenceNumber") else null

            var dueDateMillis: Long? = null
            if (daysFromNow != null && daysFromNow >= 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
                dueDateMillis = cal.timeInMillis
            }

            ParsedTransactionResult(
                intent = if (intent == "YOU_GOT") Transaction.TYPE_YOU_GOT else Transaction.TYPE_YOU_GAVE,
                amount = amount,
                contactName = contactName,
                paymentMode = paymentMode,
                note = note,
                collectionDueDate = dueDateMillis,
                referenceNumber = refNumber,
                parsedSource = "AI"
            )
        } catch (e: Exception) {
            null
        }
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
