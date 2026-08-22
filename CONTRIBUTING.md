# Contributing to myTunes

Thanks for your interest! This project has one unusual rule and a heavyweight toolchain, so
please read this page before opening a pull request.

## The one rule that is different here

**No hand-written JavaScript or TypeScript, anywhere.** The entire application — including the
DOM construction and event handling — is Java compiled to WebAssembly. The only permitted
JavaScript is the two one-expression `@JS` declarations in `BrowserWindow`.
`NoHandwrittenJavaScriptTests` fails the build if a `.js`/`.ts` source file appears, if any other
class declares `@JS`, or if `index.html` gains an inline script. Pull requests that work around
this rule will be declined regardless of how useful the feature is.

## Toolchain

| Requirement | Why |
| --- | --- |
| [Oracle GraalVM 25.0.4](https://www.graalvm.org/) (`sdk install java 25.0.4-graal`) | The only JDK shipping the Wasm backend and a matching browser interop module — GraalVM CE carries a diverged copy of that API, and the build does not compile against it |
| [Binaryen](https://github.com/WebAssembly/binaryen) 119+ (`wasm-as` on `PATH`) | Web Image assembles its output with `wasm-as` |
| Docker | Only for the container build — the image downloads its own toolchain |

## Setup and build

```sh
export JAVA_HOME=/path/to/graalvm-jdk-25.0.4
export PATH="$JAVA_HOME/bin:/path/to/binaryen/bin:$PATH"

# One-time: repackage GraalVM's browser interop module as a Maven artifact
./tools/install-webimage-api.sh

./mvnw -B test                        # JVM suite — needs no browser and no Wasm build
./mvnw -B -Pnative native:compile     # produce target/mytunes.js + target/mytunes.js.wasm

# Assemble the site and run the authoritative browser tests
mkdir -p target/site && cp -r src/main/web/. target/site/ \
  && cp target/mytunes.js target/mytunes.js.wasm target/site/
./mvnw -B test -Dsurefire.excludes= -Dtest='MyTunesBrowserTests,MiniPlayerBrowserTests' \
  -DfailIfNoTests=false
```

## Before you open a pull request

1. `./mvnw spring-javaformat:apply` — the CI gate runs `spring-javaformat:validate`.
2. `./mvnw -B test` — all JVM tests green.
3. If you touched anything the browser can observe, run the browser tests above; they are the
   authoritative suite.
4. Changes to module boundaries must keep `ModularityTests` and `ArchitectureTests` green —
   the boundaries are the design, not a suggestion.
5. Write the commit summary the way this repository's history does: a short imperative
   sentence describing the change ("Serve from an unprivileged nginx container"), not a
   `type:` prefix.

## What CI runs

Every push and pull request runs formatting validation and the JVM suite, then builds the
Docker image from scratch (including the in-container Wasm compile) and runs the browser and
container smoke tests against it. A pull request is mergeable when everything is green.

## Reporting bugs and proposing features

Use the issue templates. For anything security-relevant, follow [SECURITY.md](SECURITY.md)
instead of opening a public issue.
