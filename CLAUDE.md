# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a monorepo for managing vendor integration packages under the @zerobias-org organization. Each vendor has its own package in the `package/` directory with standardized structure including metadata, logos, and version management.

## Essential Commands

### Development Setup
```bash
# Install dependencies across all packages
npm run bootstrap

# Clean and reset the repository
npm run reset  # Full reset including dependencies
npm run clean  # Clean build artifacts only
```

### Working with Vendors
```bash
# Create a new vendor package
cp -r templates/vendorA package/[vendor-name]
# Then update package.json, index.yml, and add appropriate logo

# Validate changed packages
npm run validate

# Generate npm-shrinkwrap.json files
npm run build
```

### Publishing and Version Management
```bash
# Test version changes without committing
npm run lerna:dry-run

# Publish packages (requires ZB_TOKEN)
npm run nx:publish

# View dependency graph
npm run nx:graph
```

### Git Workflow
```bash
# Commits must follow Conventional Commits format:
# feat(vendor-name): add new feature
# fix(vendor-name): fix issue
# chore(vendor-name): update dependencies

# Pre-commit hooks will validate commit messages
```

## Architecture

### Package Structure
Each vendor package (`package/[vendor-name]/`) contains:
- `package.json` - NPM package metadata
- `index.yml` - Vendor configuration and metadata
- `logo.svg` or `logo.png` - Vendor branding
- `npm-shrinkwrap.json` - Locked dependencies (generated)
- `CHANGELOG.md` - Version history (auto-generated)

### Key Technologies
- **Lerna**: Manages versioning and publishing with independent mode
- **Nx**: Provides task running and caching capabilities
- **TypeScript**: Used for build scripts (v4.8.4)
- **Conventional Commits**: Enforced via commitlint for automated versioning

### Publishing Flow
1. Changes are made following conventional commit format
2. Lerna determines version bumps based on commit types
3. Nx corrects internal dependencies before publishing
4. Packages are published to GitHub Packages registry

## Important Notes

- **Authentication**: Set `ZB_TOKEN` environment variable for npm registry access
- **Commit Format**: All commits must follow Conventional Commits specification
- **Private Registry**: Packages publish to `npm.pkg.github.com/@zerobias-org`
- **No Direct npm publish**: Always use `npm run nx:publish` to ensure dependencies are correct
- **Vendor Naming**: Package names should match the pattern `@zerobias-org/vendor-[name]`