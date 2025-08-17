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
    
    suspend fun getCppSuggestions(context: String): List<String> = withContext(Dispatchers.IO) {
        
        try {
            val model = generativeModel ?: run {
                Log.w(TAG, "GenerativeModel is null - API key not configured")
                return@withContext getFallbackSuggestions(context)
            }
            
            val prompt = buildPrompt(context)
            
            val response: GenerateContentResponse = model.generateContent(prompt)
            
            val suggestions = parseSuggestions(response.text ?: "")
            
            return@withContext suggestions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting suggestions from Gemini API", e)
            val fallbackSuggestions = getFallbackSuggestions(context)
            return@withContext fallbackSuggestions
        }
    }
    
    private fun buildPrompt(context: String): String {
        return """
You are a C++ code completion assistant. Based on the given code context, suggest exactly 5 single-word completions that would be most helpful for C++ programming.

Rules:
1. Return ONLY 5 suggestions, each on a new line
2. Each suggestion must be a single word, symbol, or short expression
3. Focus on C++ keywords, function names, variable names, operators, or common patterns
4. Consider the context to make relevant suggestions
5. Examples of good suggestions: "void", "auto", "const", "return", "if", "else", "int", "float", "->", "::", "std", "cout", "endl", "vector", "string", "nullptr", "true", "false", "class", "struct", "template", "namespace", "using", "public", "private", "virtual", "override", "final", "static", "inline", "constexpr", "noexcept"
7. Rank the suggestions and return them in order of relevance.
8. Look for variable names or function names and suggest them.

Context: "$context"

Suggestions:
        """.trimIndent()
    }
    
    private fun parseSuggestions(response: String): List<String> {
        
        val suggestions = response.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Suggestions:") }
            .map { suggestion ->
                suggestion.replace(Regex("^#\\d+\\.\\s*"), "")
            }
            .take(5)
            .ifEmpty { 
                getFallbackSuggestions("") 
            }
        
        return suggestions
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
