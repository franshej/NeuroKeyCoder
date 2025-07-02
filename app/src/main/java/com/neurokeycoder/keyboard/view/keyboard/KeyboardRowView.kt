package com.neurokeycoder.keyboard.view.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Button

class KeyboardRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onKeyClickListener: ((String) -> Unit)? = null
    
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 4, 8, 4)
        }
    }
    
    fun setKeys(keys: List<String>) {
        removeAllViews()
        
        keys.forEach { key ->
            val keyButton = createKeyButton(key)
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
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener {
                onKeyClickListener?.invoke(text)
            }
        }
    }
    
    fun setOnKeyClickListener(listener: (String) -> Unit) {
        onKeyClickListener = listener
    }
} 