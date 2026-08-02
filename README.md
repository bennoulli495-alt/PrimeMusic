# Prime Music — Step 1 (Core)

This is the first build of Prime Music with the core features only:

- Auto-scan local mp3 files (via MediaStore)
- Song list, sorted by name
- Play / Pause / Next / Previous
- Background playback with notification + lock-screen controls
- Dark theme
- Mini player
- 4 tabs: Home, Songs, Playlists, Search (Home/Playlists/Search are placeholders for now)
- Song Info screen (⋮ menu → Song Info) with an album art link field

## How to build an APK (no computer needed)

1. Upload this whole folder to a new GitHub repository.
2. Go to https://codemagic.io and sign in with your GitHub account.
3. Add the repository in Codemagic and start a build using the `prime-music-debug` workflow (from `codemagic.yaml` in this repo).
4. When the build finishes, download the APK from the build artifacts.
5. On your phone: Settings → Security → allow "Install unknown apps" for your browser/files app, then open the downloaded APK to install.

## Next steps (coming in later updates)

Playlists, Favorites, Search, Sleep timer, Repeat/Shuffle, Recently played, Delete/Rename, Lyrics, Settings screen.
