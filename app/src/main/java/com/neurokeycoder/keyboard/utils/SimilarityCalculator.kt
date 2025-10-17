package com.neurokeycoder.keyboard.utils

object SimilarityCalculator {

    /**
     * Calculates the Levenshtein distance between two strings.
     * This measures the minimum number of single-character edits needed to change one word into another.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1].lowercaseChar() == s2[j - 1].lowercaseChar()) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }

    /**
     * Calculates similarity score between 0.0 and 1.0.
     * 1.0 means identical, 0.0 means completely different.
     */
    fun calculateSimilarity(input: String, suggestion: String): Float {
        if (input.isEmpty() || suggestion.isEmpty()) return 0f

        // Normalize strings for comparison (case-insensitive)
        val normalizedInput = input.lowercase()
        val normalizedSuggestion = suggestion.lowercase()

        // If suggestion starts with input, boost similarity
        if (normalizedSuggestion.startsWith(normalizedInput)) {
            return 0.95f
        }

        val distance = levenshteinDistance(normalizedInput, normalizedSuggestion)
        val maxLength = maxOf(input.length, suggestion.length)
        return 1f - (distance.toFloat() / maxLength)
    }

    /**
     * Finds the best matching suggestion from a list based on similarity score.
     */
    fun findBestMatch(input: String, suggestions: List<String>, threshold: Float = 0.6f): String? {
        return suggestions
            .map { it to calculateSimilarity(input, it) }
            .filter { it.second >= threshold }
            .maxByOrNull { it.second }
            ?.first
    }

    /**
     * Extracts the typing portion from a partial input that should be matched.
     * For example, from "#inclued \iostr", extract relevant parts for matching.
     */
    fun extractTypingContext(input: String): String {
        // Remove common programming symbols that don't affect word matching
        return input.replace(Regex("[\\\\/<>]"), "")
    }
}

