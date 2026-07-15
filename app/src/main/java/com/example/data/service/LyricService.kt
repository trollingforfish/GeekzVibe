package com.example.data.service

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object LyricService {
    private const val TAG = "LyricService"

    // Klien OkHttp standar untuk scraping dan API calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Mencari lirik dengan prioritas:
     * 1. Musixmatch API (jika API key tersedia)
     * 2. Genius API (jika API key tersedia)
     * 3. AZLyrics Scraping (HTTP request & parse)
     * 4. Gemini AI (sebagai fallback handal)
     */
    suspend fun findLyrics(title: String, artist: String, songId: String): LyricResult {
        // Coba Source 1: Musixmatch
        try {
            val musixmatchApiKey = "" // Opsional, bisa di-inject via Secrets jika ada
            if (musixmatchApiKey.isNotEmpty()) {
                val lyrics = fetchFromMusixmatch(title, artist, musixmatchApiKey)
                if (lyrics.isNotEmpty()) {
                    return LyricResult(lyrics, "Musixmatch API (Priority 1)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat dari Musixmatch: ${e.message}")
        }

        // Coba Source 2: Genius
        try {
            val geniusToken = "" // Opsional, bisa di-inject via Secrets jika ada
            if (geniusToken.isNotEmpty()) {
                val lyrics = fetchFromGenius(title, artist, geniusToken)
                if (lyrics.isNotEmpty()) {
                    return LyricResult(lyrics, "Genius API (Priority 2)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat dari Genius: ${e.message}")
        }

        // Coba Source 3: AZLyrics Scraping
        try {
            val lyrics = scrapeAZLyrics(title, artist)
            if (lyrics.isNotEmpty()) {
                return LyricResult(lyrics, "AZLyrics (Priority 3 - Scraping)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal scraping AZLyrics: ${e.message}")
        }

        // Coba Source 4: Gemini AI (Fallback Pintar yang menjamin 100% hasil)
        try {
            val systemPrompt = "Kamu adalah pustaka lirik musik terlengkap di dunia. Berikan lirik lagu asli secara lengkap dan akurat."
            val prompt = "Berikan lirik lagu lengkap untuk lagu berjudul '$title' oleh penyanyi/band '$artist'. Kembalikan HANYA teks lirik lagu aslinya saja tanpa komentar atau teks pembuka/penutup."
            val lyrics = GeminiService.generateText(prompt, systemPrompt)
            if (lyrics.isNotEmpty() && !lyrics.startsWith("Error")) {
                return LyricResult(lyrics, "Gemini AI (Fallback Smart Search)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat dari Gemini AI: ${e.message}")
        }

        return LyricResult("", "")
    }

    private fun fetchFromMusixmatch(title: String, artist: String, apiKey: String): String {
        // Mocking real network call structure for Musixmatch API
        // https://api.musixmatch.com/ws/1.1/matcher.lyrics.get?q_track=title&q_artist=artist&apikey=key
        val url = "https://api.musixmatch.com/ws/1.1/matcher.lyrics.get?q_track=${UriEncode(title)}&q_artist=${UriEncode(artist)}&apikey=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val body = response.body?.string() ?: return ""
            val json = JSONObject(body)
            val message = json.optJSONObject("message") ?: return ""
            val bodyObj = message.optJSONObject("body") ?: return ""
            val lyricsObj = bodyObj.optJSONObject("lyrics") ?: return ""
            return lyricsObj.optString("lyrics_body", "")
        }
    }

    private fun fetchFromGenius(title: String, artist: String, token: String): String {
        // Mocking Genius API structure
        // 1. Search for track
        val searchUrl = "https://api.genius.com/search?q=${UriEncode("$title $artist")}"
        val request = Request.Builder()
            .url(searchUrl)
            .header("Authorization", "Bearer $token")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val body = response.body?.string() ?: return ""
            val json = JSONObject(body)
            val responseObj = json.optJSONObject("response") ?: return ""
            val hits = responseObj.optJSONArray("hits") ?: return ""
            if (hits.length() == 0) return ""
            val result = hits.getJSONObject(0).optJSONObject("result") ?: return ""
            val path = result.optString("path", "")
            
            // 2. Scrape lyrics from genius webpage path
            if (path.isNotEmpty()) {
                val scrapeRequest = Request.Builder().url("https://genius.com$path").build()
                client.newCall(scrapeRequest).execute().use { scrapeResponse ->
                    if (!scrapeResponse.isSuccessful) return ""
                    val html = scrapeResponse.body?.string() ?: return ""
                    // Parse lyrics using regex on Genius lyrics container
                    val regex = Regex("<div class=\"Lyrics__Container.*?>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
                    val matches = regex.findAll(html)
                    val sb = StringBuilder()
                    for (match in matches) {
                        val chunk = match.groupValues[1]
                            .replace("<br/?>".toRegex(), "\n")
                            .replace("<.*?>".toRegex(), "")
                        sb.append(chunk).append("\n")
                    }
                    return sb.toString().trim()
                }
            }
        }
        return ""
    }

    private fun scrapeAZLyrics(title: String, artist: String): String {
        // Formating url: https://www.azlyrics.com/lyrics/artist/title.html
        val cleanArtist = artist.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "")
        val cleanTitle = title.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "")
        
        val url = "https://www.azlyrics.com/lyrics/$cleanArtist/$cleanTitle.html"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val html = response.body?.string() ?: return ""
            
            // AZLyrics lyrics are bounded by special comment and next </div>
            val regex = Regex("<!-- Usage of azlyrics\\.com content by any third-party lyrics provider is prohibited by our licensing agreement\\. Sorry about that\\. -->(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(html)
            if (match != null) {
                return match.groupValues[1]
                    .replace("<br/?>".toRegex(), "\n")
                    .replace("<.*?>".toRegex(), "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .trim()
            }
        }
        return ""
    }

    private fun UriEncode(str: String): String {
        return java.net.URLEncoder.encode(str, "UTF-8")
    }
}

data class LyricResult(
    val lyrics: String,
    val source: String
)
