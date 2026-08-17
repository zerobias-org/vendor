---
name: create-vendor
description: >-
  Create a new vendor package in the zerobias-org catalog and take it through
  the full content SDLC — scaffold → gradle gate → publishOrg + org load →
  user verifies the org artifact → PR to dev only after explicit sign-off.
  USE THIS when the user says "add vendor X", "I need a new vendor for Y",
  "register <company> as a vendor", or a ZeroBias task asks for a vendor
  package. Standalone: works in this repo alone (no meta-repo), and no
  platform task is required (task-driven mode is optional).
argument-hint: "[vendor name | task-id]"
---

# create-vendor — new vendor package, org-first SDLC

Vendors are the ROOT of the catalog dependency chain (vendor → suite? →
product → …). This skill produces ONE vendor package and delivers it
**org-first**: the default deliverable is the vendor loaded into the user's
own org; the PR to `dev` happens only after the user signs off on the
org-loaded result.

```
Phase 0 prerequisites (hard gate — /prerequisites must report READY)
Phase 1 resolve + existence check
Phase 2 branch (from dev)
Phase 3 scaffold + author content
Phase 4 gate                        ← git add BEFORE gating
Phase 5 publishOrg + org load
Phase 6 user verifies org artifact  ← 🙋 explicit sign-off required
Phase 7 PR --base dev               ← only after sign-off
```

**Modes.** Default is **request-driven**: the user names a vendor; no
platform task needed. If the user references a ZeroBias task (UUID or task
name), additionally follow the **task-driven appendix** at the end.

**Headless runs (`claude -p "make vendor x"`).** Same flow, three hard rules:
- **Pre-flight first**: run the `prerequisites` skill (Phase 0) before
  touching anything. If anything is missing, print the exact setup
  instructions and exit — never fail mid-flow.
- **The run ENDS after Phase 5** (org load). Print what was created, the
  verification link, and: *"verify the org artifact, then run
  `claude -p 'open the PR for vendor <code>'` (or continue interactively)"*.
  Phases 6–7 are human-gated and never run headless.
- **Decision forks stop the run**: vendor already exists, no official logo
  found, gate conflict → print a structured report of the state and the
  decision needed, exit cleanly, change nothing further.

**Skill-vs-reality conflicts.** If observed tool behavior contradicts this
skill, STOP: verify against the primary source (`settings.gradle.kts`,
build-tools source in `util`, the workflow YAML), act on what the source
says, and queue a fix to this skill in the same session — never force
reality to match stale text.

## Phase 0 — prerequisites (hard gate)

Invoke this repo's [`prerequisites` skill](../prerequisites/SKILL.md)
(`/prerequisites`) and get `READY` before ANYTHING else — interactive or
headless. If something is missing there are exactly two permitted actions:
install it (with consent) or stop and wait. Never work around it — no
substitute tooling, no raw HTTP instead of the `zb` MCP, no partial
continuation.

This gate applies for the WHOLE flow, not just at the start: if any
prerequisite fails mid-flow (401s, expired token, org load refused, tool
vanished), treat it as a prerequisite regression — STOP the phase you're
in, re-run `/prerequisites`, and resume only from `READY`. Never improvise
past a mid-flow credential failure.

## Phase 1 — resolve inputs + existence check

Needed: **vendor name** (natural language). Derive:
- `vendorCode` — lowercase, matching `^[\d_a-z]+$`; prefer plain lowercase
  alphanumeric (the UI's `vspCodeValidator` rejects underscores).
- Official website URL and logo URL (research if not provided).

Check it doesn't already exist — use the `zb` MCP `store` ops (the
`portal.*.search` ops found in older docs do NOT exist):

```
zerobias_execute("store.Vendor.get", { vendorCode: "<vendorCode>" })   // 404 = free
```

The `zb` MCP is a hard prerequisite (see Prerequisites) — do NOT substitute
raw HTTP calls if it's missing; stop and have it installed instead.

Also check locally: `ls package/ | grep <vendorCode>`. If the vendor exists
(platform or local), STOP and ask the user what to do (update / nothing).

## Phase 2 — branch first (never commit on main)

```bash
git fetch origin
git switch -c feat/vendor-<vendorCode> origin/dev
```

Content PRs target **`dev`** (org convention: local → org → PR to dev →
promotion toward main), so the branch is cut from `origin/dev`.

## Phase 3 — scaffold + author

```bash
./scripts/createNewProduct.sh package/<vendorCode>
echo 'plugins { id("zb.content") }' > package/<vendorCode>/build.gradle.kts
```

