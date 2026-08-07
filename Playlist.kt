package com.primemusic.app.model

data class Playlist(
    val name: String,
    val songIds: MutableList<Long> = mutableListOf()
)
