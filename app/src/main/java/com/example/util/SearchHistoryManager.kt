package com.example.util

import android.content.Context
import android.content.SharedPreferences

object SearchHistoryManager {
    private const val PREFS_NAME = "search_history_prefs"
    private const val HISTORY_KEY = "history_list"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addSearchQuery(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val prefs = getPrefs(context)
        val currentHistory = prefs.getString(HISTORY_KEY, "")?.split("||")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        
        currentHistory.remove(q)
        currentHistory.add(0, q)
        if (currentHistory.size > 10) {
            currentHistory.removeLast()
        }
        prefs.edit().putString(HISTORY_KEY, currentHistory.joinToString("||")).apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val prefs = getPrefs(context)
        return prefs.getString(HISTORY_KEY, "")?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun clearSearchHistory(context: Context) {
        getPrefs(context).edit().remove(HISTORY_KEY).apply()
    }
}
