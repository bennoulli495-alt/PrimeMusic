package com.primemusic.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.R
import com.primemusic.app.model.Song

class SongAdapter(
    private var songs: List<Song>,
    private val isFavorite: (Song) -> Boolean,
    private val onSongClick: (Song, Int) -> Unit,
    private val onMoreClick: (Song, View) -> Unit,
    private val onFavoriteClick: (Song, ImageButton) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val art: ImageView = view.findViewById(R.id.song_art)
        val title: TextView = view.findViewById(R.id.song_title)
        val subtitle: TextView = view.findViewById(R.id.song_subtitle)
        val more: ImageButton = view.findViewById(R.id.song_more)
        val favorite: ImageButton = view.findViewById(R.id.song_favorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.subtitle.text = "${song.artist} • ${song.formattedDuration()}"
        holder.art.setImageResource(R.drawable.ic_music_placeholder)
        holder.favorite.setImageResource(
            if (isFavorite(song)) R.drawable.ic_heart else R.drawable.ic_heart_outline
        )

        holder.itemView.setOnClickListener { onSongClick(song, position) }
        holder.more.setOnClickListener { onMoreClick(song, it) }
        holder.favorite.setOnClickListener { onFavoriteClick(song, holder.favorite) }
    }

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun currentList(): List<Song> = songs
}