The scaffold script creates the directory, copies the templates, and fills
`{code}`/`{id}`; you fill the remaining `{name}`/`{description}`/`{url}`
placeholders. **Verify the scaffold immediately**: the file set from
`ls -A templates/` must all be present in `ls -A package/<vendorCode>/`
(dotfiles included), and spot-check the layout against a recently-merged
vendor (e.g. `ls -A package/github`) — a scaffold bug caught here costs
seconds; caught by the gate it costs a full cycle. Required files in
`package/<vendorCode>/`:

```
package.json          # @zerobias-org/vendor-<code>
index.yml             # vendor metadata
logo.{svg|png|jpg}    # official vendor logo (SVG preferred, unmodified)
build.gradle.kts      # one-line zb.content marker (REQUIRED for publish detect)
.npmrc                # REQUIRED — validator hard-fails with ".npmrc missing"
```

**`.npmrc`** is REQUIRED (469/470 corpus packages ship the same two lines;
`templates/.npmrc` has it, but verify the scaffold actually copied it —
dotfiles are easy to miss):

```
@zerobias-org:registry=https://pkg.zerobias.org
//pkg.zerobias.org/:_authToken=${ZB_TOKEN}
```

**package.json** (matches the existing corpus — keep conventions):

```json
{
  "name": "@zerobias-org/vendor-<vendorCode>",
  "version": "1.0.0",
  "description": "Vendor package for <Vendor Name>",
  "author": "team@zerobias.com",
  "license": "ISC",
  "type": "module",
  "repository": {
    "type": "git",
    "url": "git@github.com:zerobias-org/vendor.git",
    "directory": "package/<vendorCode>/"
  },
  "scripts": { "correct:deps": "tsx ../../scripts/correctDeps.ts" },
  "publishConfig": { "registry": "https://pkg.zerobias.org/" },
  "files": ["index.yml", "logo.*"],
  "zerobias": {
    "dataloader-version": "1.0.0",
    "import-artifact": "vendor",
    "package": "<vendorCode>"
  }
}
```

- Never hand-edit `version` afterwards — CI owns bumps.
- No `dependencies` — vendor packages are pure metadata.
- `zerobias.package` MUST equal the directory name and `index.yml` `code`.

**index.yml**:

```yaml
id: <fresh-uuid-v4-lowercase>
name: <Vendor Full Name>
description: >-
  <What the vendor organization is/does.>
imageUrl: https://cdn.auditmation.io/logos/<vendorCode>.<ext>
logo: https://cdn.auditmation.io/logos/<vendorCode>.<ext>
code: <vendorCode>
type: vendor
ownerId: 00000000-0000-0000-0000-000000000000
status: active
url: https://<vendor-website>
tags: []
aliases:
  - <common alternate name>
cpeVendors: []
```

- No `created`/`updated` fields — the template/corpus omit them and the
  dataloader stamps them server-side. Fresh UUID v4; `status: active`
  (corpus convention — e.g. the github vendor). Fill `cpeVendors` with the
  NVD CPE vendor name when the vendor has CVE entries (e.g. `- tailscale`).
- `imageUrl`/`logo` extension must match the actual logo file.

**Logo**: download the official asset (try the site's `/logo.svg`,
`/assets/`, `/images/`, press-kit pages). Never modify SVG content. If none
found, note it in the PR.

## Phase 4 — gate (git add FIRST, always via zbb)

All builds go through `zbb` — **never invoke `./gradlew` directly**. Only
zbb injects the slot env (token, URLs) into the build; a bare gradle run
silently misses it.

```bash
ls -A package/<vendorCode>             # completeness check BEFORE first gate:
                                       # all required files incl. DOTFILES
                                       # (.npmrc!) — a miss costs a gate cycle
git add package/<vendorCode>/          # BEFORE gating — the gate-stamp's
                                       # sourceHash enumerates git ls-files;
                                       # untracked files are invisible to it
zbb --slot <slot> stack add "$(git rev-parse --show-toplevel)"  # once per slot,
                                       # else "no added stack is reachable"
cd "$(git rev-parse --show-toplevel)/package/<vendorCode>" && zbb --slot <slot> gate
zbb gate --check                       # validate the stamp (no slot needed)
```

⚠ Write EVERY `zbb gate` / `publishOrg` as `cd <absolute-path> && zbb …`
in ONE command — never rely on inherited shell cwd (background shells
reset it, and a repo-root `publishOrg` targets the wrong project).

