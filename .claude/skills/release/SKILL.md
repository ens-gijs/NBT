---
name: release
description: |
  Cut a Maven Central release of either nbt or nbt-mca. Use when the user
  says "release nbt vX.Y.Z", "release nbt-mca vX.Y.Z", "publish to Maven
  Central", or anything that means promoting a -SNAPSHOT to a tagged version.
  Walks the full sequence with safety checks before each irreversible step. Do
  NOT run autonomously — pause for explicit user confirmation before tagging or
  pushing.
---

# Release skill — nbt / nbt-mca

This codifies the per-module release sequence. Each module (`nbt`, `nbt-mca`)
is released independently — they have separate versions in
`gradle.properties`, separate CHANGELOGs, separate git tag prefixes, and
separate publish workflows.

The publish itself is performed by `.github/workflows/publish-nbt.yml` /
`publish-mca.yml` when the matching tag is pushed; this skill prepares
and pushes that tag.

The `publish-snapshot.yml` workflow already keeps `-SNAPSHOT` builds
flowing to the Central snapshot repo on every push to `master` — this
skill is only for cutting tagged stable releases.

## Module selection

**Ask the user up front which module is being released.** Either `nbt` or
`nbt-mca`. Then bind the per-module variables for the rest of the flow:

| Variable             | If `nbt`                                       | If `nbt-mca`                                       |
| -------------------- | ---------------------------------------------- | -------------------------------------------------- |
| `<MODULE>`           | `nbt`                                          | `nbt-mca`                                          |
| `<VERSION_KEY>`      | `nbtVersion`                                   | `mcaVersion`                                       |
| `<TAG_PREFIX>`       | `nbt-v`                                        | `mca-v`                                            |
| `<CHANGELOG>`        | `nbt/CHANGELOG.md`                             | `nbt-mca/CHANGELOG.md`                             |
| `<PUBLISH_WORKFLOW>` | `publish-nbt.yml`                              | `publish-mca.yml`                                  |
| `<M2_PATH>`          | `~/.m2/repository/io/github/ens-gijs/nbt/nbt/` | `~/.m2/repository/io/github/ens-gijs/nbt/nbt-mca/` |

## Inputs

- **Target version** — current working version of `<MODULE>` from
  `gradle.properties` (e.g. `0.1.1-SNAPSHOT`). Drop `-SNAPSHOT` and follow
  SemVer (e.g. `0.1.1`).

Confirm release version number before proceeding.

> **Note:** there is no "next dev version" / "resume development" step.
> After the release, `<VERSION_KEY>` stays at the bare release value
> (`<TARGET_VERSION>`). The next commit that actually changes `<MODULE>`
> source is responsible for bumping it to the next `-SNAPSHOT`. See
> CLAUDE.md "Version coupling between :nbt and :nbt-mca" for the rationale
> — this convention is what lets us read `gradle.properties` to answer
> "does `<MODULE>` have unreleased changes?"

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
   (about to be released, so will lose `-SNAPSHOT` in the next step).

   **If releasing `nbt-mca`:** the cross-module dep on `nbt` is the
   common gotcha. `nbt-mca` declares `api project(':nbt')`, which means
   the published POM's nbt-version is whatever `nbtVersion` is in
   `gradle.properties` at publish time. Read it and branch:

   - **`nbtVersion` is a bare release** (e.g. `0.1.1`, no `-SNAPSHOT`):
     no action needed. The released nbt-mca POM will declare
     `nbt:0.1.1` automatically. Proceed.
   - **`nbtVersion` is `-SNAPSHOT`**: nbt has unreleased changes. Two
     options to surface to the user:
     - **(A) Release `nbt` first.** Re-enter this skill for
       `<MODULE>=nbt`, finish that release end-to-end, *then* return to
       release `nbt-mca`. Both modules end up at fresh release versions.
     - **(B) Pin to the most recent `nbt-v*` tag.** Look up the latest
       `nbt-v*` tag (`git tag -l 'nbt-v*' --sort=-v:refname | head -1`),
       strip the `nbt-v` prefix to get e.g. `0.1.1`. Capture the current
       `-SNAPSHOT` value as `<NBT_DEV_VERSION>` (e.g. `0.1.2-SNAPSHOT`)
       so it can be restored after the release commit. The release
       commit edits `gradle.properties` to set `nbtVersion=0.1.1` **in
       addition to** the `mcaVersion` bump. After tagging, a follow-up
       commit restores `nbtVersion=<NBT_DEV_VERSION>` so the unreleased
       nbt changes keep accumulating.

   The build-time `verifyReleaseDeps` guard in `nbt-mca/build.gradle`
   will hard-fail the publish if you skip this step — but catching it
   here saves a CI round-trip.
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
2. **If `<MODULE>=nbt-mca` AND option (B) was chosen in pre-flight #5**
   (pin to a previously-released `nbt`): also edit `gradle.properties` to
   set `nbtVersion=<PINNED_NBT_VERSION>` (the bare release, no -SNAPSHOT).
   Keep `<NBT_DEV_VERSION>` recorded for restoration in the post-release
   commit below.
