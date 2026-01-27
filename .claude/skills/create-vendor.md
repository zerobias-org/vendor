# Create Vendor Skill

Create vendor packages from ZeroBias tasks with proper task management and git workflow.

## Trigger

```
/create-vendor [task-id]
```

**Arguments:**
- `task-id` (optional): ZeroBias task UUID or task name. If not provided, will prompt for input.

## Examples

```
/create-vendor bbd73958-f3f6-4ec7-a2ed-79cb105c9c19
/create-vendor "Create vendor: aiuc (AI Use Case)"
/create-vendor
```

---

## Workflow

### Step 1: Get Task Details

```javascript
// If UUID provided
const task = zerobias_execute("platform.Task.get", { id: taskId })

// If task name provided (search)
const results = zerobias_execute("portal.Task.search", {
  searchTaskBody: { search: "vendor name" }
})
const task = results.items.find(t => t.name.includes("vendor"))
```

**Task code is NOT searchable** - use UUID or task name.

### Step 2: Extract Vendor Information

| Field | Source | Example |
|-------|--------|---------|
| **Task ID** | `task.id` | `e5d69bed-e660-4c6c-b970-0a6d6318276a` |
| **Task Code** | `task.code` | `contextDev-4` |
| **Vendor Code** | Parse from name | `aiuc` from "Create vendor: aiuc (AI Use Case)" |
| **Vendor Name** | Parse from name | `AI Use Case` |
| **Artifact Type** | `task.customFields.artifactType` | `vendor` |
| **Branch Name** | `task.customFields.branchName` | `feat/vendor-aiuc` |
| **Repo URL** | `task.customFields.repoUrl` | `https://github.com/zerobias-org/vendor` |
| **Website URL** | Parse from description | `https://aiuc.org` |
| **Logo URL** | Parse from description | `https://aiuc.org/logo.svg` |

**Parse vendor info from task name:**
```javascript
// Task name format: "Create vendor: {code} ({name})"
const match = task.name.match(/Create vendor:\s*(\S+)\s*\(([^)]+)\)/)
const vendorCode = match[1]  // "aiuc"
const vendorName = match[2]  // "AI Use Case"
```

**Parse additional info from task description:**
```javascript
// Task description may contain website and logo URLs
// Example description:
// "Create vendor package for AI Use Case.
//  Website: https://aiuc.org
//  Logo: https://aiuc.org/assets/logo.svg"

// Extract website URL
const websiteMatch = task.description.match(/website:\s*(https?:\/\/[^\s]+)/i)
  || task.description.match(/(https?:\/\/[^\s]+)/)
const websiteUrl = websiteMatch ? websiteMatch[1] : null

// Extract logo URL (explicit)
const logoMatch = task.description.match(/logo:\s*(https?:\/\/[^\s]+)/i)
const logoUrl = logoMatch ? logoMatch[1] : null
```

### Step 3: Assign and Transition to In Progress

**IMPORTANT:** Set required fields BEFORE applying transition.

```javascript
// Get your party ID
const party = zerobias_execute("platform.Party.getMyParty", {})

// Update task with required fields and transition
zerobias_execute("platform.Task.update", {
  id: task.id,
  updateTask: {
    assigned: party.id,  // Party ID, NOT principal ID
    customFields: {
      artifactType: "vendor",
      repoUrl: "https://github.com/zerobias-org/vendor",
      branchName: `feat/vendor-${vendorCode}`
    },
    transitionId: "7f140bbe-4c10-54ac-922c-460c66392fad"  // Start
  }
})
```

**Transition Required Fields:**

| Transition | Required Fields | Required Custom Fields |
|------------|-----------------|------------------------|
| Start | assigned | repoUrl, branchName |
| Peer Review | assigned, approvers | - |
| Accept | assigned | fixVersion |

### Step 4: Add Starting Comment

```javascript
zerobias_execute("platform.Task.addComment", {
  id: task.id,
  newTaskComment: {
    commentMarkdown: `**Started:** Creating vendor package.

