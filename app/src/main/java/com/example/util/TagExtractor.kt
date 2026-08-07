package com.example.util

/**
 * TagExtractor: Extracts searchable tags from free-form text.
 *
 * Supports two modes:
 * 1. Explicit #hashtags: "#grocery #family" → ["grocery", "family"]
 * 2. Comma-separated: "grocery, family, upi" → ["grocery", "family", "upi"]
 *
 * Tags are lowercased, trimmed, and stripped of special chars.
 * The resulting tags are stored as a comma-separated String in the DB.
 */
object TagExtractor {

    /**
     * Extract tags from text. Prioritizes explicit #hashtags, then falls back to comma-separated.
     */
    fun extractTags(text: String): List<String> {
        val hashTags = Regex("""#(\w+)""").findAll(text)
            .map { it.groupValues[1].lowercase().trim() }
            .filter { it.isNotBlank() }
            .toList()

        if (hashTags.isNotEmpty()) return hashTags.distinct()

        // Fall back: comma-separated values (only if no # found)
        val commaTags = text.split(",")
            .map { it.trim().lowercase().replace(Regex("[^a-z0-9_]"), "") }
            .filter { it.isNotBlank() && it.length <= 30 }

        return commaTags.distinct()
    }

    /**
     * Convert list of tags to DB-storable comma-separated string.
     */
    fun tagsToString(tags: List<String>): String = tags.joinToString(",")

    /**
     * Parse DB tags string back to list.
     */
    fun parseTags(tagsString: String): List<String> =
        tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }

    /**
     * Merge existing tags with new ones from a note update.
     */
    fun mergeTagsWithNote(existingTags: String, note: String): String {
        val existing = parseTags(existingTags).toMutableSet()
        existing += extractTags(note)
        return tagsToString(existing.toList())
    }

    /**
     * Check if a query matches any of the stored tags.
     */
    fun matchesTag(tagsString: String, query: String): Boolean {
        val lowerQuery = query.lowercase().trimStart('#')
        return parseTags(tagsString).any { it.contains(lowerQuery) }
    }
}
