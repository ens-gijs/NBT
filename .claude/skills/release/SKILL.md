---
name: release
description: |
  Cut a Maven Central release of nbt-mca. Use when the user says "cut a release",
  "release vX.Y.Z", "publish to Maven Central", or anything that means promoting a
  -SNAPSHOT to a tagged version on master. Walks the full sequence with safety checks
  before each irreversible step. Do NOT run autonomously — pause for explicit user
  confirmation before tagging or pushing.
---

# Release skill — nbt-mca

This codifies the manual release sequence so it runs the same way every time. The
publish itself is performed by the `publish.yml` GitHub Actions workflow when a
`vX.Y.Z` tag is pushed; this skill prepares and pushes that tag.

The `publish-snapshot.yml` workflow already keeps `-SNAPSHOT` builds flowing to
the Central snapshot repo on every push to `master` — this skill is only for
cutting tagged stable releases.

## Inputs

- **Target version** Identify current working version (e.g. `0.2.0-SNAPSHOT`). Drop -SNAPSHOT suffix and follow SemVer (e.g. `0.2.0`).
- **Next dev version** (default: bump minor and append `-SNAPSHOT`, e.g. `0.3.0-SNAPSHOT`).

Confirm release version number and next dev version number before proceeding.

## Pre-flight (run all; abort on any failure)

Report each check's result before proceeding.

1. **Working tree clean.** `git status --porcelain` is empty.
2. **On `master`.** `git rev-parse --abbrev-ref HEAD` == `master`.
3. **Up to date with origin.** `git fetch origin && git status -sb` shows no
   "behind" indicator.
4. **CI green for HEAD.** `gh run list --branch master --limit 1 --json conclusion`
   reports `success`. If `gh` isn't available, ask the user to confirm CI is green.
5. **No `-SNAPSHOT` deps in published POM.** Run
   `./gradlew publishToMavenLocal` and grep `~/.m2/repository/io/github/ens-gijs/nbt/`
   POMs for `-SNAPSHOT`. Should only match the project's own version.
6. **Tag doesn't already exist.** `git rev-parse v<VERSION>` should fail.
7. **Tests pass.** `./gradlew clean build`.
8. **japicmp baseline check** (only if a previous release exists): run
   `./gradlew japicmp -PpreviousVersion=<previousReleaseVersion>` and surface
   the report. At 0.x this is advisory; at 1.x+ binary breaks should block.

After all checks pass, **show the user a summary and ask for explicit
confirmation** before doing any of the steps below.

## Release sequence

Each step is a separate commit so the release can be reverted cleanly if needed.

### Commit 1 — Release commit
1. Edit root `build.gradle`: set `version = '<TARGET_VERSION>'` (drop `-SNAPSHOT`).
2. Edit `CHANGELOG.md`:
   - Change `## [<TARGET_VERSION>] - TBD` → `## [<TARGET_VERSION>] - <YYYY-MM-DD>`.
   - If the release section is `[Unreleased]`, rename it to `[<TARGET_VERSION>] - <date>` and add a fresh empty `[Unreleased]` section above.
   - Update the comparison/tag links at the bottom.
3. Edit `README.md` dependency snippet to use `<TARGET_VERSION>` (no `-SNAPSHOT`).
4. `./gradlew build` — final confidence check.
5. `git add -A && git commit -m "Release <TARGET_VERSION>"`.

### Commit 2 — Tag, push, wait for publish action, create github release, update `latest` branch
1. `git tag -a v<TARGET_VERSION> -m "Release <TARGET_VERSION>"`.
2. **Stop and confirm with user before pushing.** Pushing the tag fires the
   publish workflow and uploads to Maven Central — this is irreversible
   (you can drop a *deployment* in the Central UI, but only before promotion;
   our workflow auto-promotes via `publishAndReleaseToMavenCentral`).
3. `git push origin master && git push origin v<TARGET_VERSION>`.
4. **Wait for publish workflow to complete.** Poll `gh run list --workflow publish.yml --limit 1 --json status,conclusion` until the run shows `status: "completed"` with `conclusion: "success"`. Report any failures to the user before proceeding.
5. **Create a GitHub release** from the tag. Once the publish workflow succeeds, run:
   ```sh
   gh release create v<TARGET_VERSION> --notes-from-tag
   ```
   This creates a release from the annotated tag, using the CHANGELOG entry as the body.

6. **Update the `latest` branch** to point to the release tag, and set it as the default branch:
   ```sh
   git branch -f latest v<TARGET_VERSION>
   git push origin latest --force
   gh repo edit --default-branch latest
   ```
   This ensures visitors to the repo on GitHub.com see the latest stable release by default instead of the development master branch.

### Commit 3 — Resume development
1. Confirm with the user that everything looks good before proceeding.
2. Edit root `build.gradle`: set `version = '<NEXT_DEV_VERSION>'` (with `-SNAPSHOT`).
3. Edit `README.md` snippet back to `<NEXT_DEV_VERSION>`.
4. `git add -A && git commit -m "Bump version to <NEXT_DEV_VERSION>"`.
5. `git push origin master`.

## Things to refuse

- Do not push a tag the user didn't explicitly confirm.
- Do not amend or force-push to `master`.
- Do not skip the pre-flight checks even if the user says "just do it" — surface
  the failure and ask if they want to override one specific check, then proceed
  with everything else.
- Do not generate or modify GPG keys, Sonatype tokens, or
  `~/.gradle/gradle.properties`. Those are user-managed.
