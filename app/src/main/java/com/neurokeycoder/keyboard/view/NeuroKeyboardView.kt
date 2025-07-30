package com.neurokeycoder.keyboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.neurokeycoder.R
import com.neurokeycoder.keyboard.view.keyboard.KeyboardRow
import com.neurokeycoder.keyboard.view.keyboard.KeyboardRowView

class NeuroKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onKeyListener: ((String) -> Unit)? = null
    private val symbolRowViews = mutableListOf<KeyboardRowView>()
    private var isShiftPressed = false
    private var isSymbolLayout = true
    
    // LLM Suggestions
    private val suggestionsRow: SuggestionsRowView by lazy {
        SuggestionsRowView(context).apply {
            setOnSuggestionClickListener { suggestion ->
                onKeyListener?.invoke(suggestion)
            }
        }
    }
    
    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.keyboard_background)
        setPadding(16, 16, 16, 16)
        setupKeyboard()
    }
    
    private fun setupKeyboard() {
        setupMainLayout()
    }
    
    private fun setupMainLayout() {
        removeAllViews()
        symbolRowViews.clear()

        // Add LLM suggestions row at the top
        addView(suggestionsRow)
        
        // Fourth row: Programming symbols
        addProgrammingSymbolsRow()
        
        addRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))

        addRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        
        // Third row: SHIFT + Z X C V B N M + BACKSPACE
        addThirdRow()
        // Fifth row: Special keys
        addSpecialRow()
    }
    
    private fun setupSymbolLayout() {
        removeAllViews()
        symbolRowViews.clear()
        
        // Add LLM suggestions row at the top
        addView(suggestionsRow)
        
        // First row: More symbols
        addRow(listOf("[", "]", "+", "-", "^", "?", "# ", "'", "\""))

        // Second row: 0-9
        addRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))

        // Third row: Additional symbols
        addRow(listOf("const ", "if ", "else ", "void ", "return ", "this "))
        
        // Third row: SHIFT + More symbols + BACKSPACE
        addSymbolThirdRow()
        
        // Fourth row: Special keys with ABC button
        addSymbolSpecialRow()
    }
    
    private fun addProgrammingSymbolsRow() {
        val programmingRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }
        
        // Programming symbols: ( ) { } [ ] & * + -
        val symbols = listOf("(", ")", "{", "}", "_", "=", "&", "*", "!", "|", ";", ",")
        symbols.forEach { symbol ->
            val keyButton = createKeyButton(symbol)
            keyButton.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            programmingRow.addView(keyButton)
        }
        
        addView(programmingRow)
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
        
        // ?123 key
        val numberKey = createSpecialKeyButton("?123", "?123")
        numberKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        specialRow.addView(numberKey)
        
        // Comma key
        val commaKey = createKeyButton(".")
        commaKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(commaKey)
        
        // ; < keys
        val semicolonKey = createKeyButton(":")
        semicolonKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(semicolonKey)

        // Space key (smaller)
        val spaceKey = createSpaceButton()
        spaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.5f)
        specialRow.addView(spaceKey)
        
        val lessThanKey = createKeyButton("<")
        lessThanKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(lessThanKey)
        
        // > : keys
        val greaterThanKey = createKeyButton(">")
        greaterThanKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f)
        specialRow.addView(greaterThanKey)
        
        // Enter key (new line)
        val enterKey = createSpecialKeyButton("ENTER", "↵")
        enterKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        specialRow.addView(enterKey)
        
        addView(specialRow)
    }
    
    private fun addSymbolSpecialRow() {
        val specialRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }
        
        // ABC key (to switch back to main layout)
        val abcKey = createSpecialKeyButton("ABC", "ABC")
        abcKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        specialRow.addView(abcKey)

        val symbolsBeforeSpace = listOf("/", "\\")
        symbolsBeforeSpace.forEach { symbol ->
            val keyButton = createKeyButton(symbol)
            keyButton.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            specialRow.addView(keyButton)
        }

        // Space key (takes most space)
        val spaceKey = createSpaceButton()
        spaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 3.0f)
        specialRow.addView(spaceKey)

        val symbolsAfterSpace = listOf("~", "@")
        symbolsAfterSpace.forEach { symbol ->
            val keyButton = createKeyButton(symbol)
            keyButton.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            specialRow.addView(keyButton)
        }
        
        addView(specialRow)
    }
    
    private fun addSymbolThirdRow() {
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
        shiftKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        thirdRow.addView(shiftKey)

        val symbolsBeforeSpace = listOf("auto ", "float ", "int ", "double ")
        symbolsBeforeSpace.forEach { symbol ->
            val keyButton = createKeyButton(symbol)
            keyButton.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, if (symbol == "double ") { 1.25f } else 1.0f)
            thirdRow.addView(keyButton)
        }
        
        // Backspace key - weight 1.0 (medium width) with long press support
        val backspaceKey = createBackspaceButton()
        backspaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        thirdRow.addView(backspaceKey)
        
        addView(thirdRow)
    }
    
    private fun addRow(keys: List<String>) {
        val rowView = KeyboardRowView(context).apply {
            setKeys(keys)
            setOnKeyClickListener { key ->
                onKeyListener?.invoke(key)
            }
        }
        symbolRowViews.add(rowView)
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
        shiftKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.5f)
        thirdRow.addView(shiftKey)
        
        // Letter keys: Z X C V B N M - using KeyboardRowView for proper shift handling
        val letterRowView = KeyboardRowView(context).apply {
            setKeys(listOf("z", "x", "c", "v", "b", "n", "m"))
            setOnKeyClickListener { key ->
                onKeyListener?.invoke(key)
            }
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 7f)
        }
        symbolRowViews.add(letterRowView)
        thirdRow.addView(letterRowView)
        
        // Backspace key - weight 1.0 (medium width) with long press support
        val backspaceKey = createBackspaceButton()
        backspaceKey.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.5f)
        thirdRow.addView(backspaceKey)
        
        addView(thirdRow)
    }
    
    private fun createBackspaceButton(): android.widget.Button {
        return android.widget.Button(context).apply {
            this.text = "⌫"
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setBackgroundResource(R.drawable.special_key_background)
            textSize = 18f
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.special_key_text))
            minHeight = 0
            minimumHeight = dpToPx(48)
            
            // Set up long press detection
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startContinuousBackspace()
                        true
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        stopContinuousBackspace()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private var backspaceTimer: android.os.Handler? = null
    private var backspaceRunnable: Runnable? = null
    
    private fun startContinuousBackspace() {
        // Initial backspace
        onKeyListener?.invoke("BACKSPACE")
        
        // Set up repeating backspace
        backspaceTimer = android.os.Handler(android.os.Looper.getMainLooper())
        backspaceRunnable = object : Runnable {
            override fun run() {
                onKeyListener?.invoke("BACKSPACE")
                backspaceTimer?.postDelayed(this, 100) // Repeat every 100ms
            }
        }
        backspaceTimer?.postDelayed(backspaceRunnable!!, 500) // Start repeating after 500ms
    }
    
    private fun stopContinuousBackspace() {
        backspaceRunnable?.let { runnable ->
            backspaceTimer?.removeCallbacks(runnable)
        }
        backspaceTimer = null
        backspaceRunnable = null
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
            setBackgroundResource(R.drawable.key_background)
            textSize = 18f
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.key_text_normal))
            minHeight = 0
            minimumHeight = dpToPx(48)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener {
                onKeyListener?.invoke(text)
            }
        }
    }
    
    private fun createSpaceButton(): android.widget.Button {
        return android.widget.Button(context).apply {
            this.text = "space"
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                3f
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setBackgroundResource(R.drawable.space_key_background)
            textSize = 16f
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.special_key_text))
            minHeight = 0
            minimumHeight = dpToPx(48)
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
                setMargins(4, 4, 4, 4)
            }
            setBackgroundResource(R.drawable.special_key_background)
            textSize = 16f
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.special_key_text))
            minHeight = 0
            minimumHeight = dpToPx(48)
            setOnClickListener {
                onKeyListener?.invoke(action)
            }
        }
    }
    
    fun setOnKeyListener(listener: (String) -> Unit) {
        onKeyListener = listener
    }
    
    fun updateShift(isShiftPressed: Boolean) {
        this.isShiftPressed = isShiftPressed
        symbolRowViews.forEach { rowView ->
            rowView.updateShift(isShiftPressed)
        }
    }
    
    fun switchToSymbolLayout() {
        isSymbolLayout = true
        setupSymbolLayout()
    }
    
    fun switchToMainLayout() {
        isSymbolLayout = false
        setupMainLayout()
    }
    
    fun updateSuggestions(suggestions: List<String>) {
        suggestionsRow.updateSuggestions(suggestions)
    }
    
    fun showLoadingSuggestions(estimatedDurationMs: Long = 2500L) {
        suggestionsRow.showLoadingProgress(estimatedDurationMs)
    }
    
    fun updateSuggestionsProgress(progress: Int) {
        suggestionsRow.updateProgress(progress)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
} 