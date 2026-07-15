package com.example.ui.screen

import androidx.compose.animation.core.*
import androidx.media3.common.Player
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.widget.formatDuration

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val shuffleActive by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0B0C)),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada lagu yang sedang diputar", color = Color(0xFFE6E1E3))
        }
        return
    }

    val song = currentSong!!

    // Pulse effect animation for album art glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B0C)) // Immersive black
            .testTag("player_screen")
    ) {
        // --- AMBIENT GLOW BLOBS ---
        // Top Left Purple Blob
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-80).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(Color(0xFF6750A4).copy(alpha = 0.3f))
                .blur(100.dp)
        )

        // Center Right Coral Blob
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = 100.dp)
                .size(320.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB4AB).copy(alpha = 0.25f))
                .blur(120.dp)
        )

        // --- CORE INTERFACE LAYOUT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Kembali",
                        tint = Color(0xFFE6E1E3)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E3).copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = song.album,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6E1E3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { /* Menu Opsi tambahan */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        tint = Color(0xFFE6E1E3)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // 2. Album Art with Pulsating Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Blurred Ambient Shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .scale(pulseScale)
                        .blur(40.dp)
                        .background(Color(0xFF6750A4).copy(alpha = 0.4f), shape = RoundedCornerShape(48.dp))
                )

                // Main Album Art Image
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(24.dp, shape = RoundedCornerShape(48.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(48.dp))
                        .clip(RoundedCornerShape(48.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // 3. Track Details (Title & Artist) + Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE6E1E3).copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite(song) },
                    modifier = Modifier.size(48.dp).testTag("fav_toggle")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Suka",
                        tint = if (song.isFavorite) Color(0xFFFFB4AB) else Color(0xFFE6E1E3),
                        modifier = Modifier.scale(1.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Progress Bar & Seek Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val sliderPosition = remember(position) { position.toFloat() }
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("progress_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(position),
                        fontSize = 11.sp,
                        color = Color(0xFFE6E1E3).copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDuration(duration),
                        fontSize = 11.sp,
                        color = Color(0xFFE6E1E3).copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Volume Control Slider (Full Integration)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Mute",
                    tint = Color(0xFFE6E1E3).copy(alpha = 0.4f)
                )
                Slider(
                    value = volume,
                    onValueChange = { viewModel.setVolume(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.weight(1f).testTag("volume_slider")
                )
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = "Volume Keras",
                    tint = Color(0xFFE6E1E3).copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Playback Control Row (Shuffle, Prev, PlayBox, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Mode Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleActive) Color(0xFFFFB4AB) else Color(0xFFE6E1E3).copy(alpha = 0.4f)
                    )
                }

                // Skip Previous Button
                IconButton(
                    onClick = { viewModel.skipPrevious() },
                    modifier = Modifier.testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = Color(0xFFE6E1E3),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Circle Box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .shadow(16.dp, shape = RoundedCornerShape(32.dp))
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF0D0B0C),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Skip Next Button
                IconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Selanjutnya",
                        tint = Color(0xFFE6E1E3),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    val tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFFFB4AB) else Color(0xFFE6E1E3).copy(alpha = 0.4f)
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = tint
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // 7. Footer Lyrics Swipe-up / Quick View panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable(onClick = onLyricsClick)
                    .testTag("quick_lyrics_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Small slide handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Lirik",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LYRICS & TERJEMAHAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E3),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "BUKA LAYAR PENUH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4),
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preview of the lyrics
                    Text(
                        text = "Ketuk untuk melihat lirik lagu lengkap dan hasil terjemahan Bahasa Indonesia...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE6E1E3).copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
