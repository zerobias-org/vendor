---
name: migrate-packages
description: Migrate the next batch of vendor packages to the gradle pipeline. Drops per-package build.gradle.kts marker, ensures .npmrc, runs ./gradlew :<vendor>:gate, fixes drift, major-bumps the version (1.x → 2.0.0), commits per-package.
argument-hint: "[<vendor>...] [--batch=N] [--dry-run]"
---

# Migrate Vendor Packages

Per-repo companion to `/migrate-content-to-zbb` (which bootstrapped this repo onto gradle). Use this skill to migrate vendors **one at a time** within `org/vendor`. Layout: `package/<vendor>/` (depth 1).

## Trigger

```
/migrate-packages [<vendor>...] [--batch=N] [--dry-run]
```

Examples:
- `/migrate-packages` — pick the next N pending vendors from `MIGRATION_STATUS.md` (or `find` if no tracker).
- `/migrate-packages github okta atlassian` — migrate exactly these.
- `/migrate-packages --batch=5 --dry-run` — show the next 5 candidates without changing anything.

## Pre-flight

1. `git status` — must be on a feature branch, not `main`.
2. Confirm gradle bootstrap is in place: root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle-ci.properties` exist; `.github/workflows/publish.yml` uses `zbb-publish-reusable.yml`. If anything is missing, abort and direct the user to `/migrate-content-to-zbb`.
3. Identify candidates: vendors WITHOUT `package/<vendor>/build.gradle.kts`.
4. Order: simplest first (smallest `index.yml`, no logo edge cases). Skip anything in `MIGRATION_STATUS.md`'s `Flagged` section — fix that drift in a separate commit before migrating.

## Per-vendor loop

For each vendor in the batch, do steps 1–6 in order, then commit and move on.

### 1. Drop the marker
Create `package/<vendor>/build.gradle.kts`:
```kotlin
plugins { id("zb.content") }
```

### 2. Ensure `.npmrc`
The validator requires `package/<vendor>/.npmrc`. If absent, copy from a sibling already-migrated vendor (e.g. `package/accelq/.npmrc`).

### 3. Run **full** `:gate` (NOT just `:validateContent`)
```bash
./gradlew :<vendor>:gate
```
**Why full `:gate` matters:** the publish workflow's preflight rejects any vendor without a committed `gate-stamp.json` (`gate-stamp.json is missing or invalid — run zbb gate locally and commit the stamp before publishing`). The stamp is written by the `:writeGateStamp` task that runs at the end of `:gate`. Running only `:validateContent` does NOT produce a stamp — the vendor will pass local file-checks but fail in CI.

`:gate` chains: `validate` → `lint` → `compile` → `test*` → `buildArtifacts` → `testIntegrationDataloader` → `writeGateStamp`. Without `NEON_API_KEY` / `NEON_PROJECT_ID` in env, `testIntegrationDataloader` is **skipped** (not failed) — the stamp still gets written, and CI re-runs the dataloader test against an ephemeral Neon branch on push. Locally not having Neon creds is fine; just confirm the stamp file appears at `package/<vendor>/gate-stamp.json` after the build.

The validator surfaces drift one error at a time. Common fixes for vendors:

- **`package.json name`** must equal `@zerobias-org/vendor-<dirName>` (with `.` in dirName replaced by `-`). Fix the `name` to match the directory; do NOT rename the directory.
- **`zerobias.package`** must equal the directory name (no `.tag` / `.suite` suffix — vendors are bare). Legacy `auditmation.package` is accepted but should be renamed to `zerobias.package`.
- **Missing `.npmrc`** — see step 2.
- **Logo issues**:
  - Multiple logo files (`logo.svg` + `logo.png`) — keep one.
  - Magic-byte mismatch (e.g. `logo.svg` is actually an HTML S3 error page) — re-fetch the original or remove.
  - Size out of range (<500B / >5MB) — replace.
  - `package.json` `files` array doesn't include the logo — add `"logo.*"` (or the exact filename).
  - Vendors without a logo are valid (logos are optional in this validator) — early-return path; no action needed.
- **Duplicate `id` UUID** (`:validateUniqueIds` collision) — investigate which other vendor owns that UUID. The newcomer needs a fresh UUID via `uuidgen`. Existing UUIDs are stable — don't change them.

Re-run `:gate` after each fix until it passes.

### 4. Major-bump version
```bash
# package/<vendor>/package.json: bump major.
# 1.x.x → 2.0.0    (most existing vendors)
# 0.x.x → 1.0.0    (rare)
# 2.x.x — no-op    (already on the gradle line)
```
Universal repo rule: every vendor's first gradle publish gets a major bump. This is the version-line transition, not a content change.

### 5. (Optional) Re-run `:gate` after the version bump
Cheap sanity check — version field is in `package.json`, the validator reads it.

### 6. Commit
One commit per vendor. Conventional commit format:
```
feat(vendor-<vendor>)!: migrate to gradle pipeline (<oldVer> → 2.0.0)
```
The `!` marks the major bump as breaking. Stage exactly: `package/<vendor>/build.gradle.kts`, `package/<vendor>/.npmrc` (if you added it), `package/<vendor>/package.json` (version bump), **`package/<vendor>/gate-stamp.json`** (mandatory — preflight rejects without it), and any drift fixes you made (e.g. `package/<vendor>/index.yml`, `package/<vendor>/logo.svg`).

### 7. (After the batch) Verify on a feature branch
```bash
gh workflow run publish.yml --ref <branch>
```
On a feature branch, `version` (single-writer) is skipped and `publish` runs in pre-release mode (no `latest` dist-tag). Confirm `detect` lists exactly the vendors you bumped, then validate the published artifacts before merging to main.

## Picking the next batch

Order rules of thumb:
1. Skip the `MIGRATION_STATUS.md` `Flagged` section until its drift is fixed in a separate PR.
2. Prefer vendors with the simplest `index.yml` first — fewer fields = fewer chances of dataloader-rule drift.
3. Group related vendors (e.g. all `aws-*` services) when the failure mode is shared (same template leftovers).
4. Cap each PR at ~10 vendors. Easier to review, easier to bisect if something breaks.
5. Run `./scripts/migration-status.sh` after each batch to refresh the tracker.

## What NOT to do

- Do NOT change vendor `id` UUIDs. They're stable identifiers; changing one detaches existing DB rows.
- Do NOT rename directories to make `package.json name` match. The validator's job is to catch metadata drift; metadata follows the directory.
- Do NOT skip the major bump because "no source changed". The bump reflects the publish-pipeline transition.
- Do NOT batch unrelated vendors into one commit. One commit per vendor keeps `git revert` precise.
- Do NOT touch the `.husky/` directory — empty placeholder, not part of the canonical pattern.

## Reference files

- `package/accelq/`, `package/action1/`, `package/adobe/` — already migrated; use as drop-in references.
- `templates/index.yml`, `templates/package.json` — what a NEW vendor looks like (placeholder syntax: single-curly `{code}` / `{name}`).
- `MIGRATION_STATUS.md` — pending / done / flagged tracker. Regenerate via `./scripts/migration-status.sh`.
- Root `build.gradle.kts:9-38` — the validator philosophy comment (read once if you've forgotten why a check exists).
- `org/util/packages/build-tools/.../SchemaPrimitives.kt` — validator helpers and error message shapes.

## See also

- `/migrate-content-to-zbb` — meta-repo skill that bootstrapped this repo. Use only when migrating a new repo onto gradle, not for per-package work here.
