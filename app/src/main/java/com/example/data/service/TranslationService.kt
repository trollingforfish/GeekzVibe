package com.example.data.service

import android.util.Log

object TranslationService {
    private const val TAG = "TranslationService"

    /**
     * Menerjemahkan lirik asli ke Bahasa Indonesia menggunakan Gemini AI.
     */
    suspend fun translateLyrics(originalLyrics: String): String {
        if (originalLyrics.isEmpty()) return ""
        
        val systemPrompt = "Kamu adalah penerjemah lirik lagu profesional. Terjemahkan lirik lagu ke Bahasa Indonesia secara puitis dan mengalir, namun tetap mempertahankan makna emosional aslinya. Terjemahkan baris demi baris, pertahankan jumlah baris dan format baris yang persis sama dengan aslinya."
        val prompt = "Terjemahkan lirik lagu berikut ini ke Bahasa Indonesia baris demi baris. Berikan HANYA teks terjemahannya saja tanpa catatan kaki, penjelasan, atau teks pembuka/penutup:\n\n$originalLyrics"
        
        return try {
            val translated = GeminiService.generateText(prompt, systemPrompt)
            if (translated.isNotEmpty() && !translated.startsWith("Error")) {
                translated.trim()
            } else {
                "Gagal menerjemahkan lirik menggunakan AI. Silakan coba lagi nanti."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menerjemahkan lirik: ${e.message}", e)
            "Terjadi kesalahan saat menerjemahkan lirik."
        }
    }
}
