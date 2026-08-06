package com.example.util

import com.example.data.model.CategoryItem

object CategoryTagExtractor {

    private val KEYWORD_MAP = mapOf(
        "kirana" to "Groceries",
        "grocery" to "Groceries",
        "groceries" to "Groceries",
        "milk" to "Groceries",
        "vegetable" to "Groceries",
        "vegetables" to "Groceries",
        "fruit" to "Groceries",
        "food" to "Groceries",
        "dinner" to "Groceries",
        "lunch" to "Groceries",
        "tea" to "Groceries",
        "coffee" to "Groceries",
        "rent" to "Housing",
        "flat" to "Housing",
        "room" to "Housing",
        "bill" to "Bills",
        "bills" to "Bills",
        "electricity" to "Bills",
        "wifi" to "Bills",
        "water" to "Bills",
        "recharge" to "Bills",
        "phone" to "Bills",
        "salary" to "Salary",
        "wage" to "Salary",
        "fuel" to "Travel",
        "petrol" to "Travel",
        "diesel" to "Travel",
        "cab" to "Travel",
        "auto" to "Travel",
        "taxi" to "Travel",
        "travel" to "Travel",
        "medical" to "Health",
        "doctor" to "Health",
        "medicine" to "Health",
        "pharma" to "Health"
    )

    fun extractCategoryTag(note: String?, categories: List<CategoryItem> = emptyList()): String {
        if (note.isNullOrBlank()) return "General"

        // 1. Check for explicit hashtag in note (e.g., #Groceries)
        val hashtagRegex = Regex("#([A-Za-z0-9_]+)")
        val match = hashtagRegex.find(note)
        if (match != null) {
            val tag = match.groupValues[1]
            return tag.replaceFirstChar { it.uppercase() }
        }

        // 2. Keyword matching against user categories if available
        val lowerNote = note.lowercase()
        for (cat in categories) {
            if (lowerNote.contains(cat.name.lowercase())) {
                return cat.name
            }
        }

        // 3. Built-in keyword mapping
        for ((kw, tag) in KEYWORD_MAP) {
            if (lowerNote.contains(kw)) {
                return tag
            }
        }

        return "General"
    }
}
