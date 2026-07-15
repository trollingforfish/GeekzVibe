package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.widget.SongTile

@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }

    var showAddSongsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (selectedPlaylist == null) {
            // --- VIEW 1: DAFTAR PLAYLIST ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Playlist Saya",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Kelola kumpulan musik terbaikmu",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                    }

                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape)
                            .testTag("create_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat Playlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Grid/List Playlists
                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum ada playlist.",
                                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Mulai buat playlist pertamamu dengan mengetuk tombol +",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPlaylist(playlist) }
                                    .testTag("playlist_card_${playlist.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Folder Icon Container
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = "Playlist Folder",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (playlist.description.isNotEmpty()) {
                                            Text(
                                                text = playlist.description,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Action Delete
                                    IconButton(
                                        onClick = { viewModel.deletePlaylist(playlist.id) },
                                        modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Playlist",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- VIEW 2: DETAIL PLAYLIST ---
            val playlist = selectedPlaylist!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                // Detail Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectPlaylist(null) },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Kembali ke Playlist",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (playlist.description.isNotEmpty()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            )
                        }
                    }

                    // Add Song to Playlist Button
                    IconButton(
                        onClick = { showAddSongsDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .size(40.dp)
                            .testTag("add_song_to_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah lagu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playlist songs list
                if (playlistSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Playlist ini masih kosong.",
                                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Ketuk tombol + di kanan atas untuk menambahkan lagu favoritmu ke playlist ini.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlistSongs) { song ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongTile(
                                        song = song,
                                        onClick = {
                                            viewModel.playQueue(playlistSongs, playlistSongs.indexOf(song))
                                            onSongClick(song)
                                        },
                                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                                        onAddToPlaylistClick = null,
                                        isPlaying = currentSong?.id == song.id
                                    )
                                }

                                // Remove song from playlist
                                IconButton(
                                    onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                                    modifier = Modifier.testTag("remove_song_${song.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Hapus dari playlist",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG BUAT PLAYLIST BARU ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Playlist Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Nama Playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                    )

                    OutlinedTextField(
                        value = playlistDescInput,
                        onValueChange = { playlistDescInput = it },
                        label = { Text("Deskripsi (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("playlist_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.trim().isNotEmpty()) {
                            viewModel.createPlaylist(playlistNameInput.trim(), playlistDescInput.trim())
                            playlistNameInput = ""
                            playlistDescInput = ""
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_playlist")
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- DIALOG TAMBAH LAGU KE PLAYLIST ---
    if (showAddSongsDialog && selectedPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            title = { Text("Tambahkan Lagu") },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    Text("Pilih lagu untuk ditambahkan ke playlist '${selectedPlaylist!!.name}':", modifier = Modifier.padding(bottom = 12.dp))

                    if (allSongs.isEmpty()) {
                        Text("Belum ada lagu yang tersedia.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allSongs) { song ->
                                val alreadyInPlaylist = playlistSongs.any { it.id == song.id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !alreadyInPlaylist) {
                                            viewModel.addSongToPlaylist(selectedPlaylist!!.id, song.id)
                                            // auto dismiss or update state
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, fontWeight = FontWeight.Bold, color = if (alreadyInPlaylist) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
                                        Text(song.artist, fontSize = 12.sp, color = if (alreadyInPlaylist) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }

                                    if (alreadyInPlaylist) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sudah Ada",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Tambah",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAddSongsDialog = false },
                    modifier = Modifier.testTag("dismiss_add_songs")
                ) {
                    Text("Selesai")
                }
            }
        )
    }
}
