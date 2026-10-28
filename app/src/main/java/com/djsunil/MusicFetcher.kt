package com.djsunil

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setIsPlayable(true)
                .build()
        )
        .build()
}

class MusicFetcher(private val context: Context) {

    suspend fun fetchSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"

        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val artist = c.getString(artistCol)
                    val album = c.getString(albumCol)
                    songs.add(
                        Song(
                            id = id,
                            title = c.getString(titleCol) ?: "Unknown title",
                            artist = if (artist.isNullOrBlank() || artist == MediaStore.UNKNOWN_STRING) "Unknown artist" else artist,
                            album = if (album.isNullOrBlank() || album == MediaStore.UNKNOWN_STRING) "Unknown album" else album,
                            durationMs = c.getLong(durCol),
                            uri = ContentUris.withAppendedId(collection, id)
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission missing: return whatever we have (empty).
        } catch (_: Exception) {
        }
        songs.sortBy { it.title.lowercase() }
        songs
    }
}
