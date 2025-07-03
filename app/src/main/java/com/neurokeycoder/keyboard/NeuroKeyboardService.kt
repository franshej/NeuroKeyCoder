package com.neurokeycoder.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.neurokeycoder.keyboard.view.NeuroKeyboardView

class NeuroKeyboardService : InputMethodService() {
    
    private lateinit var keyboardView: NeuroKeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    private var isUpperCase = false
    
    override fun onCreate() {
        super.onCreate()
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
        when (key) {
            "BACKSPACE" -> {
                val ic = currentInputConnection
                ic?.deleteSurroundingText(1, 0)
            }
            "SPACE" -> {
                val ic = currentInputConnection
                ic?.commitText(" ", 1)
            }
            "ENTER" -> {
                val ic = currentInputConnection
                ic?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            }
            "SHIFT" -> {
                toggleCase()
            }
            "?123" -> {
                // TODO: Switch to number/symbol keyboard layout
                // For now, just output a placeholder
                val ic = currentInputConnection
                ic?.commitText("123", 1)
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
                val textToCommit = if (isUpperCase) key else key.lowercase()
                ic?.commitText(textToCommit, 1)
            }
        }
    }
    
    private fun toggleCase() {
        isUpperCase = !isUpperCase
        keyboardView.updateCase(isUpperCase)
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Update keyboard layout based on input type if needed
    }
} 