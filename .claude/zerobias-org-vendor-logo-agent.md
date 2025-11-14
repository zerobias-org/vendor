# Zerobias Org Vendor Logo Agent

## Purpose
This agent automates the process of grabbing a vendor's logo from their homepage and integrating it into the vendor package.

## For Contributors
If you're contributing to the zerobias-org/vendor repository, please see [CONTRIBUTING.md](../../CONTRIBUTING.md) for the complete fork and pull request workflow. This document focuses on the technical steps for adding logos.

## Logo Format Rules
- **Accepted formats**: SVG or PNG only
- **One logo per vendor**: Each vendor can have ONLY ONE logo file (either `logo.svg` OR `logo.png`, never both)
- **Preference**: SVG is preferred over PNG when both are available
- **Naming**: Logo file must be named exactly `logo.svg` or `logo.png`

## Workflow

### Step 0: Check for Existing Logo
- Check if vendor already has a logo file:
  - Look for `package/{vendor}/logo.svg`
  - Look for `package/{vendor}/logo.png`
- If a logo already exists, STOP and ask user if they want to replace it
- **IMPORTANT**: Never allow both logo.svg and logo.png to exist simultaneously

### Step 1: Read and Verify Vendor Configuration
- Read the vendor's `package/{vendor}/index.yml` file to get the homepage URL from the `url` field
- **Verify the URL is correct** - check that it matches the actual vendor's website
- If URL seems incorrect (e.g., points to wrong domain), correct it before proceeding

### Step 2: Fetch Logo from Homepage (Primary Method)

**Option A: Using WebFetch (for simple sites)**
- Use WebFetch to visit the vendor's homepage
- Extract the logo URL from the page (look in header, footer, or main navigation)
- Identify the primary logo (usually the main brand logo)
- **Prefer SVG format** over PNG if both are available
- Determine the logo format (SVG or PNG)

**Option B: Using curl + HTML parsing (for sites with bot protection)**
- Fetch HTML using curl with User-Agent header:
  ```bash
  curl -sL "https://www.{vendor}.com" -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
  ```
- Parse HTML for logo references:
  - Inline SVG: Search for `<svg` elements with class/id containing "logo"
  - SVG/PNG references: `grep -i "logo.*\.svg\|logo.*\.png"`
  - Common patterns: `<img.*logo`, `src=.*logo`, `href=.*logo`
- Extract the logo URL or inline SVG content

**If both methods fail (403, 404, Cloudflare challenge, or other errors), proceed to Step 2B**

### Step 2B: Fallback Logo Discovery Methods
When direct homepage access fails, try these alternatives in order:

1. **WebSearch for Logo Sources**
   - Search: `"{vendor name}" official logo SVG download`
   - Look for reliable sources: Wikimedia Commons, official press kits, Brandfetch

2. **Try Wikimedia Commons**
   - Search for the vendor logo on Wikimedia Commons
   - Use WebFetch on the Wikimedia page OR try direct URL pattern:
   - Pattern: `https://upload.wikimedia.org/wikipedia/commons/{hash}/{Vendor_Name}_logo.svg`
   - Example: `https://upload.wikimedia.org/wikipedia/commons/3/31/Epic_Games_logo.svg`

3. **Try Common CDN Patterns**
   - Many vendors use predictable CDN patterns
   - Examples: `https://cdn.{vendor}.com/logo.svg`, `https://www.{vendor}.com/assets/logo.svg`

4. **Ask User**
   - If all automated methods fail, ask user to provide logo URL or file

### Step 3: Download and Validate Logo
- Download the logo using curl: `curl -sL "{logo_url}" -o package/{vendor}/logo.{ext}`
- **Validate the downloaded file** using the test criteria below
- Save it to `package/{vendor}/logo.svg` (for SVG) or `package/{vendor}/logo.png` (for PNG)
- **Critical**: Ensure the file extension matches the actual format downloaded

**Logo Validation Test Criteria:**

| Test | Command | Pass Criteria | Fail Indicators |
|------|---------|---------------|-----------------|
| **File Type** | `file package/{vendor}/logo.svg` | Output: `SVG Scalable Vector Graphics image` | `XML 1.0 document`, `HTML document`, `ASCII text`, `ERROR` |
| **File Type** | `file package/{vendor}/logo.png` | Output: `PNG image data, {width} x {height}...` | `JPEG`, `GIF`, `ERROR` |
| **File Size (SVG)** | `ls -lh package/{vendor}/logo.svg` | 1KB - 100KB (most < 50KB) | < 200 bytes (likely error), > 5MB (too large) |
| **File Size (PNG)** | `ls -lh package/{vendor}/logo.png` | 5KB - 500KB | < 500 bytes (likely error), > 5MB (too large) |
| **Content Check** | `head -5 package/{vendor}/logo.svg` | Starts with `<?xml` or `<svg` | Contains `<Error>`, `AccessDenied`, `<html>` |

If validation fails, delete the file and try another source

### Step 4: Update package.json
- Read `package/{vendor}/package.json`
- Add the logo file to the `files` array:
  - Add `"logo.svg"` if SVG format
  - Add `"logo.png"` if PNG format
- The files array should include both `"index.yml"` and the logo file
- **Never include both** `"logo.svg"` and `"logo.png"` in the files array

### Step 5: Update index.yml
- Read `package/{vendor}/index.yml`
- Add the logo URL after the `name:` field with the correct extension:
  - For SVG: `logo: https://cdn.auditmation.io/logos/{vendor}.svg`
  - For PNG: `logo: https://cdn.auditmation.io/logos/{vendor}.png`
- **Important**: The extension in the URL must match the actual file format

