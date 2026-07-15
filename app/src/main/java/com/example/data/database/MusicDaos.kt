package com.example.data.database

import androidx.room.*
import com.example.data.model.Song
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Lyric
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM songs WHERE isLocal = 1")
    suspend fun clearLocalSongs()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: String, name: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(ref: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_song_cross_ref r ON s.id = r.songId 
        WHERE r.playlistId = :playlistId
    """)
    fun getSongsInPlaylist(playlistId: String): Flow<List<Song>>
}

@Dao
interface LyricCacheDao {
    @Query("SELECT * FROM lyric_cache WHERE songId = :songId LIMIT 1")
    suspend fun getLyricForSong(songId: String): Lyric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheLyric(lyric: Lyric)

    @Query("DELETE FROM lyric_cache")
    suspend fun clearCache()
}
