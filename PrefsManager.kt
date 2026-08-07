package com.primemusic.app.util

import android.content.Context
import com.primemusic.app.model.Playlist
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight persistence layer backed by SharedPreferences + JSON.
 * Keeps things simple for now (no database) while still surviving app restarts.
 */
object PrefsManager {

    private const val PREFS_NAME = "prime_music_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_RECENTS = "recents"
    private const val KEY_PLAY_COUNTS = "play_counts"
    private const val MAX_RECENTS = 30

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Favorites ----------

    fun getFavorites(context: Context): MutableSet<Long> {
        val raw = prefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toMutableSet()
    }

    fun isFavorite(context: Context, songId: Long): Boolean =
        getFavorites(context).contains(songId)

    fun toggleFavorite(context: Context, songId: Long): Boolean {
        val favs = getFavorites(context)
        val nowFavorite = if (favs.contains(songId)) {
            favs.remove(songId)
            false
        } else {
            favs.add(songId)
            true
        }
        prefs(context).edit()
            .putStringSet(KEY_FAVORITES, favs.map { it.toString() }.toSet())
            .apply()
        return nowFavorite
    }

    // ---------- Playlists ----------

    fun getPlaylists(context: Context): MutableList<Playlist> {
        val raw = prefs(context).getString(KEY_PLAYLISTS, null) ?: return mutableListOf()
        val result = mutableListOf<Playlist>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            val ids = mutableListOf<Long>()
            val idsArr = obj.getJSONArray("songIds")
            for (j in 0 until idsArr.length()) ids.add(idsArr.getLong(j))
            result.add(Playlist(name, ids))
        }
        return result
    }

    private fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("name", playlist.name)
            obj.put("songIds", JSONArray(playlist.songIds))
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    fun createPlaylist(context: Context, name: String): Boolean {
        val playlists = getPlaylists(context)
        if (playlists.any { it.name.equals(name, ignoreCase = true) }) return false
        playlists.add(Playlist(name))
        savePlaylists(context, playlists)
        return true
    }

    fun deletePlaylist(context: Context, name: String) {
        val playlists = getPlaylists(context)
        playlists.removeAll { it.name == name }
        savePlaylists(context, playlists)
    }

    fun addSongToPlaylist(context: Context, playlistName: String, songId: Long) {
        val playlists = getPlaylists(context)
        val playlist = playlists.find { it.name == playlistName } ?: return
        if (!playlist.songIds.contains(songId)) {
            playlist.songIds.add(songId)
            savePlaylists(context, playlists)
        }
    }

    fun removeSongFromPlaylist(context: Context, playlistName: String, songId: Long) {
        val playlists = getPlaylists(context)
        val playlist = playlists.find { it.name == playlistName } ?: return
        playlist.songIds.remove(songId)
        savePlaylists(context, playlists)
    }

    // ---------- Recently played ----------

    fun getRecentlyPlayed(context: Context): List<Long> {
        val raw = prefs(context).getString(KEY_RECENTS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getLong(it) }
    }

    fun recordPlayed(context: Context, songId: Long) {
        val recents = getRecentlyPlayed(context).toMutableList()
        recents.remove(songId)
        recents.add(0, songId)
        while (recents.size > MAX_RECENTS) recents.removeAt(recents.size - 1)
        prefs(context).edit().putString(KEY_RECENTS, JSONArray(recents).toString()).apply()

        // Bump play count too
        val counts = getPlayCounts(context).toMutableMap()
        counts[songId] = (counts[songId] ?: 0) + 1
        val obj = JSONObject()
        counts.forEach { (id, count) -> obj.put(id.toString(), count) }
        prefs(context).edit().putString(KEY_PLAY_COUNTS, obj.toString()).apply()
    }

    fun getPlayCounts(context: Context): Map<Long, Int> {
        val raw = prefs(context).getString(KEY_PLAY_COUNTS, null) ?: return emptyMap()
        val obj = JSONObject(raw)
        val map = mutableMapOf<Long, Int>()
        obj.keys().forEach { key -> map[key.toLong()] = obj.getInt(key) }
        return map
    }

    fun getMostPlayed(context: Context, limit: Int = 10): List<Long> {
        return getPlayCounts(context).entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}
