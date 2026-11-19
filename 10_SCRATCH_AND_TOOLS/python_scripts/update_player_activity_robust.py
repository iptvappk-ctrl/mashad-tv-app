with open("MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt", "r", encoding="utf-8") as f:
    text = f.read()

# Add currentBackupUrls state in PlayerActivity
old_state = """    private val currentUserAgent = mutableStateOf("")
    private val currentReferer = mutableStateOf("")"""

new_state = """    private val currentUserAgent = mutableStateOf("")
    private val currentReferer = mutableStateOf("")
    private val currentBackupUrls = androidx.compose.runtime.mutableStateListOf<String>()"""

if "\r\n" in text:
    old_state = old_state.replace("\n", "\r\n")
    new_state = new_state.replace("\n", "\r\n")

text = text.replace(old_state, new_state)

# In extractStreamFromIntent
old_extract = """        if (url.isNotBlank()) {
            currentStreamUrl.value = url
            currentTitle.value = title
            currentUserAgent.value = userAgent
            currentReferer.value = referer
        }"""

new_extract = """        val backups = intent?.getStringArrayListExtra("com.madar.player.extra.BACKUP_URLS")
            ?: intent?.getStringArrayListExtra("backup_stream_urls")
            ?: arrayListOf()

        if (url.isNotBlank()) {
            currentStreamUrl.value = url
            currentTitle.value = title
            currentUserAgent.value = userAgent
            currentReferer.value = referer
            currentBackupUrls.clear()
            currentBackupUrls.addAll(backups.filter { it.isNotBlank() && it != url })
        }"""

if "\r\n" in text:
    old_extract = old_extract.replace("\n", "\r\n")
    new_extract = new_extract.replace("\n", "\r\n")

text = text.replace(old_extract, new_extract)

# In setContent -> PlayerScreen call
old_screen_call = """                    PlayerScreen(
                        streamUrl = url,
                        title = title,
                        userAgent = userAgent,
                        referer = referer,
                        onClose = { finish() }
                    )"""

new_screen_call = """                    PlayerScreen(
                        streamUrl = url,
                        title = title,
                        userAgent = userAgent,
                        referer = referer,
                        backupUrls = currentBackupUrls,
                        onSwitchStream = { next -> currentStreamUrl.value = next },
                        onClose = { finish() }
                    )"""

if "\r\n" in text:
    old_screen_call = old_screen_call.replace("\n", "\r\n")
    new_screen_call = new_screen_call.replace("\n", "\r\n")

text = text.replace(old_screen_call, new_screen_call)

# In PlayerScreen definition
old_ps_def = """private fun PlayerScreen(
    streamUrl: String,
    title: String,
    userAgent: String,
    referer: String,
    onClose: () -> Unit
) {"""

new_ps_def = """private fun PlayerScreen(
    streamUrl: String,
    title: String,
    userAgent: String,
    referer: String,
    backupUrls: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onSwitchStream: (String) -> Unit,
    onClose: () -> Unit
) {"""

if "\r\n" in text:
    old_ps_def = old_ps_def.replace("\n", "\r\n")
    new_ps_def = new_ps_def.replace("\n", "\r\n")

text = text.replace(old_ps_def, new_ps_def)

# In onPlayerError
old_err = """                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = error.errorCodeName
                        isBuffering = false
                        // Only switch to WebView for embed pages; direct streams (HLS/MP4) must NOT go to WebView
                        if (isEmbedPage) {
                            useWebViewFallback = true
                        }
                        // For direct streams, just show error - user can retry
                    }"""

new_err = """                    override fun onPlayerError(error: PlaybackException) {
                        if (backupUrls.isNotEmpty()) {
                            val next = backupUrls.removeAt(0)
                            onSwitchStream(next)
                            return
                        }
                        playbackError = error.errorCodeName
                        isBuffering = false
                        if (isEmbedPage) {
                            useWebViewFallback = true
                        }
                    }"""

if "\r\n" in text:
    old_err = old_err.replace("\n", "\r\n")
    new_err = new_err.replace("\n", "\r\n")

text = text.replace(old_err, new_err)

# In error button retry
old_retry_btn = """                    Button(onClick = {
                        playbackError = null
                        isBuffering = true
                        exoRetryKey++
                    }) {"""

new_retry_btn = """                    Button(onClick = {
                        playbackError = null
                        isBuffering = true
                        if (backupUrls.isNotEmpty()) {
                            val next = backupUrls.removeAt(0)
                            onSwitchStream(next)
                        } else {
                            exoRetryKey++
                        }
                    }) {"""

if "\r\n" in text:
    old_retry_btn = old_retry_btn.replace("\n", "\r\n")
    new_retry_btn = new_retry_btn.replace("\n", "\r\n")

text = text.replace(old_retry_btn, new_retry_btn)

with open("MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt", "w", encoding="utf-8") as f:
    f.write(text)

print("Updated PlayerActivity.kt with auto-fallback and backup URLs!")