### Step 6: Rebuild Dependencies (see zerobias-org-vendor-shrinkwrap agent)
- Delete existing `package/{vendor}/npm-shrinkwrap.json`
- Run `npm i reset` in the vendor directory
- Run `npm shrinkwrap` to generate new npm-shrinkwrap.json

## Example Usage

### Example 1: SVG Logo (eclipsefoundation)

1. Check for existing logo → none found
2. Read `package/eclipsefoundation/index.yml` → get `https://www.eclipse.org/`
3. WebFetch homepage → find logo at `https://www.eclipse.org/eclipse.org-common/themes/solstice/public/images/logo/eclipse-foundation-grey-orange.svg`
4. Download to `package/eclipsefoundation/logo.svg`
5. Add `"logo.svg"` to `package/eclipsefoundation/package.json` files array
6. Add `logo: https://cdn.auditmation.io/logos/eclipsefoundation.svg` to `index.yml`
7. Rebuild npm-shrinkwrap.json

### Example 2: PNG Logo (vendor with PNG only)

1. Check for existing logo → none found
2. Read `package/somevendor/index.yml` → get vendor URL
3. WebFetch homepage → find logo (only PNG available)
4. Download to `package/somevendor/logo.png`
5. Add `"logo.png"` to `package/somevendor/package.json` files array
6. Add `logo: https://cdn.auditmation.io/logos/somevendor.png` to `index.yml`
7. Rebuild npm-shrinkwrap.json

### Example 3: Using Fallback Methods (epicgames)

1. Check for existing logo → none found
2. Read `package/epicgames/index.yml` → get `https://www.unrealengine.com`
3. **Verify URL** → incorrect! Should be `https://www.epicgames.com`
4. Fix URL in index.yml to `https://www.epicgames.com`
5. WebFetch homepage → **FAILS with 403 (Access Denied)**
6. **Fallback: WebSearch** → search "Epic Games official logo SVG download"
7. Find Wikimedia Commons has Epic Games logo
8. **Try Wikimedia direct URL**: `https://upload.wikimedia.org/wikipedia/commons/3/31/Epic_Games_logo.svg`
9. Download successfully → validate with `file` command → confirms "SVG Scalable Vector Graphics image"
10. Check file size → 25KB (valid)
11. Add `"logo.svg"` to `package/epicgames/package.json` files array
12. Add `logo: https://cdn.auditmation.io/logos/epicgames.svg` to `index.yml`
13. Rebuild npm-shrinkwrap.json

## Notes

- Always verify the logo is the correct brand logo (not a favicon or generic icon)
- **Prefer SVG format over PNG** when both are available on the vendor's site
- If multiple logo variations exist, choose the primary/main brand logo
- The logo URL in index.yml should always use the CDN domain `cdn.auditmation.io`
- Logo filename (without extension) must match the vendor folder name exactly
- **Never have both logo.svg and logo.png** for the same vendor
- The file extension in the CDN URL must match the actual file type (.svg or .png)

## Common Issues and Solutions

### Issue: WebFetch returns 403 Forbidden or Cloudflare Challenge
- **Cause**: Website blocks automated requests or uses bot protection (Cloudflare, etc.)
- **Symptom**: HTML contains "Just a moment...", challenge page, or access denied message
- **Solutions**:
  1. Try curl with User-Agent header (may bypass simple bot detection)
  2. If Cloudflare/bot protection detected, skip to fallback methods
  3. Use WebSearch + Wikimedia Commons (most reliable for protected sites)

### Issue: Downloaded file is not valid SVG/PNG
- **Symptom**: File contains error messages or XML errors
- **Solution**: Validate with `file` command, delete invalid file, try another source

### Issue: URL in index.yml is incorrect
- **Symptom**: Doesn't match vendor's actual website
- **Solution**: Correct the URL in index.yml before attempting logo download

### Issue: Cannot find logo on homepage
- **Cause**: Logo might be loaded dynamically or behind authentication
- **Solution**: Use WebSearch to find official logo sources like Wikimedia, Brandfetch, or press kits

## Reliable Logo Sources

When fallback methods are needed, these sources are typically reliable:

1. **Wikimedia Commons** - `https://commons.wikimedia.org` or direct upload URL
2. **Brandfetch** - `https://brandfetch.com/{domain}`
3. **Official Press Kits** - Check vendor's website for media/press section
4. **WorldVectorLogo** - `https://worldvectorlogo.com`
5. **SeekLogo** - `https://seeklogo.com` (verify license)

Always prefer official sources and Wikimedia Commons for well-known brands.

## HTML Parsing Techniques

When homepage is accessible (no bot protection), use these grep patterns to find logos:

```bash
# Find SVG file references
curl -sL "https://www.{vendor}.com" -H "User-Agent: Mozilla/5.0" | grep -oP 'src="[^"]*logo[^"]*\.svg"' | head -5

# Find PNG file references
curl -sL "https://www.{vendor}.com" -H "User-Agent: Mozilla/5.0" | grep -oP 'src="[^"]*logo[^"]*\.png"' | head -5

# Find inline SVG elements with logo class/id
curl -sL "https://www.{vendor}.com" -H "User-Agent: Mozilla/5.0" | grep -i '<svg.*logo' | head -3

# Extract specific logo URL pattern
curl -sL "https://www.{vendor}.com" -H "User-Agent: Mozilla/5.0" | grep -oP 'https?://[^"]*logo[^"]*\.(svg|png)' | head -10
```

**Note**: Modern sites often:
- Load logos via JavaScript (won't appear in initial HTML)
- Use Cloudflare/bot protection (blocks automated access)
- Serve different content to bots vs browsers

For these cases, fallback methods (WebSearch + Wikimedia) are more reliable.
