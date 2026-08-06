package com.example.util

import android.content.Context
import com.example.data.model.ContactWithBalance
import com.example.data.model.TraceLog
import com.example.data.repository.SummaryTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateAiText(context: Context, prompt: String, systemPrompt: String = ""): String = withContext(Dispatchers.IO) {
        val provider = AiConfigManager.getProvider(context)
        try {
            when (provider) {
                AiProvider.GEMINI -> generateGeminiText(context, prompt, systemPrompt)
                AiProvider.OPENAI -> generateOpenAiText(context, prompt, systemPrompt)
                AiProvider.ANTHROPIC -> generateAnthropicText(context, prompt, systemPrompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "AI Service Error (${provider.displayName}): ${e.localizedMessage}"
        }
    }

    private fun generateGeminiText(context: Context, prompt: String, systemPrompt: String): String {
        val customKey = AiConfigManager.getCustomApiKey(context)
        val apiKey = if (customKey.isNotBlank()) customKey else AiConfigManager.getEffectiveGeminiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "Gemini API key is not configured. Please add your key in AI Engine Settings or AI Studio secrets."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            val fullPrompt = if (systemPrompt.isNotBlank()) "$systemPrompt\n\n$prompt" else prompt
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return "Gemini API Call failed (${response.code}): $bodyStr"
        }

        val jsonObj = JSONObject(bodyStr)
        val candidates = jsonObj.optJSONArray("candidates")
        val firstCand = candidates?.optJSONObject(0)
        val contentObj = firstCand?.optJSONObject("content")
        val parts = contentObj?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        return text ?: "No response content returned by Gemini."
    }

    private fun generateOpenAiText(context: Context, prompt: String, systemPrompt: String): String {
        val apiKey = AiConfigManager.getCustomApiKey(context)
        val endpoint = AiConfigManager.getCustomEndpoint(context)
        val model = AiConfigManager.getCustomModel(context)

        if (apiKey.isBlank()) {
            return "API key required for OpenAI compatible provider. Please update AI Engine Settings."
        }

        val messagesArr = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messagesArr.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        messagesArr.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val jsonBody = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o-mini" })
            put("messages", messagesArr)
        }

        val request = Request.Builder()
            .url(endpoint.ifBlank { "https://api.openai.com/v1/chat/completions" })
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return "OpenAI Endpoint error (${response.code}): $bodyStr"
        }

        val jsonObj = JSONObject(bodyStr)
        val choices = jsonObj.optJSONArray("choices")
        val msgObj = choices?.optJSONObject(0)?.optJSONObject("message")
        return msgObj?.optString("content") ?: "No choices returned by endpoint."
    }

    private fun generateAnthropicText(context: Context, prompt: String, systemPrompt: String): String {
        val apiKey = AiConfigManager.getCustomApiKey(context)
        val endpoint = AiConfigManager.getCustomEndpoint(context)
        val model = AiConfigManager.getCustomModel(context)

        if (apiKey.isBlank()) {
            return "API key required for Anthropic provider. Please update AI Engine Settings."
        }

        val jsonBody = JSONObject().apply {
            put("model", model.ifBlank { "claude-3-haiku-20240307" })
            put("max_tokens", 1024)
            if (systemPrompt.isNotBlank()) {
                put("system", systemPrompt)
            }
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val request = Request.Builder()
            .url(endpoint.ifBlank { "https://api.anthropic.com/v1/messages" })
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return "Anthropic API error (${response.code}): $bodyStr"
        }

        val jsonObj = JSONObject(bodyStr)
        val contentArr = jsonObj.optJSONArray("content")
        return contentArr?.optJSONObject(0)?.optString("text") ?: "No response text returned by Claude."
    }

    // 1. AI Feature: Financial Advisor & Cashflow Intelligence
    suspend fun generateFinancialAdvice(context: Context, contacts: List<ContactWithBalance>, summary: SummaryTotals): String {
        val sb = StringBuilder()
        sb.append("Current Ledger Summary:\n")
        sb.append("- Total Net Balance: ${summary.netBalance}\n")
        sb.append("- Total You'll Get (Credits): ${summary.totalGet}\n")
        sb.append("- Total You'll Pay (Debts): ${summary.totalPay}\n")
        sb.append("- Total Contacts: ${contacts.size}\n\n")
        sb.append("Top Outstanding Accounts:\n")
        contacts.take(5).forEach { c ->
            sb.append("- ${c.contact.name}: Net ${c.netBalance}\n")
        }

        val prompt = "Based on this Khata ledger summary, provide concise, actionable financial advice, cashflow health score, risk assessment, and 3 specific steps for debt collection or settlement:\n\n$sb"
        val sysPrompt = "You are KhataTrack AI, an expert business credit manager and financial ledger advisor for shopkeepers and small businesses."

        return generateAiText(context, prompt, sysPrompt)
    }

    // 2. AI Feature: Payment Reminder Generator
    suspend fun generatePaymentReminder(
        context: Context,
        contactName: String,
        amount: Double,
        tone: String
    ): String {
        val formattedAmount = CurrencyFormatter.formatRupee(Math.abs(amount))
        val prompt = "Generate a short $tone WhatsApp/SMS payment reminder message for customer '$contactName' who owes $formattedAmount. Include place for UPI link or Khata statement reference."
        val sysPrompt = "You are a polite and professional credit collection assistant. Keep messages clear, respectful, and ready for copy-pasting."

        return generateAiText(context, prompt, sysPrompt)
    }

    // 3. AI Feature: Smart Transaction Categorizer
    suspend fun categorizeTransaction(
        context: Context,
        note: String,
        amount: Double,
        type: String
    ): String {
        val prompt = "Categorize this ledger transaction ($type of $amount, note: '$note'). Return a JSON with categoryName, icon, and brief riskAssessment."
        val sysPrompt = "You are a transaction accounting classifier. Provide clear categorization."

        return generateAiText(context, prompt, sysPrompt)
    }

    // 4. AI Feature: Audit & Anomaly Detector
    suspend fun detectAuditAnomalies(
        context: Context,
        contacts: List<ContactWithBalance>,
        traceLogs: List<TraceLog>
    ): String {
        val prompt = "Scan these ${contacts.size} contacts and ${traceLogs.size} recent activity audit logs for anomalies (e.g. inactive credit accounts, uncollected debt over 60 days, rapid unpins/deletions). List top 3 audit insights in bullet points."
        val sysPrompt = "You are KhataTrack AI Auditor specialized in detecting credit risks, accounting errors, and compliance issues."

        return generateAiText(context, prompt, sysPrompt)
    }
}
