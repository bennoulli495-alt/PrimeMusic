package com.primemusic.app.model

/**
 * Represents a single local mp3 file scanned from device storage.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,      // in milliseconds
    val filePath: String,
    val fileSize: Long,      // in bytes
    val albumArtUri: String?, // content:// uri if embedded art exists (from MediaStore)
    var userAlbumLink: String? = null // user-provided online album art link (Song Info screen)
) {
    fun formattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun formattedFileSize(): String {
        val kb = fileSize / 1024.0
        return if (kb > 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
    }

    fun format(): String {
        val dot = filePath.lastIndexOf('.')
        return if (dot != -1) filePath.substring(dot + 1).uppercase() else "MP3"
    }
}
