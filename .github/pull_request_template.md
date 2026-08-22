## What changed and why

<!-- Link the issue if one exists. -->

## How it was verified

<!-- Which of these ran green locally? Delete lines that do not apply. -->

- [ ] `./mvnw spring-javaformat:apply` (formatting)
- [ ] `./mvnw -B test` (JVM suite)
- [ ] Browser tests against a built image (for anything the browser can observe)

## Checklist

- [ ] No hand-written JavaScript/TypeScript introduced (`NoHandwrittenJavaScriptTests` stays green)
- [ ] Module boundaries respected (`ModularityTests`, `ArchitectureTests` stay green)
- [ ] Docs updated where behaviour changed
