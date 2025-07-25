package com.neurokeycoder.keyboard.view.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.neurokeycoder.R

class KeyboardRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onKeyClickListener: ((String) -> Unit)? = null
    private var originalKeys = listOf<String>()
    private var isShiftPressed = false
    
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(2, 2, 2, 2)
        }
    }
    
    fun setKeys(keys: List<String>) {
        originalKeys = keys
        removeAllViews()
        
        keys.forEach { key ->
            val keyButton = createKeyButton(key)
            addView(keyButton)
        }
    }
    
    private fun createKeyButton(displayText: String, originalKey: String): Button {
        return Button(context).apply {
            this.text = displayText
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setBackgroundResource(R.drawable.key_background)
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.key_text_normal))
            minHeight = 0
            minimumHeight = dpToPx(48)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener {
                onKeyClickListener?.invoke(originalKey)
            }
        }
    }
    
    private fun createKeyButton(text: String): Button {
        return createKeyButton(text, text)
    }
    
    fun setOnKeyClickListener(listener: (String) -> Unit) {
        onKeyClickListener = listener
    }
    
    fun updateShift(isShiftPressed: Boolean) {
        this.isShiftPressed = isShiftPressed
        removeAllViews()
        
        originalKeys.forEach { key ->
            val displayText = if (isShiftPressed) getShiftedKey(key) else key
            val keyButton = createKeyButton(displayText, key)
            addView(keyButton)
        }
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
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
} 