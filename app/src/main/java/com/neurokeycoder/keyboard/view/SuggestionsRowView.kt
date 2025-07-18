package com.neurokeycoder.keyboard.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onSuggestionClickListener: ((String) -> Unit)? = null
    private val suggestionButtons = mutableListOf<Button>()
    private val loadingIndicator: TextView
    
    companion object {
        private const val TAG = "SuggestionsRowView"
    }
    
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 4, 4, 4)
        }
        
        // Add loading indicator
        loadingIndicator = TextView(context).apply {
            text = "Getting suggestions..."
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            visibility = GONE
        }
        addView(loadingIndicator)
        
        // Initialize with empty suggestions
        updateSuggestions(emptyList())
    }
    
    fun updateSuggestions(suggestions: List<String>) {
        Log.d(TAG, "Updating suggestions: $suggestions")
        
        // Remove existing suggestion buttons
        suggestionButtons.forEach { removeView(it) }
        suggestionButtons.clear()
        
        if (suggestions.isEmpty()) {
            Log.d(TAG, "No suggestions provided, showing placeholder")
            showPlaceholder()
            return
        }
        
        hideLoadingIndicator()
        
        // Create buttons for each suggestion
        suggestions.take(5).forEach { suggestion ->
            Log.d(TAG, "Creating button for suggestion: '$suggestion'")
            val button = createSuggestionButton(suggestion)
            suggestionButtons.add(button)
            addView(button)
        }
        
        Log.d(TAG, "Created ${suggestionButtons.size} suggestion buttons")
    }
    
    private fun createSuggestionButton(text: String): Button {
        return Button(context).apply {
            this.text = text
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(2, 2, 2, 2)
            }
            
            // Styling for suggestion buttons
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 14f
            setTextColor(Color.BLUE)
            minHeight = 0
            minWidth = 0
            minimumHeight = 80
            
            setOnClickListener {
                Log.d(TAG, "Suggestion clicked: '$text'")
                onSuggestionClickListener?.invoke(text)
            }
        }
    }
    
    private fun showPlaceholder() {
        // Show a single placeholder button
        val placeholderButton = Button(context).apply {
            text = "AI Suggestions"
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
            setBackgroundResource(android.R.drawable.btn_default)
            textSize = 12f
            setTextColor(Color.GRAY)
            minimumHeight = 80
            isEnabled = false
        }
        suggestionButtons.add(placeholderButton)
        addView(placeholderButton)
    }
    
    fun showLoadingIndicator() {
        Log.d(TAG, "Showing loading indicator")
        
        // Remove existing buttons
        suggestionButtons.forEach { removeView(it) }
        suggestionButtons.clear()
        
        loadingIndicator.visibility = VISIBLE
    }
    
    fun hideLoadingIndicator() {
        Log.d(TAG, "Hiding loading indicator")
        loadingIndicator.visibility = GONE
    }
    
    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClickListener = listener
    }
}
