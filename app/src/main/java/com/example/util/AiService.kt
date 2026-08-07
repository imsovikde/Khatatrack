package com.example.util

import android.content.Context
import com.example.data.model.ContactWithBalance
import com.example.data.model.TraceLog
import com.example.data.repository.SummaryTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * AiService — unified AI text generation for all configured providers.
 * Routes to Gemini, OpenAI, NVIDIA NIM, Anthropic, or Custom based on saved per-provider config.
 * Each provider reads its own independent keys/endpoints — no shared state overwriting.
 */
object AiService {

    suspend fun generateAiText(
        context: Context,
        prompt: String,
        systemPrompt: String = ""
    ): String = withContext(Dispatchers.IO) {
        val provider = AiConfigManager.getProvider(context)
        try {
            when (provider) {
                AiProvider.GEMINI -> generateGeminiText(context, prompt, systemPrompt)
                AiProvider.ANTHROPIC -> generateAnthropicText(context, prompt, systemPrompt)
                // NVIDIA NIM, OpenAI, CUSTOM all use the OpenAI-compatible chat completions API
                AiProvider.NVIDIA_NIM, AiProvider.OPENAI, AiProvider.CUSTOM ->
                    generateOpenAiCompatibleText(context, provider, prompt, systemPrompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "AI Error (${provider.displayName}): ${e.localizedMessage}"
        }
    }

    // ─── Gemini ────────────────────────────────────────────────────────────────
    private fun generateGeminiText(context: Context, prompt: String, systemPrompt: String): String {
        val apiKey = AiConfigManager.getApiKey(context, AiProvider.GEMINI)
            .ifBlank { AiConfigManager.getEffectiveGeminiKey() }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "Gemini API key not configured. Add it in AI Hub → Settings tab."
        }

        val modelId = AiConfigManager.getModelId(context, AiProvider.GEMINI).ifBlank { "gemini-2.0-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val fullPrompt = if (systemPrompt.isNotBlank()) "$systemPrompt\n\n$prompt" else prompt
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = AiConfigManager.httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) return "Gemini error (${response.code}): ${bodyStr.take(300)}"

        val jsonObj = JSONObject(bodyStr)
        return jsonObj.optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
            ?: "No response from Gemini."
    }

    // ─── OpenAI-Compatible (NVIDIA NIM, OpenAI, Custom) ───────────────────────
    private fun generateOpenAiCompatibleText(
        context: Context,
        provider: AiProvider,
        prompt: String,
        systemPrompt: String
    ): String {
        val apiKey = AiConfigManager.getApiKey(context, provider)
        if (apiKey.isBlank()) {
            return "API key required for ${provider.displayName}. Configure it in AI Hub → Settings."
        }

        val baseUrl = AiConfigManager.getBaseUrl(context, provider).ifBlank {
            when (provider) {
                AiProvider.NVIDIA_NIM -> "https://integrate.api.nvidia.com/v1"
                AiProvider.OPENAI -> "https://api.openai.com/v1"
                else -> "https://api.openai.com/v1"
            }
        }
        val chatUrl = AiConfigManager.buildChatUrl(provider, baseUrl)
        val modelId = AiConfigManager.getModelId(context, provider).ifBlank {
            when (provider) {
                AiProvider.NVIDIA_NIM -> "nvidia/nemotron-3-ultra-550b-a55b"
                else -> "gpt-4o-mini"
            }
        }

        val messagesArr = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
            }
            put(JSONObject().apply { put("role", "user"); put("content", prompt) })
        }

