package com.primemusic.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.primemusic.app.MainActivity
import com.primemusic.app.R
import com.primemusic.app.adapter.SongAdapter
import com.primemusic.app.util.PrefsManager
import com.primemusic.app.util.SongLibrary

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var recycler: RecyclerView
    private lateinit var hint: TextView
    private lateinit var adapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_search)
        hint = view.findViewById(R.id.search_hint)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongAdapter(
            songs = emptyList(),
            isFavorite = { song -> PrefsManager.isFavorite(requireContext(), song.id) },
            onSongClick = { _, index ->
                (activity as? MainActivity)?.playSong(adapter.currentList(), index)
            },
            onMoreClick = { song, _ ->
                (activity as? MainActivity)?.showSongInfo(song)
            },
            onFavoriteClick = { song, button ->
                val nowFav = PrefsManager.toggleFavorite(requireContext(), song.id)
                button.setImageResource(if (nowFav) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            }
        )
        recycler.adapter = adapter

        // Warm the cache so results appear instantly once the user types.
        SongLibrary.getAll(requireContext())

        val input = view.findViewById<EditText>(R.id.search_input)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (query.isBlank()) {
                    adapter.updateData(emptyList())
                    recycler.visibility = View.GONE
                    hint.visibility = View.VISIBLE
                    hint.text = "Start typing to search your library"
                } else {
                    val results = SongLibrary.search(query)
                    adapter.updateData(results)
                    recycler.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
                    hint.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                    hint.text = "No matches for \"$query\""
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
