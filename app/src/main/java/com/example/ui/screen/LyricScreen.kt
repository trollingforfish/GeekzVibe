package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.TranslationMode
import com.example.ui.widget.LyricDisplay

@Composable
fun LyricScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val lyric by viewModel.currentLyrics.collectAsState()
    val isSearching by viewModel.isSearchingLyrics.collectAsState()
    val lyricError by viewModel.lyricError.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val translationMode by viewModel.translationMode.collectAsState()

    val scrollState = rememberScrollState()

    if (currentSong == null) return
    val song = currentSong!!

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B0C)) // Immersive black
            .testTag("lyric_screen")
    ) {
        // --- AMBIENT GLOW BLOBS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(Color(0xFF6750A4).copy(alpha = 0.25f))
                .blur(110.dp)
        )

        // --- CORE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Transparent Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("lyric_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Kembali",
                        tint = Color(0xFFE6E1E3)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 13.sp,
                        color = Color(0xFFE6E1E3).copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Terjemahkan Button
                Button(
                    onClick = { viewModel.translateLyrics() },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("translate_lyrics_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showTranslation) Color(0xFF6750A4) else Color.White,
                        contentColor = if (showTranslation) Color.White else Color(0xFF0D0B0C)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Terjemahkan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lyric?.translatedLyrics.isNullOrEmpty()) "TERJEMAHKAN" else if (showTranslation) "ASLI" else "TERJEMAHAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 2. Interactive Translation Mode Selector (only show if lyrics has translation available)
            AnimatedVisibility(
                visible = lyric != null && lyric!!.translatedLyrics.isNotEmpty() && showTranslation,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mode Toggle (Switch views)
                    Button(
                        onClick = { viewModel.setTranslationMode(TranslationMode.TOGGLE) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (translationMode == TranslationMode.TOGGLE) Color(0xFF6750A4).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (translationMode == TranslationMode.TOGGLE) Color(0xFFFFB4AB) else Color.White.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("Toggle Terjemahan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Mode Side-by-Side (original & translated side by side)
                    Button(
                        onClick = { viewModel.setTranslationMode(TranslationMode.SIDE_BY_SIDE) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (translationMode == TranslationMode.SIDE_BY_SIDE) Color(0xFF6750A4).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (translationMode == TranslationMode.SIDE_BY_SIDE) Color(0xFFFFB4AB) else Color.White.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("Bilingual (Side-by-Side)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.05f))

            // 3. Main Lyrics Viewer Box
            LyricDisplay(
                lyric = lyric,
                isSearching = isSearching,
                error = lyricError,
                showTranslation = showTranslation,
                translationMode = translationMode,
                modifier = Modifier.weight(1f),
                scrollState = scrollState
            )
        }
    }
}
