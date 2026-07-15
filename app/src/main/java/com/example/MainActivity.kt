package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.lifecycleScope
import com.example.ui.screen.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.widget.MiniPlayer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class PlayerTab {
    HOME,
    PLAYLIST,
    SEARCH,
    PROFILE
}

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Amati Toast messages dari ViewModel
        lifecycleScope.launch {
            viewModel.toastMessage.collectLatest { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MusicViewModel) {
    var currentTab by remember { mutableStateOf(PlayerTab.HOME) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // Hitung persentase progress untuk mini player
    val progress = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0.0f
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showFullPlayer && !showLyricsView) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    NavigationBarItem(
                        selected = currentTab == PlayerTab.HOME,
                        onClick = { currentTab = PlayerTab.HOME },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == PlayerTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentTab == PlayerTab.PLAYLIST,
                        onClick = { currentTab = PlayerTab.PLAYLIST },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == PlayerTab.PLAYLIST) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Playlist"
                            )
                        },
                        label = { Text("Playlist") },
                        modifier = Modifier.testTag("nav_playlist")
                    )
                    NavigationBarItem(
                        selected = currentTab == PlayerTab.SEARCH,
                        onClick = { currentTab = PlayerTab.SEARCH },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == PlayerTab.SEARCH) Icons.Default.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search") },
                        modifier = Modifier.testTag("nav_search")
                    )
                    NavigationBarItem(
                        selected = currentTab == PlayerTab.PROFILE,
                        onClick = { currentTab = PlayerTab.PROFILE },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == PlayerTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                                contentDescription = "Profile"
                            )
                        },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showLyricsView) {
                LyricScreen(
                    viewModel = viewModel,
                    onBackClick = { showLyricsView = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (showFullPlayer) {
                PlayerScreen(
                    viewModel = viewModel,
                    onBackClick = { showFullPlayer = false },
                    onLyricsClick = { showLyricsView = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    // Main tab screens selector
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            PlayerTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onSongClick = { showFullPlayer = true }
                            )
                            PlayerTab.PLAYLIST -> PlaylistScreen(
                                viewModel = viewModel,
                                onSongClick = { showFullPlayer = true }
                            )
                            PlayerTab.SEARCH -> SearchScreen(
                                viewModel = viewModel,
                                onSongClick = { showFullPlayer = true }
                            )
                            PlayerTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel
                            )
                        }
                    }

                    // Bottom Mini Player
                    if (currentSong != null) {
                        MiniPlayer(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            progress = progress,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onSkipNextClick = { viewModel.skipNext() },
                            onPlayerClick = { showFullPlayer = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
