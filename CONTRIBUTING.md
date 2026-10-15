# Contributing

## Workflow
1. `git checkout main; git pull`
2. `git checkout -b <type>/<short-description>` (example: `feature/mission-crud`)
3. Commit small and often, then `git push -u origin <branch>`
4. Open a Pull Request (`gh pr create --fill`), link the issue with `Closes #N`
5. Wait for the green `build` check and 1 approval, then squash-merge

Never push to `main`. It is protected.

## Branch names
`feature/...`, `fix/...`, `docs/...`, `chore/...`, `test/...`

## Commit messages (Conventional Commits)
`<type>: <what changed>` with type in: feat, fix, docs, chore, test, refactor.
Examples: `feat: add mission entity`, `fix: reject overlapping assignments`

## Review rules
- Nobody merges their own PR without a review.
- Review within 24 hours. Be specific, ask questions, suggest changes.
- A PR with a failing `build` check is never merged.

## Database
- Schema changes are new Flyway files: `src/main/resources/db/migration/V<n>__description.sql`.
- Never edit a migration that is already on `main`.
- The dev database listens on port 5455 (`docker compose up -d db`).
