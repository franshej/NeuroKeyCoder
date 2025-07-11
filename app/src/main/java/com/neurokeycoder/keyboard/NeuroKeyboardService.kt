package com.neurokeycoder.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.neurokeycoder.keyboard.view.NeuroKeyboardView

class NeuroKeyboardService : InputMethodService() {
    
    private lateinit var keyboardView: NeuroKeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    private var isShiftPressed = false
    
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
            else -> getShiftedSymbol(key)
        }
    }
    
    private fun getShiftedSymbol(symbol: String): String {
        return when (symbol) {
            "1" -> "!"
            "2" -> "@"
            "3" -> "#"
            "4" -> "$"
            "5" -> "%"
            "6" -> "^"
            "7" -> "&"
            "8" -> "*"
            "9" -> "("
            "0" -> ")"
            "+" -> "="
            "-" -> "_"
            "*" -> "×"
            "/" -> "÷"
            "=" -> "+"
            "(" -> ")"
            ")" -> "("
            ";" -> ":"
            "," -> "<"
            "{" -> "["
            "}" -> "]"
            "[" -> "{"
            "]" -> "}"
            "<" -> "≤"
            ">" -> "≥"
            "&" -> "&&"
            else -> symbol
        }
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Update keyboard layout based on input type if needed
    }
} 