        val jsonBody = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArr)
            put("temperature", 0.7)
            put("max_tokens", 1500)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(chatUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = AiConfigManager.httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) return "${provider.displayName} error (${response.code}): ${bodyStr.take(400)}"

        val jsonObj = JSONObject(bodyStr)
        return jsonObj.optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            ?: "No response from ${provider.displayName}."
    }

    // ─── Anthropic Claude ─────────────────────────────────────────────────────
    private fun generateAnthropicText(context: Context, prompt: String, systemPrompt: String): String {
        val apiKey = AiConfigManager.getApiKey(context, AiProvider.ANTHROPIC)
        if (apiKey.isBlank()) {
            return "Anthropic API key required. Configure it in AI Hub → Settings."
        }

        val baseUrl = AiConfigManager.getBaseUrl(context, AiProvider.ANTHROPIC).ifBlank { "https://api.anthropic.com/v1" }
        val chatUrl = AiConfigManager.buildChatUrl(AiProvider.ANTHROPIC, baseUrl)
        val modelId = AiConfigManager.getModelId(context, AiProvider.ANTHROPIC).ifBlank { "claude-3-haiku-20240307" }

        val jsonBody = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 1500)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val request = Request.Builder()
            .url(chatUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = AiConfigManager.httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) return "Anthropic error (${response.code}): ${bodyStr.take(400)}"

        val jsonObj = JSONObject(bodyStr)
        return jsonObj.optJSONArray("content")?.optJSONObject(0)?.optString("text")
            ?: "No response from Claude."
    }

    // ─── AI Feature Functions ─────────────────────────────────────────────────

    suspend fun generateFinancialAdvice(context: Context, contacts: List<ContactWithBalance>, summary: SummaryTotals): String {
        val sb = StringBuilder()
        sb.append("Ledger Summary:\n")
        sb.append("- Net Balance: ${summary.netBalance}\n")
        sb.append("- Total You'll Get: ${summary.totalGet}\n")
        sb.append("- Total You'll Pay: ${summary.totalPay}\n")
        sb.append("- Total Contacts: ${contacts.size}\n\nTop Accounts:\n")
        contacts.take(5).forEach { sb.append("- ${it.contact.name}: Net ${it.netBalance}\n") }

        val prompt = "Based on this Khata ledger, provide actionable financial advice, cashflow health score, risk assessment, and 3 debt collection steps:\n\n$sb"
        val sys = "You are KhataTrack AI, an expert business credit manager and ledger advisor for shopkeepers and small businesses. Be concise, specific, and professional."
        return generateAiText(context, prompt, sys)
    }

    suspend fun generatePaymentReminder(context: Context, contactName: String, amount: Double, tone: String): String {
        val prompt = "Generate a short $tone WhatsApp/SMS payment reminder for '$contactName' who owes ${CurrencyFormatter.formatRupee(Math.abs(amount))}. Include a placeholder for UPI link."
        val sys = "You are a polite, professional payment collection assistant. Keep messages clear and copy-paste ready."
        return generateAiText(context, prompt, sys)
    }

    suspend fun categorizeTransaction(context: Context, note: String, amount: Double, type: String): String {
        val prompt = "Categorize this ledger transaction: type=$type, amount=$amount, note='$note'. Return JSON: {\"categoryName\":\"...\",\"icon\":\"...\",\"riskLevel\":\"Low/Medium/High\"}"
        val sys = "You are a transaction accounting classifier. Return valid JSON only."
        return generateAiText(context, prompt, sys)
    }

    suspend fun detectAuditAnomalies(context: Context, contacts: List<ContactWithBalance>, traceLogs: List<TraceLog>): String {
        val prompt = "Audit ${contacts.size} contacts and ${traceLogs.size} recent logs. Flag: inactive credits >60 days, rapid edits, suspicious deletions, uncollected debts. List top 3 bullet points."
        val sys = "You are KhataTrack AI Auditor. Detect credit risks, accounting anomalies, and compliance issues. Be factual and specific."
        return generateAiText(context, prompt, sys)
    }

    /**
     * Generates smart searchable tags for a list of transaction notes.
     * Returns a JSON map: { "noteText": ["tag1","tag2"] }
     */
    suspend fun generateBatchTags(context: Context, notes: List<String>): String {
        if (notes.isEmpty()) return "{}"
        val noteList = notes.take(30).joinToString("\n") { "- $it" }
        val prompt = """
            For each transaction note below, generate 1-3 short searchable tags (lowercase, no spaces, max 15 chars each).
            Return ONLY valid JSON as: {"note text": ["tag1","tag2"], ...}
            Notes:
            $noteList
        """.trimIndent()
        val sys = "You are a financial transaction tagger. Output valid JSON only. Be concise."
        return generateAiText(context, prompt, sys)
    }
}
