# Contributing a vendor

Anyone can author and validate a vendor package. There are two lanes,
depending on whether you have a ZeroBias platform account. AI-assisted
contributors: the [`create-vendor` skill](.claude/skills/create-vendor/SKILL.md)
encodes both lanes end to end — just say "make vendor X" in Claude Code.

## Lane 1 — anyone with a GitHub account (no ZeroBias account needed)

You can validate everything except the platform load locally.

1. **Tools**: git, Java 17+, Node 22, the `gh` CLI (and optionally
   `@zerobias-org/zbb`).
2. **GitHub token with `read:packages`** — required even though the packages
   are public (GitHub Packages Maven does not allow anonymous reads; the
   gradle plugin resolves from it). Either export a personal access token
   that has the `read:packages` scope:

   ```bash
   export GITHUB_TOKEN=<your PAT>
   ```

   or reuse your `gh` login:

   ```bash
   gh auth refresh -s read:packages
   export GITHUB_TOKEN=$(gh auth token)
   ```

   (Heads-up: an *invalid* `GITHUB_TOKEN` in your environment silently
   shadows a valid `gh` keyring login — `gh auth status` will show it.)

3. **Scaffold + author**:

   ```bash
   ./scripts/createNewProduct.sh package/<code>
   # fill {name}/{description}/{url} in index.yml, add the official logo,
   echo 'plugins { id("zb.content") }' > package/<code>/build.gradle.kts
   ```

4. **Validate** (from the repo root):

   ```bash
   ./gradlew :<code>:gate
   ```

   The platform dataloader step prints `ZB_TOKEN not set — skipping` — that is
   expected in this lane; everything else must pass. The run writes
   `package/<code>/gate-stamp.json` — commit it.

5. **PR against `dev`** (never `main`). Maintainers review before merge;
   CI loads the vendor into the catalog after it merges.

## Lane 2 — ZeroBias platform users

A **catalog** vendor (shared content, the usual case) follows the same flow
as Lane 1: gate → PR against `dev`, with no `zerobias.orgId`. An
**org-only** vendor (`zerobias.orgId` set) is loaded into your org with
`publishOrg` and verified there; it is not a PR to the shared catalog.
`zerobias.orgId` declares the package's owner for every content type (see
the dataloader's `docs/STABLE_IDS.md`).

1. One-time credential setup + session launch (owns all three credential
   homes, verifies your key is an org OWNER, then starts Claude Code with
   everything exported):

   ```bash
   ./scripts/setup-org-credentials.sh --launch
   ```

2. In the session, say **"make vendor \<name\>"**. The skill
   asks which kind it is and runs scaffold → gate → PR against `dev`
   (catalog), or scaffold → gate → `publishOrg` → you verify the org
   artifact (org-only).

Notes for both lanes: commits follow Conventional Commits
(`feat(vendor-<code>): …`); never commit on `main`/`dev` directly; never
hand-edit `version` after creation (CI owns bumps); the vendor `code` must
match `^[\d_a-z]+$` (prefer plain lowercase alphanumeric).
