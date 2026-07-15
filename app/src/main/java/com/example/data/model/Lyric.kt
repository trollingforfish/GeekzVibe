package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyric_cache")
data class Lyric(
    @PrimaryKey val songId: String,
    val originalLyrics: String,
    val translatedLyrics: String = "",
    val source: String = "Unknown", // Musixmatch, Genius, AZLyrics, Gemini
    val timestamp: Long = System.currentTimeMillis()
)
