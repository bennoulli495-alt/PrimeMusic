package com.primemusic.app

import android.Manifest
import android.app.AlertDialog
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.primemusic.app.fragments.HomeFragment
import com.primemusic.app.fragments.PlaylistDetailFragment
import com.primemusic.app.fragments.PlaylistsFragment
import com.primemusic.app.fragments.SearchFragment
import com.primemusic.app.fragments.SongsFragment
import com.primemusic.app.model.Song
import com.primemusic.app.service.MusicService
import com.primemusic.app.util.PrefsManager
import com.primemusic.app.util.SongLibrary

class MainActivity : AppCompatActivity(), MusicService.PlaybackListener {

    private var musicService: MusicService? = null
    private var isBound = false

    private lateinit var miniPlayer: android.view.View
    private lateinit var miniTitle: TextView
    private lateinit var miniSubtitle: TextView
    private lateinit var miniPlayPause: ImageButton
    private lateinit var miniShuffle: ImageButton
    private lateinit var miniRepeat: ImageButton

    // Keeps user-pasted album art links for the current session (Song Info screen).
    private val albumLinkOverrides = mutableMapOf<Long, String>()

    // Pending delete/rename retried after the user grants permission via system dialog.
    private var pendingDeleteSong: Song? = null
    private var pendingDeleteCallback: ((Boolean) -> Unit)? = null
    private var pendingRenameSong: Song? = null
    private var pendingRenameTitle: String? = null
    private var pendingRenameCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshCurrentFragment()
        } else {
            Toast.makeText(this, getString(R.string.grant_permission), Toast.LENGTH_LONG).show()
        }
    }

    private val intentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val granted = result.resultCode == RESULT_OK
        pendingDeleteSong?.let { song ->
            if (granted) performDelete(song, pendingDeleteCallback) else pendingDeleteCallback?.invoke(false)
            pendingDeleteSong = null
            pendingDeleteCallback = null
        }
        pendingRenameSong?.let { song ->
            val title = pendingRenameTitle
            if (granted && title != null) performRename(song, title, pendingRenameCallback) else pendingRenameCallback?.invoke(false)
            pendingRenameSong = null
            pendingRenameTitle = null
            pendingRenameCallback = null
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as MusicService.MusicBinder
            musicService = localBinder.getService()
            musicService?.listener = this@MainActivity
            updateMiniPlayerFromService()
            updateShuffleRepeatIcons()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.title = getString(R.string.app_name)

        miniPlayer = findViewById(R.id.mini_player)
        miniTitle = findViewById(R.id.mini_title)
        miniSubtitle = findViewById(R.id.mini_subtitle)
        miniPlayPause = findViewById(R.id.mini_play_pause)
        miniShuffle = findViewById(R.id.mini_shuffle)
        miniRepeat = findViewById(R.id.mini_repeat)

        miniPlayPause.setOnClickListener { musicService?.togglePlayPause() }
        findViewById<ImageButton>(R.id.mini_next).setOnClickListener { musicService?.playNext() }
        findViewById<ImageButton>(R.id.mini_prev).setOnClickListener { musicService?.playPrevious() }
        miniShuffle.setOnClickListener {
            val service = musicService ?: return@setOnClickListener
            service.setShuffle(!service.getShuffle())
            updateShuffleRepeatIcons()
        }
        miniRepeat.setOnClickListener {
            val service = musicService ?: return@setOnClickListener
            val next = (service.getRepeatMode() + 1) % 3
            service.setRepeatMode(next)
            updateShuffleRepeatIcons()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_songs -> SongsFragment()
                R.id.nav_playlists -> PlaylistsFragment()
                R.id.nav_search -> SearchFragment()
                else -> SongsFragment()
            }
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_songs
        }

        startService(Intent(this, MusicService::class.java))
        maybeRequestPermissionOnFirstLaunch()
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            musicService?.listener = null
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(this, "Settings screen — coming in the next update", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_sleep_timer -> {
                showSleepTimerDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ---- Called by fragments ----

    fun playSong(playlist: List<Song>, startIndex: Int) {
        if (playlist.isEmpty() || startIndex !in playlist.indices) return
        musicService?.playSongs(playlist, startIndex)
        miniPlayer.visibility = android.view.View.VISIBLE
    }

    fun requestAudioPermission() {
        permissionLauncher.launch(requiredAudioPermission())
    }

    fun openPlaylistDetail(playlistName: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, PlaylistDetailFragment.newInstance(playlistName))
            .addToBackStack("playlist_detail")
            .commit()
    }

    fun showAddToPlaylistDialog(song: Song) {
        val playlists = PrefsManager.getPlaylists(this)
        val names = playlists.map { it.name }.toMutableList()
        names.add("+ Create new playlist")

        AlertDialog.Builder(this)
            .setTitle("Add to Playlist")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == names.size - 1) {
                    showCreatePlaylistThenAdd(song)
                } else {
                    PrefsManager.addSongToPlaylist(this, names[which], song.id)
                    Toast.makeText(this, "Added to \"${names[which]}\"", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreatePlaylistThenAdd(song: Song) {
        val input = EditText(this).apply { hint = "Playlist name" }
        AlertDialog.Builder(this)
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create & Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    PrefsManager.createPlaylist(this, name)
                    PrefsManager.addSongToPlaylist(this, name, song.id)
                    Toast.makeText(this, "Added to \"$name\"", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showSongInfo(song: Song) {
        val input = EditText(this).apply {
            hint = "Paste album art link (optional)"
            setText(albumLinkOverrides[song.id] ?: song.userAlbumLink ?: "")
        }

        val message = """
            Title: ${song.title}
            Artist: ${song.artist}
            Duration: ${song.formattedDuration()}
            Size: ${song.formattedFileSize()}
            Format: ${song.format()}
            Path: ${song.filePath}
        """.trimIndent()

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(TextView(this@MainActivity).apply { text = message })
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Song Info")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                albumLinkOverrides[song.id] = input.text.toString()
                Toast.makeText(this, "Album link saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ---- Sleep timer ----

    private fun showSleepTimerDialog() {
        val service = musicService
        val options = arrayOf("Off", "15 minutes", "30 minutes", "45 minutes", "60 minutes")
        val current = if (service?.isSleepTimerActive() == true) service.getSleepTimerRemainingMinutes() else 0
        val title = if (current > 0) "Sleep Timer (${current}m left)" else "Sleep Timer"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        service?.cancelSleepTimer()
                        Toast.makeText(this, "Sleep timer off", Toast.LENGTH_SHORT).show()
                    }
                    1 -> startSleep(15)
                    2 -> startSleep(30)
                    3 -> startSleep(45)
                    4 -> startSleep(60)
                }
            }
            .show()
    }

    private fun startSleep(minutes: Int) {
        musicService?.startSleepTimer(minutes)
        Toast.makeText(this, "Playback will pause in $minutes minutes", Toast.LENGTH_SHORT).show()
    }

    // ---- Delete / Rename (scoped storage aware) ----

    fun deleteSong(song: Song, onDone: (Boolean) -> Unit) {
        performDelete(song, onDone)
    }

    private fun performDelete(song: Song, onDone: ((Boolean) -> Unit)?) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.let {
            android.content.ContentUris.withAppendedId(it, song.id)
        }
        try {
            val rows = contentResolver.delete(uri, null, null)
            SongLibrary.getAll(this, forceRefresh = true)
            cleanupReferencesToSong(song.id)
            onDone?.invoke(rows > 0)
            if (rows > 0) Toast.makeText(this, "Deleted \"${song.title}\"", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                pendingDeleteSong = song
                pendingDeleteCallback = onDone
                val sender: IntentSender = e.userAction.actionIntent.intentSender
                intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                Toast.makeText(this, "Couldn't delete this file", Toast.LENGTH_SHORT).show()
                onDone?.invoke(false)
            }
        }
    }

    fun renameSong(song: Song, newTitle: String, onDone: (Boolean) -> Unit) {
        performRename(song, newTitle, onDone)
    }

    private fun performRename(song: Song, newTitle: String, onDone: ((Boolean) -> Unit)?) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.let {
            android.content.ContentUris.withAppendedId(it, song.id)
        }
        val values = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, newTitle) }
        try {
            val rows = contentResolver.update(uri, values, null, null)
            SongLibrary.getAll(this, forceRefresh = true)
            onDone?.invoke(rows > 0)
            if (rows > 0) Toast.makeText(this, "Renamed to \"$newTitle\"", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                pendingRenameSong = song
                pendingRenameTitle = newTitle
                pendingRenameCallback = onDone
                val sender: IntentSender = e.userAction.actionIntent.intentSender
                intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                Toast.makeText(this, "Couldn't rename this file", Toast.LENGTH_SHORT).show()
                onDone?.invoke(false)
            }
        }
    }

    private fun cleanupReferencesToSong(songId: Long) {
        // Remove the deleted song from any playlists/favorites so stale entries don't linger.
        val playlists = PrefsManager.getPlaylists(this)
        playlists.forEach { PrefsManager.removeSongFromPlaylist(this, it.name, songId) }
        if (PrefsManager.isFavorite(this, songId)) PrefsManager.toggleFavorite(this, songId)
    }

    // ---- Helpers ----

    private fun requiredAudioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    private fun maybeRequestPermissionOnFirstLaunch() {
        val permission = requiredAudioPermission()
        val granted = ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(permission)
        }
    }

    private fun refreshCurrentFragment() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (current is SongsFragment) {
            current.loadSongsIfPermitted(forceRefresh = true)
        }
    }

    private fun updateMiniPlayerFromService() {
        val service = musicService ?: return
        val song = service.currentSong()
        if (song != null) {
            miniPlayer.visibility = android.view.View.VISIBLE
            onSongChanged(song)
            onPlaybackStateChanged(service.isPlaying())
        }
    }

    private fun updateShuffleRepeatIcons() {
        val service = musicService ?: return
        val activeColor = ContextCompat.getColor(this, R.color.accent)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_primary)

        miniShuffle.setColorFilter(if (service.getShuffle()) activeColor else inactiveColor, PorterDuff.Mode.SRC_IN)

        when (service.getRepeatMode()) {
            0 -> {
                miniRepeat.setImageResource(R.drawable.ic_repeat)
                miniRepeat.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN)
            }
            1 -> {
                miniRepeat.setImageResource(R.drawable.ic_repeat)
                miniRepeat.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            }
            2 -> {
                miniRepeat.setImageResource(R.drawable.ic_repeat_one)
                miniRepeat.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    // ---- MusicService.PlaybackListener ----

    override fun onSongChanged(song: Song?) {
        runOnUiThread {
            miniTitle.text = song?.title ?: "No song playing"
            miniSubtitle.text = song?.artist ?: ""
            if (song != null) {
                PrefsManager.recordPlayed(this, song.id)
            }
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        runOnUiThread {
            miniPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }
    }
}
