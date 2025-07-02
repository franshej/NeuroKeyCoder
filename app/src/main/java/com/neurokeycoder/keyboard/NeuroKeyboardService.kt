package com.neurokeycoder.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.neurokeycoder.keyboard.view.NeuroKeyboardView

class NeuroKeyboardService : InputMethodService() {
    
    private lateinit var keyboardView: NeuroKeyboardView
    private lateinit var inputMethodManager: InputMethodManager
    
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
            else -> {
                val ic = currentInputConnection
                ic?.commitText(key, 1)
            }
        }
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Update keyboard layout based on input type if needed
    }
} 