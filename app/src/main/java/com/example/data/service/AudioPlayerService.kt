package com.example.data.service

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioPlayerService {
    private const val TAG = "AudioPlayerService"

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // States
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Equalizer States (Bands: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz)
    private val _eqBands = MutableStateFlow(listOf(50, 50, 50, 50, 50)) // 0 to 100 range
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    private val _isEqEnabled = MutableStateFlow(true)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled.asStateFlow()

    // Playlist/Queue
    private var playlistSongs: List<Song> = emptyList()
    private var currentIndex: Int = -1

    fun initPlayer(context: Context) {
        if (exoPlayer != null) return

        exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
            volume = _volume.value
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                    _isPlaying.value = isPlayingChanged
                    if (isPlayingChanged) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _duration.value = duration
                        }
                        Player.STATE_ENDED -> {
                            handlePlaybackEnded()
                        }
                        Player.STATE_BUFFERING -> {}
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "ExoPlayer Error: ${error.message}", error)
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            })
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int) {
        playlistSongs = songs
        currentIndex = startIndex
        if (startIndex in playlistSongs.indices) {
            playSong(playlistSongs[startIndex])
        }
    }

    fun playSong(song: Song) {
        exoPlayer?.let { player ->
            _currentSong.value = song
            currentIndex = playlistSongs.indexOfFirst { it.id == song.id }

            val mediaItem = MediaItem.fromUri(song.path)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        exoPlayer?.volume = clamped
    }

    fun toggleShuffle() {
        val nextMode = !_shuffleModeEnabled.value
        _shuffleModeEnabled.value = nextMode
        // In simple queue logic, we simulate shuffle behavior when picking next
    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
    }

    fun skipNext() {
        if (playlistSongs.isEmpty()) return

        if (_shuffleModeEnabled.value) {
            currentIndex = playlistSongs.indices.random()
        } else {
            currentIndex = (currentIndex + 1) % playlistSongs.size
        }

        if (currentIndex in playlistSongs.indices) {
            playSong(playlistSongs[currentIndex])
        }
    }

    fun skipPrevious() {
        if (playlistSongs.isEmpty()) return

        currentIndex = if (currentIndex - 1 < 0) {
            playlistSongs.size - 1
        } else {
            currentIndex - 1
        }

        if (currentIndex in playlistSongs.indices) {
            playSong(playlistSongs[currentIndex])
        }
    }

    private fun handlePlaybackEnded() {
        when (_repeatMode.value) {
            Player.REPEAT_MODE_ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            Player.REPEAT_MODE_ALL -> {
                skipNext()
            }
            else -> {
                if (currentIndex + 1 < playlistSongs.size) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    // Equalizer controls
    fun toggleEq(enabled: Boolean) {
        _isEqEnabled.value = enabled
    }

    fun updateEqBand(index: Int, value: Int) {
        val current = _eqBands.value.toMutableList()
        if (index in current.indices) {
            current[index] = value.coerceIn(0, 100)
            _eqBands.value = current
        }
    }

    fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
    }
}
