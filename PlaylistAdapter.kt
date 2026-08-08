package com.primemusic.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.R
import com.primemusic.app.model.Playlist
import com.primemusic.app.util.PrefsManager

class PlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onClick: (Playlist) -> Unit,
    private val onMoreClick: (Playlist, View) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.playlist_icon)
        val name: TextView = view.findViewById(R.id.playlist_name)
        val count: TextView = view.findViewById(R.id.playlist_count)
        val more: ImageButton = view.findViewById(R.id.playlist_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.name.text = playlist.name
        holder.count.text = "${playlist.songIds.size} songs"
        holder.icon.setImageResource(
            when (playlist.name) {
                PrefsManager.VIRTUAL_FAVORITES -> R.drawable.ic_heart
                PrefsManager.VIRTUAL_RECENTLY_ADDED -> R.drawable.ic_timer
                else -> R.drawable.ic_folder
            }
        )
        holder.more.visibility = if (playlist.isVirtual) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onClick(playlist) }
        holder.more.setOnClickListener { onMoreClick(playlist, it) }
    }

    override fun getItemCount(): Int = playlists.size

    fun updateData(newPlaylists: List<Playlist>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }
}
