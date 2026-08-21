# myTunes

A radio player inspired by [DevTunes FM](https://radio.madza.dev/), built to answer one question:

> Can a complete Spring Boot application — the framework, the domain, **and the user interface** —
> be compiled from Java to WebAssembly and run entirely inside a browser, with no server?

**Yes.** Spring Boot 4.1.1 and Spring Modulith 2.1.0 start in the browser tab in about 200 ms with
57 beans, build the interface against the live DOM, play audio, and persist preferences. The
container that serves it holds no JVM and no jar.

```
[mytunes] Spring Boot 4.1.1 started in the browser in 206ms with 57 beans
[mytunes] interface ready
```

---

## Contents

- [What this is](#what-this-is) · [Why Java to WebAssembly](#why-java-to-webassembly)
- [Architecture](#architecture) · [Modules](#modules) · [The zero-JavaScript rule](#the-zero-javascript-rule)
- [Prerequisites](#prerequisites) · [Build from a clean clone](#build-from-a-clean-clone) · [Docker](#docker)
- [Tests](#tests) · [Quality harness](#quality-harness) · [Parity evidence](#parity-evidence)
- [Stored data](#stored-data) · [Stations and streams](#stations-and-streams) · [Artwork](#artwork)
- [Browser support](#browser-support) · [Mobile](#mobile) · [Media Session and mini player](#media-session-and-mini-player)
- [GraalVM Web Image: status and limits](#graalvm-web-image-status-and-limits) · [Known issues](#known-issues)

---

## What this is

myTunes is a single-page radio player: pick a station, press play, change the background, adjust the
volume. It looks and behaves like DevTunes FM. What makes it unusual is that **all of it is Java**.
The station catalogue, the player state machine, the preference store, the DOM construction, the
event handling and the audio control are Java classes compiled ahead of time into a WebAssembly
module by GraalVM Web Image.

There is no backend. Nothing is rendered on a server, and no API is called.

## Why Java to WebAssembly

The interesting part is not "compile Java to Wasm" — several projects do that. It is that
**WebAssembly has no access to the DOM**, so a browser UI written in Java needs an interop layer,
and the usual answer is to hand-write JavaScript for it. This project set out to avoid that, and to
find out whether a framework as reflection-heavy and lifecycle-heavy as Spring Boot survives the
closed-world, ahead-of-time, single-threaded environment a Wasm module runs in.

Six specific adaptations were required to get there, each documented at its call site and
summarised under [GraalVM Web Image: status and limits](#graalvm-web-image-status-and-limits).

## Architecture

```
Docker (nginx, unprivileged)
  └── static assets only, no JVM, no jar
        ├── index.html          bootstrap shell, no behaviour
        ├── styles.css          presentation
        ├── backgrounds/, icons/, audio/
        ├── mytunes.js          GraalVM-generated loader  (the only script the page loads)
        └── mytunes.js.wasm     the entire application
                 ├── Spring Boot 4.1.1
                 ├── Spring Modulith 2.1.0
                 ├── domain: stations, player, persistence, themes
                 └── browser: DOM, audio, localStorage, Media Session, Picture-in-Picture
```

The browser downloads the module, the generated loader instantiates it and calls `main()`, and
`main()` runs `SpringApplication.run(...)` inside the tab.

## Modules

Spring Modulith boundaries are declared in each `package-info.java` and verified by
`ModularityTests`. The rule that matters most is that **only `platform` may touch the browser**,
which is what keeps the domain unit-testable on a plain JVM even though the product only ever runs
as WebAssembly.

| Module | Responsibility | May depend on |
| --- | --- | --- |
| `stations` | Station catalogue and stream URLs | — |
| `themes` | Background artwork and accent colours | — |
| `persistence` | `Preferences`, versioned store, storage abstraction | — |
| `player` | Player state machine. No browser APIs at all | `stations`, `persistence` |
| `platform` | Every JavaScript interop call in the project | `persistence` |
| `ui` | Builds and renders the interface | `player`, `stations`, `themes`, `persistence`, `platform` |

## The zero-JavaScript rule

The repository contains **no `.js`, `.mjs`, `.cjs` or TypeScript source file**, no JavaScript DOM
bridge and no JavaScript audio adapter. The page loads exactly one script: the loader GraalVM
generates.

There is one exception, and it is deliberately visible. GraalVM Web Image offers no way to obtain a
browser global from Java — its interop API has no global accessor, and `@JS.Import` imports a
JavaScript *class* rather than an instance. This was verified against Oracle GraalVM 25.0.4,
GraalVM CE 25.2.4 and the Early Access build `jdk-25i3-25.0.4.1-ea.02`. Reaching the browser
therefore needs exactly two one-expression declarations, both in
[`BrowserWindow`](src/main/java/com/patbaumgartner/mytunes/platform/BrowserWindow.java):

```java
@JS("return document;") static native JSObject document();
@JS("return window;")   static native JSObject window();
```

Everything else — creating elements, registering listeners, controlling audio, reading
`localStorage`, constructing `MediaMetadata` via `Reflect.construct`, driving the Picture-in-Picture
window — is plain Java through `get`, `set` and `call`.

`NoHandwrittenJavaScriptTests` enforces this: it fails if any JavaScript or TypeScript source
appears anywhere in the repository, if any class other than `BrowserWindow` declares `@JS`, if
that surface grows beyond two expressions, or if `index.html` gains an inline script or an event
attribute.

## Prerequisites

| | |
| --- | --- |
| **Oracle GraalVM 25.0.4** | Required, not a preference. It is the only JDK that ships the Wasm backend (`lib/svm/tools/svm-wasm`) and the browser interop module. GraalVM CE has the API but **not** the backend. `sdk install java 25.0.4-graal` |
| **Binaryen 119+** | Web Image assembles output with `wasm-as`, which must be on `PATH` |
| **Docker** | Only for the container build |
| **Java Quality Harness** | Not yet on Maven Central; clone and `./mvnw install -DskipTests` |

## Build from a clean clone

```sh
export JAVA_HOME=/path/to/graalvm-jdk-25.0.4
export PATH="$JAVA_HOME/bin:/path/to/binaryen/bin:$PATH"

# One-time bootstrap. Repackages GraalVM's browser interop module as a Maven artifact so the
# whole build, Spring Boot's AOT step and the quality harness share one compiler configuration.
./tools/install-webimage-api.sh

./mvnw -B -Pnative native:compile      # produces target/mytunes.js and target/mytunes.js.wasm

# Assemble the site
mkdir -p target/site && cp -r src/main/web/. target/site/ \
  && cp target/mytunes.js target/mytunes.js.wasm target/site/
```

`tools/generate-artwork.py` and `tools/generate-audio.py` regenerate the backgrounds, icons and the
self-hosted station. Their output is committed, so a normal build does not need Python.

## Docker

The image needs no local JVM or GraalVM.

```sh
docker build -t mytunes:latest .
docker run -d --name mytunes -p 8099:8080 mytunes:latest
# http://localhost:8099
```

Readiness is `GET /healthz` (also wired as a `HEALTHCHECK`). The runtime stage is unprivileged
nginx containing only the bootstrap page, CSS, artwork, the generated loader and the Wasm module.
You can check the central claim yourself:

```sh
docker run --rm --entrypoint sh mytunes:latest -c "find / -name '*.jar' -o -name java -type f"   # empty
```

Generated Wasm is **not** committed. It is produced during the build.

## Tests

The suite is layered. Everything that can be verified without a browser is, and everything that
cannot is verified in one.

```sh
./mvnw -B test        # 40 JVM tests
```

| Layer | Covers |
| --- | --- |
| Unit | Player state machine, versioned persistence, station and background catalogues |
| Spring context | `PlayerModuleIntegrationTests` refreshes a real context for the `player` module and its declared dependencies with `@ApplicationModuleTest`, proving the module boundaries are sufficient off-browser |
| Modularity | `ModularityTests` — Spring Modulith `detectViolations()` |
| Architecture | `ArchitectureTests` — Taikai, recorded for the harness |
| Constraint | `NoHandwrittenJavaScriptTests` — the zero-JavaScript rule |

Browser tests are **authoritative**, because the application only ever executes in a browser. They
are excluded from the default run because they need a built image, and skip themselves if it is
absent.

```sh
# Local build
./mvnw -B test -Dsurefire.excludes= -Dtest='MyTunesBrowserTests,MiniPlayerBrowserTests' \
  -DfailIfNoTests=false

# …or against the running container
./mvnw -B test -Dsurefire.excludes= -Dtest='MyTunesBrowserTests,MiniPlayerBrowserTests' \
  -DfailIfNoTests=false -Dmytunes.baseUrl=http://127.0.0.1:8099

# Container smoke tests: content types, cache headers, and that the image holds no JVM or jar
./mvnw -B test -Dsurefire.excludes= -Dtest=DockerSmokeTests -DfailIfNoTests=false \
  -Dmytunes.baseUrl=http://127.0.0.1:8099 -Dmytunes.dockerImage=mytunes:latest

# Re-capture the DevTunes FM reference screenshots (reaches the public internet, so opt-in)
./mvnw -B test -Dsurefire.excludes= -Dtest=OriginalSiteCaptureTests -DfailIfNoTests=false \
  -Dmytunes.captureOriginal=true
```

They assert real behaviour, not intent: that `currentTime` actually advances, that the volume
element follows the slider, that state survives a reload, that the page loads exactly one script,
and that `docker run ... find / -name '*.jar' -o -name java` returns nothing.

Module size and startup timing are recorded to `docs/parity/console/wasm-diagnostics.log` on every
browser run:

```
wallClockToInterfaceReadyMs=1008
springStartup=[mytunes] Spring Boot 4.1.1 started in the browser in 240ms with 57 beans
mytunes.js.wasm=30003411 bytes
mytunes.js=96732 bytes
```

## Quality harness

[java-quality-harness](https://github.com/patbaumgartner/java-quality-harness) is the definition of
done. Install it once, then:

```sh
./mvnw clean                               # see the note below
./mvnw jqh:check -Djqh.tier=pre-commit     # the gate
./mvnw jqh:fix                             # apply every safe automatic fix
```

Run the gate on a clean tree. A preceding native build leaves Spring Boot's AOT-generated
bean-definition classes in `target/classes`, and those are build output rather than authored
code: forbidden-apis cannot resolve their references and reports a scan error, and Taikai would
hold them to this project's conventions. That matches the harness's own principle that it never
grades output left behind by an earlier command.

**Current result: 0 blocking findings.** format, nullness, nullness-contract, checkstyle, PMD,
SpotBugs, forbidden-apis, dependency-convergence, dependency-policy, tests, test-hygiene,
secret-scan, java-version, configuration, architecture and modularity all pass.

Two mandatory checks still report a failure, and both are left visible rather than tuned away. The
harness suppresses an exempted *finding* but deliberately keeps the *check* red, so an exemption can
never make a mandatory check look clean — which is why the verdict is non-zero while blocking
findings are zero.

| Check | Why it cannot pass | Recorded as |
| --- | --- | --- |
| `coverage` | JaCoCo instruments JVM bytecode. The `platform`, `ui` and `wasm` classes execute as WebAssembly, where no Java agent exists, so they measure 0% at any threshold. They are verified in a real browser instead | Scoped, owned, expiring exemption in `.jqh.yaml`. The domain they delegate to is held to the full default thresholds |
| `test-hygiene` | `OriginalSiteCaptureTests` loads the live DevTunes FM site, which is network-dependent by definition. A local fake would defeat the purpose of a parity reference. It is opt-in and skips by default | Same mechanism. No other test in this repository reaches the network |

## Parity evidence

Screenshots at four fixed breakpoints for both the original and myTunes, console logs, and a
feature-by-feature checklist with a verdict and named evidence for every row:

- [`docs/parity/parity-checklist.md`](docs/parity/parity-checklist.md) — 27 PASS, 2 PARTIAL, 1 BLOCKED
- `docs/parity/original/`, `docs/parity/mytunes/`, `docs/parity/console/`

Reproduction commands are at the end of the checklist. Breakpoints are `desktop-wide` 1920×1080,
`desktop-standard` 1440×900, `mobile-portrait` 390×844, `mobile-small` 360×640.

## Stored data

`localStorage`, through the Java interop layer. Nothing secret is stored; these are display and
playback preferences.

| Key | Meaning |
| --- | --- |
| `mytunes.schema` | Storage format version, currently `1` |
| `mytunes.station` | Selected station id |
| `mytunes.background` | Selected background id |
| `mytunes.volume` | `0.0`–`1.0` |
| `mytunes.muted` | `true` / `false` |

A record written by a **newer** schema is discarded rather than half-read, so a future format
degrades to defaults instead of restoring corrupt state. Unparseable values fall back individually.
Storage failures (private mode, exhausted quota) are contained: losing a volume preference must
never stop the radio.

## Stations and streams

**DevTunes FM's streams are not reused.** Its audio comes from SoundCloud CDN URLs carrying signed
CloudFront `Policy`, `Signature` and `Key-Pair-Id` parameters minted by its own private endpoint.
Those are time-limited credentials belonging to someone else's session. Copying them would be
exactly the credential theft this project was told to avoid, so none appear here — and
`StationCatalogueTests` fails the build if any stream URL ever gains those parameters.

| Station | Source | Observed behaviour |
| --- | --- | --- |
| **myTunes Signal** (default) | This repository, `tools/generate-audio.py` | Always available. Same origin, no CORS question |
| Groove Salad, Drone Zone, Lush, Space Station, Deep Space One, DEF CON Radio | [SomaFM](https://somafm.com/) | Answer curl with `Access-Control-Allow-Origin: *` and `audio/mpeg`, but returned **HTTP 403 to browser requests on every mount tested** on 2026-08-21 |

Caveats, stated plainly:

- SomaFM's 403 to browser requests is **their prerogative**, not a defect here. They are
  listener-supported and entitled to refuse hotlinking. The entries stay configured and documented
  rather than silently deleted, and the default station is self-hosted so real playback is always
  demonstrable.
- **Stream availability changes independently of myTunes.** A URL that works today may not tomorrow.
- **Before any production or commercial use**, replace these with streams you own or are licensed to
  use, and honour each provider's terms.
- No API key, token, cookie or credential is committed. `secret-scan` passes.

## Artwork

The five backgrounds and nine icons are **original flat-design SVG** generated by
`tools/generate-artwork.py`, and the default station is an ambient loop synthesised by
`tools/generate-audio.py`. They are covered by this repository's own licence.

DevTunes FM serves wallpapers sourced from wallhaven. Their redistribution terms are not
established, so **none are reused**, and `BackgroundCatalogueTests` asserts that all artwork is
served from this repository.

## Browser support

Verified in Chromium via Playwright. Requires WebAssembly with GC and exception handling, so a
current Chromium, Firefox or Safari. Node 22–24 needs `--experimental-wasm-exnref`.

The uncompressed module is ~30 MB (built with `-g` debug annotations and no size tuning); nginx
serves it gzipped and marks it immutable. Startup from navigation to a rendered interface is under
about a second locally. Correctness was prioritised over size, as instructed; reducing it by
dropping `-g` and trimming reachability is the obvious next step.

## Mobile

Verified at 390×844 and 360×640. The transport and volume rows stack to full width, touch targets
stay at the 44 px minimum, and `env(safe-area-inset-bottom)` is respected. Zero console errors at
every breakpoint.

Browser autoplay policy is respected rather than worked around: the first audible playback needs a
real tap. iOS does not expose volume to script, so the slider is inert there while mute still works —
a platform rule, not a bug.

## Media Session and mini player

Both are feature-detected and degrade to nothing where unsupported.

**Media Session** publishes title, station, album and artwork, and handles play, pause, next and
previous, which is what drives lock-screen and keyboard media keys.

**Document Picture-in-Picture** opens a floating always-on-top mini player. `requestWindow()`
returns a second `Window`, which reaches Java as an ordinary `JSObject`, so the mini player is built
with the same Java DOM vocabulary and needs no extra JavaScript.

One limit stated honestly: the mini player stays visible while the tab is hidden or you work in
other applications, but **it cannot outlive the browser process**. No web application can keep a
window open after the browser that owns it is closed. That would require an installed native
application, and no amount of browser API makes it possible.

## GraalVM Web Image: status and limits

Web Image is **experimental**. Limits that shaped this code, all found by building and running:

1. **Single-threaded.** No `Thread`, no `ScheduledExecutorService`, no `@Scheduled`. Spring Boot's
   shutdown hook is disabled and `spring-modulith-moments` is excluded, because its cron-scheduled
   passage-of-time events force a worker thread.
2. **`StackWalker` always throws.** `SpringApplication.deduceMainApplicationClass()` walks the stack,
   so it is substituted to read `primarySources` instead. log4j's `StackLocator` does too, so
   `spring-boot-starter-logging` is excluded.
3. **`java.io.Console` cannot link.** `System.console()` is substituted to return `null`, which is
   the specified value when there is no terminal.
4. **`@JS.Import`, `@JS.Export` and `JSObject` subclasses are unimplemented** in shipping releases.
5. **Java stack traces are empty inside Wasm.** Diagnosis needs `-g` plus a raised
   `Error.stackTraceLimit` to read named Wasm symbols.
6. **The build requires Oracle GraalVM**, because the interop API is a JDK module.

## Known issues

- **Live track titles are not shown.** The original reads them from its own playlist service.
  myTunes has no server, and ICY stream metadata is not exposed to browser JavaScript, so per-track
  titles are not obtainable. The station name and genre are shown instead; `PlayerState.nowPlaying`
  is in place for when a source exists.
- **SomaFM stations return 403 in a browser.** See [Stations and streams](#stations-and-streams).
- **`coverage` cannot pass.** See [Quality harness](#quality-harness).
- **Module size is untuned** at ~30 MB uncompressed.
- **Maven coordinate spelling.** The brief contained both `com.patbaumgartner` and, once,
  `com.patbaumagartner`. `com.patbaumgartner` was confirmed as correct and is used throughout.

## Licence

[Apache License 2.0](LICENSE). That covers the code and the generated artwork and audio, which is
the point of generating them rather than reusing someone else's: the whole bundle can be
redistributed under one clear licence.

Third-party streams are not covered and remain subject to their providers' terms.
