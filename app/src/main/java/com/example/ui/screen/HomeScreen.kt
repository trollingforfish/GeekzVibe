package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.widget.SongTile
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val sleepTimerActive by viewModel.isSleepTimerActive.collectAsState()
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }

    // Dapatkan salam berdasarkan waktu lokal
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Selamat Pagi"
            in 12..15 -> "Selamat Siang"
            in 16..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Efek Gradasi Latar Belakang Imersif
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Mulai harimu dengan melodi indah",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Pencarian Terintegrasi
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar"),
                placeholder = { Text("Cari lagu, artis, atau album...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Kontrol Premium (Scan, Sleep Timer, Equalizer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scan Button
                Button(
                    onClick = { viewModel.scanLocalSongs() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Lagu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Sleep Timer Button
                Button(
                    onClick = { showTimerDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("timer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (sleepTimerActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerActive) formatSleepTime(sleepRemaining) else "Sleep Timer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Equalizer Button
                Button(
                    onClick = { showEqDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("eq_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = "EQ")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Equalizer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Judul List Lagu
            Text(
                text = if (searchQuery.isNotEmpty()) "Hasil Pencarian" else "Semua Lagu",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Daftar Lagu dengan LazyColumn
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Lagu tidak ditemukan" else "Tidak ada lagu. Klik 'Scan Lagu' untuk memindai.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp), // Beri jarak untuk mini player
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(filteredSongs) { index, song ->
                        SongTile(
                            song = song,
                            onClick = {
                                viewModel.playQueue(filteredSongs, index)
                                onSongClick(song)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            onAddToPlaylistClick = null, // Di handle di halaman playlist
                            isPlaying = currentSong?.id == song.id
                        )
                    }
                }
            }
        }

        // Overlay Loading saat Scanning
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memindai Penyimpanan...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mencari file audio .mp3, .m4a, .wav",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG SLEEP TIMER ---
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Set Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih waktu untuk menghentikan pemutar musik secara otomatis:")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30).forEach { mins ->
                            Button(
                                onClick = {
                                    viewModel.setSleepTimer(mins)
                                    showTimerDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("${mins}m")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(45, 60).forEach { mins ->
                            Button(
                                onClick = {
                                    viewModel.setSleepTimer(mins)
                                    showTimerDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("${mins}m")
                            }
                        }
                    }

                    if (sleepTimerActive) {
                        Button(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Batalkan Timer")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // --- DIALOG EQUALIZER ---
    if (showEqDialog) {
        val eqEnabled by viewModel.isEqEnabled.collectAsState()
        val eqBandsValue by viewModel.eqBands.collectAsState()
        val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")

        AlertDialog(
            onDismissRequest = { showEqDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Graphic Equalizer")
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { viewModel.toggleEqualizer(it) }
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sesuaikan band frekuensi audio untuk efek suara terbaik.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    frequencies.forEachIndexed { index, freq ->
                        val bandVal = eqBandsValue.getOrElse(index) { 50 }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(freq, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${bandVal - 50} dB", fontSize = 12.sp)
                            }
                            Slider(
                                value = bandVal.toFloat(),
                                onValueChange = { viewModel.updateEqualizerBand(index, it.toInt()) },
                                valueRange = 0f..100f,
                                enabled = eqEnabled,
                                steps = 10
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqDialog = false }) {
                    Text("Selesai")
                }
            }
        )
    }
}

// Format sisa waktu sleep timer (mm:ss)
fun formatSleepTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
