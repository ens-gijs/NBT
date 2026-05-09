---
name: release
description: |
  Cut a Maven Central release of either nbt-core or nbt-mca. Use when the user
  says "release nbt-core vX.Y.Z", "release nbt-mca vX.Y.Z", "publish to Maven
  Central", or anything that means promoting a -SNAPSHOT to a tagged version.
  Walks the full sequence with safety checks before each irreversible step. Do
  NOT run autonomously — pause for explicit user confirmation before tagging or
  pushing.
---

# Release skill — nbt-core / nbt-mca

This codifies the per-module release sequence. Each module (`nbt-core`,
`nbt-mca`) is released independently — they have separate versions in
`gradle.properties`, separate CHANGELOGs, separate git tag prefixes, and
separate publish workflows.

The publish itself is performed by `.github/workflows/publish-core.yml` /
`publish-mca.yml` when the matching tag is pushed; this skill prepares
and pushes that tag.

The `publish-snapshot.yml` workflow already keeps `-SNAPSHOT` builds
flowing to the Central snapshot repo on every push to `master` — this
skill is only for cutting tagged stable releases.

## Module selection

**Ask the user up front which module is being released.** Either `nbt-core`
or `nbt-mca`. Then bind the per-module variables for the rest of the flow:

| Variable             | If `nbt-core`                          | If `nbt-mca`                          |
| -------------------- | -------------------------------------- | ------------------------------------- |
| `<MODULE>`           | `nbt-core`                             | `nbt-mca`                             |
| `<VERSION_KEY>`      | `coreVersion`                          | `mcaVersion`                          |
| `<TAG_PREFIX>`       | `core-v`                               | `mca-v`                               |
| `<CHANGELOG>`        | `nbt-core/CHANGELOG.md`                | `nbt-mca/CHANGELOG.md`                |
| `<PUBLISH_WORKFLOW>` | `publish-core.yml`                     | `publish-mca.yml`                     |
| `<M2_PATH>`          | `~/.m2/repository/io/github/ens-gijs/nbt/nbt-core/` | `~/.m2/repository/io/github/ens-gijs/nbt/nbt-mca/` |

## Inputs

- **Target version** — current working version of `<MODULE>` from
  `gradle.properties` (e.g. `0.1.1-SNAPSHOT`). Drop `-SNAPSHOT` and follow
  SemVer (e.g. `0.1.1`).
- **Next dev version** (default: bump minor and append `-SNAPSHOT`,
  e.g. `0.2.0-SNAPSHOT`).

Confirm release version number and next dev version number before
proceeding.

## Pre-flight (run all; abort on any failure)

Report each check's result before proceeding.

1. **Working tree clean.** `git status --porcelain` is empty.
2. **On `master`.** `git rev-parse --abbrev-ref HEAD` == `master`.
3. **Up to date with origin.** `git fetch origin && git status -sb` shows no
   "behind" indicator.
4. **CI green for HEAD.** `gh run list --branch master --limit 1 --json conclusion`
   reports `success`. If `gh` isn't available, ask the user to confirm.
5. **No `-SNAPSHOT` deps in `<MODULE>`'s published POM.** Run
   `./gradlew :<MODULE>:publishToMavenLocal` and grep `<M2_PATH>` POMs for
   `-SNAPSHOT`. The only acceptable match is the project's own version
   (which is about to be released, so will lose `-SNAPSHOT` in the next
   step). If `nbt-mca` is being released, its dep on `nbt-core` must point
   at a non-SNAPSHOT version on Central — if `nbt-core` is currently
   `-SNAPSHOT`, release `nbt-core` first.
6. **Tag doesn't already exist.** `git rev-parse <TAG_PREFIX><VERSION>` should fail.
7. **Tests pass.** `./gradlew clean build`.
8. **japicmp baseline check** (only if a previous release of `<MODULE>` exists):
   run `./gradlew :<MODULE>:japicmp -PpreviousVersion=<previousReleaseVersion>`
   and surface the report. At 0.x this is advisory; at 1.x+ binary breaks
   should block.

After all checks pass, **show the user a summary and ask for explicit
confirmation** before doing any of the steps below.

## Release sequence

Each step is a separate commit so the release can be reverted cleanly if needed.

### Commit 1 — Release commit
1. Edit `gradle.properties`: set `<VERSION_KEY>=<TARGET_VERSION>` (drop `-SNAPSHOT`).
2. Edit `<CHANGELOG>`:
   - Change `## [<TARGET_VERSION>] - TBD` → `## [<TARGET_VERSION>] - <YYYY-MM-DD>`.
   - If the release section is `[Unreleased]`, rename it to
     `[<TARGET_VERSION>] - <date>` and add a fresh empty `[Unreleased]`
     section above.
   - Update the comparison/tag links at the bottom.
3. Edit `README.md` — if the dependency snippet for `<MODULE>` shows the
   version explicitly, bump it to `<TARGET_VERSION>` (drop `-SNAPSHOT`).
4. `./gradlew :<MODULE>:build` — final confidence check.
5. `git add -A && git commit -m "Release <MODULE> <TARGET_VERSION>"`.

### Commit 2 — Tag, push, wait for publish action, create GitHub release
1. `git tag -a <TAG_PREFIX><TARGET_VERSION> -m "Release <MODULE> <TARGET_VERSION>"`.
2. **Stop and confirm with user before pushing.** Pushing the tag fires
   the publish workflow and uploads to Maven Central — this is irreversible
   (you can drop a *deployment* in the Central UI, but only before promotion;
   our workflow auto-promotes via `publishAndReleaseToMavenCentral`).
3. `git push origin master && git push origin <TAG_PREFIX><TARGET_VERSION>`.
4. **Wait for publish workflow to complete.** Poll
   `gh run list --workflow <PUBLISH_WORKFLOW> --limit 1 --json status,conclusion`
   until the run shows `status: "completed"` with `conclusion: "success"`.
   Report any failures to the user before proceeding.
5. **Create a GitHub release** from the tag:
   ```sh
   gh release create <TAG_PREFIX><TARGET_VERSION> \
     --title "<MODULE> <TARGET_VERSION>" \
     --notes-from-tag
   ```
   The `--title` makes the release card readable in the GitHub UI even
   though the tag has the prefix.

### Commit 3 — Resume development
1. Confirm with the user that everything looks good before proceeding.
2. Edit `gradle.properties`: set `<VERSION_KEY>=<NEXT_DEV_VERSION>` (with
   `-SNAPSHOT`).
3. Edit `README.md` — if the dependency snippet shows the version
   explicitly, restore it to `<NEXT_DEV_VERSION>`.
4. `git add -A && git commit -m "Bump <MODULE> version to <NEXT_DEV_VERSION>"`.
5. `git push origin master`.

## Things to refuse

- Do not push a tag the user didn't explicitly confirm.
- Do not amend or force-push to `master`.
- Do not skip the pre-flight checks even if the user says "just do it" —
  surface the failure and ask if they want to override one specific check,
  then proceed with everything else.
- Do not generate or modify GPG keys, Sonatype tokens, or
  `~/.gradle/gradle.properties`. Those are user-managed.
- Do not release `nbt-mca` against a `-SNAPSHOT` `nbt-core` dependency.
  If `nbt-mca` is being released and its current `nbt-core` dep is
  `-SNAPSHOT`, halt and tell the user to release `nbt-core` first.
