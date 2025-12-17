package com.neurokeycoder.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class GeminiService(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("neurokey_prefs", Context.MODE_PRIVATE)
    }
    
    private var generativeModel: GenerativeModel? = null
    
    // System instruction to be prepended to prompts for consistent behavior
    private val systemInstruction = """You are a C++ code completion assistant.
Return exactly 6 lines:
Line 1: One complete C++ statement or line (e.g., "int main() {", "#include <iostream>", "return 0;")
Lines 2-6: Five single words/symbols (max 18 characters each, e.g., "void", "int", "const", "return", "{")

Rules:
- No labels, numbering, or explanations
- No markdown code blocks (no ```)
- Just plain text suggestions
- One suggestion per line

"""

    companion object {
        private const val TAG = "GeminiService"
        private const val API_KEY_PREF = ""
        private const val DEFAULT_API_KEY = "YOUR_API_KEY_HERE" // User needs to replace this
        private const val REQUEST_TIMEOUT_MS = 8000L // 8 seconds timeout
        private const val MAX_OUTPUT_TOKENS = 150 // Limit response length for speed
    }
    
    init {
        initializeModel()
    }
    
    private fun initializeModel() {
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty() && apiKey != DEFAULT_API_KEY) {
            generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash-lite",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f // Lower temperature for more consistent, faster responses
                    topK = 20 // Limit choices for faster generation
                    topP = 0.8f
                    maxOutputTokens = MAX_OUTPUT_TOKENS // Limit output length
                }
            )
            Log.d(TAG, "GenerativeModel initialized with optimized config")
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
            
            // Add timeout to prevent hanging indefinitely
            val response: GenerateContentResponse = withTimeout(REQUEST_TIMEOUT_MS) {
                model.generateContent(prompt)
            }

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
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "Gemini API request timed out after ${REQUEST_TIMEOUT_MS}ms")
            val fallbackSuggestions = getFallbackSuggestions(context)
            val fallbackSentence = getFallbackSentenceSuggestion(context)
            return@withContext Pair(fallbackSuggestions, fallbackSentence)
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
        // Extract last 5 lines + current incomplete token
        val lines = context.lines()
        val relevantContext = lines.takeLast(5).joinToString("\n")
        return systemInstruction + "Complete this C++ code:\n$relevantContext"
    }
    
    private fun parseWordSuggestions(response: String): List<String> {
        val lines = response.lines()
            .map { it.trim() }
            .filter { line -> 
                line.isNotEmpty() && 
                !line.startsWith("Suggestions:", ignoreCase = true) &&
                !line.startsWith("Format:", ignoreCase = true) &&
                !line.startsWith("Context:", ignoreCase = true) &&
                !line.startsWith("Rules", ignoreCase = true) &&
                !line.startsWith("```", ignoreCase = true) && // Filter code block markers
                !line.endsWith("```", ignoreCase = true) &&
                line != "cpp" && // Filter language identifiers
                line != "c++"
            }
            .map { suggestion ->
                // Remove common prefixes like "1.", "#1", "Word 1:", "Line 1:", etc.
                suggestion.replace(Regex("^(#?\\d+\\.?\\s*|Word\\s*\\d+:\\s*|Line\\s*\\d+:\\s*)", RegexOption.IGNORE_CASE), "").trim()
            }
            .map { it.trim('`', '"', '\'') } // Remove code block markers and quotes
            .filter { it.isNotEmpty() }
        
        // Skip the first line (sentence suggestion) and take the next lines for word suggestions
        val wordCandidates = lines.drop(1)
            .filter { it.length <= 18 } // Word suggestions must be max 18 characters
            .take(5)

        // If we don't have enough valid word suggestions, use fallbacks
        val suggestions = if (wordCandidates.size >= 3) {
            wordCandidates
        } else {
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
                !line.startsWith("Rules", ignoreCase = true) &&
                !line.startsWith("```", ignoreCase = true) && // Filter code block markers
                !line.endsWith("```", ignoreCase = true) &&
                line != "cpp" && // Filter language identifiers
                line != "c++"
            }
            .map { suggestion ->
                // Remove common prefixes like "1.", "#1", "Line 1:", etc.
                suggestion.replace(Regex("^(#?\\d+\\.?\\s*|Line\\s*\\d+:\\s*)", RegexOption.IGNORE_CASE), "").trim()
            }
            .map { it.trim('`', '"', '\'') } // Remove code block markers and quotes
            .filter { it.isNotEmpty() }
        
        // First line is the sentence/line suggestion
        val firstLine = lines.firstOrNull() ?: ""
        
        // Validate that it's actually a useful sentence (not just a word)
        // A valid sentence should be at least 5 characters and contain useful C++ syntax
        if (firstLine.length < 5 || firstLine.length > 100) {
            return "" // Too short or too long, return empty to trigger fallback
        }

        // Filter out common useless responses
        val uselessPatterns = listOf(
            "```",
            "code",
            "suggestion",
            "example",
            "here",
            "following"
        )

        if (uselessPatterns.any { firstLine.lowercase().contains(it) }) {
            return "" // Useless response, trigger fallback
        }

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
