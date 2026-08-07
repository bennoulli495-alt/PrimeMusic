package com.primemusic.app.util

import android.content.Context
import com.primemusic.app.model.Song

/**
 * Simple in-memory cache so Songs / Search / Playlists / Home tabs
 * don't each independently re-scan MediaStore.
 */
object SongLibrary {

    private var cache: List<Song> = emptyList()
    private var loaded = false

    fun getAll(context: Context, forceRefresh: Boolean = false): List<Song> {
        if (!loaded || forceRefresh) {
            cache = MediaScanner.scanAudioFiles(context)
            loaded = true
        }
        return cache
    }

    fun findById(songId: Long): Song? = cache.find { it.id == songId }

    fun findByIds(ids: List<Long>): List<Song> {
        val map = cache.associateBy { it.id }
        return ids.mapNotNull { map[it] }
    }

    fun search(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return cache.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
    }
}
