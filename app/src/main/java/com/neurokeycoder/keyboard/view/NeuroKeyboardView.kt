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
    
    init {
        orientation = VERTICAL
        setupKeyboard()
    }
    
    private fun setupKeyboard() {
        // First row: Q W E R T Y U I O P
        addRow(listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"))
        
        // Second row: A S D F G H J K L
        addRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"))
        
        // Third row: Z X C V B N M
        addRow(listOf("Z", "X", "C", "V", "B", "N", "M"))
        
        // Fourth row: Special keys
        addSpecialRow()
    }
    
    private fun addRow(keys: List<String>) {
        val rowView = KeyboardRowView(context).apply {
            setKeys(keys)
            setOnKeyClickListener { key ->
                onKeyListener?.invoke(key)
            }
        }
        addView(rowView)
    }
    
    private fun addSpecialRow() {
        val specialRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 4, 8, 4)
            }
        }
        
        // Shift key
        val shiftKey = createKeyButton("SHIFT")
        specialRow.addView(shiftKey)
        
        // Space key (takes most space)
        val spaceKey = createKeyButton("SPACE")
        spaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 3f)
        specialRow.addView(spaceKey)
        
        // Backspace key
        val backspaceKey = createKeyButton("BACKSPACE")
        specialRow.addView(backspaceKey)
        
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
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener {
                onKeyListener?.invoke(text)
            }
        }
    }
    
    fun setOnKeyListener(listener: (String) -> Unit) {
        onKeyListener = listener
    }
} 