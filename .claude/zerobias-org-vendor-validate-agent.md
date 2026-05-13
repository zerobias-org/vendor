# Zerobias Org Vendor Validate Agent

> **Status:** Superseded. Vendor validation moved to gradle. This file is
> kept as a pointer so existing references don't 404.

## Where validation lives now

Validation is owned by gradle's `zb.content` plugin. The schema rules
themselves live in this repo's root `build.gradle.kts`, composed from
the `SchemaPrimitives` library shipped by `zerobias-org/util`'s
`build-tools`. No per-vendor validate script, no `scripts/validate.ts`.

```bash
# Schema check on index.yml + package.json (fast, offline)
./gradlew :<vendor>:validateContent

# Full gate — schema + dataloader on an ephemeral Neon Postgres branch
./gradlew :<vendor>:gate
```

`gate` writes `package/<vendor>/gate-stamp.json` on success. Commit that
file with your changes — the publish workflow uses it as a preflight
record.

## What gets checked

The validator at `build.gradle.kts` (look for `extra["contentValidator"]`)
enforces (mirrors `com/platform/dataloader` `VendorFileHandler` so failures
shift left from prod load to gate time):

- `index.yml` exists and parses as YAML
  - `id` is a valid UUID
  - `code` is a non-blank string, matches the leaf directory name, and
    matches `^[\d_a-z]+$` (lowercase alphanumeric with underscores)
  - `name` is a non-blank string
  - `status` is one of `VspStatusEnum`: `draft`, `active`, `rejected`,
    `deleted`, `verified`
  - `description`, if present, is non-blank
  - `url`, if present, is an absolute URL (scheme + host)
  - `logo`, if present, is an absolute URL
  - `aliases` and `cpeVendors`, if present, are string lists
  - `tags`, if present, is a list of UUIDs (each item)
- `package.json` exists and parses as JSON
  - `name` matches `@zerobias-org/vendor-<code>` (with dots replaced by dashes)
  - `description` is non-blank
  - `zerobias.import-artifact` (or legacy `auditmation.import-artifact`) is `vendor`
  - `zerobias.package` (or legacy `auditmation.package`) matches `code`
  - `zerobias.dataloader-version` (or legacy) is non-blank
- `.npmrc` exists

## Adding a new check

Pick the right primitive from `com.zerobias.buildtools.content.SchemaPrimitives`
(`requireUuid`, `requireNonBlankString`, `requireEnum`, `requireStringList`,
`requireIso8601`, `requireCodeMatchesDir`, `getPath`, `parseYaml`, `parseJson`)
and add a line inside `extra["contentValidator"]` in the root
`build.gradle.kts`. Keep validator changes scoped to this repo — util
ships only the primitives.

## Logo rules

Logo presence/format is not enforced by the gradle validator yet (it's
enforced socially via the create-vendor skill checklist + reviewer eye).
The historical "exactly one of logo.svg / logo.png" rule still holds.

## See also

- `CLAUDE.md` — repo overview and gradle commands
- `.claude/skills/create-vendor.md` — full new-vendor walkthrough
- `build.gradle.kts` — validator implementation