**Task:** ${task.code}
**Vendor:** ${vendorCode}
**Branch:** feat/vendor-${vendorCode}
**Repo:** https://github.com/zerobias-org/vendor`
  }
})
```

### Step 5: Check if Vendor Already Exists

**IMPORTANT:** Vendors are the ROOT of the dependency chain. No dependencies to check.

```javascript
// Check if vendor already exists in catalog
const vendors = zerobias_execute("portal.Vendor.search", {
  searchVendorBody: { search: vendorCode }
})

const exists = vendors.items.some(v =>
  v.code?.toLowerCase() === vendorCode.toLowerCase()
)

if (exists) {
  // Vendor already exists - task may be complete or duplicate
  // Add comment and close/cancel task
}
```

### Step 6: Create Git Branch

```bash
cd /path/to/zerobias-org/vendor
git checkout main
git pull origin main
git checkout -b feat/vendor-{vendorCode}
```

### Step 7: Create Vendor Package Structure

```bash
# Create package directory
mkdir -p package/{vendorCode}
cd package/{vendorCode}
```

**Required files:**

```
package/{vendorCode}/
├── package.json          # @zerobias-org/vendor-{code}
├── index.yml             # Vendor metadata
├── logo.svg              # Official vendor logo
├── npm-shrinkwrap.json   # Generated dependency lock
└── .npmrc                # Registry configuration
```

### Step 8: Create package.json

```json
{
  "name": "@zerobias-org/vendor-{vendorCode}",
  "version": "1.0.0",
  "description": "Vendor package for {Vendor Name}.",
  "author": "team@zerobias.com",
  "license": "ISC",
  "publishConfig": {
    "registry": "https://npm.pkg.github.com/"
  },
  "repository": {
    "type": "git",
    "url": "git@github.com:zerobias-org/vendor.git",
    "directory": "vendor/"
  },
  "files": ["index.yml", "logo.svg"],
  "scripts": {
    "nx:publish": "../../scripts/publish.sh",
    "prepublishtest": "../../scripts/prepublish.sh",
    "correct:deps": "ts-node ../../scripts/correctDeps.ts",
    "validate": "ts-node ../../scripts/validate.ts"
  },
  "auditmation": {
    "package": "{vendorCode}",
    "import-artifact": "vendor",
    "dataloader-version": "5.0.14"
  },
  "dependencies": {}
}
```

### Step 9: Create index.yml

```yaml
code: {vendorCode}
status: active
id: {generate-uuid-v4}
name: {Vendor Full Name}
type: vendor
ownerId: 00000000-0000-0000-0000-000000000000
created: {current-iso-timestamp}
updated: {current-iso-timestamp}
url: https://{vendor-website}.com
description: >-
  Description of the vendor organization.
logo: https://cdn.auditmation.io/logos/{vendorCode}.svg
cpeVendors:
  - {vendorCode}
```

**CRITICAL:**
- Generate new UUID v4 for `id`
- Use real current timestamp (not placeholder like `00:00:00.000Z`)
- Include proper vendor URL and description

### Step 10: Extract and Download Logo

**Extract logo URL from task description:**

The task description may contain:
- Direct logo URL: `logo: https://example.com/logo.svg`
- Website URL: `https://vendor-website.com`

```javascript
// Parse logo URL from description
const logoMatch = task.description.match(/logo:\s*(https?:\/\/[^\s]+)/i)
const logoUrl = logoMatch ? logoMatch[1] : null

// Parse website URL from description
const websiteMatch = task.description.match(/https?:\/\/[^\s]+/)
const websiteUrl = websiteMatch ? websiteMatch[0] : null

// Logo sources to try (in order):
// 1. Explicit logo URL from task
// 2. Common logo paths on vendor website
// 3. Search for official logo
```

**Download logo:**

