package com.primemusic.app.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.MainActivity
import com.primemusic.app.R
import com.primemusic.app.adapter.SongAdapter
import com.primemusic.app.model.Song
import com.primemusic.app.util.PrefsManager
import com.primemusic.app.util.SongLibrary

class SongsFragment : Fragment(R.layout.fragment_songs) {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: SongAdapter
    private var allSongs: List<Song> = emptyList()

    private val permission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_songs)
        emptyState = view.findViewById(R.id.empty_state)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongAdapter(
            songs = emptyList(),
            isFavorite = { song -> PrefsManager.isFavorite(requireContext(), song.id) },
            onSongClick = { song, index ->
                (activity as? MainActivity)?.playSong(allSongs, index)
            },
            onMoreClick = { song, anchor ->
                showSongMenu(song, anchor)
            },
            onFavoriteClick = { song, button ->
                toggleFavorite(song, button)
            }
        )
        recycler.adapter = adapter

        view.findViewById<View>(R.id.btn_grant_permission).setOnClickListener {
            (activity as? MainActivity)?.requestAudioPermission()
        }

        loadSongsIfPermitted()
    }

    override fun onResume() {
        super.onResume()
        loadSongsIfPermitted()
    }

    private fun loadSongsIfPermitted() {
        if (!isAdded) return
        val granted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            allSongs = SongLibrary.getAll(requireContext())
            adapter.updateData(allSongs)
            recycler.visibility = if (allSongs.isEmpty()) View.GONE else View.VISIBLE
            emptyState.visibility = if (allSongs.isEmpty()) View.VISIBLE else View.GONE
        } else {
            recycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        }
    }

    private fun toggleFavorite(song: Song, button: ImageButton) {
        val nowFavorite = PrefsManager.toggleFavorite(requireContext(), song.id)
        button.setImageResource(if (nowFavorite) R.drawable.ic_heart else R.drawable.ic_heart_outline)
    }

    private fun showSongMenu(song: Song, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        val isFav = PrefsManager.isFavorite(requireContext(), song.id)
        popup.menu.add(0, 1, 0, "Add to Playlist")
        popup.menu.add(0, 2, 1, if (isFav) "Remove from Favorites" else "Add to Favorites")
        popup.menu.add(0, 3, 2, "Song Info")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    (activity as? MainActivity)?.showAddToPlaylistDialog(song)
                    true
                }
                2 -> {
                    PrefsManager.toggleFavorite(requireContext(), song.id)
                    adapter.notifyDataSetChanged()
                    true
                }
                3 -> {
                    (activity as? MainActivity)?.showSongInfo(song)
                    true
                }
                else -> true
            }
        }
        popup.show()
    }
}
