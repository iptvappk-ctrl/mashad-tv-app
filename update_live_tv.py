with open("MadarTV/app/src/main/java/com/madar/tv/ui/LiveTvScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

# 1. Update onStreamSelected callback in LiveTvScreen
old_launch = """            0 -> MatchesTab(onStreamSelected = { stream, title, backups ->
                val launched = PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    backupUrls = backups,
                    onNotInstalled = { playerInstallPrompt = true },
                )
                if (!launched && !playerInstallPrompt) {
                    playerInstallPrompt = true
                }
            })
            1 -> ChannelsTab(onStreamSelected = { stream, title, backups ->
                val launched = PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    backupUrls = backups,
                    onNotInstalled = { playerInstallPrompt = true },
                )
                if (!launched && !playerInstallPrompt) {
                    playerInstallPrompt = true
                }
            })"""

old_target = """            0 -> MatchesTab(onStreamSelected = { stream, title ->
                val launched = PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    onNotInstalled = { playerInstallPrompt = true },
                )
                if (!launched && !playerInstallPrompt) {
                    playerInstallPrompt = true
                }
            })
            1 -> ChannelsTab(onStreamSelected = { stream, title ->
                val launched = PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    onNotInstalled = { playerInstallPrompt = true },
                )
                if (!launched && !playerInstallPrompt) {
                    playerInstallPrompt = true
                }
            })"""

if "\r\n" in text:
    old_target = old_target.replace("\n", "\r\n")
    old_launch = old_launch.replace("\n", "\r\n")

if old_target in text:
    text = text.replace(old_target, old_launch)
    print("Updated LiveTvScreen tab callback!")
else:
    print("Could not find old_target in LiveTvScreen.kt")

# 2. Update MatchesTab signature
text = text.replace(
    "fun MatchesTab(onStreamSelected: (YacineStream, String) -> Unit) {",
    "fun MatchesTab(onStreamSelected: (YacineStream, String, List<String>) -> Unit) {"
)

# 3. Update ChannelsTab signature
text = text.replace(
    "fun ChannelsTab(onStreamSelected: (YacineStream, String) -> Unit) {",
    "fun ChannelsTab(onStreamSelected: (YacineStream, String, List<String>) -> Unit) {"
)

# 4. In MatchesTab pendingStreams
text = text.replace(
    "onStreamSelected(best, pendingTitle)",
    "val backups = ps.filter { it.url != best.url }.map { it.url }\n            onStreamSelected(best, pendingTitle, backups)"
)

# 5. In ChannelsTab pendingStreams
text = text.replace(
    "onStreamSelected(best, pendingTitle)",
    "val backups = ps.filter { it.url != best.url }.map { it.url }\n            onStreamSelected(best, pendingTitle, backups)"
)

# 6. In MatchesTab onMatchClick: prioritize channel stream and provide backups
old_match_click = """                            // 1. Try exact live match event stream first
                            val matched = findMatchingYacineEvent(match, events)
                            if (matched != null) {
                                loadingStreams = true
                                val streams = runCatching {
                                    YacineRepository.getEventStreams(matched.id)
                                }.getOrElse { emptyList() }
                                loadingStreams = false
                                if (streams.isNotEmpty()) {
                                    val best = streams.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                                        ?: streams.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                                        ?: streams.first()
                                    onStreamSelected(best, title)
                                    return@launch
                                }
                            }

                            // 2. If no event scheduled yet, auto-launch primary sports channel directly
                            val chs = if (sportsChannels.isNotEmpty()) sportsChannels else runCatching {
                                YacineRepository.getSportsChannels()
                            }.getOrElse { emptyList() }

                            val targetChannel = chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) && (it.name.contains("1") || it.name.contains("Premium")) }
                                ?: chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) }
                                ?: chs.firstOrNull()

                            if (targetChannel != null) {
                                loadingStreams = true
                                val streams = runCatching {
                                    YacineRepository.getChannelStreams(targetChannel.id)
                                }.getOrElse { emptyList() }
                                loadingStreams = false
                                if (streams.isNotEmpty()) {
                                    val best = streams.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                                        ?: streams.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                                        ?: streams.first()
                                    onStreamSelected(best, "$title · ${targetChannel.name}")
                                    return@launch
                                }
                            }"""

new_match_click = """                            loadingStreams = true
                            val chs = if (sportsChannels.isNotEmpty()) sportsChannels else runCatching {
                                YacineRepository.getSportsChannels()
                            }.getOrElse { emptyList() }

                            val matched = findMatchingYacineEvent(match, events)
                            val chHint = matched?.channel.orEmpty()

                            val targetChannel = (if (chHint.isNotBlank()) chs.firstOrNull { it.name.contains(chHint, true) || chHint.contains(it.name, true) } else null)
                                ?: chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) && (it.name.contains("1") || it.name.contains("Premium")) }
                                ?: chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) }
                                ?: chs.firstOrNull()

                            val allStreams = mutableListOf<YacineStream>()

                            // Channel streams have guaranteed fresh 200 OK tokens
                            if (targetChannel != null) {
                                val chStreams = runCatching { YacineRepository.getChannelStreams(targetChannel.id) }.getOrElse { emptyList() }
                                allStreams.addAll(chStreams)
                            }
                            if (matched != null) {
                                val evStreams = runCatching { YacineRepository.getEventStreams(matched.id) }.getOrElse { emptyList() }
                                allStreams.addAll(evStreams)
                            }
                            loadingStreams = false

                            val validStreams = allStreams.filter { it.url.isNotBlank() && !it.url.contains("rex.new-redirect.online") }.distinctBy { it.url }
                            if (validStreams.isNotEmpty()) {
                                val best = validStreams.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                                    ?: validStreams.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                                    ?: validStreams.first()
                                val backups = validStreams.filter { it.url != best.url }.map { it.url }
                                val label = targetChannel?.name?.let { " · $it" } ?: ""
                                onStreamSelected(best, "$title$label", backups)
                                return@launch
                            }"""

if "\r\n" in text:
    old_match_click = old_match_click.replace("\n", "\r\n")
    new_match_click = new_match_click.replace("\n", "\r\n")

if old_match_click in text:
    text = text.replace(old_match_click, new_match_click)
    print("Updated onMatchClick logic!")
else:
    print("Could not find old_match_click in LiveTvScreen.kt")

with open("MadarTV/app/src/main/java/com/madar/tv/ui/LiveTvScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
print("Finished updating LiveTvScreen.kt")
