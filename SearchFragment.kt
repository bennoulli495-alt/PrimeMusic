package com.primemusic.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.primemusic.app.R

class SearchFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<android.widget.TextView>(R.id.placeholder_title).text = getString(R.string.title_search)
    }
}
