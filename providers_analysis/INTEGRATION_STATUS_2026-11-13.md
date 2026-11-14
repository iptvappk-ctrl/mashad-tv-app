# MadarTV provider integration status — 2026-11-13

This report records the current implementation and a bounded live route check. It does not claim every title or stream is playable: a successful HTML response is not proof that a page contains the requested title, that its embed resolves, or that the video plays on a handset.

## What is configured

`MultiProviderRepository.PROVIDERS` contains 22 unique public-catalog provider configurations. ArabSeed is integrated separately through `ArabseedRepository`, making 23 unique catalog sources. The pasted 25-row list counts the stream-extractor guide as a row and lists CinemaForAll separately from Cima4U even though both use `c4u.cam`; neither creates an additional unique provider.

All enabled configurations participate in catalog pagination, search, and cross-provider playback discovery. A source contributes only to the media types and listing routes its configuration supports. Search remains useful for providers that do not expose a paginated series or movie catalog.

## Live route check

On 2026-11-13, 54 configured base and mirror search routes were first requested in parallel using a neutral test query. Later, the app configuration was updated from those results and a 30-title search smoke check was run against 23 unique sources. It found at least one exact title result for all 30 works after retrying one Arabic-title miss with its English alias; 19/23 source routes completed the full sweep. The check recorded search-result anchors only; it did not open a movie or episode stream.

| Source | Current code route result | Handling |
| --- | --- | --- |
| Akwam | `akw.cam` returned an Akwam page; `.cx` returned HTML without a useful title | Keep the valid route; mirror remains unverified |
| WeCima | `.cam` earlier redirected to `myweecima1.best`; the latter failed DNS during the 30-title sweep | `.shop` removed; current DNS availability is intermittent/unconfirmed |
| FaselHD | `.top` returned a redirect page, `.co` returned 403, `.tech` returned 451 | No route confirmed; cooldown avoids repeated slow retries |
| EgyBest | `egybest.live` returned a search page and matched 23/30 smoke-test titles | Configured `.mx` served unrelated gambling content; `.best` timed out, so both are excluded |
| Shahid4U | `.cam` and `.tv` returned redirect pages; `.to` returned no useful title | No route confirmed |
| Cima4U | `c4u.cam`, `cima4u.watch`, and `cima4u4k.site` returned Cima4U search pages | Keep all three configured routes |
| Fushaar | `fushaar.com` returned a Fushaar page | Keep |
| TopCinema | `topcinemaa.vip` redirected to `topcinemaa.one`, which returned a search page | Keep the configured final host |
| ArabLionz | `arablionz.cam` returned an ArabLionz page | Keep |
| Lodynet | `lodynet.watch` and `.com` resolved to Lodynet content on `.watch` | Keep |
| Esheeq | `qesset.net/search/{query}/` returned search results; the tested `/series/` and `/category/movies/` routes returned 404 | Search-only route configured; no paginated category route confirmed |
| Movizland | `movizland.life` returned a search page and matched 24/30 smoke-test titles | `.online` redirected and `.com` had no useful title; excluded |
| Prestige | `.asia` resolved to a Prestige page on configured `prstej.lat` | Keep |
| CimaClub | `.live` and `.living` returned CimaClub pages | Keep |
| EgyDead | `.surf` returned 403; `egidead.cam` returned an EgyDead search page | Keep the working mirror |
| CimaNow | `.info` redirected to unrelated `myseed.name` and is rejected; `.cc`/`.net` resolved to the CimaNow page on `.cc` | Keep `.cc`; reject unrelated redirect |
| CimaWbas | `cimawbas.rest` returned a search page; `.com` returned 404 and `.net` failed DNS | Keep the base route |
| Cima4Up | `cima4up-official.cyou` returned a search page; `.tv` timed out, `.co` led to a parked `ww38` page, `.cam` failed DNS | Removed the three dead/parked mirrors |
| Movies4U | `movies4u.icu` returned a Movies4U page | Keep |
| MovieWatcher | `moviewatcher-offcial.cyou` returned a search page; `.site` served a loading interstitial, `.io` failed DNS, `.is` resolved back to the main host | Removed the dead/interstitial mirrors; kept main host and `.is` |
| SpaceMov | Main `.com` returned 403; `.xyz` returned a SpaceMov page | Keep `.xyz` fallback |
| Moviflex | `.net` returned HTML without a useful title; configured mirrors failed DNS | Retain only the base pending a valid route |
| ArabSeed | Separate repository and parser; not included in this 54-route sweep | Existing MySeed parser remains the primary ArabSeed integration |

The exact 30-title query list and per-source match counts are in [SMOKE_TEST_30_2026-11-13.md](SMOKE_TEST_30_2026-11-13.md). The title `وحيد في المنزل` required the English alias `Home Alone`. Matching a title in a listing is not evidence that the item has a working player link.

## Request and playback flow

1. Flutter checks its local/disk cache first, then requests the primary catalog/search and the provider catalog/search. The provider repository fans requests out across enabled configurations and streams partial results to the UI.
2. Each provider request follows redirects only when the final host remains within that provider's configured base or mirrors. Non-HTML responses, parked domains, unrelated gambling pages, and redirect/interstitial pages are rejected before card parsing.
3. A short route cooldown records dead or bad mirrors so the app does not pay the same timeout on every search, detail page, or season request. Transient failures cool down briefly; unrelated redirects/interstitials cool down longer.
4. Detail pages expose their available embed/server rows. Cross-provider discovery searches by normalized title and year, checks candidate pages, and returns alternatives. The player receives direct stream URLs first, followed by page URLs as fallback.
5. The player advances to its next source on a playback error, rejected HTTP response, or prolonged startup buffering. It blocks pop-up windows and requests to known advertising/tracking hosts. An ad filter is not a guarantee that every ad inside a remote player or video stream can be removed.
6. Downloads use the same best-candidate resolution path. `ZakzoukaDownloadService` accepts progressive media and static, clear, non-byte-range VOD HLS; it rejects HTML pages, encrypted HLS, live playlists, and unsupported HLS layouts.

## Episode and season normalization

`SeriesCatalogParser` extracts season and episode numbers from Arabic/Western digits, labels, URLs, and nearby season tabs. It drops recommendation/sidebar rows, deduplicates episode URLs, assigns episodes to explicit seasons, and sorts by numeric season then episode number. When a source page is partial, provider season pages are used to repair the sequence. Provider metadata can still be incomplete or wrong, so an inferred episode count cannot be treated as authoritative when every source omits episodes.

## Checks run in this work session

- `flutter analyze --no-pub --no-fatal-infos --no-fatal-warnings`: exit 0; 58 existing info-level lints, no errors or warnings.
- `flutter test --no-pub`: passed (1 widget test).
- Android app `:app:compileDebugKotlin`: successful.
- Player `:madar-player:assembleRelease`: successful.
- No emulator or phone was exercised in this run. No real movie, series episode, or live channel was confirmed playing, and this report does not claim a zero-defect release.
