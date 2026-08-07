package com.primemusic.app.fragments

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

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recentAdapter: SongAdapter
    private lateinit var mostPlayedAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SongLibrary.getAll(requireContext())

        recentAdapter = SongAdapter(
            songs = emptyList(),
            isFavorite = { song -> PrefsManager.isFavorite(requireContext(), song.id) },
            onSongClick = { _, index -> (activity as? MainActivity)?.playSong(recentAdapter.currentList(), index) },
            onMoreClick = { song, _ -> (activity as? MainActivity)?.showSongInfo(song) },
            onFavoriteClick = { song, button ->
                val nowFav = PrefsManager.toggleFavorite(requireContext(), song.id)
                button.setImageResource(if (nowFav) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            }
        )
        view.findViewById<RecyclerView>(R.id.recycler_recent).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAdapter
        }

        mostPlayedAdapter = SongAdapter(
            songs = emptyList(),
            isFavorite = { song -> PrefsManager.isFavorite(requireContext(), song.id) },
            onSongClick = { _, index -> (activity as? MainActivity)?.playSong(mostPlayedAdapter.currentList(), index) },
            onMoreClick = { song, _ -> (activity as? MainActivity)?.showSongInfo(song) },
            onFavoriteClick = { song, button ->
                val nowFav = PrefsManager.toggleFavorite(requireContext(), song.id)
                button.setImageResource(if (nowFav) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            }
        )
        view.findViewById<RecyclerView>(R.id.recycler_most_played).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mostPlayedAdapter
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val recentIds = PrefsManager.getRecentlyPlayed(requireContext())
        val recentSongs = SongLibrary.findByIds(recentIds)
        recentAdapter.updateData(recentSongs)
        view?.findViewById<View>(R.id.recent_empty)?.visibility =
            if (recentSongs.isEmpty()) View.VISIBLE else View.GONE
        view?.findViewById<RecyclerView>(R.id.recycler_recent)?.visibility =
            if (recentSongs.isEmpty()) View.GONE else View.VISIBLE

        val mostPlayedIds = PrefsManager.getMostPlayed(requireContext())
        val mostPlayedSongs = SongLibrary.findByIds(mostPlayedIds)
        mostPlayedAdapter.updateData(mostPlayedSongs)
        view?.findViewById<View>(R.id.most_played_empty)?.visibility =
            if (mostPlayedSongs.isEmpty()) View.VISIBLE else View.GONE
        view?.findViewById<RecyclerView>(R.id.recycler_most_played)?.visibility =
            if (mostPlayedSongs.isEmpty()) View.GONE else View.VISIBLE
    }
}
