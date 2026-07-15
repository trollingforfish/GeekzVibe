package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.example.data.model.Playlist
import com.example.data.model.Lyric
import com.example.data.repository.MusicRepository
import com.example.data.service.AudioPlayerService
import com.example.data.service.GeminiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class TranslationMode {
    SIDE_BY_SIDE,
    TOGGLE
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val playerService = AudioPlayerService

    // --- Firebase Auth & Firestore States ---
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Custom Gemini API Key State ---
    private val _customGeminiApiKey = MutableStateFlow("")
    val customGeminiApiKey: StateFlow<String> = _customGeminiApiKey.asStateFlow()

    // --- Data Flows ---
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // --- Search/Filter State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isEmpty()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Player States ---
    val currentSong: StateFlow<Song?> = playerService.currentSong
    val isPlaying: StateFlow<Boolean> = playerService.isPlaying
    val duration: StateFlow<Long> = playerService.duration
    val currentPosition: StateFlow<Long> = playerService.currentPosition
    val volume: StateFlow<Float> = playerService.volume
    val shuffleModeEnabled: StateFlow<Boolean> = playerService.shuffleModeEnabled
    val repeatMode: StateFlow<Int> = playerService.repeatMode

    // --- Equalizer States ---
    val isEqEnabled: StateFlow<Boolean> = playerService.isEqEnabled
    val eqBands: StateFlow<List<Int>> = playerService.eqBands

    // --- Lyrics & Translation States ---
    private val _currentLyrics = MutableStateFlow<Lyric?>(null)
    val currentLyrics: StateFlow<Lyric?> = _currentLyrics.asStateFlow()

    private val _isSearchingLyrics = MutableStateFlow(false)
    val isSearchingLyrics: StateFlow<Boolean> = _isSearchingLyrics.asStateFlow()

    private val _lyricError = MutableStateFlow<String?>(null)
    val lyricError: StateFlow<String?> = _lyricError.asStateFlow()

    private val _translationMode = MutableStateFlow(TranslationMode.TOGGLE)
    val translationMode: StateFlow<TranslationMode> = _translationMode.asStateFlow()

    private val _showTranslation = MutableStateFlow(false)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    // --- Sleep Timer State ---
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow(0L) // Sisa waktu dalam milidetik
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private val _isSleepTimerActive = MutableStateFlow(false)
    val isSleepTimerActive: StateFlow<Boolean> = _isSleepTimerActive.asStateFlow()

    // --- UI State ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        playerService.initPlayer(application)
        
        // Inisialisasi Firebase secara aman
        try {
            com.google.firebase.FirebaseApp.initializeApp(application)
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            _currentUser.value = auth?.currentUser
            observePremiumStatus()
        } catch (e: Exception) {
            Log.w("MusicViewModel", "Firebase default tidak terinisialisasi: ${e.message}. Mencoba inisialisasi dengan opsi tiruan untuk pencegahan error.")
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDummyKeyForGeekzVibeMusicPlayer")
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setProjectId("geekzvibe-dummy-project")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(application, options)
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                _currentUser.value = auth?.currentUser
                observePremiumStatus()
            } catch (ex: Exception) {
                Log.w("MusicViewModel", "Inisialisasi Firebase tiruan juga gagal: ${ex.message}")
            }
        }

        // Load custom Gemini API Key
        val sharedPrefs = application.getSharedPreferences("geekzvibe_prefs", Context.MODE_PRIVATE)
        val savedKey = sharedPrefs.getString("gemini_api_key", "") ?: ""
        _customGeminiApiKey.value = savedKey
        if (savedKey.isNotEmpty()) {
            GeminiService.setCustomApiKey(savedKey)
        }

        viewModelScope.launch {
            // Seed default royalty-free songs
            repository.seedDefaultSongs()
        }

        // Auto load lyrics when current song changes
        viewModelScope.launch {
            currentSong.collect { song ->
                _currentLyrics.value = null
                _lyricError.value = null
                _showTranslation.value = false
                if (song != null) {
                    fetchLyricsForSong(song)
                }
            }
        }
    }

    // --- Media Scanner ---
    fun scanLocalSongs() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.scanLocalSongs()
                _toastMessage.emit("Berhasil memindai $count lagu lokal")
            } catch (e: Exception) {
                _toastMessage.emit("Gagal memindai lagu lokal: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    // --- Song & Playlist CRUD ---
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFav = !song.isFavorite
            repository.toggleFavorite(song.id, newFav)
            
            // Sync current list / selected list if any
            _toastMessage.emit(if (newFav) "Ditambahkan ke Favorit" else "Dihapus dari Favorit")
        }
    }

    fun createPlaylist(name: String, desc: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, desc)
            _toastMessage.emit("Playlist '$name' berhasil dibuat")
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            _toastMessage.emit("Playlist dihapus")
        }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, name)
            _toastMessage.emit("Playlist diganti nama")
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getSongsInPlaylist(playlist.id).collect { songs ->
                    _playlistSongs.value = songs
                }
            }
        } else {
            _playlistSongs.value = emptyList()
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _toastMessage.emit("Lagu ditambahkan ke playlist")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _toastMessage.emit("Lagu dihapus dari playlist")
            // refresh selected list
            selectedPlaylist.value?.let { selectPlaylist(it) }
        }
    }

    // --- Lyrics & Translation Logic ---
    fun fetchLyricsForSong(song: Song) {
        viewModelScope.launch {
            _isSearchingLyrics.value = true
            _lyricError.value = null
            try {
                val lyric = repository.getLyrics(song)
                if (lyric != null) {
                    _currentLyrics.value = lyric
                } else {
                    _lyricError.value = "Lirik tidak ditemukan di semua sumber."
                }
            } catch (e: Exception) {
                _lyricError.value = "Error memuat lirik: ${e.message}"
            } finally {
                _isSearchingLyrics.value = false
            }
        }
    }

    fun translateLyrics() {
        val lyric = _currentLyrics.value ?: return
        if (lyric.translatedLyrics.isNotEmpty()) {
            _showTranslation.value = !_showTranslation.value
            return
        }

        viewModelScope.launch {
            _isSearchingLyrics.value = true
            try {
                _toastMessage.emit("Menerjemahkan lirik via Gemini AI...")
                val updatedLyric = repository.translateAndCacheLyrics(lyric.songId, lyric.originalLyrics)
                _currentLyrics.value = updatedLyric
                _showTranslation.value = true
            } catch (e: Exception) {
                _toastMessage.emit("Gagal menerjemahkan lirik: ${e.message}")
            } finally {
                _isSearchingLyrics.value = false
            }
        }
    }

    fun setTranslationMode(mode: TranslationMode) {
        _translationMode.value = mode
    }

    fun toggleShowTranslation() {
        _showTranslation.value = !_showTranslation.value
    }

    // --- Player Control Passthrough ---
    fun playQueue(songs: List<Song>, startIndex: Int) {
        playerService.setQueue(songs, startIndex)
    }

    fun togglePlayPause() = playerService.togglePlayPause()
    fun seekTo(positionMs: Long) = playerService.seekTo(positionMs)
    fun setVolume(vol: Float) = playerService.setVolume(vol)
    fun toggleShuffle() = playerService.toggleShuffle()
    fun cycleRepeatMode() = playerService.cycleRepeatMode()
    fun skipNext() = playerService.skipNext()
    fun skipPrevious() = playerService.skipPrevious()

    // --- Equalizer control ---
    fun toggleEqualizer(enabled: Boolean) = playerService.toggleEq(enabled)
    fun updateEqualizerBand(index: Int, value: Int) = playerService.updateEqBand(index, value)

    // --- Sleep Timer Logic ---
    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        val durationMs = minutes * 60 * 1000L
        _sleepTimerRemaining.value = durationMs
        _isSleepTimerActive.value = true

        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            // Timer habis, hentikan lagu
            if (isPlaying.value) {
                togglePlayPause()
            }
            _isSleepTimerActive.value = false
            _sleepTimerRemaining.value = 0
            _toastMessage.emit("Sleep Timer aktif: Lagu dihentikan.")
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _isSleepTimerActive.value = false
        _sleepTimerRemaining.value = 0
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Custom Gemini API Key Management ---
    fun saveGeminiApiKey(key: String) {
        _customGeminiApiKey.value = key
        val sharedPrefs = getApplication<Application>().getSharedPreferences("geekzvibe_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_api_key", key).apply()
        GeminiService.setCustomApiKey(key)
        viewModelScope.launch {
            _toastMessage.emit("API Key Gemini disimpan")
        }
    }

    // --- Firebase Auth & Firestore Management ---
    private fun observePremiumStatus() {
        val user = _currentUser.value
        if (user == null) {
            _isPremium.value = false
            return
        }
        
        val db = firestore
        if (db == null) {
            return
        }

        try {
            db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("MusicViewModel", "Gagal mendengar status premium: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val premiumStatus = snapshot.getBoolean("isPremium") ?: false
                        _isPremium.value = premiumStatus
                    } else {
                        _isPremium.value = false
                    }
                }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error observing premium status: ${e.message}")
        }
    }

    fun registerWithEmail(email: String, password: String) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            viewModelScope.launch {
                _toastMessage.emit("Firebase belum terkonfigurasi. Silakan ikuti panduan setup.")
            }
            return
        }

        _authError.value = null
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                initializeUserInFirestore(result.user?.uid)
                observePremiumStatus()
                viewModelScope.launch {
                    _toastMessage.emit("Pendaftaran berhasil!")
                }
            }
            .addOnFailureListener { e ->
                _authError.value = e.message
                viewModelScope.launch {
                    _toastMessage.emit("Gagal mendaftar: ${e.message}")
                }
            }
    }

    fun loginWithEmail(email: String, password: String) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            viewModelScope.launch {
                _toastMessage.emit("Firebase belum terkonfigurasi. Silakan ikuti panduan setup.")
            }
            return
        }

        _authError.value = null
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                observePremiumStatus()
                viewModelScope.launch {
                    _toastMessage.emit("Masuk berhasil!")
                }
            }
            .addOnFailureListener { e ->
                _authError.value = e.message
                viewModelScope.launch {
                    _toastMessage.emit("Gagal masuk: ${e.message}")
                }
            }
    }

    fun logout() {
        auth?.signOut()
        _currentUser.value = null
        _isPremium.value = false
        viewModelScope.launch {
            _toastMessage.emit("Keluar berhasil.")
        }
    }

    private fun initializeUserInFirestore(uid: String?) {
        val db = firestore
        if (uid == null || db == null) return
        val userMap = mapOf(
            "uid" to uid,
            "isPremium" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userMap)
            .addOnFailureListener { e ->
                Log.w("MusicViewModel", "Gagal inisialisasi user di Firestore: ${e.message}")
            }
    }

    fun simulatePremiumPurchase() {
        val user = _currentUser.value
        val db = firestore
        
        if (user == null) {
            viewModelScope.launch {
                _toastMessage.emit("Silakan daftar atau masuk terlebih dahulu.")
            }
            return
        }

        viewModelScope.launch {
            _toastMessage.emit("Memulai simulasi In-App Purchase...")
            delay(1500) // Simulate processing
            
            if (db != null) {
                val userRef = db.collection("users").document(user.uid)
                userRef.update("isPremium", true)
                    .addOnSuccessListener {
                        _isPremium.value = true
                        viewModelScope.launch {
                            _toastMessage.emit("Pembelian berhasil! Status premium Anda aktif.")
                        }
                    }
                    .addOnFailureListener { e ->
                        // Fallback jika rules Firestore belum disetup
                        _isPremium.value = true
                        viewModelScope.launch {
                            _toastMessage.emit("Simulasi Premium aktif (lokal): ${e.message}")
                        }
                    }
            } else {
                // Fallback jika Firebase belum terhubung
                _isPremium.value = true
                viewModelScope.launch {
                    _toastMessage.emit("Simulasi Premium aktif secara lokal!")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelSleepTimer()
    }
}