`gate` = `validateContent` (schema + package-identity + logo checks) +
the Neon dataloader step — which runs **iff `ZB_TOKEN` is present** in the
slot env (older docs mentioning `NEON_API_KEY` as the guard are stale).
With the org-owner token from Phase 0 it runs for real. On success it
writes `package/<vendorCode>/gate-stamp.json` — **commit that file**; CI's
publishGuard rejects publishes without a valid committed stamp. CI does
not rerun your tests — it validates the committed stamp.

If you gated before adding new files, re-gate after `git add`.
Legacy `npm install` / `npm shrinkwrap` / `npm run validate` are gone —
zbb owns the lifecycle. Don't commit a shrinkwrap.

## Phase 5 — publishOrg + load into the user's org

Publishes an org-private rc version (`<X.Y.Z+1>-rc.<orgIdStripped>.<n>`, computed by zbb — never hand-authored) and queues
a dataloader job into the target org — no PR, no shared catalog involved.

1. Set the target in `package.json`: `"zerobias": { …, "orgId": "<org-uuid>" }`.
2. Environment — must be in the **slot/stack env** (a plain shell `export`
   does not reach the gradle build). Secret in the slot's local env:
   `zbb --slot <slot> env set ZB_API_KEY <org-owner-key>` (and `ZB_TOKEN` =
   the registry key); non-secret in `zbb.yaml` `env:` or the slot env:
   - `ZB_API_KEY` — **ORG key: org OWNER API key of the target org** (falls
     back to `ZB_TOKEN` on pre-split build-tools; a member key cannot load
     artifacts into the org; a different-org key fails the `/dana/me` check).
   - `ZB_TOKEN` — **REGISTRY key** (`pkg.zerobias.org`; must be PROD-issued
     while the registry accepts only prod keys) — also drives the gate's
     Neon step.
   - `ZB_PLATFORM_URL: https://<env>/api` (default is prod) — the org load
     (`dataloaderOrgJob`) uses THIS + `ZB_API_KEY`.
   - `DATALOADER_SERVICE_URL` — **LEAVE UNSET** (defaults to prod
     `https://app.zerobias.com/api/dataloader`). The gate's Neon step
     deliberately auths with the prod-issued `ZB_TOKEN` against the prod
     dataloader-service even when the org target is another env; pointing
     it at the target env makes the prod key 401 there. Override only for
     loader dev.
   - `NPM_CONFIG_TAG: dev` — npm 11 requires a dist-tag for prereleases.

   ⚠ **Slot-env mutation gate:** changing any slot value that redirects
   traffic or identity (URLs, `ZB_ORG_ID`, keys) MID-FLOW requires showing
   the user the evidence and the exact `env set`, and getting confirmation
   BEFORE running it — even when source code proves the change correct.
   Never silently repoint an environment.
3. Run as ONE command with an absolute path — never rely on inherited cwd:
   `cd <repo>/package/<vendorCode> && zbb --slot <slot> publishOrg`
   (never bare `./gradlew` — zbb injects the slot env)
4. Verify it landed (by **code**, not UUID):
   `zerobias_execute("store.Vendor.get", { vendorCode: "<vendorCode>" })`
   — and show the user in the app catalog.
5. **Iterate here**: edit → re-gate → re-run `zbb --slot <slot> publishOrg`
   until the user is satisfied. Loading happens ONLY through
   `zbb publishOrg` — never POST the dataloader API directly, and never
   use the MCP to load artifacts (MCP ops are for reads/verification
   only).

Notes: org users can only queue org-private (`-rc.<org>`) loads — a plain
catalog-semver load is 403 (platform-admin only). The `zb.content` plugin
resolves **mavenLocal-first** (`settings.gradle.kts` never uses
`includeBuild` — a sibling `util` clone and its branch are irrelevant): a
locally-published build-tools in `~/.m2` is what actually loads. Verify
plugin capabilities (e.g. the ZB_API_KEY split) by grepping the `~/.m2`
jar, not the util checkout.

## Phase 6 — user verification + sign-off  ⭐

Show the user the org-loaded vendor (catalog UI or the `store.Vendor.get`
result). ⚠️ The logo will render BROKEN in the UI at this stage —
`cdn.auditmation.io/logos/<code>.<ext>` 404s until the vendor reaches
`main`, where the publish workflow's `cdn-update` job uploads it (the
dataloader never touches the CDN). Tell the user up front; have them judge
the data fields, and verify the logo locally (it must be inside the
published rc tarball). **Do NOT proceed to the PR until the user explicitly confirms**
(e.g. "looks good, ship it"). Silence or further tweak requests are NOT
sign-off — if unclear, ask. Headless runs never reach this phase — they
stop after Phase 5 by design.

