package com.primemusic.app.model

data class Playlist(
    val name: String,
    val songIds: MutableList<Long> = mutableListOf(),
    val isVirtual: Boolean = false // true for auto-generated entries like Favorites / Recently Added
)
