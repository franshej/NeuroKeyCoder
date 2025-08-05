package com.neurokeycoder.keyboard.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.neurokeycoder.R

class SuggestionsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onSuggestionClickListener: ((String) -> Unit)? = null
    private val suggestionButtons = mutableListOf<Button>()
    private val loadingContainer: LinearLayout
    private val progressBar: ProgressBar
    private val progressText: TextView
    
    // Progress tracking
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var startTime: Long = 0
    private var estimatedDuration: Long = 2500 // 2.5 seconds default
    
    companion object {
        private const val TAG = "SuggestionsRowView"
        private const val DEFAULT_DURATION_MS = 2500L // 2.5 seconds
        private const val PROGRESS_UPDATE_INTERVAL = 50L // Update every 50ms for smooth animation
    }
    
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 4, 4, 4)
        }
        
        // Create loading container with progress bar and text
        loadingContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 4, 8, 4)
            }
            visibility = GONE
        }
        
        // Progress bar
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(12)
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            max = 100
            progress = 0
            
            // Use custom progress drawable
            progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_bar_horizontal)
            
            // Ensure the progress bar is properly initialized
            isIndeterminate = false
        }
        
        // Progress text
        progressText = TextView(context).apply {
            text = "Getting AI suggestions..."
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.suggestion_text))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        
        loadingContainer.addView(progressBar)
        loadingContainer.addView(progressText)
        addView(loadingContainer)
        
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
        
        hideLoadingProgress()
        
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
                setMargins(4, 4, 4, 4)
            }
            
            // Styling for suggestion buttons
            setBackgroundResource(R.drawable.key_background)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.suggestion_text))
            minHeight = 0
            minWidth = 0
            minimumHeight = dpToPx(44)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            setOnClickListener {
                Log.d(TAG, "Suggestion clicked: '$text' - will trigger smart replacement")
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
                setMargins(4, 4, 4, 4)
            }
            setBackgroundResource(R.drawable.special_key_background)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.special_key_text))
            minimumHeight = dpToPx(44)
            isEnabled = false
        }
        suggestionButtons.add(placeholderButton)
        addView(placeholderButton)
    }
    
    fun showLoadingProgress(estimatedDurationMs: Long = DEFAULT_DURATION_MS) {
        Log.d(TAG, "Starting progress bar with estimated duration: ${estimatedDurationMs}ms")
        
        // Remove existing buttons
        suggestionButtons.forEach { removeView(it) }
        suggestionButtons.clear()
        
        // Reset progress
        post {
            progressBar.progress = 0
            progressText.text = "Starting AI suggestions..."
            progressBar.invalidate()
        }
        
        estimatedDuration = estimatedDurationMs
        startTime = System.currentTimeMillis()
        
        loadingContainer.visibility = VISIBLE
        startProgressAnimation()
    }
    
    fun hideLoadingProgress() {
        Log.d(TAG, "Hiding progress bar")
        stopProgressAnimation()
        loadingContainer.visibility = GONE
    }
    
    fun updateProgress(currentProgress: Int) {
        val clampedProgress = currentProgress.coerceIn(0, 100)
        Log.d(TAG, "Updating progress to: $clampedProgress% (was ${progressBar.progress}%)")
        
        // Use post to ensure UI thread execution
        post {
            progressBar.progress = clampedProgress
            progressText.text = "AI thinking... $clampedProgress%"
            
            // Force invalidation to ensure visual update
            progressBar.invalidate()
        }
    }
    
    private fun startProgressAnimation() {
        stopProgressAnimation() // Stop any existing animation
        
        Log.d(TAG, "Starting progress animation with ${PROGRESS_UPDATE_INTERVAL}ms intervals")
        
        progressHandler = Handler(Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                if (loadingContainer.visibility != VISIBLE) {
                    Log.d(TAG, "Loading container not visible, stopping animation")
                    return
                }
                
                val elapsed = System.currentTimeMillis() - startTime
                val progress = ((elapsed.toFloat() / estimatedDuration) * 100).toInt()
                
                Log.d(TAG, "Animation tick: elapsed=${elapsed}ms, progress=$progress%, estimatedDuration=$estimatedDuration")
                
                if (progress < 100) {
                    updateProgress(progress)
                    progressHandler?.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                } else {
                    // If we reach 100% but still waiting, show a "completing" state
                    Log.d(TAG, "Progress reached 100%, showing completion state")
                    post {
                        progressBar.progress = 100
                        progressText.text = "Finalizing suggestions..."
                        progressBar.invalidate()
                    }
                }
            }
        }
        
        // Start the animation immediately
        progressHandler?.post(progressRunnable!!)
    }
    
    private fun stopProgressAnimation() {
        progressRunnable?.let { runnable ->
            progressHandler?.removeCallbacks(runnable)
        }
        progressHandler = null
        progressRunnable = null
    }
    
    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClickListener = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
