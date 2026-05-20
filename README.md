# Vendor mono-repo
Monorepo of vendor.

## Contributing

Vendor creation and logo work are documented as Claude Code skills/agents
under `.claude/`:

- **[Create a vendor](./.claude/skills/create-vendor.md)** — full new-vendor walkthrough (driven from a ZeroBias task)
- **[Logo agent](./.claude/zerobias-org-vendor-logo-agent.md)** — sourcing, validating, and updating vendor logos
- **[Validate agent](./.claude/zerobias-org-vendor-validate-agent.md)** — what the gradle validator checks and how to extend it

Repo-wide conventions (branching, conventional commits, gradle commands)
are in [CLAUDE.md](./CLAUDE.md).

# Starting Development
##  Fork Repository

Under the [zerobias-org/vendor](https://github.com/zerobias-org/vendor) repo, click the Fork button toward top right of the screen
* Under owner choose your personal account for development, and leave the repository name as vendor
* Make sure the `Copy the main branch only` is unchecked, this is important
* Click the `Create Fork` button

Once you have forked the repository change, your working environment to the `dev` branch.
* Any PRs made against the `zerobias-org/vendor` repo should be agaisnt it's dev branch or they will be rejected.

## NPM Packages authentication
**ZB_TOKEN**

Set `ZB_TOKEN` in your environment variables to authenticate with npm registry.
`ZB_TOKEN` needs to be an API key from [ZeroBias](https://app.zerobias.com).

## Getting Started

**Run `npm install` in the root directory after cloning** — this installs the
commitlint hooks (conventional-commit enforcement) and `tsx` for the
per-package `correct:deps` helper. The vendor build/validate/publish lifecycle
itself is owned by gradle (`zb.content` plugin), not by npm.

## Creating a new vendor

### Run the create-new-vendor script

Run `sh scripts/createNewProduct.sh package/<vendor-name>` to scaffold the
package folder from `templates/`.

### Mark the vendor for the gradle pipeline

Add a one-line marker so the publish workflow's `detect` job picks the vendor up:

```bash
echo 'plugins { id("zb.content") }' > package/<vendor-name>/build.gradle.kts
```

### Validate new vendors

Validation runs through gradle (`zb.content` plugin). The schema rules
live in this repo's root `build.gradle.kts` (composed from
`SchemaPrimitives` shipped by `zerobias-org/util`):

```bash
# Schema check only (fast)
./gradlew :<vendor-name>:validateContent

# Full gate (validate + dataloader against an ephemeral Neon branch);
# writes gate-stamp.json on success — commit that file with your changes
./gradlew :<vendor-name>:gate
```

> Legacy `npm install` / `npm shrinkwrap` / `npm run validate` are no
> longer needed — gradle owns vendor lifecycle.

**Now you can commit your changes following the instructions below, then open a PR against the main repository branch**

## Commit conventions and Version management

### Versioning and publishing: gradle + `zbb`

Vendor versions and publishing are driven by the gradle pipeline (`zb.content`
plugin) and the shared `Publish` GitHub Actions workflow
(`zbb-publish-reusable.yml`) — **not** by lerna. There are no manual version
bumps in pull requests; the workflow's single-writer `version` job aggregates
per-package bumps into one `chore(release):` commit on `main`.

The `Publish` workflow triggers on push to `main` / `qa` / `dev` / `uat`:

1. `detect` — diffs `package/**/build.gradle.kts` to find changed vendors.
2. `version` (main only) — single-writer pre-matrix bump (`zbb version`).
3. matrix `publish (<vendor>)` — `zbb publish` per vendor: `gate-stamp.json`
   preflight → `npm publish --tag next` → cumulative promote to dev/qa/uat/latest.
4. `update-bundle` — refreshes `@zerobias-org/vendor-bundle` and publishes the next patch.
5. `sync` — propagates `main → uat → qa → dev` after a successful main publish.

Local validation before pushing:

```bash
# Schema check only (fast)
./gradlew :<vendor-name>:validateContent

# Full gate (validate + dataloader against an ephemeral Neon branch);
# writes gate-stamp.json on success — commit that file with your changes
./gradlew :<vendor-name>:gate
```

### Git Hooks: commitlint

A `commit-msg` hook validates every commit message against
[Conventional Commits](https://www.conventionalcommits.org/). Commits that
don't comply are rejected. (Build/lint/test enforcement now lives in the gradle
`:gate` task and CI, not in a pre-commit hook.)

*Example: good commit*
```
touch badcommit.txt
git add badcommit.txt
git commit -m 'this is a bad commit'
⧗   input: this is a bad commit
✖   subject may not be empty [subject-empty]
✖   type may not be empty [type-empty]

✖   found 2 problems, 0 warnings
ⓘ   Get help: https://github.com/conventional-changelog/commitlint/#what-is-commitlint
```

*Example: Good commit*
```
touch goodcommit.txt
git add goodcommit.txt 
git commit -m 'feat: some cool new feature'
[MOD-599-readme 4e70f32b] feat: some cool new feature
 1 file changed, 0 insertions(+), 0 deletions(-)
 create mode 100644 goodcommit.txt
```

### Commit Message Format
Every commit should follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0-beta.4/).
This section summarizes some of its guidelines.

Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional.
Either the **body** or **footer** must begin with `BREAKING CHANGE` if the commit is a breaking change.

Example — `feat(lang): add polish language`

#### Type
Must be one of the following:

* **feat**: A new feature.
* **fix**: A bug fix.
* **docs**: Documentation only changes.
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).
* **refactor**: A code change that neither fixes a bug nor adds a feature.
* **perf**: A code change that improves performance.
* **test**: Adding missing tests.
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation.

#### Scope
The scope is optional and could be anything specifying place of the commit change. For example `release`, `api`, `mappers`, etc...

#### Body
The body may include the motivation for the change and contrast this with previous behavior.
It should contain any information about **Breaking Changes** if not provided in the **footer**.

#### Footer
The footer should contain any information about **Breaking Changes** if not provided in the **body**.

**Breaking Changes** should start with the word `BREAKING CHANGE:` with a space or two newlines.
A description of the change should always follow.