3. Edit `<CHANGELOG>`:
   - Change `## [<TARGET_VERSION>] - TBD` → `## [<TARGET_VERSION>] - <YYYY-MM-DD>`.
   - If the release section is `[Unreleased]`, rename it to
     `[<TARGET_VERSION>] - <date>` and add a fresh empty `[Unreleased]`
     section above.
   - Update the comparison/tag links at the bottom.
4. Edit `README.md` — if the dependency snippet for `<MODULE>` shows the
   version explicitly, bump it to `<TARGET_VERSION>` (drop `-SNAPSHOT`).
5. `./gradlew :<MODULE>:build` — final confidence check.
6. `./gradlew :<MODULE>:publishToMavenLocal` — generates the POM that will
   ship. Verify the dep block: for nbt-mca, the `<dependency>` for
   `nbt` must show `<version>` as a release (no `-SNAPSHOT`).
   The `verifyReleaseDeps` task wired into `:nbt-mca:publish*` will
   refuse the publish anyway if this is wrong; this step catches it
   one round-trip earlier.
7. `git add -A && git commit -m "Release <MODULE> <TARGET_VERSION>"`.

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

### Commit 3 — Restore pinned nbtVersion (only if option (B) was used)

**Skip this step entirely unless:** `<MODULE>=nbt-mca` AND `nbtVersion`
was pinned to a previous release in commit 1 (pre-flight option B).

For all other cases — including the standard nbt release flow and
nbt-mca releases where nbt was already at a bare release version — the
release is **complete after commit 2**. There is no "resume development"
auto-bump. `<VERSION_KEY>` stays at `<TARGET_VERSION>` until a later
commit actually changes the module's source.

If commit 3 applies:

1. Confirm with the user that everything looks good before proceeding.
2. Edit `gradle.properties`: restore `nbtVersion=<NBT_DEV_VERSION>` (the
   `-SNAPSHOT` value captured in pre-flight #5). This puts nbt-mca's
   source-build back on the active nbt SNAPSHOT line so subsequent
   nbt-mca SNAPSHOT publishes correctly declare a SNAPSHOT nbt dep.
3. `git add gradle.properties && git commit -m "Restore nbtVersion to <NBT_DEV_VERSION> after nbt-mca <TARGET_VERSION> release"`.
4. `git push origin master`.

## Things to refuse

- Do not push a tag the user didn't explicitly confirm.
- Do not amend or force-push to `master`.
- Do not skip the pre-flight checks even if the user says "just do it" —
  surface the failure and ask if they want to override one specific check,
  then proceed with everything else.
- Do not generate or modify GPG keys, Sonatype tokens, or
  `~/.gradle/gradle.properties`. Those are user-managed.
- Do not release `nbt-mca` against a `-SNAPSHOT` `nbt` dependency.
  If `nbt-mca` is being released and `nbtVersion` in `gradle.properties`
  is `-SNAPSHOT`, halt and apply pre-flight #5's option (A) or (B)
  before proceeding. The `verifyReleaseDeps` task in
  `nbt-mca/build.gradle` is the build-time backstop.
