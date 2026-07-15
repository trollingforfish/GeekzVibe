package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.Song
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Lyric
import com.example.data.service.LyricService
import com.example.data.service.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val lyricCacheDao = db.lyricCacheDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    companion object {
        private const val TAG = "MusicRepository"
    }

    /**
     * Memasukkan lagu streaming bawaan (seed data) jika database kosong.
     */
    suspend fun seedDefaultSongs() = withContext(Dispatchers.IO) {
        val currentSongs = allSongs.first()
        if (currentSongs.isEmpty()) {
            val defaultSongs = listOf(
                Song(
                    id = "seed_1",
                    title = "Starlight Serenade",
                    artist = "The Neon Dreamers",
                    album = "Midnight City Collection",
                    duration = 292000L, // 04:52
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    albumArt = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?auto=format&fit=crop&q=80&w=600",
                    isLocal = false
                ),
                Song(
                    id = "seed_2",
                    title = "Cosmic Whispers",
                    artist = "Lofi Eclipse",
                    album = "Chilled Universe",
                    duration = 425000L, // 07:05
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    albumArt = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&q=80&w=600",
                    isLocal = false
                ),
                Song(
                    id = "seed_3",
                    title = "Midnight Drive",
                    artist = "Retro Wave",
                    album = "Neon Horizon",
                    duration = 344000L, // 05:44
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    albumArt = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600",
                    isLocal = false
                ),
                Song(
                    id = "seed_4",
                    title = "Ocean Breeze",
                    artist = "Summer Breeze",
                    album = "Island Escape",
                    duration = 302000L, // 05:02
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    albumArt = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?auto=format&fit=crop&q=80&w=600",
                    isLocal = false
                ),
                Song(
                    id = "seed_5",
                    title = "Echoes of Silence",
                    artist = "Aura",
                    album = "Acoustic Whispers",
                    duration = 264000L, // 04:24
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    albumArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600",
                    isLocal = false
                )
            )
            songDao.insertSongs(defaultSongs)
            
            // Buat playlist bawaan "Lagu Favorit Saya"
            val defaultPlaylist = Playlist("fav_playlist", "Lagu Favorit Saya", "Kumpulan lagu-lagu kesukaanmu")
            playlistDao.insertPlaylist(defaultPlaylist)
        }
    }

    /**
     * Memindai file musik lokal (.mp3, .m4a, .wav) dari MediaStore.
     */
    suspend fun scanLocalSongs(): Int = withContext(Dispatchers.IO) {
        val localSongs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Lagu Tidak Dikenal"
                    val artist = cursor.getString(artistColumn) ?: "<Unknown Artist>"
                    val album = cursor.getString(albumColumn) ?: "<Unknown Album>"
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Gambar default lofi jika lokal
                    val albumArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&q=80&w=200"

                    localSongs.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            path = contentUri.ifEmpty { path },
                            albumArt = albumArtUrl,
                            isLocal = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memindai MediaStore: ${e.message}", e)
        }

        if (localSongs.isNotEmpty()) {
            songDao.insertSongs(localSongs)
        }
        return@withContext localSongs.size
    }

    // --- Song Operations ---
    suspend fun toggleFavorite(songId: String, isFavorite: Boolean) {
        songDao.updateFavorite(songId, isFavorite)
    }

    // --- Playlist Operations ---
    suspend fun createPlaylist(name: String, description: String = "") {
        val id = "playlist_${System.currentTimeMillis()}"
        playlistDao.insertPlaylist(Playlist(id, name, description))
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun renamePlaylist(id: String, name: String) {
        playlistDao.renamePlaylist(id, name)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistDao.insertPlaylistSong(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsInPlaylist(playlistId: String): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    // --- Lyric & Translation Operations ---
    /**
     * Mengambil lirik dari database cache lokal. Jika belum ada, cari online dan simpan ke cache.
     */
    suspend fun getLyrics(song: Song): Lyric? = withContext(Dispatchers.IO) {
        val cached = lyricCacheDao.getLyricForSong(song.id)
        if (cached != null) {
            return@withContext cached
        }

        // Tidak ada di cache, cari dari 3 sumber
        val result = LyricService.findLyrics(song.title, song.artist, song.id)
        if (result.lyrics.isNotEmpty()) {
            val newLyric = Lyric(
                songId = song.id,
                originalLyrics = result.lyrics,
                translatedLyrics = "",
                source = result.source
            )
            lyricCacheDao.cacheLyric(newLyric)
            return@withContext newLyric
        }
        return@withContext null
    }

    /**
     * Menerjemahkan lirik yang sudah ada ke Bahasa Indonesia dan mengupdate cache.
     */
    suspend fun translateAndCacheLyrics(songId: String, originalLyrics: String): Lyric = withContext(Dispatchers.IO) {
        val translated = TranslationService.translateLyrics(originalLyrics)
        val cached = lyricCacheDao.getLyricForSong(songId)
        val updated = if (cached != null) {
            cached.copy(translatedLyrics = translated)
        } else {
            Lyric(songId = songId, originalLyrics = originalLyrics, translatedLyrics = translated, source = "Gemini AI Translator")
        }
        lyricCacheDao.cacheLyric(updated)
        return@withContext updated
    }
}
