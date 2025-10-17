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

            Log.d(TAG, "Sentence match - Similarity: $similarity (threshold: $SENTENCE_SIMILARITY_THRESHOLD)")

            // Check if we should concatenate instead of replace
            // This happens when the suggestion seems to be a continuation of what the user typed
            val shouldConcatenate = checkIfShouldConcatenate(userTyped, clickedSuggestion)

            if (similarity >= SENTENCE_SIMILARITY_THRESHOLD || shouldConcatenate) {
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = if (shouldConcatenate) userTyped + clickedSuggestion else clickedSuggestion,
                    similarity = similarity,
                    isAiSuggestion = true,
                    isSentence = true,
                    shouldConcatenate = shouldConcatenate
                )
            }
        }

        // Check if it's one of the AI word suggestions
        if (clickedSuggestion in aiWordSuggestions) {
            val similarity = SimilarityCalculator.calculateSimilarity(userTyped, clickedSuggestion)

            Log.d(TAG, "AI word match - Similarity: $similarity (threshold: $WORD_SIMILARITY_THRESHOLD)")

            // Check if we should concatenate for word suggestions too
            val shouldConcatenate = checkIfShouldConcatenate(userTyped, clickedSuggestion)

            if (similarity >= WORD_SIMILARITY_THRESHOLD) {
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = clickedSuggestion,
                    similarity = similarity,
                    isAiSuggestion = true,
                    isSentence = false,
                    shouldConcatenate = false
                )
            } else if (shouldConcatenate) {
                // Low similarity but should concatenate (e.g., "proc" + "ess(...)")
                return AutocompleteSuggestion(
                    original = userTyped,
                    corrected = userTyped + clickedSuggestion,
                    similarity = 0.5f, // Moderate score for concatenation
                    isAiSuggestion = true,
                    isSentence = false,
                    shouldConcatenate = true
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
                    isSentence = false,
                    shouldConcatenate = false
                )
            }
        }

        // Default: Check if concatenation makes sense for any AI suggestion
        val isAiSugg = clickedSuggestion in aiWordSuggestions || clickedSuggestion == aiSentenceSuggestion
        val shouldConcat = if (isAiSugg) checkIfShouldConcatenate(userTyped, clickedSuggestion) else false

        return AutocompleteSuggestion(
            original = userTyped,
            corrected = if (shouldConcat) userTyped + clickedSuggestion else clickedSuggestion,
            similarity = if (shouldConcat) 0.5f else 1.0f,
            isAiSuggestion = isAiSugg,
            isSentence = isSentence(clickedSuggestion),
            shouldConcatenate = shouldConcat
        )
    }

    /**
     * Determines if the user's typed text and the suggestion should be concatenated.
     * This is true when:
     * 1. The suggestion doesn't start with the typed text (low similarity)
     * 2. But the concatenation would form a meaningful word/phrase
     *
     * Example: "proc" + "ess(3.14);" -> "process(3.14);"
     */
    private fun checkIfShouldConcatenate(userTyped: String, suggestion: String): Boolean {
        if (userTyped.isEmpty() || suggestion.isEmpty()) return false

        // If suggestion already contains the typed text, don't concatenate
        if (suggestion.lowercase().contains(userTyped.lowercase())) {
            return false
        }

        // If typed text is very short (1-2 chars), don't concatenate unless it's meaningful
        if (userTyped.length <= 2 && !suggestion.startsWith("(") && !suggestion.startsWith("[")) {
            return false
        }

        // Check if concatenation would form a recognizable pattern
        val concatenated = userTyped + suggestion

        // Pattern 1: function call pattern (word + parentheses)
        // e.g., "proc" + "ess(3.14)" = "process(3.14)"
        if (suggestion.matches(Regex("^[a-z]+\\(.*"))) {
            return true
        }

        // Pattern 2: completion of a word followed by operators/punctuation
        // e.g., "std" + "::vector" = "std::vector"
        if (suggestion.matches(Regex("^::[a-zA-Z].*"))) {
            return true
        }

        // Pattern 3: array access pattern
        // e.g., "arr" + "[0]" = "arr[0]"
        if (suggestion.matches(Regex("^\\[.*\\].*"))) {
            return true
        }

        // Pattern 4: member access pattern
        // e.g., "obj" + ".method()" = "obj.method()"
        if (suggestion.matches(Regex("^\\.[a-zA-Z].*"))) {
            return true
        }

        // Pattern 5: Check if the first word of suggestion could complete the typed text
        // e.g., "proc" + "ess" where suggestion starts with letters that could continue the word
        val suggestionFirstPart = suggestion.takeWhile { it.isLetterOrDigit() }
        if (suggestionFirstPart.isNotEmpty() && userTyped.all { it.isLetterOrDigit() }) {
            // Check if concatenating makes a word that exists in common patterns
            val potentialWord = userTyped + suggestionFirstPart
            if (potentialWord.length >= 4) { // Minimum meaningful word length
                return true
            }
        }

        return false
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
