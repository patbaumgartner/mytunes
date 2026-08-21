# Security Policy

## Supported versions

myTunes is a research project demonstrating Spring Boot compiled to WebAssembly. Only the
current state of `main` is supported.

| Version | Supported |
| --- | --- |
| `main` | ✅ |
| anything older | ❌ |

## What runs where — the threat model in two sentences

The entire application executes inside the visitor's browser sandbox; the container serves
static files through unprivileged nginx and holds no JVM, no application code and no data.
There is no server-side attack surface beyond nginx itself, no stored user data beyond
`localStorage` display preferences, and no credentials anywhere in the repository.

## Reporting a vulnerability

Please report vulnerabilities privately through
[GitHub Security Advisories](https://github.com/patbaumgartner/mytunes/security/advisories/new)
— do **not** open a public issue for anything you believe is exploitable.

You can expect an acknowledgement within a week. If the report is accepted, a fix lands on
`main` with credit to the reporter (unless you prefer otherwise).

## Scope notes

- The SomaFM stream URLs are third-party services; issues with those endpoints belong to
  [SomaFM](https://somafm.com/), not this project.
- The `Content-Security-Policy` served by the container is part of the security posture and is
  asserted by `DockerSmokeTests`; weakening it is treated as a defect.
