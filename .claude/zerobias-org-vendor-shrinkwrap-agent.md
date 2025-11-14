# Zerobias Org Vendor Shrinkwrap Agent

## Purpose
This agent handles the rebuilding of npm-shrinkwrap.json files for vendor packages after changes to package files or dependencies.

## Workflow

### Step 1: Remove Existing Shrinkwrap
- Delete `package/{vendor}/npm-shrinkwrap.json` if it exists
- This ensures a clean rebuild without cached dependency states

### Step 2: Install Dependencies
- Navigate to `package/{vendor}/` directory
- Run `npm i reset` to install/update the `reset` dependency
- This is a required dependency for all vendor packages

### Step 3: Generate Shrinkwrap
- Run `npm shrinkwrap` in the vendor directory
- This creates a new `npm-shrinkwrap.json` file
- The shrinkwrap file locks dependency versions for reproducible builds

## Command Sequence

```bash
cd /home/toor/local_zerobiasorg/vendor/package/{vendor}
rm -f npm-shrinkwrap.json
npm i reset
npm shrinkwrap
```

## When to Use

This agent should be run after:
- Adding new files to the package (e.g., logo.svg)
- Modifying package.json files array
- Updating dependencies in package.json
- Any structural changes to the vendor package

## Example Usage

For vendor `eclipsefoundation`:

```bash
cd /home/toor/local_zerobiasorg/vendor/package/eclipsefoundation
rm -f npm-shrinkwrap.json
npm i reset
npm shrinkwrap
```

## Notes

- Always use absolute paths when running commands to avoid directory confusion
- The `reset` package is a standard dependency for all vendor packages
- npm-shrinkwrap.json should be committed to the repository
- This process ensures consistent package publishing
