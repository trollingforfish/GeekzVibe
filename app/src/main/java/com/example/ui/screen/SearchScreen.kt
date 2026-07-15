package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.widget.SongTile

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    val trendingGenres = listOf(
        GenreItem("Pop Melodis", Color(0xFF6750A4)),
        GenreItem("Lofi Chillout", Color(0xFF9C27B0)),
        GenreItem("Retro Wave", Color(0xFFE91E63)),
        GenreItem("Akustik Tenang", Color(0xFF009688)),
        GenreItem("Indie Indonesia", Color(0xFF4CAF50)),
        GenreItem("Electronic Synth", Color(0xFFFF9800))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = "Eksplor Musik",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Temukan melodi favorit dan lirik puitis",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_search_bar"),
            placeholder = { Text("Cari lagu, penyanyi, atau album...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (searchQuery.isEmpty()) {
            // Show trending genres grid
            Text(
                text = "Genre Terpopuler",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(trendingGenres) { genre ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(genre.color)
                            .clickable { viewModel.updateSearchQuery(genre.name) }
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = genre.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Show search results list
            Text(
                text = "Hasil Pencarian",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada lagu yang cocok dengan kata pencarian 😢",
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
                    contentPadding = PaddingValues(bottom = 80.dp),
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
                            onAddToPlaylistClick = null,
                            isPlaying = currentSong?.id == song.id
                        )
                    }
                }
            }
        }
    }
}

data class GenreItem(
    val name: String,
    val color: Color
)
