package com.primemusic.app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.MainActivity
import com.primemusic.app.R
import com.primemusic.app.adapter.PlaylistAdapter
import com.primemusic.app.model.Playlist
import com.primemusic.app.util.PrefsManager
import com.primemusic.app.util.SongLibrary

class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {

    companion object {
        const val RECENTLY_ADDED_LIMIT = 25
    }

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: PlaylistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_playlists)
        emptyState = view.findViewById(R.id.empty_playlists)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PlaylistAdapter(
            playlists = emptyList(),
            onClick = { playlist ->
                (activity as? MainActivity)?.openPlaylistDetail(playlist.name)
            },
            onMoreClick = { playlist, anchor ->
                if (!playlist.isVirtual) showPlaylistMenu(playlist, anchor)
            }
        )
        recycler.adapter = adapter

        view.findViewById<View>(R.id.btn_new_playlist).setOnClickListener {
            showCreatePlaylistDialog()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val context = requireContext()
        SongLibrary.getAll(context)

        val favoriteIds = PrefsManager.getFavorites(context).toMutableList()
        val recentlyAddedIds = SongLibrary.getAll(context)
            .sortedByDescending { it.dateAdded }
            .take(RECENTLY_ADDED_LIMIT)
            .map { it.id }
            .toMutableList()

        val virtualPlaylists = mutableListOf(
            Playlist(PrefsManager.VIRTUAL_FAVORITES, favoriteIds, isVirtual = true),
            Playlist(PrefsManager.VIRTUAL_RECENTLY_ADDED, recentlyAddedIds, isVirtual = true)
        )

        val userPlaylists = PrefsManager.getPlaylists(context)
        val allPlaylists = virtualPlaylists + userPlaylists

        adapter.updateData(allPlaylists)
        // Virtual entries always exist, so the "empty" state only concerns user playlists.
        recycler.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext()).apply { hint = "Playlist name" }
        AlertDialog.Builder(requireContext())
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (name.equals(PrefsManager.VIRTUAL_FAVORITES, true) ||
                        name.equals(PrefsManager.VIRTUAL_RECENTLY_ADDED, true)
                    ) {
                        android.widget.Toast.makeText(requireContext(), "That name is reserved", android.widget.Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val created = PrefsManager.createPlaylist(requireContext(), name)
                    if (!created) {
                        android.widget.Toast.makeText(requireContext(), "A playlist with that name already exists", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistMenu(playlist: Playlist, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Delete Playlist")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete \"${playlist.name}\"?")
                    .setMessage("This only removes the playlist, not the song files.")
                    .setPositiveButton("Delete") { _, _ ->
                        PrefsManager.deletePlaylist(requireContext(), playlist.name)
                        refresh()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            true
        }
        popup.show()
    }
}
