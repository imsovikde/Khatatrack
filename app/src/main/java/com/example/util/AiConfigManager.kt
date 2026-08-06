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
    GEMINI("Built-in Gemini 3.5 Flash (Free)"),
    OPENAI("OpenAI Compatible (ChatGPT, Nvidia, etc)"),
    ANTHROPIC("Anthropic Compatible (Claude)")
}

object AiConfigManager {
    private const val PREFS_NAME = "khata_ai_config"
    private const val KEY_PROVIDER = "ai_provider"
    private const val KEY_CUSTOM_KEY = "custom_api_key"
    private const val KEY_CUSTOM_ENDPOINT = "custom_endpoint"
    private const val KEY_CUSTOM_MODEL = "custom_model"
    private const val KEY_USE_AI_PARSING = "use_ai_parsing"

    fun getUseAiParsing(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_AI_PARSING, false)
    }

    fun setUseAiParsing(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_AI_PARSING, enabled).apply()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getProvider(context: Context): AiProvider {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
        return try {
            AiProvider.valueOf(name)
        } catch (e: Exception) {
            AiProvider.GEMINI
        }
    }

    fun setProvider(context: Context, provider: AiProvider) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun getCustomApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_KEY, "") ?: ""
    }

    fun getCustomEndpoint(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultUrl = "https://api.openai.com/v1/chat/completions"
        return prefs.getString(KEY_CUSTOM_ENDPOINT, defaultUrl) ?: defaultUrl
    }

    fun getCustomModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
    }

    fun saveCustomConfig(context: Context, endpoint: String, apiKey: String, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CUSTOM_ENDPOINT, endpoint.trim())
            .putString(KEY_CUSTOM_KEY, apiKey.trim())
            .putString(KEY_CUSTOM_MODEL, model.trim())
            .apply()
    }

    fun getEffectiveGeminiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun validateConnection(
        provider: AiProvider,
        endpoint: String,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    val keyToUse = if (apiKey.isNotBlank()) apiKey else getEffectiveGeminiKey()
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$keyToUse"
                    val jsonBody = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", "Say Hello")))
                        }))
                    }
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("Gemini API connection successful!")
                    } else {
                        Result.failure(Exception("Gemini HTTP Error ${response.code}: ${response.message}"))
                    }
                }
                AiProvider.OPENAI -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key cannot be empty"))
                    val url = if (endpoint.isBlank()) "https://api.openai.com/v1/chat/completions" else endpoint
                    val jsonBody = JSONObject().apply {
                        put("model", if (model.isNotBlank()) model else "gpt-4o-mini")
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Test connection")
                        }))
                    }
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("OpenAI compatible endpoint connection successful!")
                    } else {
                        Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()}"))
                    }
                }
                AiProvider.ANTHROPIC -> {
                    if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key cannot be empty"))
                    val url = if (endpoint.isBlank()) "https://api.anthropic.com/v1/messages" else endpoint
                    val jsonBody = JSONObject().apply {
                        put("model", if (model.isNotBlank()) model else "claude-3-haiku-20240307")
                        put("max_tokens", 10)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Test connection")
                        }))
                    }
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        Result.success("Anthropic connection successful!")
                    } else {
                        Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
