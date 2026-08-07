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

class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {

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
                showPlaylistMenu(playlist, anchor)
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
        val playlists = PrefsManager.getPlaylists(requireContext())
        adapter.updateData(playlists)
        recycler.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext()).apply { hint = "Playlist name" }
        AlertDialog.Builder(requireContext())
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
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
