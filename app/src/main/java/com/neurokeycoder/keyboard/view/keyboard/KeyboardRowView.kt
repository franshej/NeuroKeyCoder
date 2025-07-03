package com.neurokeycoder.keyboard.view.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import android.view.Gravity

class KeyboardRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onKeyClickListener: ((String) -> Unit)? = null
    private var originalKeys = listOf<String>()
    private var originalNumbers = listOf<String>()
    private var isUpperCase = false
    private var hasNumbers = false
    
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
        hasNumbers = false
        removeAllViews()
        
        keys.forEach { key ->
            val keyButton = createKeyButton(key)
            addView(keyButton)
        }
    }
    
    fun setKeysWithNumbers(letters: List<String>, numbers: List<String>) {
        originalKeys = letters
        originalNumbers = numbers
        hasNumbers = true
        removeAllViews()
        
        letters.forEachIndexed { index, letter ->
            val keyButton = createKeyButtonWithNumber(letter, numbers.getOrNull(index) ?: "")
            addView(keyButton)
        }
    }
    
    private fun createKeyButton(text: String): Button {
        return Button(context).apply {
            this.text = text
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(1, 1, 1, 1)
            }
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                onKeyClickListener?.invoke(text)
            }
        }
    }
    
    private fun createKeyButtonWithNumber(letter: String, number: String): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(1, 1, 1, 1)
            }
        }
        
        // Number text (small, top)
        val numberText = TextView(context).apply {
            this.text = number
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.GRAY)
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        
        // Letter button
        val letterButton = Button(context).apply {
            this.text = letter
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                onKeyClickListener?.invoke(letter)
            }
        }
        
        container.addView(numberText)
        container.addView(letterButton)
        
        return container
    }
    
    fun setOnKeyClickListener(listener: (String) -> Unit) {
        onKeyClickListener = listener
    }
    
    fun updateCase(isUpperCase: Boolean) {
        this.isUpperCase = isUpperCase
        removeAllViews()
        
        if (hasNumbers) {
            originalKeys.forEachIndexed { index, key ->
                val displayText = if (isUpperCase) key else key.lowercase()
                val keyButton = createKeyButtonWithNumber(displayText, originalNumbers.getOrNull(index) ?: "")
                addView(keyButton)
            }
        } else {
            originalKeys.forEach { key ->
                val displayText = if (isUpperCase) key else key.lowercase()
                val keyButton = createKeyButton(displayText)
                addView(keyButton)
            }
        }
    }
} 