```bash
# If logo URL specified in task
curl -o logo.svg "{logoUrl}"

# Or try common logo locations on vendor website
curl -o logo.svg "https://{vendor-website}/logo.svg"
curl -o logo.svg "https://{vendor-website}/assets/logo.svg"
curl -o logo.svg "https://{vendor-website}/images/logo.svg"

# Verify download
ls -lh logo.svg
```

**Logo best practices:**
- Use official SVG logos when available
- Never modify SVG content
- Verify file size after download
- If no logo found, note in PR that logo needs to be added

### Step 11: Create .npmrc

```
@zerobias-org:registry=https://npm.pkg.github.com/
//npm.pkg.github.com/:_authToken=${ZB_TOKEN}
```

### Step 12: Install and Build

```bash
npm install
npm shrinkwrap
npm run validate
```

### Step 13: Commit and Push

```bash
git add package/{vendorCode}/
git commit -m "feat(vendor-{vendorCode}): add {Vendor Name} vendor package

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"

git push origin feat/vendor-{vendorCode}
```

### Step 14: Create Pull Request

```bash
gh pr create --title "feat(vendor-{vendorCode}): add {Vendor Name}" --body "$(cat <<'EOF'
## Summary
- **Task:** {task.code}
- **Vendor:** {vendorCode}
- **Package:** @zerobias-org/vendor-{vendorCode}

## Validation
- [x] `npm run validate` passes
- [x] index.yml has all required fields
- [x] Logo file present

## Task Reference
- **Task Code:** {task.code}
- **Task ID:** {task.id}

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

### Step 15: Update Task Status

```javascript
// Add completion comment
zerobias_execute("platform.Task.addComment", {
  id: task.id,
  newTaskComment: {
    commentMarkdown: `## Vendor Created

**Task:** ${task.code}
**Package:** @zerobias-org/vendor-${vendorCode}
**Branch:** feat/vendor-${vendorCode}
**PR:** ${prUrl}

### Next Steps
- PR needs review and merge
- After merge, vendor will be available in catalog`
  }
})

// Transition to awaiting_approval
zerobias_execute("platform.Task.update", {
  id: task.id,
  updateTask: {
    transitionId: "f017a447-0994-594d-9417-39cbc9a4de88"  // Peer Review
  }
})
```

---

## Linking to Parent Task

If this vendor was created as a dependency for another task:

```javascript
const relatesToLinkType = "b8bd95d0-b33c-11f0-8af3-dfaccf31600e"

zerobias_execute("platform.Resource.linkResources", {
  fromResource: vendorTaskId,
  toResource: parentTaskId,  // Note: toResource, NOT toResourceId
  linkType: relatesToLinkType
})
```

---

## Common Issues

### npm-shrinkwrap.json errors
```bash
# Regenerate shrinkwrap
rm -f npm-shrinkwrap.json
npm install
npm shrinkwrap
```

### Validation fails
- Check `index.yml` has all required fields
- Verify UUID format (lowercase v4)
- Ensure timestamps are real (not placeholders)
- Check `package.json` naming matches `@zerobias-org/vendor-{code}`

### Logo not found
- Search for official vendor logo
- Use placeholder if official not available
- Document in PR that logo needs to be updated

---

## Workflow Transitions Reference

| Transition | Target Status | ID |
|------------|---------------|-----|
| Start | in_progress | `7f140bbe-4c10-54ac-922c-460c66392fad` |
| Peer Review | awaiting_approval | `f017a447-0994-594d-9417-39cbc9a4de88` |
| Accept | released | `1d2e9381-f609-5e26-8bc6-7bbb65a9048d` |
| Reject | in_progress | `dda277e6-12d4-581b-922c-4e80d58d9083` |
| Cancel | cancelled | `711aa97f-f0bf-5c56-936f-f5e54d9de1f3` |

**Note:** Always get actual IDs from `task.nextTransitions`.

---

## References

- **Meta-repo CLAUDE.md:** `../../CLAUDE.md`
- **Orchestration docs:** `../../docs/orchestration/`
- **Templates:** `templates/`
