package com.example.util

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiProvider(val displayName: String) {
    GEMINI("Built-in Gemini (Free)"),
    OPENAI("OpenAI Compatible"),
    NVIDIA_NIM("Nvidia NIM"),
    ANTHROPIC("Anthropic (Claude)"),
    CUSTOM("Custom OpenAI-Compatible")
}

/**
 * Per-provider AI configuration storage.
 * Each provider maintains its own independent set of: API key, endpoint URL, and model ID.
 * Switching providers NEVER overwrites another provider's stored config.
 */
object AiConfigManager {
    private const val PREFS_NAME = "khata_ai_config"
    private const val KEY_PROVIDER = "ai_provider"
    private const val KEY_USE_AI_PARSING = "use_ai_parsing"

    // Per-provider key prefixes — each provider gets its own independent storage
    private fun keyForProvider(provider: AiProvider, suffix: String) = "${provider.name}_$suffix"

    // Official default base URLs per provider (base, NOT the /chat/completions path)
    private val defaultBaseUrls = mapOf(
        AiProvider.GEMINI to "https://generativelanguage.googleapis.com/v1beta",
        AiProvider.OPENAI to "https://api.openai.com/v1",
        AiProvider.NVIDIA_NIM to "https://integrate.api.nvidia.com/v1",
        AiProvider.ANTHROPIC to "https://api.anthropic.com/v1",
        AiProvider.CUSTOM to ""
    )

    // Default models per provider
    private val defaultModels = mapOf(
        AiProvider.GEMINI to "gemini-2.0-flash",
        AiProvider.OPENAI to "gpt-4o-mini",
        AiProvider.NVIDIA_NIM to "nvidia/nemotron-3-ultra-550b-a55b",
        AiProvider.ANTHROPIC to "claude-3-haiku-20240307",
        AiProvider.CUSTOM to ""
    )

    // ─── OkHttp client ────────────────────────────────────────────────────────
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // ─── Provider selection ────────────────────────────────────────────────────
    fun getProvider(context: Context): AiProvider {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
        return try { AiProvider.valueOf(name) } catch (e: Exception) { AiProvider.GEMINI }
    }

