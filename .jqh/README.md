# Java Quality Harness hooks

These scripts are the harness's entry points from a git hook and from a coding agent.
Both delegate to `mvn jqh:check` so that a local result and a CI result cannot
disagree.

| Script | Tier | When |
|---|---|---|
| `pre-commit` | `pre-commit` | Before every commit. Blocks on mandatory failures. |
| `agent-post-edit` | `fast` | After an agent edits or generates code. |

Before fixing findings by hand, apply the automatic ones:

```sh
mvn jqh:fix
```

Enable the git hook:

```sh
git config core.hooksPath .jqh/hooks
```

CI runs the full repository validation:

```sh
mvn jqh:check -Djqh.tier=ci
```

Releases and scheduled builds run the extended tier, which adds PIT mutation testing
on top of everything the `ci` tier runs:

```sh
mvn jqh:check -Djqh.tier=extended
```

Run `mvn jqh:help -Ddetail=true` for every goal and parameter.

## Outputs

Every run writes four views of one result to `target/jqh/`:

- `jqh-report.json` — machine readable, the contract for tooling
- `jqh-report.sarif` — for code scanning interfaces
- `jqh-summary.txt` — the concise human summary
- `jqh-agent-prompt.md` — the remediation prompt for a coding agent

## Exceptions

`JQH_SKIP` exists for genuine emergencies and announces itself loudly. It is not a
remediation strategy. The sanctioned way to not satisfy a rule is a scoped,
documented, owned and expiring exception in `.jqh.yaml`, which stays visible in
review and stops working on its expiry date.
