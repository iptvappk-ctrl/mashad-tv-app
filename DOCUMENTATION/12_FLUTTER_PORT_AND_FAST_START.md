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

The independent project lives in `MadarFlutter`. It provides an RTL light interface, animated tab transitions, catalogue filters, search, saved favourites, details, cast, season and episode lists, exact adjacent-episode navigation, live events, stale-while-revalidate JSON cache, and a native Android bridge to `com.madar.player`.

The Flutter app uses the existing web API contract:

- `/api/cinema/movies`, `/api/cinema/series`, `/api/cinema/search`
- `/api/cinema/detail`, `/api/cinema/resolve`
- `/api/yacine/events`, `/api/yacine/stream`

The API origin can be supplied at build time with `MADAR_API_BASE_URL` or changed from the network-settings button in the Flutter app; the chosen HTTPS origin is saved locally. The default `http://10.0.2.2:3000` only reaches a web server running on the development PC from an Android emulator. A physical phone needs an HTTPS host reachable from that phone. No backend URL was configured as a public production endpoint in this workspace. No API secrets are placed in the Flutter project.

## Build commands

From `MadarFlutter`:

```powershell
& 'C:\src\flutter\bin\flutter.bat' build apk --debug --dart-define=MADAR_API_BASE_URL=https://YOUR-HOST
```

For the Android app and companion player:

```powershell
cd MadarTV
./gradlew.bat :app:assembleDebug :madar-player:assembleDebug
```

## Verification boundary

Build success confirms compilation and packaging only. It does not confirm that third-party source pages are reachable, that a particular match has an active stream, or that the server accepts a stream on an actual phone. The workspace has no attached Android device, and there is no configured public API host in this checkout. The 750 ms target is first-screen/skeleton dismissal; a cold catalogue still depends on the network and provider response times.
