package com.neurokeycoder.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("neurokey_prefs", Context.MODE_PRIVATE)
    }
    
    private var generativeModel: GenerativeModel? = null
    
    companion object {
        private const val TAG = "GeminiService"
        private const val API_KEY_PREF = ""
        private const val DEFAULT_API_KEY = "YOUR_API_KEY_HERE" // User needs to replace this
    }
    
    init {
        initializeModel()
    }
    
    private fun initializeModel() {
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
            generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash-lite",
                apiKey = apiKey
            )
        }
    }
    
    fun setApiKey(apiKey: String) {
        prefs.edit().putString(API_KEY_PREF, apiKey).apply()
        initializeModel()
    }
    
    fun getApiKey(): String {
        return prefs.getString(API_KEY_PREF, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }
    
    suspend fun getCppSuggestions(context: String): Pair<List<String>, String> = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: run {
                Log.w(TAG, "GenerativeModel is null - API key not configured")
                return@withContext Pair(getFallbackSuggestions(context), getFallbackSentenceSuggestion(context))
            }
            
            val prompt = buildPrompt(context)
            
            val response: GenerateContentResponse = model.generateContent(prompt)
            
            val responseText = response.text ?: ""
            Log.d(TAG, "Raw Gemini response:\n$responseText")
            
            val sentenceSuggestion = parseSentenceSuggestion(responseText)
            Log.d(TAG, "Parsed sentence: '$sentenceSuggestion'")
            
            val wordSuggestions = parseWordSuggestions(responseText)
            Log.d(TAG, "Parsed words: $wordSuggestions")
            
            // If no sentence was parsed, provide a fallback
            val finalSentence = if (sentenceSuggestion.isEmpty()) {
                getFallbackSentenceSuggestion(context)
            } else {
                sentenceSuggestion
            }
            Log.d(TAG, "Final sentence: '$finalSentence'")

            return@withContext Pair(wordSuggestions, finalSentence)
        } catch (e: CancellationException) {
            // Request was cancelled (e.g., coroutine scope cancelled)
            Log.d(TAG, "Suggestion request cancelled")
            throw e // Re-throw to properly handle cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting suggestions from Gemini API", e)
            val fallbackSuggestions = getFallbackSuggestions(context)
            val fallbackSentence = getFallbackSentenceSuggestion(context)
            return@withContext Pair(fallbackSuggestions, fallbackSentence)
        }
    }
    
    private fun buildPrompt(context: String): String {
        return """
You are a C++ code completion assistant. Based on the given code context, suggest exactly 1 complete line of C++ code and 5 single-word completions.

Rules for the 1 line suggestion (FIRST):
1. Return 1 complete, syntactically valid line of C++ code that would logically follow the context
2. Examples: "#include <iostream>", "int main() {", "std::vector<int> vec = {1, 2, 3};", "for (int i = 0; i < n; i++) {", "for (auto& item : vec) {", "if (count > 0) {", "while (running) {", "std::cout << result << std::endl;", "return 0;", "auto result = func(x, y);", "std::string name = \"\";", "class MyClass {", "void process() {", "} else {", "break;", "continue;"
3. The line should be practical and commonly used in C++ programming
4. Include proper spacing and formatting
5. End with appropriate punctuation (semicolon, opening brace, etc.)
6. Prioritize modern C++ patterns (auto, range-based for, smart pointers, STL containers)

Rules for the 5 word suggestions (AFTER the line):
1. Return 5 suggestions, each on a new line
2. Each suggestion must be a single word, symbol, or short expression
3. Focus on C++ keywords, function names, variable names, operators, or common patterns
4. Consider the context to make relevant suggestions
5. Examples: "void", "auto", "const", "return", "if", "else", "int", "float", "->", "::", "std", "cout", "endl", "vector", "string", "nullptr", "true", "false"

Format:
Line 1: [Complete C++ line]
Line 2: [Word 1]
Line 3: [Word 2]
Line 4: [Word 3]
Line 5: [Word 4]
Line 6: [Word 5]

Context: "$context"

Suggestions:
        """.trimIndent()
    }
    
    private fun parseWordSuggestions(response: String): List<String> {
        val lines = response.lines()
            .map { it.trim() }
            .filter { line -> 
                line.isNotEmpty() && 
                !line.startsWith("Suggestions:", ignoreCase = true) &&
                !line.startsWith("Format:", ignoreCase = true) &&
                !line.startsWith("Context:", ignoreCase = true) &&
                !line.startsWith("Rules", ignoreCase = true)
            }
            .map { suggestion ->
                // Remove common prefixes like "1.", "#1", "Word 1:", "Line 1:", etc.
                suggestion.replace(Regex("^(#?\\d+\\.?\\s*|Word\\s*\\d+:\\s*|Line\\s*\\d+:\\s*)", RegexOption.IGNORE_CASE), "").trim()
            }
            .filter { it.isNotEmpty() }
        
        // Skip the first line (sentence suggestion) and take the next 5 for word suggestions
        val suggestions = lines.drop(1).take(5)
            .ifEmpty { 
                getFallbackSuggestions("") 
            }
        
        return suggestions
    }

    private fun parseSentenceSuggestion(response: String): String {
        val lines = response.lines()
            .map { it.trim() }
            .filter { line -> 
                line.isNotEmpty() && 
                !line.startsWith("Suggestions:", ignoreCase = true) &&
                !line.startsWith("Format:", ignoreCase = true) &&
                !line.startsWith("Context:", ignoreCase = true) &&
                !line.startsWith("Rules", ignoreCase = true)
            }
            .map { suggestion ->
                // Remove common prefixes like "1.", "#1", "Line 1:", etc.
                suggestion.replace(Regex("^(#?\\d+\\.?\\s*|Line\\s*\\d+:\\s*)", RegexOption.IGNORE_CASE), "").trim()
            }
            .filter { it.isNotEmpty() }
        
        // First line is the sentence/line suggestion
        val firstLine = lines.firstOrNull() ?: ""
        
        // If the first line looks like it might be wrapped in brackets or quotes, clean it up
        return firstLine.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .trim()
    }
    
    private fun getFallbackSentenceSuggestion(context: String): String {
        // Analyze context for smart fallback line suggestions
        val contextLower = context.lowercase()
        
        return when {
            contextLower.isEmpty() || contextLower.isBlank() -> {
                "int main() {"
            }
            contextLower.contains("#include") -> {
                "int main() {"
            }
            contextLower.contains("if") && !contextLower.contains("else") -> {
                "} else {"
            }
            contextLower.contains("for") || contextLower.contains("while") -> {
                "break;"
            }
            contextLower.contains("class") || contextLower.contains("struct") -> {
                "void process() {"
            }
            contextLower.contains("cout") -> {
                "std::cout << result << std::endl;"
            }
            contextLower.contains("vector") -> {
                "for (auto& item : vec) {"
            }
            contextLower.contains("int main") -> {
                "return 0;"
            }
            contextLower.contains("void") -> {
                "return;"
            }
            contextLower.endsWith("{") || contextLower.trim().endsWith("{") -> {
                "auto result = 0;"
            }
            else -> {
                "std::cout << \"Hello\" << std::endl;"
            }
        }
    }
    
    private fun getFallbackSuggestions(context: String): List<String> {
        
        // Analyze context for smarter fallbacks
        val contextLower = context.lowercase()
        
        val suggestions = when {
            contextLower.contains("if") || contextLower.contains("while") || contextLower.contains("for") -> {
                listOf("(", ")", "{", "}", "else")
            }
            contextLower.contains("class") || contextLower.contains("struct") -> {
                listOf("public", "private", "protected", "{", "}")
            }
            contextLower.contains("template") -> {
                listOf("<", ">", "typename", "class", "auto")
            }
            contextLower.contains("std") -> {
                listOf("vector", "string", "cout", "endl", "map")
            }
            contextLower.contains("int") || contextLower.contains("float") || contextLower.contains("double") -> {
                listOf("=", ";", "[", "]", "*")
            }
            contextLower.contains("void") -> {
                listOf("(", ")", "{", "return", ";")
            }
            contextLower.contains("return") -> {
                listOf(";", "0", "nullptr", "true", "false")
            }
            contextLower.contains("include") -> {
                listOf("<iostream>", "<vector>", "<string>", "<algorithm>", "<memory>")
            }
            else -> {
                listOf("void", "int", "auto", "const", "return")
            }
        }
        
        return suggestions
    }
    
    fun isConfigured(): Boolean {
        val apiKey = getApiKey()
        return apiKey.isNotEmpty()
    }
}
