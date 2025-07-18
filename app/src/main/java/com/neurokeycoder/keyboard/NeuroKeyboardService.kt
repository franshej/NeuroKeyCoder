package com.neurokeycoder.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.neurokeycoder.ai.GeminiService
import com.neurokeycoder.keyboard.view.NeuroKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NeuroKeyboardService : InputMethodService() {
    
    private lateinit var keyboardView: NeuroKeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    private var isShiftPressed = false
    
    // LLM and context tracking
    private lateinit var geminiService: GeminiService
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var suggestionJob: Job? = null
    private var typedContext = StringBuilder() // Track text typed from keyboard
    
    companion object {
        private const val TAG = "NeuroKeyboardService"
        private const val CONTEXT_DEBOUNCE_DELAY = 1000L // 1 second delay before fetching suggestions
        private const val MAX_CONTEXT_LENGTH = 200 // Maximum characters to consider for context
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NeuroKeyboardService created")
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        geminiService = GeminiService(this)
    }
    
    override fun onCreateInputView(): View {
        keyboardView = NeuroKeyboardView(this)
        keyboardView.setOnKeyListener { key ->
            handleKeyPress(key)
            true
        }
        return keyboardView
    }
    
    private fun handleKeyPress(key: String) {
        Log.d(TAG, "Key pressed: '$key'")
        when (key) {
            "BACKSPACE" -> {
                val ic = currentInputConnection
                ic?.deleteSurroundingText(1, 0)
                // Remove last character from typed context
                if (typedContext.isNotEmpty()) {
                    typedContext.deleteCharAt(typedContext.length - 1)
                    Log.d(TAG, "Updated typed context after backspace: '${typedContext}'")
                }
                requestSuggestionsWithDelay()
            }
            "SPACE" -> {
                val ic = currentInputConnection
                ic?.commitText(" ", 1)
                typedContext.append(" ")
                Log.d(TAG, "Updated typed context after space: '${typedContext}'")
                requestSuggestionsWithDelay()
            }
            "ENTER" -> {
                val ic = currentInputConnection
                ic?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
                typedContext.append("\n")
                Log.d(TAG, "Updated typed context after enter: '${typedContext}'")
                requestSuggestionsWithDelay()
            }
            "SHIFT" -> {
                toggleShift()
            }
            "?123" -> {
                // Switch to symbol layout
                keyboardView.switchToSymbolLayout()
            }
            "ABC" -> {
                // Switch back to main layout
                keyboardView.switchToMainLayout()
            }
            "GLOBE" -> {
                // TODO: Switch input language
                // For now, just output a placeholder
                val ic = currentInputConnection
                ic?.commitText("🌐", 1)
            }
            "SEARCH" -> {
                val ic = currentInputConnection
                ic?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
            }
            "," -> {
                val ic = currentInputConnection
                ic?.commitText(",", 1)
            }
            "." -> {
                val ic = currentInputConnection
                ic?.commitText(".", 1)
            }
            else -> {
                val ic = currentInputConnection
                val textToCommit = if (isShiftPressed) getShiftedKey(key) else key
                ic?.commitText(textToCommit, 1)
                
                // Add to typed context
                typedContext.append(textToCommit)
                Log.d(TAG, "Added '$textToCommit' to typed context: '${typedContext}'")
                
                // Keep context within reasonable length
                if (typedContext.length > MAX_CONTEXT_LENGTH) {
                    typedContext.delete(0, typedContext.length - MAX_CONTEXT_LENGTH)
                    Log.d(TAG, "Trimmed typed context to max length: '${typedContext}'")
                }
                
                requestSuggestionsWithDelay()
            }
        }
    }
    
    private fun toggleShift() {
        isShiftPressed = !isShiftPressed
        keyboardView.updateShift(isShiftPressed)
    }
    
    private fun getShiftedKey(key: String): String {
        // For letters, toggle case. For symbols, get alternative symbols.
        return when {
            key.matches(Regex("[A-Z]")) -> key.lowercase()
            key.matches(Regex("[a-z]")) -> key.uppercase()
            else -> key
        }
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "Keyboard started - restarting: $restarting")
        
        // Reset context when keyboard is launched
        typedContext.clear()
        Log.d(TAG, "Cleared typed context on keyboard start")
        
        // Get initial context from input field if available
        getInputFieldContext()
        
        // Request initial suggestions
        requestSuggestionsWithDelay()
    }
    
    private fun getInputFieldContext() {
        val ic = currentInputConnection ?: run {
            Log.d(TAG, "No input connection available")
            return
        }
        
        try {
            // Try to get text before cursor (up to MAX_CONTEXT_LENGTH characters)
            val textBeforeCursor = ic.getTextBeforeCursor(MAX_CONTEXT_LENGTH, 0)
            if (!textBeforeCursor.isNullOrEmpty()) {
                typedContext.clear()
                typedContext.append(textBeforeCursor)
                Log.d(TAG, "Got initial context from input field: '${typedContext}'")
            } else {
                Log.d(TAG, "No text found before cursor in input field")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing input field context", e)
        }
    }
    
    private fun requestSuggestionsWithDelay() {
        Log.d(TAG, "Requesting suggestions with delay")
        
        // Cancel any pending suggestion request
        suggestionJob?.cancel()
        
        if (!geminiService.isConfigured()) {
            Log.w(TAG, "Gemini service not configured, showing empty suggestions")
            keyboardView.updateSuggestions(emptyList())
            return
        }
        
        suggestionJob = serviceScope.launch {
            Log.d(TAG, "Starting suggestion request job")
            
            // Show loading indicator
            keyboardView.showLoadingSuggestions()
            
            // Wait for debounce delay
            Log.d(TAG, "Waiting ${CONTEXT_DEBOUNCE_DELAY}ms for debounce")
            delay(CONTEXT_DEBOUNCE_DELAY)
            
            // Get current context
            val context = getCurrentContext()
            Log.d(TAG, "Final context for suggestions: '$context'")
            
            try {
                // Get suggestions from Gemini
                val suggestions = geminiService.getCppSuggestions(context)
                Log.d(TAG, "Received suggestions from service: $suggestions")
                keyboardView.updateSuggestions(suggestions)
            } catch (e: Exception) {
                Log.e(TAG, "Error in suggestion request", e)
                // Fallback to empty suggestions on error
                keyboardView.updateSuggestions(emptyList())
            }
        }
    }
    
    private fun getCurrentContext(): String {
        Log.d(TAG, "Getting current context")
        
        // First try to get context from input field
        val ic = currentInputConnection
        var context = ""
        
        if (ic != null) {
            try {
                val textBeforeCursor = ic.getTextBeforeCursor(MAX_CONTEXT_LENGTH, 0)
                if (!textBeforeCursor.isNullOrEmpty()) {
                    context = textBeforeCursor.toString()
                    Log.d(TAG, "Got context from input field: '$context'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting context from input field", e)
            }
        }
        
        // If we couldn't get input field context, use typed context
        if (context.isEmpty()) {
            context = typedContext.toString()
            Log.d(TAG, "Using typed context: '$context'")
        }
        
        Log.d(TAG, "Final context (length ${context.length}): '$context'")
        return context
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NeuroKeyboardService destroyed")
        suggestionJob?.cancel()
    }
} 