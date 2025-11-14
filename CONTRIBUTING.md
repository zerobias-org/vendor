# Contributing to Zerobias Org Vendor

Thank you for your interest in contributing to the zerobias-org vendor repository! This guide will help you understand how to contribute changes, particularly for adding or updating vendor logos.

## Table of Contents
- [Getting Started](#getting-started)
- [Fork and Branch Workflow](#fork-and-branch-workflow)
- [Adding or Updating Vendor Logos](#adding-or-updating-vendor-logos)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Code Review Process](#code-review-process)

## Getting Started

### Prerequisites
- Git installed on your machine
- Node.js and npm installed
- A GitHub account
- `ZB_TOKEN` environment variable set for npm registry access (for publishing)

### Repository Information
- **Upstream Repository**: `https://github.com/zerobias-org/vendor`
- **Main Branch**: `main` (or `master`)
- **Development Branch**: `dev`
- **Package Registry**: GitHub Packages (`npm.pkg.github.com/@zerobias-org`)

## Fork and Branch Workflow

We use a **fork-based workflow** for contributions. Follow these steps:

### 1. Fork the Repository

1. Go to https://github.com/zerobias-org/vendor
2. Click the "Fork" button in the top-right corner
3. This creates a copy of the repository under your GitHub account: `https://github.com/YOUR-USERNAME/vendor`

### 2. Clone Your Fork

```bash
# Clone your fork to your local machine
git clone git@github.com:YOUR-USERNAME/vendor.git

# Navigate to the repository
cd vendor

# Add the upstream repository as a remote
git remote add upstream https://github.com/zerobias-org/vendor.git

# Verify remotes
git remote -v
# Should show:
# origin    git@github.com:YOUR-USERNAME/vendor.git (fetch)
# origin    git@github.com:YOUR-USERNAME/vendor.git (push)
# upstream  https://github.com/zerobias-org/vendor.git (fetch)
# upstream  https://github.com/zerobias-org/vendor.git (push)
```

### 3. Keep Your Fork Synchronized

Before starting work, ensure your fork is up to date:

```bash
# Fetch changes from upstream
git fetch upstream

# Switch to your main/master branch
git checkout main  # or 'master' depending on repo

# Merge upstream changes
git merge upstream/main

# Push updates to your fork
git push origin main
```

### 4. Create a Feature Branch

Always create a new branch for your changes:

```bash
# Create and switch to a new branch
git checkout -b feature/your-feature-name

# Examples:
# git checkout -b feature/add-logo-google
# git checkout -b feature/update-microsoft-url
# git checkout -b fix/logo-duplicate-issue
```

**Branch Naming Convention:**
- `feature/` - For new features or additions (e.g., adding logos)
- `fix/` - For bug fixes
- `docs/` - For documentation updates
- `chore/` - For maintenance tasks

## Adding or Updating Vendor Logos

### Using the Agent Workflows

We have documented agent workflows in the `.claude/` directory to help automate logo management:

- **`.claude/zerobias-org-vendor-logo-agent.md`** - Complete workflow for adding vendor logos
- **`.claude/zerobias-org-vendor-shrinkwrap-agent.md`** - Rebuilding npm-shrinkwrap.json
- **`.claude/zerobias-org-vendor-validate-agent.md`** - Validating vendor packages

### Manual Steps for Adding a Logo

#### 1. Verify Vendor Package Structure

```bash
cd package/VENDOR-NAME
```

Ensure the vendor directory contains:
- `index.yml` (required)
- `package.json` (required)
- `.npmrc` (required)

#### 2. Obtain the Vendor Logo

**Option A: From Vendor Website**
```bash
# Try fetching from homepage
curl -sL "https://www.VENDOR.com" -H "User-Agent: Mozilla/5.0" | grep -i logo

# If found, download the logo
curl -sL "LOGO_URL" -o package/VENDOR-NAME/logo.svg
```

**Option B: From Wikimedia Commons**
```bash
# Search for the vendor on Wikimedia Commons
# Download from direct URL
curl -sL "https://upload.wikimedia.org/wikipedia/commons/X/XX/Vendor_logo.svg" -o package/VENDOR-NAME/logo.svg
```

**Option C: From Other Reliable Sources**
- Brandfetch: `https://brandfetch.com/DOMAIN`
- Official press kits
- WorldVectorLogo

#### 3. Validate the Logo File

Run these validation tests to ensure the logo file is correct:

```bash
# Test 1: Check file type
file package/VENDOR-NAME/logo.svg

# Test 2: Check file size
ls -lh package/VENDOR-NAME/logo.svg

# Test 3: Visual inspection (optional)
head -5 package/VENDOR-NAME/logo.svg
```

**Logo Validation Test Criteria:**

| Test | Command | Pass Criteria | Fail Indicators |
|------|---------|---------------|-----------------|
| **File Type** | `file package/{vendor}/logo.svg` | Output: `SVG Scalable Vector Graphics image` | `XML 1.0 document`, `HTML document`, `ASCII text`, `ERROR` |
| **File Type** | `file package/{vendor}/logo.png` | Output: `PNG image data, {width} x {height}...` | `JPEG`, `GIF`, `ERROR` |
| **File Size (SVG)** | `ls -lh package/{vendor}/logo.svg` | 1KB - 100KB (most < 50KB) | < 200 bytes (likely error), > 5MB (too large) |
| **File Size (PNG)** | `ls -lh package/{vendor}/logo.png` | 5KB - 500KB | < 500 bytes (likely error), > 5MB (too large) |
| **Content Check** | `head -5 package/{vendor}/logo.svg` | Starts with `<?xml` or `<svg` | Contains `<Error>`, `AccessDenied`, `<html>` |

**Examples:**

✅ **Valid SVG:**
```bash
$ file package/epicgames/logo.svg
package/epicgames/logo.svg: SVG Scalable Vector Graphics image

$ ls -lh package/epicgames/logo.svg
-rw-r--r-- 1 user user 25K Nov 14 14:37 package/epicgames/logo.svg

$ head -3 package/epicgames/logo.svg
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 647 751">
<path d="M95.9384 29.6047..."/>
```

❌ **Invalid File (Error Message):**
```bash
$ file package/vendor/logo.svg
package/vendor/logo.svg: XML 1.0 document, ASCII text

$ ls -lh package/vendor/logo.svg
-rw-r--r-- 1 user user 243 Nov 14 14:36 package/vendor/logo.svg

$ head -3 package/vendor/logo.svg
<?xml version="1.0" encoding="UTF-8"?>
<Error><Code>AccessDenied</Code>
<Message>Access Denied</Message>
```

**Important Logo Rules:**
- **Accepted formats**: SVG or PNG only
- **One logo per vendor**: Only one logo file (logo.svg OR logo.png, never both)
- **Preference**: SVG is preferred over PNG
- **File naming**: Must be exactly `logo.svg` or `logo.png`

#### 4. Update package.json

Add the logo file to the `files` array:

```json
{
  "files": [
    "index.yml",
    "logo.svg"
  ]
}
```

For PNG logos:
```json
{
  "files": [
    "index.yml",
    "logo.png"
  ]
}
```

#### 5. Update index.yml

Add the logo URL after the `name:` field:

```yaml
id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
code: vendorname
status: verified
name: Vendor Name
logo: https://cdn.auditmation.io/logos/vendorname.svg
description: Vendor description
type: vendor
ownerId: 00000000-0000-0000-0000-000000000000
url: https://www.vendor.com
```

**Logo URL Format:**
- For SVG: `https://cdn.auditmation.io/logos/{vendor-name}.svg`
- For PNG: `https://cdn.auditmation.io/logos/{vendor-name}.png`
- The extension must match the actual file type

#### 6. Rebuild npm-shrinkwrap.json

```bash
cd package/VENDOR-NAME

# Delete existing shrinkwrap
rm -f npm-shrinkwrap.json

# Refresh dependencies
npm install

# Generate new shrinkwrap
npm shrinkwrap
```

#### 7. Validate the Changes

```bash
cd package/VENDOR-NAME

# Run validation
npm run validate
```

Expected output:
```
Validated index.yml
Validated package.json
Validated .npmrc
Validation of artifact completed successfully.
```

### Updating an Existing Logo

To update/replace an existing logo:

1. **Remove the old logo file**:
   ```bash
   rm package/VENDOR-NAME/logo.svg  # or logo.png
   ```

2. **Follow steps 2-7** from "Adding a Logo" above

3. **Ensure package.json and index.yml** reference the correct file extension

## Submitting a Pull Request

### 1. Commit Your Changes

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:

```bash
# Stage your changes
git add package/VENDOR-NAME/

# Commit with conventional format
git commit -m "feat(vendor-name): add logo for Vendor Name

- Added logo.svg to vendor package
- Updated package.json and index.yml
- Rebuilt npm-shrinkwrap.json
```

**Commit Message Format:**
- `feat(vendor-name):` - New features (adding logo, new vendor)
- `fix(vendor-name):` - Bug fixes
- `chore(vendor-name):` - Maintenance tasks
- `docs:` - Documentation updates

### 2. Push to Your Fork

```bash
# Push your branch to your fork
git push origin feature/your-feature-name
```

### 3. Create a Pull Request

1. Go to your fork on GitHub: `https://github.com/YOUR-USERNAME/vendor`
2. Click "Compare & pull request" button
3. **Important**: Set the base repository and branch:
   - **Base repository**: `zerobias-org/vendor`
   - **Base branch**: `dev` (NOT `main`)
   - **Head repository**: `YOUR-USERNAME/vendor`
   - **Compare branch**: `feature/your-feature-name`

4. Fill in the PR template:

```markdown
## Description
Brief description of changes (e.g., "Added logo for Google vendor")

## Changes Made
- [ ] Added logo file (logo.svg or logo.png)
- [ ] Updated package.json files array
- [ ] Updated index.yml with logo URL
- [ ] Rebuilt npm-shrinkwrap.json
- [ ] Validated changes with `npm run validate`

## Type of Change
- [ ] New feature (adding logo)
- [ ] Bug fix (fixing incorrect logo)
- [ ] Documentation update
- [ ] Other (please describe)

## Testing
- [ ] Validation passes (`npm run validate`)
- [ ] Logo file is valid SVG/PNG
- [ ] Logo URL follows correct format
- [ ] No duplicate logo files (no both .svg and .png)

## Screenshots (if applicable)
Attach screenshots of the logo if helpful

## Additional Notes
Any additional information about the changes
```

5. Click "Create pull request"

### 4. Pull Request Requirements

Your PR must:
- ✅ Target the `dev` branch (not `main`)
- ✅ Follow conventional commit format
- ✅ Pass all validation checks
- ✅ Have a clear description of changes
- ✅ Include only related changes (don't mix unrelated updates)
- ✅ Follow the one logo per vendor rule
- ✅ Use correct logo URL format

## Code Review Process

### What Happens Next

1. **Automated Checks**: CI/CD will run validation on your changes
2. **Code Review**: Maintainers will review your PR
3. **Feedback**: You may receive comments or change requests
4. **Approval**: Once approved, a maintainer will merge your PR into `dev`
5. **Release**: Changes in `dev` will eventually be merged to `main` and published

### Responding to Feedback

If changes are requested:

```bash
# Make the requested changes in your local branch
# ... edit files ...

# Commit the changes
git add .
git commit -m "fix: address PR feedback"

# Push to your fork (updates the PR automatically)
git push origin feature/your-feature-name
```

### After Your PR is Merged

Clean up your local branches:

```bash
# Switch to main
git checkout main

# Pull latest changes from upstream
git pull upstream main

# Delete your feature branch
git branch -d feature/your-feature-name

# Delete the remote branch from your fork (optional)
git push origin --delete feature/your-feature-name
```

## Common Issues and Solutions

### Issue: Cannot push to zerobias-org/vendor directly
**Solution**: You must fork the repository and push to your fork, then create a PR.

### Issue: PR targets wrong branch (main instead of dev)
**Solution**: When creating PR, change the base branch to `dev` before submitting.

### Issue: Validation fails
**Solution**: Run `npm run validate` locally before committing. Check error messages and fix issues.

### Issue: Both logo.svg and logo.png exist
**Solution**: Delete one logo file (keep SVG if both exist). Only one logo per vendor is allowed.

### Issue: Logo URL extension doesn't match file
**Solution**: Ensure index.yml logo URL extension (.svg or .png) matches the actual file type.

### Issue: Merge conflicts
**Solution**:
```bash
# Update your branch with latest dev
git fetch upstream
git merge upstream/dev
# Resolve conflicts
git add .
git commit -m "fix: resolve merge conflicts"
git push origin feature/your-feature-name
```

## Getting Help

- **Documentation**: Check `.claude/` directory for agent workflows
- **Issues**: Search existing issues at https://github.com/zerobias-org/vendor/issues
- **New Issue**: Create a new issue if you need help

## Additional Resources

- [Git Fork Workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/forking-workflow)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Pull Requests](https://docs.github.com/en/pull-requests)

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (ISC).

---

Thank you for contributing to zerobias-org vendor! 🎉
