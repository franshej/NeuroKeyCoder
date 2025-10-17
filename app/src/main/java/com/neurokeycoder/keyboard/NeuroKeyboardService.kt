package com.neurokeycoder.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.neurokeycoder.ai.GeminiService
import com.neurokeycoder.keyboard.view.NeuroKeyboardView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NeuroKeyboardService : InputMethodService() {
    
    private lateinit var keyboardView: NeuroKeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    private var isShiftPressed = false
    
    // LLM and context tracking
    private lateinit var geminiService: GeminiService
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var suggestionJob: Job? = null
    private var typedContext = StringBuilder()

    // Progress tracking for suggestions
    private val requestDurations = mutableListOf<Long>()
    private val maxHistorySize = 10

    // Cursor position tracking for selective LLM triggering
    private var lastCursorPosition = -1
    private var expectingCursorChange = false
    
    companion object {
        private const val TAG = "NeuroKeyboardService"
        private const val MAX_CONTEXT_LENGTH = 200
        private const val DEFAULT_ESTIMATION_MS = 1000L
    }
    
    override fun onCreate() {
        super.onCreate()
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        geminiService = GeminiService(this)
    }
    
    override fun onCreateInputView(): View {
        keyboardView = NeuroKeyboardView(this)
        keyboardView.setOnKeyListener { key ->
            handleKeyPress(key)
        }
        return keyboardView
    }
    
    private fun handleKeyPress(key: String) {
        
        when (key) {
            "BACKSPACE" -> {
                val ic = currentInputConnection
                expectingCursorChange = true
                ic?.deleteSurroundingText(1, 0)
                if (typedContext.isNotEmpty()) {
                    typedContext.deleteCharAt(typedContext.length - 1)
                }
            }
            "SPACE" -> {
                val ic = currentInputConnection
                expectingCursorChange = true
                ic?.commitText(" ", 1)
                typedContext.append(" ")
                requestSuggestions()
            }
            "ENTER" -> {
                val ic = currentInputConnection
                expectingCursorChange = true
                ic?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
                typedContext.append("\n")
                requestSuggestions()
            }
            "SHIFT" -> {
                toggleShift()
            }
            "?123" -> {
                keyboardView.switchToSymbolLayout()
            }
            "ABC" -> {
                keyboardView.switchToMainLayout()
            }
            else -> {
                if (isApplyingSuggestion(key)) {
                    applySuggestionWithSmartReplacement(key)
                    return
                }
                val ic = currentInputConnection
                val textToCommit = if (isShiftPressed) getShiftedKey(key) else key
                expectingCursorChange = true
                ic?.commitText(textToCommit, 1)

                typedContext.append(textToCommit)

                // Keep context within reasonable length
                if (typedContext.length > MAX_CONTEXT_LENGTH) {
                    typedContext.delete(0, typedContext.length - MAX_CONTEXT_LENGTH)
                }

                // Check if we should trigger API based on specific characters or word length
                val currentWordLength = getCurrentWordLength()
                val shouldTriggerForCharacter = textToCommit == "#"
                val shouldTriggerForWordLength = currentWordLength == 2 || currentWordLength == 5

                if (shouldTriggerForCharacter) {
                    requestSuggestions()
                } else if (shouldTriggerForWordLength) {
                    requestSuggestions()
                }
            }
        }
    }
    
    private fun toggleShift() {
        isShiftPressed = !isShiftPressed
        keyboardView.updateShift(isShiftPressed)
    }
    
    private fun getShiftedKey(key: String): String {
        return when {
            key.matches(Regex("[A-Z]")) -> key.lowercase()
            key.matches(Regex("[a-z]")) -> key.uppercase()
            else -> key
        }
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        
        // Reset context and cursor tracking when keyboard is launched
        typedContext.clear()
        lastCursorPosition = -1
        
        updateInputFieldContext()
        
        requestSuggestions()
    }
    
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        
        // Check if cursor position changed (not just selection)
        if (newSelStart != lastCursorPosition && newSelStart == newSelEnd) {
            if (expectingCursorChange) {
                // This cursor change was caused by our own input, ignore it
                expectingCursorChange = false
                lastCursorPosition = newSelStart
            } else {
                // This is an external cursor movement (user tapped/navigated), trigger LLM
                lastCursorPosition = newSelStart
                
                updateInputFieldContext()
                
                requestSuggestions()
            }
        } else {
            expectingCursorChange = false
        }
    }
    
    private fun updateInputFieldContext() {
        val ic = currentInputConnection ?: run {
            return
        }
        
        try {
            // Try to get text before cursor (up to MAX_CONTEXT_LENGTH characters)
            val textBeforeCursor = ic.getTextBeforeCursor(MAX_CONTEXT_LENGTH, 0)
            if (!textBeforeCursor.isNullOrEmpty()) {
                typedContext.clear()
                typedContext.append(textBeforeCursor)
            }
        } catch (e: Exception) {
            // Silently handle input field access errors
        }
    }
    
    /**
     * Requests LLM suggestions if no request is currently in progress.
     * Called for:
     * 1. Cursor movement (onUpdateSelection)
     * 2. Space key press
     * 3. Enter/newline key press
     * 4. Initial keyboard launch (onStartInputView)
     * 5. C++ keyword suggestion application (suggestions containing spaces)
     * 6. Word length triggers (2nd and 5th character in a word)
     * 7. Specific character triggers (# for preprocessor directives)
     * 
     * NOT called for other regular character input or regular suggestion application.
     */
    private fun requestSuggestions() {
        // Skip if a request is already in progress
        if (suggestionJob?.isActive == true) {
            Log.d(TAG, "Suggestion request already in progress, skipping")
            return
        }
        
        if (!geminiService.isConfigured()) {
            keyboardView.updateWordSuggestions(emptyList())
            return
        }
        
        suggestionJob = serviceScope.launch {
            val context = getCurrentContext()
            val estimatedDuration = getEstimatedDuration()
            
            keyboardView.showLoadingSuggestions(estimatedDuration)
            
            val requestStartTime = System.currentTimeMillis()
            
            try {
                val suggestions = geminiService.getCppSuggestions(context)
                
                val actualDuration = System.currentTimeMillis() - requestStartTime
                recordRequestDuration(actualDuration)
                
                // Update sentence suggestion first (will appear above word suggestions)
                keyboardView.updateSentenceSuggestion(suggestions.second)
                keyboardView.updateWordSuggestions(suggestions.first)
            } catch (e: CancellationException) {
                // Request was cancelled (e.g., keyboard service destroyed)
                // Don't update UI
                Log.d(TAG, "Suggestion request cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error in suggestion request", e)
                keyboardView.updateSentenceSuggestion("")
                keyboardView.updateWordSuggestions(emptyList())
            }
        }
    }
    
    private fun getEstimatedDuration(): Long {
        return if (requestDurations.isNotEmpty()) {
            requestDurations.average().toLong()
        } else {
            DEFAULT_ESTIMATION_MS
        }
    }
    
    private fun recordRequestDuration(duration: Long) {
        requestDurations.add(duration)
        
        // Keep only the most recent requests
        if (requestDurations.size > maxHistorySize) {
            requestDurations.removeAt(0)
        }
    }
    
    private fun getCurrentContext(): String {
        // First try to get context from input field
        val ic = currentInputConnection
        var context = ""
        
        if (ic != null) {
            try {
                val textBeforeCursor = ic.getTextBeforeCursor(MAX_CONTEXT_LENGTH, 0)
                if (!textBeforeCursor.isNullOrEmpty()) {
                    context = textBeforeCursor.toString()
                }
            } catch (e: Exception) {
                // Silently handle input field access errors
            }
        }
        
        // If we couldn't get input field context, use typed context
        if (context.isEmpty()) {
            context = typedContext.toString()
        }
        
        return context
    }
    
    private fun isApplyingSuggestion(key: String): Boolean {
        val specialKeywords = setOf(
            "BACKSPACE", "SPACE", "ENTER", "SHIFT", "?123", "ABC", 
            "GLOBE", "SEARCH", ",", "."
        )
        
        if (key in specialKeywords) {
            return false
        }
        
        // A suggestion is being applied if the key is longer than 1 character
        // and contains letters (not just symbols like "{", "}", etc.)
        // This covers cases like "cout", "return", "std::vector", etc.
        return key.length > 1 && key.any { it.isLetter() }
    }
    
    private fun applySuggestionWithSmartReplacement(suggestion: String) {
        val ic = currentInputConnection ?: return
        
        expectingCursorChange = true // Flag that we're about to change cursor position
        
        try {
            val currentWord = getCurrentPartialWord()
            
            if (currentWord.isNotEmpty() && suggestion.startsWith(currentWord, ignoreCase = true)) {
                // Smart replacement: delete the partial word and insert the full suggestion
                val charsToDelete = currentWord.length
                
                ic.deleteSurroundingText(charsToDelete, 0)
                ic.commitText(suggestion, 1)
                
                // Update typed context by removing the partial word and adding the suggestion
                if (typedContext.endsWith(currentWord)) {
                    typedContext.delete(typedContext.length - currentWord.length, typedContext.length)
                }
                typedContext.append(suggestion)
                
            } else {
                ic.commitText(suggestion, 1)
                typedContext.append(suggestion)
            }
            
            // Keep context within reasonable length
            if (typedContext.length > MAX_CONTEXT_LENGTH) {
                typedContext.delete(0, typedContext.length - MAX_CONTEXT_LENGTH)
            }
            
            // Check if this suggestion contains a space (indicating C++ keyword button)
            if (suggestion.contains(" ")) {
                requestSuggestions()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying suggestion with smart replacement", e)
            // Fallback to regular insertion
            expectingCursorChange = true
            ic.commitText(suggestion, 1)
            typedContext.append(suggestion)
            
            // Check if this suggestion contains a space (indicating C++ keyword button)
            if (suggestion.contains(" ")) {
                requestSuggestions()
            }
        }
    }
    
    private fun getCurrentPartialWord(): String {
        val ic = currentInputConnection ?: return ""
        
        try {
            val textBeforeCursor = ic.getTextBeforeCursor(50, 0) ?: return ""
            
            // Find the last word (sequence of letters/numbers/underscores)
            val wordPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*$")
            val match = wordPattern.find(textBeforeCursor)
            val currentWord = match?.value ?: ""
            
            return currentWord
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current partial word", e)
            return ""
        }
    }
    
    private fun getCurrentWordLength(): Int {
        val ic = currentInputConnection ?: return 0
        
        try {
            val textBeforeCursor = ic.getTextBeforeCursor(50, 0) ?: return 0
            
            // Find the last word (sequence of letters/numbers/underscores)
            val wordPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*$")
            val match = wordPattern.find(textBeforeCursor)
            val currentWordLength = match?.value?.length ?: 0
            
            return currentWordLength
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current word length", e)
            return 0
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        suggestionJob?.cancel()
    }
} 