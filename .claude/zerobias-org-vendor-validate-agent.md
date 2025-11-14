# Zerobias Org Vendor Validate Agent

## Purpose
This agent validates vendor package records to ensure they meet all requirements for the zerobias-org vendor monorepo. It checks the structure, required fields, and consistency of vendor packages.

## Validation Script
The validation is performed using: `scripts/validate.ts`

## What Gets Validated

### 1. Directory Structure
- Vendor directory must exist under `package/{vendor}/`
- Must contain required files:
  - `index.yml` (required)
  - `package.json` (required)
  - `.npmrc` (required)
  - `logo.svg` OR `logo.png` (optional but recommended, **NEVER BOTH**)
  - `npm-shrinkwrap.json` (generated, should exist)

**Critical Logo Rule**: Each vendor can have ONLY ONE logo file - either `logo.svg` OR `logo.png`, never both simultaneously.

### 2. index.yml Validation
Required fields:
- **id**: Must be a valid UUID (not placeholder `{id}`)
- **code**: Vendor code identifier (not placeholder `{code}`)
- **name**: Vendor name (not placeholder `{name}`)
- **description**: Vendor description (not placeholder `{description}`)
- **url**: Vendor homepage URL (not placeholder `{url}`)
- **status**: Must be a valid VspStatusEnum value (e.g., "verified", "draft")
- **type**: Must be set to "vendor"
- **ownerId**: Owner UUID (typically `00000000-0000-0000-0000-000000000000`)

Optional fields:
- **logo**: URL to logo, must match file format:
  - For SVG: `https://cdn.auditmation.io/logos/{vendor}.svg`
  - For PNG: `https://cdn.auditmation.io/logos/{vendor}.png`
  - **Extension must match the actual logo file type**
- **imageUrl**: If present, must be a valid URL
- **aliases**: If present, must be a string array

### 3. package.json Validation
Required fields:
- **name**: Must match pattern `@zerobias-org/vendor-{code}`
- **description**: Must not be placeholder `{name}`
- **files**: Array of files to include in package
  - Must include `"index.yml"` (required)
  - May include `"logo.svg"` OR `"logo.png"` (optional)
  - **Must NOT include both** `"logo.svg"` and `"logo.png"` simultaneously
  - Logo file in files array must match the actual file that exists
- **auditmation**: Object with:
  - **import-artifact**: Must be set to "vendor"
  - **package**: Must match the vendor code
  - **dataloader-version**: Must be specified

### 4. .npmrc Validation
- File must exist in vendor directory
- Contains npm registry configuration

## Running Validation

### Validate Single Vendor
```bash
cd package/{vendor}
npm run validate
# or directly:
ts-node ../../scripts/validate.ts
```

### Validate Changed Packages (from root)
```bash
npm run validate
```

## Common Validation Errors

### Placeholder Not Replaced
- Error: `code in index.yml needs replacement from {code}`
- Fix: Replace placeholder values with actual vendor data

### Missing Required Field
- Error: `id not found in index.yml`
- Fix: Add the missing field with appropriate value

### Invalid Package Name
- Error: `package.json missing name or not set to @zerobias-org/vendor-<code>`
- Fix: Ensure package.json name matches `@zerobias-org/vendor-{code}` pattern

### Missing Auditmation Section
- Error: `package.json missing auditmation section`
- Fix: Add auditmation object with required fields

### Invalid UUID
- Error thrown when parsing UUID
- Fix: Generate valid UUID for the id field

### Logo Validation Errors

#### Multiple Logo Files
- Error: Both `logo.svg` and `logo.png` exist
- Fix: Keep only ONE logo file (prefer SVG), delete the other

#### Logo File Mismatch
- Error: `logo.svg` listed in package.json files array but `logo.png` file exists
- Fix: Update package.json files array to match the actual logo file

#### Logo URL Extension Mismatch
- Error: `logo: https://cdn.auditmation.io/logos/{vendor}.svg` in index.yml but `logo.png` file exists
- Fix: Update index.yml logo URL extension to match actual file (.png)

#### Missing Logo File
- Error: `logo.svg` listed in package.json but file doesn't exist
- Fix: Either add the logo file or remove it from package.json files array

## Agent Workflow

### Step 1: Pre-validation Checks
- Verify vendor directory exists
- Check all required files are present
- **Logo file validation**:
  - Check if `logo.svg` exists
  - Check if `logo.png` exists
  - **FAIL if both exist** - only one logo type allowed
  - Record which logo type exists (if any)

### Step 2: Parse Configuration Files
- Parse `index.yml` using YAML parser
- Parse `package.json` using JSON parser

### Step 3: Validate index.yml
- Check all required fields are present
- Verify no placeholder values remain
- Validate UUID format for id field
- Validate URL format for url and logo fields
- Verify status is valid enum value
- **Logo URL validation** (if logo field exists):
  - Verify logo URL matches pattern `https://cdn.auditmation.io/logos/{vendor}.(svg|png)`
  - Extract extension from logo URL (.svg or .png)
  - **Verify extension matches actual logo file** that exists
  - **FAIL if logo URL says .svg but logo.png exists (or vice versa)**

### Step 4: Validate package.json
- Check name matches pattern `@zerobias-org/vendor-{code}`
- Verify description is not placeholder
- Validate auditmation section exists and is complete
- Check files array includes required files
- **Logo file validation**:
  - If logo file exists, ensure it's in files array
  - Verify files array doesn't contain both `"logo.svg"` and `"logo.png"`
  - If `"logo.svg"` in files array, verify `logo.svg` file exists
  - If `"logo.png"` in files array, verify `logo.png` file exists
  - **FAIL if files array and filesystem don't match**

### Step 5: Report Results
- Output validation success or detailed error messages
- Return exit code 0 for success, 1 for failure

## Example Usage

```bash
# Validate eclipsefoundation package
cd package/eclipsefoundation
npm run validate

# Expected output on success:
# Validated index.yml
# Validated package.json
# Validated .npmrc
# Validation of artifact completed successfully.
```

## Integration with Other Agents

This validation agent should be run:
- **After** zerobias-org-vendor-logo-agent completes
- **After** zerobias-org-vendor-shrinkwrap-agent completes
- **Before** committing changes
- **Before** publishing packages

## Notes

- Validation is enforced via npm scripts in each vendor's package.json
- The validate script runs in the context of the vendor directory
- TypeScript is required to run the validation script
- Dependencies from @auditmation packages are used for type checking
