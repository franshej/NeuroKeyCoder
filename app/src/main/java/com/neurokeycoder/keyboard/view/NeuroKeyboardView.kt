package com.neurokeycoder.keyboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.neurokeycoder.R
import com.neurokeycoder.keyboard.view.keyboard.KeyboardRow
import com.neurokeycoder.keyboard.view.keyboard.KeyboardRowView

class NeuroKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onKeyListener: ((String) -> Unit)? = null
    private val letterRowViews = mutableListOf<KeyboardRowView>()
    private var isUpperCase = false
    
    init {
        orientation = VERTICAL
        setupKeyboard()
    }
    
    private fun setupKeyboard() {
        // First row: Q W E R T Y U I O P (with numbers above)
        addRowWithNumbers(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        )
        
        // Second row: A S D F G H J K L
        addRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"))
        
        // Third row: SHIFT + Z X C V B N M + BACKSPACE
        addThirdRow()
        
        // Fourth row: Special keys
        addSpecialRow()
    }
    
    private fun addRowWithNumbers(letters: List<String>, numbers: List<String>) {
        val rowView = KeyboardRowView(context).apply {
            setKeysWithNumbers(letters, numbers)
            setOnKeyClickListener { key ->
                onKeyListener?.invoke(key)
            }
        }
        letterRowViews.add(rowView)
        addView(rowView)
    }
    
    private fun addRow(keys: List<String>) {
        val rowView = KeyboardRowView(context).apply {
            setKeys(keys)
            setOnKeyClickListener { key ->
                onKeyListener?.invoke(key)
            }
        }
        letterRowViews.add(rowView)
        addView(rowView)
    }
    
    private fun addThirdRow() {
        val thirdRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }
        
        // Shift key - weight 1.0 (medium width)
        val shiftKey = createSpecialKeyButton("SHIFT", "⇧")
        shiftKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2.0f)
        thirdRow.addView(shiftKey)
        
        // Letter keys: Z X C V B N M - each weight 1.0 (standard width)
        val letterKeys = listOf("Z", "X", "C", "V", "B", "N", "M")
        letterKeys.forEach { key ->
            val keyButton = createKeyButton(key)
            keyButton.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            thirdRow.addView(keyButton)
        }
        
        // Backspace key - weight 1.0 (medium width)
        val backspaceKey = createSpecialKeyButton("BACKSPACE", "⌫")
        backspaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2.0f)
        thirdRow.addView(backspaceKey)
        
        addView(thirdRow)
    }
    
    private fun addSpecialRow() {
        val specialRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }
        
        // ?123 key - weight 0.5 (small)
        val numberKey = createSpecialKeyButton("?123", "?123")
        numberKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        specialRow.addView(numberKey)
        
        // Comma key - weight 0.5 (small)
        val commaKey = createKeyButton(",")
        commaKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(commaKey)
        
        // Space key - weight 4.0 (very wide)
        val spaceKey = createSpaceButton()
        spaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 3.0f)
        specialRow.addView(spaceKey)
        
        // Period key - weight 0.5 (small)
        val periodKey = createKeyButton(".")
        periodKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(periodKey)
        
        addView(specialRow)
    }
    
    private fun createKeyButton(text: String): android.widget.Button {
        return android.widget.Button(context).apply {
            this.text = text
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(1, 1, 1, 1)
            }
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                onKeyListener?.invoke(text)
            }
        }
    }
    
    private fun createSpaceButton(): android.widget.Button {
        return android.widget.Button(context).apply {
            this.text = "SPACE"
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                3f
            ).apply {
                setMargins(1, 1, 1, 1)
            }
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                onKeyListener?.invoke("SPACE")
            }
        }
    }
    
    private fun createSpecialKeyButton(action: String, displayText: String): android.widget.Button {
        return android.widget.Button(context).apply {
            this.text = displayText
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(1, 1, 1, 1)
            }
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                onKeyListener?.invoke(action)
            }
        }
    }
    
    fun setOnKeyListener(listener: (String) -> Unit) {
        onKeyListener = listener
    }
    
    fun updateCase(isUpperCase: Boolean) {
        this.isUpperCase = isUpperCase
        letterRowViews.forEach { rowView ->
            rowView.updateCase(isUpperCase)
        }
    }
} 