package com.neurokeycoder

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.neurokeycoder.ai.GeminiService
import com.neurokeycoder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var geminiService: GeminiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        geminiService = GeminiService(this)
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnEnableKeyboard.setOnClickListener {
            openKeyboardSettings()
        }
        
        binding.btnSettings.setOnClickListener {
            openAppSettings()
        }
        
        binding.btnApiKey.setOnClickListener {
            showApiKeyDialog()
        }
        
        updateApiKeyButtonText()
    }
    
    private fun updateApiKeyButtonText() {
        val isConfigured = geminiService.isConfigured()
        binding.btnApiKey.text = if (isConfigured) {
            "Update Gemini API Key"
        } else {
            "Set Gemini API Key (Required)"
        }
    }
    
    private fun showApiKeyDialog() {
        val editText = EditText(this).apply {
            hint = "Enter your Gemini API key"
            setText(if (geminiService.isConfigured()) "***API_KEY_SET***" else "")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Gemini API Configuration")
            .setMessage("Enter your Gemini API key to enable AI suggestions.\n\nGet your free API key from: https://ai.google.dev/")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = editText.text.toString().trim()
                if (apiKey.isNotEmpty() && apiKey != "***API_KEY_SET***") {
                    geminiService.setApiKey(apiKey)
                    updateApiKeyButtonText()
                    Toast.makeText(this, "API key saved successfully!", Toast.LENGTH_SHORT).show()
                } else if (apiKey != "***API_KEY_SET***") {
                    Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Get API Key") { _, _ ->
                // Open browser to get API key
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ai.google.dev/"))
                startActivity(intent)
            }
            .show()
    }
    
    private fun openKeyboardSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
} 