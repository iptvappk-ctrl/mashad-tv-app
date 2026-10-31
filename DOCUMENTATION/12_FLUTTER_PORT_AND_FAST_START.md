# Flutter Copy, Fast First Paint, and Episode Ordering

## Native Android changes

- The home `Scaffold` is composed behind the intro so cache reads and network refresh begin immediately.
- The intro appears only on the first launch and now finishes in under one second by its animation schedule.
- A cached catalogue is drawn first. On a cold cache the full-page skeleton is dismissed after 750 ms while source requests continue in parallel and merge as they finish.
- Catalogue polling was reduced from every eight minutes to every twenty minutes.
- The catalogue skeleton no longer runs a continuous shimmer animation.
- Provider de-duplication and series collapse now use normalized-title buckets instead of scanning the entire accumulated catalogue for each row. Conflicting known release years are kept separate.
- Episode previous/next actions require the exact adjacent number in the same season. A missing number disables that action instead of jumping to a different episode.
- Season bundles are normalized and sorted before display. Unknown-season episode links are omitted when the source page identifies multiple seasons. Season cache keys were versioned to `v2` so stale mixed-season bundles are not reused.
- Live playback forwards server headers and forms the `Origin` from the referer origin. Match and channel streams open in the companion player; if it is missing, the app shows a Play Store prompt instead of opening the video inline.

## Flutter application

The independent project lives in `MadarFlutter`. Its user-facing shell mirrors the native app: a light RTL cinema catalog with the same six content tabs and subcategories, plus `أفلام ومسلسلات`, `بثات الكورة`, and `المفضلة` bottom navigation. The user does not enter or configure a content server.

`FlutterMediaBridge.kt` connects Flutter to the same Android repositories used by MadarTV: `ArabseedRepository`, `MultiProviderRepository`, `YacineRepository`, and `FootballRepository`. Disk caches initialize in the Flutter host process. Flutter reads cached content first, renders primary-source results as they arrive, and loads provider results in the background before merging and de-duplicating them.

The Flutter flow includes search, details/story/cast, related parts, numbered seasons and episodes, exact same-season previous/next navigation, playback server selection through the companion player, match and channel streams, saved favorites, and the Android download service. Bounded poster decoding and retained tab state keep navigation light.

Flutter content and playback still depend on the third-party providers and their current responses. Code integration does not certify that every source is reachable, that a stream is valid, or that rights/access remain available.

## Build commands

From `MadarFlutter`:

```powershell
& 'C:\src\flutter\bin\flutter.bat' build apk --release --target-platform android-arm64
```

The Flutter bridge calls the existing Android repositories directly; there is no API-origin field or `MADAR_API_BASE_URL` setting in the user app.

For the Android app and companion player:

```powershell
cd MadarTV
./gradlew.bat :app:assembleDebug :madar-player:assembleDebug
```

The Flutter parity APK uses version `1.0.2+3` and targets Android ARM64. Release builds currently use the debug signing config for manual device installation; store publication requires a protected release key.

## Verification boundary

Build success confirms compilation and packaging only. It does not confirm that third-party source pages are reachable, that a particular match has an active stream, or that the player accepts a stream on an actual phone. No attached Android device was available during the last documented check. A cold catalog still depends on network and provider response times; cached data appears immediately when available.
