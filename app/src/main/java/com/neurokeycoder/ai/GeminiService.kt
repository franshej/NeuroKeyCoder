package com.neurokeycoder.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
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
                modelName = "gemini-2.0-flash",
                apiKey = apiKey
            )
        }
    }
    
    fun setApiKey(apiKey: String) {
        Log.d(TAG, "Setting new API key (length: ${apiKey.length})")
        prefs.edit().putString(API_KEY_PREF, apiKey).apply()
        initializeModel()
    }
    
    fun getApiKey(): String {
        return prefs.getString(API_KEY_PREF, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }
    
    suspend fun getCppSuggestions(context: String): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting C++ suggestions for context: '$context'")
        Log.d(TAG, "Context length: ${context.length} characters")
        
        try {
            val model = generativeModel ?: run {
                Log.w(TAG, "GenerativeModel is null - API key not configured")
                return@withContext getFallbackSuggestions(context)
            }
            
            val prompt = buildPrompt(context)
            Log.d(TAG, "Built prompt for Gemini API")
            
            val response: GenerateContentResponse = model.generateContent(prompt)
            Log.d(TAG, "Received response from Gemini API: ${response.text}")
            
            val suggestions = parseSuggestions(response.text ?: "")
            Log.d(TAG, "Parsed suggestions: $suggestions")
            
            return@withContext suggestions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting suggestions from Gemini API", e)
            val fallbackSuggestions = getFallbackSuggestions(context)
            Log.d(TAG, "Using fallback suggestions: $fallbackSuggestions")
            return@withContext fallbackSuggestions
        }
    }
    
    private fun buildPrompt(context: String): String {
        return """
You are a C++ code completion assistant. Based on the given code context, suggest exactly 5 single-word completions that would be most helpful for C++ programming.

Rules:
1. Return ONLY 5 suggestions, each on a new line
2. Each suggestion must be a single word, symbol, or short expression (no spaces except in function calls like "getInt()")
3. Focus on C++ keywords, function names, variable names, operators, or common patterns
4. Consider the context to make relevant suggestions
5. Examples of good suggestions: "void", "auto", "const", "return", "if", "else", "int", "float", "->", "::", "std", "cout", "endl", "vector", "string", "nullptr", "true", "false", "class", "struct", "template", "namespace", "using", "public", "private", "virtual", "override", "final", "static", "inline", "constexpr", "noexcept"
6. Put a space after each suggestion if you think it's a good suggestion.

Context: "$context"

Suggestions:
        """.trimIndent()
    }
    
    private fun parseSuggestions(response: String): List<String> {
        Log.d(TAG, "Parsing suggestions from response: '$response'")
        
        val suggestions = response.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Suggestions:") }
            .take(5)
            .ifEmpty { 
                Log.d(TAG, "No valid suggestions found in response, using fallback")
                getFallbackSuggestions("") 
            }
        
        Log.d(TAG, "Final parsed suggestions: $suggestions")
        return suggestions
    }
    
    private fun getFallbackSuggestions(context: String): List<String> {
        Log.d(TAG, "Getting fallback suggestions for context: '$context'")
        
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
                // General C++ suggestions
                listOf("void", "int", "auto", "const", "return")
            }
        }
        
        Log.d(TAG, "Generated fallback suggestions: $suggestions")
        return suggestions
    }
    
    fun isConfigured(): Boolean {
        val apiKey = getApiKey()
        return apiKey.isNotEmpty()
    }
}