## Phase 7 — PR to dev (after sign-off only)

1. Flip ownership to the shared catalog: **delete `zerobias.orgId` from
   `package.json`**. No re-gate needed — for content packages the
   gate-stamp's sourceHash covers the `files` payload (`index.yml`,
   `logo.*`), not `package.json`. Leftover `-rc.<org>.<n>` npm versions
   don't collide with catalog semver.
2. Commit — selective staging, conventional message, no co-authors:

```bash
git add package/<vendorCode>/
git commit -m "feat(vendor-<vendorCode>): add <Vendor Name> vendor package"
git push -u origin feat/vendor-<vendorCode>
```

3. PR against **dev**:

```bash
gh pr create --base dev \
  --title "feat(vendor-<vendorCode>): add <Vendor Name>" \
  --body "…summary, validation checklist (gate ✓, gate-stamp committed ✓,
          org-loaded + user-verified ✓), and anything needing SME review
          (placeholder logo, unusual code, naming judgment calls)…"
```

The PR is how content reaches the shared catalog; the org-private artifact
from Phase 5 stays in the user's org either way.

## Common issues

**First rule for any SERVER-side failure** (dataloader jobs, platform
calls): re-run the identical command ONCE before diagnosing or escalating —
pod-side state (secret re-syncs, deploys) changes independently of your
session, and a retry is far cheaper than a wrong escalation.

- **Publish workflow skips the vendor** → missing `build.gradle.kts`
  marker; add the one-liner and push.
- **`validateContent` fails** → `code` regex / dir / `zerobias.package`
  mismatch, UUID not v4 lowercase, or logo file vs `files`/extension
  mismatch.
- **`testIntegrationDataloader` errors locally** (instead of skipping) →
  slot misconfigured; check the stack is added to the slot and the slot env
  holds `ZB_TOKEN` + `ZB_PLATFORM_URL` (`zbb --slot <slot> env get … | tail -n1` —
  zbb may prefix a vault banner on stdout, the value is the last line).
- **`publishOrg` 401 on `/dana/me` or the org load is refused** →
  the ORG key (`ZB_API_KEY`, fallback `ZB_TOKEN`) is not an org OWNER key of the org in `zerobias.orgId` —
  member keys authenticate but cannot load; get an owner key.
- **Gate's Neon step 401s on `POST /branches`** → `DATALOADER_SERVICE_URL`
  was overridden to a non-prod env (the prod `ZB_TOKEN` is rejected there).
  Unset it — the step is designed to run against the prod default with the
  prod key; the ephemeral Neon branch is env-agnostic validation.
- **`dataloaderOrgJob` fails with `npm … 401 Unauthorized … Invalid API
  key`** (server-side, `/root/.npm` in the log) → the TARGET env's
  `platform-dataloader-service` pod fetches the package with its OWN
  `ZB_TOKEN` (Vault `operations-kv/<env>/zerobias/org : zb-token`) — the
  job payload carries only name+version, so client keys are never used and
  no client-side change can fix it. Retry `publishOrg` cheaply first (pods
  may have re-synced a rotated secret); if it persists, escalate to
  platform infra: rotate the Vault key to a registry-accepted platform API
  key and restart the pod.
- **`publishOrg` rejects the name** → the package name/code already exists
  in the shared catalog; org-publish only works for brand-new / org-owned
  names.

## Task-driven appendix (only when the user references a ZeroBias task)

- Fetch: `platform.Task.get` (UUID). Task code is not searchable.
- Assign + start: `platform.Party.getMyParty` → `platform.Task.update` with
  `assigned` (party id), `customFields` (`artifactType: vendor`, `repoUrl`,
  `branchName`), and the Start transition — **always take transition IDs
  from `task.nextTransitions`**, never hardcode them.
- Comment progress at start and completion (`platform.Task.addComment`).
- After the PR: transition to Peer Review. Link to a parent task with
  `platform.Resource.linkResources` (`fromResource`/`toResource`) if this
  vendor was created as a dependency.

## References (this repo only)

- [`CLAUDE.md`](../../../CLAUDE.md) — repo conventions, publish workflow,
  validator philosophy.
- [`scripts/createNewProduct.sh`](../../../scripts/createNewProduct.sh) —
  scaffold script.
