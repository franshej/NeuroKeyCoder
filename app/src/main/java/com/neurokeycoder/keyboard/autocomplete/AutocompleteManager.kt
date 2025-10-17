package com.neurokeycoder.keyboard.autocomplete

import android.util.Log
import com.neurokeycoder.keyboard.utils.SimilarityCalculator

class AutocompleteManager {

    private val wordDictionary = mutableSetOf<String>()
    private var aiWordSuggestions = mutableListOf<String>()
    private var aiSentenceSuggestion: String = ""

    companion object {
        private const val TAG = "AutocompleteManager"
        private const val WORD_SIMILARITY_THRESHOLD = 0.5f
        private const val SENTENCE_SIMILARITY_THRESHOLD = 0.4f
    }

    init {
        loadDefaultDictionary()
    }

    private fun loadDefaultDictionary() {
        // Common programming keywords
        wordDictionary.addAll(listOf(
            // Fundamental types
            "int", "double", "char", "bool", "void", "auto",

            // Control flow
            "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",

            // Type modifiers and specifiers
            "const", "constexpr", "static", "unsigned", "signed", "virtual",

            // Class and struct related
            "class", "struct", "public", "private", "protected", "this", "new", "delete", "friend",

            // Scoping and templates
            "namespace", "using", "template", "typename",

            // Literals
            "true", "false", "nullptr"
        ))
    }

    /**
     * Updates the AI suggestions (both words and sentence).
     */
    fun updateAiSuggestions(wordSuggestions: List<String>, sentenceSuggestion: String) {
        aiWordSuggestions.clear()
        aiWordSuggestions.addAll(wordSuggestions)
        aiSentenceSuggestion = sentenceSuggestion

        Log.d(TAG, "Updated AI suggestions - Words: $wordSuggestions, Sentence: '$sentenceSuggestion'")
    }

    /**
     * Determines if a suggestion should be treated as a sentence or word.
     */
    private fun isSentence(text: String): Boolean {
        return text.contains(' ') || text.contains(';') || text.contains('\n') || text.contains('<') || text.contains('>')
    }

    /**
     * Finds the best matching suggestion for the current user input.
     * Always returns a suggestion, even if no fuzzy match is found.
     */
    fun findBestMatchingSuggestion(userTyped: String, clickedSuggestion: String): AutocompleteSuggestion {
        Log.d(TAG, "Finding match - User typed: '$userTyped', Clicked: '$clickedSuggestion'")

        // Check if the clicked suggestion is the AI sentence suggestion
        if (clickedSuggestion == aiSentenceSuggestion && aiSentenceSuggestion.isNotEmpty()) {
            // For sentence suggestions, extract the typing context for comparison
            val typingContext = SimilarityCalculator.extractTypingContext(userTyped)
            val suggestionContext = SimilarityCalculator.extractTypingContext(clickedSuggestion)

            val similarity = SimilarityCalculator.calculateSimilarity(typingContext, suggestionContext)

            Log.d(TAG, "Sentence match - Similarity: $similarity")

            if (similarity >= SENTENCE_SIMILARITY_THRESHOLD) {
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = clickedSuggestion,
                    similarity = similarity,
                    isAiSuggestion = true,
                    isSentence = true
                )
            }
        }

        // Check if it's one of the AI word suggestions
        if (clickedSuggestion in aiWordSuggestions) {
            val similarity = SimilarityCalculator.calculateSimilarity(userTyped, clickedSuggestion)

            Log.d(TAG, "AI word match - Similarity: $similarity")

            if (similarity >= WORD_SIMILARITY_THRESHOLD) {
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = clickedSuggestion,
                    similarity = similarity,
                    isAiSuggestion = true,
                    isSentence = false
                )
            }
        }

        // Check dictionary words
        if (clickedSuggestion in wordDictionary) {
            val similarity = SimilarityCalculator.calculateSimilarity(userTyped, clickedSuggestion)

            Log.d(TAG, "Dictionary word match - Similarity: $similarity")

            if (similarity >= WORD_SIMILARITY_THRESHOLD) {
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = clickedSuggestion,
                    similarity = similarity,
                    isAiSuggestion = false,
                    isSentence = false
                )
            }
        }

        // Default: treat as word suggestion if no special pattern detected
        return AutocompleteSuggestion(
            original = userTyped,
            corrected = clickedSuggestion,
            similarity = 1.0f,
            isAiSuggestion = clickedSuggestion in aiWordSuggestions || clickedSuggestion == aiSentenceSuggestion,
            isSentence = isSentence(clickedSuggestion)
        )
    }

    /**
     * Applies a suggestion with smart replacement logic.
     * Returns a pair of (new text, new cursor position).
     */
    fun applySuggestion(
        currentText: String,
        cursorPosition: Int,
        suggestion: AutocompleteSuggestion
    ): Pair<String, Int> {
        return if (suggestion.isSentence) {
            // Replace entire current line
            replaceCurrentLine(currentText, cursorPosition, suggestion.corrected)
        } else {
            // Replace only the current word
            replaceCurrentWord(currentText, cursorPosition, suggestion.corrected)
        }
    }

    /**
     * Replaces the current word under the cursor with the suggestion.
     */
    private fun replaceCurrentWord(text: String, cursorPosition: Int, replacement: String): Pair<String, Int> {
        val beforeCursor = text.substring(0, cursorPosition)
        val afterCursor = text.substring(cursorPosition)

        // Define word boundary characters
        val wordBoundaries = charArrayOf(' ', '\n', '\t', '(', ')', '{', '}', '[', ']', ';', ',', '.', ':', '<', '>')

        // Find word start (before cursor)
        val wordStart = beforeCursor.lastIndexOfAny(wordBoundaries) + 1

        // Find word end (after cursor)
        val wordEndOffset = afterCursor.indexOfAny(wordBoundaries)
        val wordEnd = if (wordEndOffset == -1) afterCursor.length else wordEndOffset

        val before = text.substring(0, wordStart)
        val after = text.substring(cursorPosition + wordEnd)
        val newText = before + replacement + after
        val newCursorPosition = wordStart + replacement.length

        Log.d(TAG, "Word replacement - Before: '$before', Replace: '${text.substring(wordStart, cursorPosition + wordEnd)}', After: '$after'")
        Log.d(TAG, "New text: '$newText', New cursor: $newCursorPosition")

        return Pair(newText, newCursorPosition)
    }

    /**
     * Replaces the current line with the suggestion.
     */
    private fun replaceCurrentLine(text: String, cursorPosition: Int, replacement: String): Pair<String, Int> {
        val beforeCursor = text.substring(0, cursorPosition)
        val afterCursor = text.substring(cursorPosition)

        // Find line boundaries
        val lineStart = beforeCursor.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
        val lineEndOffset = afterCursor.indexOf('\n')
        val lineEnd = if (lineEndOffset == -1) text.length else cursorPosition + lineEndOffset

        val before = text.substring(0, lineStart)
        val after = text.substring(lineEnd)
        val newText = before + replacement + after
        val newCursorPosition = lineStart + replacement.length

        Log.d(TAG, "Line replacement - Before: '$before', Replace: '${text.substring(lineStart, lineEnd)}', After: '$after'")
        Log.d(TAG, "New text: '$newText', New cursor: $newCursorPosition")

        return Pair(newText, newCursorPosition)
    }
}
