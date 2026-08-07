package com.primemusic.app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.MainActivity
import com.primemusic.app.R
import com.primemusic.app.adapter.SongAdapter
import com.primemusic.app.util.PrefsManager
import com.primemusic.app.util.SongLibrary

class PlaylistDetailFragment : Fragment(R.layout.fragment_playlist_detail) {

    companion object {
        private const val ARG_PLAYLIST_NAME = "playlist_name"
        fun newInstance(playlistName: String) = PlaylistDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_PLAYLIST_NAME, playlistName) }
        }
    }

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: SongAdapter
    private var playlistName: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistName = arguments?.getString(ARG_PLAYLIST_NAME) ?: ""

        view.findViewById<android.widget.TextView>(R.id.detail_title).text = playlistName
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recycler = view.findViewById(R.id.recycler_playlist_songs)
        emptyView = view.findViewById(R.id.empty_playlist_songs)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongAdapter(
            songs = emptyList(),
            isFavorite = { song -> PrefsManager.isFavorite(requireContext(), song.id) },
            onSongClick = { _, index ->
                val songs = adapter.currentList()
                (activity as? MainActivity)?.playSong(songs, index)
            },
            onMoreClick = { song, anchor ->
                val popup = android.widget.PopupMenu(requireContext(), anchor)
                popup.menu.add(0, 1, 0, "Remove from Playlist")
                popup.menu.add(0, 2, 1, "Song Info")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> {
                            PrefsManager.removeSongFromPlaylist(requireContext(), playlistName, song.id)
                            refresh()
                            true
                        }
                        2 -> {
                            (activity as? MainActivity)?.showSongInfo(song)
                            true
                        }
                        else -> true
                    }
                }
                popup.show()
            },
            onFavoriteClick = { song, button ->
                val nowFav = PrefsManager.toggleFavorite(requireContext(), song.id)
                button.setImageResource(if (nowFav) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            }
        )
        recycler.adapter = adapter

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val playlist = PrefsManager.getPlaylists(requireContext()).find { it.name == playlistName }
        val songs = SongLibrary.findByIds(playlist?.songIds ?: emptyList())
        adapter.updateData(songs)
        recycler.visibility = if (songs.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
    }
}