    fun setProvider(context: Context, provider: AiProvider) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    // ─── AI Parsing toggle ─────────────────────────────────────────────────────
    fun getUseAiParsing(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_AI_PARSING, false)
    }

    fun setUseAiParsing(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_AI_PARSING, enabled).apply()
    }

    // ─── Per-provider getters ──────────────────────────────────────────────────

    fun getApiKey(context: Context, provider: AiProvider): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(keyForProvider(provider, "api_key"), "") ?: ""
    }

    fun getBaseUrl(context: Context, provider: AiProvider): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(keyForProvider(provider, "base_url"), null)
        return saved ?: defaultBaseUrls[provider] ?: ""
    }

    fun getModelId(context: Context, provider: AiProvider): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(keyForProvider(provider, "model_id"), null)
        return saved ?: defaultModels[provider] ?: ""
    }

    // ─── Per-provider save ────────────────────────────────────────────────────
    fun saveProviderConfig(context: Context, provider: AiProvider, apiKey: String, baseUrl: String, modelId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(keyForProvider(provider, "api_key"), apiKey.trim())
            .putString(keyForProvider(provider, "base_url"), baseUrl.trim())
            .putString(keyForProvider(provider, "model_id"), modelId.trim())
            .apply()
    }

    // ─── Convenience accessors for active provider ────────────────────────────
    fun getCustomApiKey(context: Context): String = getApiKey(context, getProvider(context))
    fun getCustomEndpoint(context: Context): String = getBaseUrl(context, getProvider(context))
    fun getCustomModel(context: Context): String = getModelId(context, getProvider(context))

    // Legacy compat — saves to the currently active provider
    fun saveCustomConfig(context: Context, endpoint: String, apiKey: String, model: String) {
        saveProviderConfig(context, getProvider(context), apiKey, endpoint, model)
    }

    // ─── Gemini built-in key ──────────────────────────────────────────────────
    fun getEffectiveGeminiKey(): String {
        return try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
    }

    // ─── Build correct chat completions URL from base URL ─────────────────────
    fun buildChatUrl(provider: AiProvider, baseUrl: String): String {
        val base = baseUrl.trimEnd('/')
        return when (provider) {
            AiProvider.GEMINI -> base // Gemini uses a different path structure
            AiProvider.ANTHROPIC -> if (base.endsWith("/messages")) base else "$base/messages"
            else -> if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        }
    }

    // ─── Model list fetching ───────────────────────────────────────────────────
    suspend fun fetchAvailableModels(
        provider: AiProvider,
        baseUrl: String,
        apiKey: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    val keyToUse = if (apiKey.isNotBlank()) apiKey else getEffectiveGeminiKey()
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$keyToUse"
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val models = JSONObject(bodyString).optJSONArray("models")
                        val list = mutableListOf<String>()
                        if (models != null) {
                            for (i in 0 until models.length()) {
                                val name = models.getJSONObject(i).optString("name").removePrefix("models/")
                                if (name.contains("gemini")) list.add(name)
                            }
                        }
                        Result.success(list.sorted())
                    } else {
                        Result.failure(Exception("Gemini HTTP ${response.code}"))
                    }
                }
                AiProvider.ANTHROPIC -> {
                    // Anthropic doesn't expose a models list endpoint — return curated list
                    Result.success(listOf(
                        "claude-opus-4-5",
                        "claude-sonnet-4-5",
                        "claude-haiku-4-5",
                        "claude-3-5-sonnet-20241022",
                        "claude-3-haiku-20240307",
                        "claude-3-opus-20240229"
                    ))
                }
                AiProvider.NVIDIA_NIM -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key required"))
                    val base = baseUrl.ifBlank { "https://integrate.api.nvidia.com/v1" }.trimEnd('/')
                    val url = "$base/models"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val dataArr = JSONObject(body).optJSONArray("data")
                        val list = mutableListOf<String>()
                        if (dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                list.add(dataArr.getJSONObject(i).optString("id"))
                            }
                        }
                        if (list.isEmpty()) {
                            // Fallback to known NIM models
                            Result.success(listOf(
                                "nvidia/nemotron-3-ultra-550b-a55b",
                                "nvidia/nemotron-3-super-49b-instruct",
                                "meta/llama-3.1-8b-instruct",
                                "meta/llama-3.1-70b-instruct",
                                "meta/llama-3.1-405b-instruct",
                                "mistralai/mistral-7b-instruct",
                                "microsoft/phi-3-mini-128k-instruct"
                            ))
                        } else {
                            Result.success(list.sorted())
                        }
                    } else {
                        // Return known model list as fallback on API error
                        Result.success(listOf(
                            "nvidia/nemotron-3-ultra-550b-a55b",
                            "nvidia/nemotron-3-super-49b-instruct",
                            "meta/llama-3.1-8b-instruct",
                            "meta/llama-3.1-70b-instruct",
                            "meta/llama-3.1-405b-instruct"
                        ))
                    }
                }
                AiProvider.OPENAI, AiProvider.CUSTOM -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key required"))
                    val base = baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
                    val modelsUrl = "$base/models"
                    val request = Request.Builder()
                        .url(modelsUrl)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val dataArr = JSONObject(body).optJSONArray("data")
                        val list = mutableListOf<String>()
                        if (dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                list.add(dataArr.getJSONObject(i).optString("id"))
                            }
                        }
                        Result.success(list.sorted())
                    } else {
                        Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()?.take(200)}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Connection validation ─────────────────────────────────────────────────
    suspend fun validateConnection(
        provider: AiProvider,
        baseUrl: String,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    val keyToUse = if (apiKey.isNotBlank()) apiKey else getEffectiveGeminiKey()
                    if (keyToUse.isBlank()) return@withContext Result.failure(Exception("No API key — add yours or use the built-in"))
                    val modelId = model.ifBlank { "gemini-2.0-flash" }
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$keyToUse"
                    val jsonBody = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", "Reply with the single word: OK")))
                        }))
                    }
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("✅ Gemini connection successful! Model: $modelId")
                    } else {
                        val errBody = response.body?.string()?.take(300) ?: ""
                        Result.failure(Exception("Gemini HTTP ${response.code}: $errBody"))
                    }
                }

                AiProvider.NVIDIA_NIM -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("NVIDIA API key required (get it at build.nvidia.com)"))
                    // NVIDIA NIM: base URL is https://integrate.api.nvidia.com/v1
                    // Chat endpoint: POST /v1/chat/completions
                    val base = baseUrl.ifBlank { "https://integrate.api.nvidia.com/v1" }.trimEnd('/')
                    val chatUrl = buildChatUrl(provider, base)
                    val modelId = model.ifBlank { "nvidia/nemotron-3-ultra-550b-a55b" }

                    val jsonBody = JSONObject().apply {
                        put("model", modelId)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply with the single word: OK")
                        }))
                        put("max_tokens", 5)
                        put("temperature", 0.1)
                        put("stream", false)
                    }
                    val request = Request.Builder()
                        .url(chatUrl)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("✅ NVIDIA NIM connected! Model: $modelId")
                    } else {
                        val errBody = response.body?.string()?.take(400) ?: ""
                        Result.failure(Exception("NIM HTTP ${response.code}: $errBody"))
                    }
                }

                AiProvider.OPENAI, AiProvider.CUSTOM -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("API key required"))
                    val base = baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
                    val chatUrl = buildChatUrl(provider, base)
                    val modelId = model.ifBlank { "gpt-4o-mini" }

                    val jsonBody = JSONObject().apply {
                        put("model", modelId)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply with the single word: OK")
                        }))
                        put("max_tokens", 5)
                    }
                    val request = Request.Builder()
                        .url(chatUrl)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("✅ ${provider.displayName} connected! Model: $modelId")
                    } else {
                        val errBody = response.body?.string()?.take(400) ?: ""
                        Result.failure(Exception("HTTP ${response.code}: $errBody"))
                    }
                }

                AiProvider.ANTHROPIC -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("Anthropic API key required"))
                    val base = baseUrl.ifBlank { "https://api.anthropic.com/v1" }.trimEnd('/')
                    val chatUrl = buildChatUrl(provider, base)
                    val modelId = model.ifBlank { "claude-3-haiku-20240307" }

                    val jsonBody = JSONObject().apply {
                        put("model", modelId)
                        put("max_tokens", 10)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply with the single word: OK")
                        }))
                    }
                    val request = Request.Builder()
                        .url(chatUrl)
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("✅ Anthropic Claude connected! Model: $modelId")
                    } else {
                        val errBody = response.body?.string()?.take(400) ?: ""
                        Result.failure(Exception("Anthropic HTTP ${response.code}: $errBody"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
