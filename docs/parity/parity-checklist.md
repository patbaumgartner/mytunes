# Parity checklist — myTunes vs. DevTunes FM

Reference: [DevTunes FM](https://radio.madza.dev/), captured on 2026-08-21 into
[`original/`](original/). myTunes evidence in [`mytunes/`](mytunes/) and [`console/`](console/) is
produced by the browser and container test suites; every verdict below names the test or log that
backs it. Breakpoints are `desktop-wide` 1920×1080, `desktop-standard` 1440×900, `mobile-portrait`
390×844, `mobile-small` 360×640.

Verdicts: **PASS** — behaviour reproduced and verified. **PARTIAL** — reproduced with a stated,
externally imposed limit. **BLOCKED** — not achievable without a server or an API browsers do not
provide. **DIVERGES** — deliberately different because the products differ.

## Startup and architecture

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 1 | Application boots and renders a working interface | PASS | `MyTunesBrowserTests#springBootStartsInsideTheBrowserAndBuildsTheInterface`, [`console/mytunes-startup.log`](console/mytunes-startup.log) |
| 2 | The page loads exactly one script, the generated loader | PASS | `MyTunesBrowserTests#loadsNoScriptOtherThanTheGeneratedLoader` |
| 3 | No server-side application: the container holds no JVM and no jar | PASS | `DockerSmokeTests#theImageContainsNoJvmAndNoApplicationJar`, [`console/docker-image-contents.log`](console/docker-image-contents.log) |
| 4 | Zero console errors at every breakpoint | PASS | [`console/mytunes-breakpoints.log`](console/mytunes-breakpoints.log) |

## Station selection

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 5 | Station name and genre shown in the header selector | PASS | [`mytunes/desktop-standard.png`](mytunes/desktop-standard.png) vs. [`original/desktop-standard.png`](original/desktop-standard.png) |
| 6 | Station menu opens from the header and selects a station | PASS | `MyTunesBrowserTests#remembersStationBackgroundAndVolumeAcrossAReload` |
| 7 | Next / previous station, wrapping at the ends | PASS | `MyTunesBrowserTests#keyboardMediaControlsDriveThePlayer`, `PlayerStateTests` |
| 8 | Catalogue of third-party stations | PARTIAL | SomaFM mounts answered HTTP 403 to browser requests on every mount tested (their prerogative as a listener-supported service). Entries stay configured; the self-hosted default station always plays. `StationCatalogueTests` |
| 9 | No reuse of the original's signed stream credentials | PASS | [`console/original-network.log`](console/original-network.log) shows the original streaming from CloudFront-signed SoundCloud URLs; `StationCatalogueTests` fails the build if any stream URL gains `Policy`/`Signature`/`Key-Pair-Id` parameters |

## Playback

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 10 | Play starts real audio after a user gesture — `currentTime` advances | PASS | `MyTunesBrowserTests#playsRealAudioAfterAClickAndRespondsToVolumeAndPause` |
| 11 | Pause genuinely pauses the audio element | PASS | same test |
| 12 | Browser autoplay policy respected: first audible playback needs a real tap | PASS | playback only starts from a click handler; `PlayerState` has no self-starting path |
| 13 | Stream failure is shown to the listener, not swallowed | PASS | `PlayerStateTests` (FAILED status with message), `AudioElement` surfaces the `play()` rejection |
| 14 | Live per-track titles | BLOCKED | The original reads titles from its own playlist service. myTunes has no server, and ICY stream metadata is not exposed to browser code. Station name and genre are shown instead; `PlayerState.nowPlaying` is in place for when a source exists |

## Volume and mute

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 15 | Volume slider drives the audio element | PASS | `MyTunesBrowserTests#playsRealAudioAfterAClickAndRespondsToVolumeAndPause` asserts `audio.volume` follows the slider |
| 16 | Mute toggle; raising the volume unmutes | PASS | `PlayerStateTests` |
| 17 | Volume and mute survive a reload | PASS | `PreferencesStoreTests` round-trips every field; `MyTunesBrowserTests#remembersStationBackgroundAndVolumeAcrossAReload` proves the store is live in the browser |

## Backgrounds

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 18 | Full-viewport artwork backdrop | PASS | all four screenshots per site |
| 19 | Background switching changes the artwork | PASS | `MyTunesBrowserTests#switchingBackgroundChangesTheArtwork` |
| 20 | Selected background survives a reload | PASS | `MyTunesBrowserTests#remembersStationBackgroundAndVolumeAcrossAReload` |
| 21 | Original artwork only — nothing reused from the original's wallpaper source | PASS | [`console/original-network.log`](console/original-network.log) shows the original loading wallhaven images; `BackgroundCatalogueTests` asserts every myTunes asset is served from this repository |

## Keyboard and operating-system integration

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 22 | Keyboard shortcuts (space/k play, m mute, arrows station/volume, Escape) | PASS | `MyTunesBrowserTests#keyboardMediaControlsDriveThePlayer` verifies ArrowRight end to end; the remaining transitions are covered by `PlayerStateTests` |
| 23 | Media Session metadata and transport actions for lock screen and media keys | PASS | `MyTunesBrowserTests#exposesMediaSessionMetadataWhereTheBrowserSupportsIt`; actions are registered on the same feature-detected session |
| 24 | Floating always-on-top mini player | PASS | `MiniPlayerBrowserTests`, [`console/mini-player.log`](console/mini-player.log) — `documentPictureInPicture` present and the window opened |

## Responsive behaviour

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 25 | desktop-wide 1920×1080 | PASS | [`mytunes/desktop-wide.png`](mytunes/desktop-wide.png), zero console errors |
| 26 | desktop-standard 1440×900 | PASS | [`mytunes/desktop-standard.png`](mytunes/desktop-standard.png) |
| 27 | mobile-portrait 390×844 | PASS | [`mytunes/mobile-portrait.png`](mytunes/mobile-portrait.png) |
| 28 | mobile-small 360×640 | PASS | [`mytunes/mobile-small.png`](mytunes/mobile-small.png) |
| 29 | Play, station and volume controls reachable at every size | PASS | `MyTunesBrowserTests#capturesParityEvidenceAtEveryBreakpoint` asserts visibility at each breakpoint |

## Serving

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 30 | Every asset served with a type the browser accepts, including `application/wasm` | PASS | `DockerSmokeTests#servesEveryAssetWithATypeTheBrowserAccepts`, [`console/docker-smoke.log`](console/docker-smoke.log) |
| 31 | Security headers on every response, cached and uncached | PASS | `DockerSmokeTests#sendsSecurityHeadersOnEveryResponse` |
| 32 | Immutable caching for the rebuilt-per-image module and assets | PASS | `DockerSmokeTests#servesTheWasmModuleAsAnImmutableCacheableAsset` |

## Known divergences

| # | Feature | Verdict | Evidence |
| --- | --- | --- | --- |
| 33 | Playlist / queue and repeat controls | DIVERGES | The original plays a SoundCloud playlist, where a queue and repeat make sense. myTunes plays continuous live radio streams, which have no track boundaries to queue or repeat |
| 34 | Settings and account controls | DIVERGES | Replaced by background, mini-player and info controls; myTunes has no accounts and no server to hold settings |
| 35 | Volume slider on iOS | PARTIAL | iOS does not expose volume control to script, so the slider is inert there while mute still works — a platform rule, not exercised by the Chromium-based suite |

**Totals: 30 PASS · 2 PARTIAL · 1 BLOCKED · 2 DIVERGES**

## Reproducing this evidence

```sh
# myTunes screenshots, console logs and diagnostics (needs target/site, see the README build steps)
./mvnw -B test -Dsurefire.excludes= -Dtest='MyTunesBrowserTests,MiniPlayerBrowserTests' \
  -DfailIfNoTests=false

# Container evidence (needs the Docker image built and running on :8099)
./mvnw -B test -Dsurefire.excludes= -Dtest=DockerSmokeTests -DfailIfNoTests=false \
  -Dmytunes.baseUrl=http://127.0.0.1:8099 -Dmytunes.dockerImage=mytunes:latest

# Reference screenshots of the original (reaches the public internet, so opt-in)
./mvnw -B test -Dsurefire.excludes= -Dtest=OriginalSiteCaptureTests -DfailIfNoTests=false \
  -Dmytunes.captureOriginal=true
```
