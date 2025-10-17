package com.neurokeycoder.keyboard.autocomplete

data class AutocompleteSuggestion(
    val original: String,          // The user's typed text
    val corrected: String,          // The suggested correction/completion
    val similarity: Float,          // Similarity score (0.0 to 1.0)
    val isAiSuggestion: Boolean = false,
    val isSentence: Boolean = false, // True if it's a full line/sentence suggestion
    val shouldConcatenate: Boolean = false // True if should concatenate instead of replace
)
