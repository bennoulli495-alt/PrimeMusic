package com.primemusic.app.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.primemusic.app.model.Song

/**
 * Scans the entire device storage for playable audio files using MediaStore,
 * which is the standard, permission-respecting way to discover media on Android.
 */
object MediaScanner {

    fun scanAudioFiles(context: Context): List<Song> {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Only music files, exclude things like ringtones/notification sounds/short clips
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val duration = cursor.getLong(durationCol)
                val path = cursor.getString(dataCol) ?: ""
                val size = cursor.getLong(sizeCol)
                val albumId = cursor.getLong(albumIdCol)

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        filePath = path,
                        fileSize = size,
                        albumArtUri = albumArtUri
                    )
                )
            }
        }

        return songs
    }

    fun getContentUriForSong(songId: Long): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songId
        )
    }
}
