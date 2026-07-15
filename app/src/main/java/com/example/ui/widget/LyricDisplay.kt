package com.example.ui.widget

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lyric
import com.example.ui.viewmodel.TranslationMode

@Composable
fun LyricDisplay(
    lyric: Lyric?,
    isSearching: Boolean,
    error: String?,
    showTranslation: Boolean,
    translationMode: TranslationMode,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyric_display_container"),
        contentAlignment = Alignment.Center
    ) {
        if (isSearching) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Mencari lirik dari sumber...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                )
            }
        } else if (error != null || lyric == null) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎵",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = error ?: "Lirik tidak ditemukan.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gunakan tombol cari online atau coba lagu bawaan.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Display lyrics
            val originalLines = lyric.originalLyrics.split("\n")
            val translatedLines = if (lyric.translatedLyrics.isNotEmpty()) {
                lyric.translatedLyrics.split("\n")
            } else {
                emptyList()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sumber: ${lyric.source}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                for (i in originalLines.indices) {
                    val originalLine = originalLines[i]
                    val translatedLine = if (i in translatedLines.indices) translatedLines[i] else ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logic of display based on showTranslation and translationMode
                        if (!showTranslation || lyric.translatedLyrics.isEmpty()) {
                            // Show ONLY original
                            Text(
                                text = originalLine,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 28.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            when (translationMode) {
                                TranslationMode.TOGGLE -> {
                                    // Switch between original or translation
                                    Text(
                                        text = translatedLine.ifEmpty { originalLine },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontStyle = if (translatedLine.isNotEmpty()) FontStyle.Italic else FontStyle.Normal,
                                            lineHeight = 28.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                TranslationMode.SIDE_BY_SIDE -> {
                                    // Side-by-side stacking original + translated directly under
                                    Text(
                                        text = originalLine,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                            lineHeight = 24.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    if (translatedLine.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = translatedLine,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 22.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(64.dp)) // Padding at bottom for scroll clearance
            }
        }
    }
}
