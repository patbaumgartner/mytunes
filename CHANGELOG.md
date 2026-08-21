# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Continuous integration: formatting + JVM suite, a from-scratch container build with browser
  and smoke tests against the image, CodeQL analysis, and dependency review on pull requests.
- Dependabot updates for Maven, GitHub Actions and Docker.
- Community health files: contributing guide, security policy, code of conduct, issue and pull
  request templates.
- Committed parity evidence under `docs/parity/` with a feature-by-feature checklist.

### Changed

- **Breaking for downstream test runs:** the test suite no longer depends on the private
  `jqh-test-support` artifact; a clean public clone can now run `./mvnw test` with only the
  documented GraalVM bootstrap. Playwright and Taikai moved from the removed `quality` profile
  to regular test dependencies, and `-DskipQualityDeps` no longer exists.

### Removed

- The Java Quality Harness configuration (`.jqh.yaml`, `.jqh/`) and its Maven plugin wiring,
  which referenced tooling that is not publicly available.
- Dead platform API: unused audio getters, the unused Picture-in-Picture `isOpen()` state, and
  an unused package-private accessor.

## [1.0.0] - unreleased

Initial implementation: Spring Boot 4.1.1 + Spring Modulith 2.1.0 compiled to WebAssembly by
GraalVM Web Image, running entirely in the browser with a Java-built DOM interface, audio
playback, persistent preferences, Media Session integration and a Document Picture-in-Picture
mini player, served by an unprivileged nginx container holding no JVM.
