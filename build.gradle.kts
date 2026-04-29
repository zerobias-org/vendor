import com.zerobias.buildtools.content.SchemaPrimitives

plugins {
    id("zb.workspace")
}

group = "com.zerobias.content"

/**
 * Mirrors the dataloader's URL validation: `new URL(value)` from
 * `@zerobias-org/types-core-js`. Strings must parse as absolute URLs
 * with a scheme. Java's URI is permissive so we additionally require
 * the scheme + host shape the dataloader enforces in practice.
 */
fun requireUrlFormat(value: Any?, field: String, tag: String) {
    require(value is String && value.isNotBlank()) { "$tag $field must be a non-blank string" }
    try {
        val uri = java.net.URI(value)
        require(uri.scheme != null && uri.host != null) {
            "$tag $field must be an absolute URL (scheme + host): got '$value'"
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("$tag $field is not a valid URL: $value (${e.message})")
    }
}

// ════════════════════════════════════════════════════════════
// Vendor schema validator — owned by this repo.
//
// Composed from SchemaPrimitives (com.zerobias.buildtools.content),
// applied via zb.content's slot. Util ships the primitives; this repo
// owns the vendor-specific rules.
// ════════════════════════════════════════════════════════════
extra["contentValidator"] = { proj: org.gradle.api.Project ->
    val projectDir = proj.projectDir
    val tag = "[vendor-validator] ${proj.path}"

    require(projectDir.resolve("index.yml").isFile)    { "$tag index.yml missing in ${projectDir.path}" }
    require(projectDir.resolve("package.json").isFile) { "$tag package.json missing in ${projectDir.path}" }
    require(projectDir.resolve(".npmrc").isFile)       { "$tag .npmrc missing in ${projectDir.path}" }

    // index.yml schema — mirrors com/platform/dataloader VendorFileHandler.
    val indexDoc = SchemaPrimitives.parseYaml(projectDir.resolve("index.yml"))
    SchemaPrimitives.requireUuid(indexDoc["id"], "index.yml id")
    SchemaPrimitives.requireNonBlankString(indexDoc["code"], "index.yml code")
    SchemaPrimitives.requireNonBlankString(indexDoc["name"], "index.yml name")
    // status: VspStatusEnum from @zerobias-com/platform-core core.yml
    SchemaPrimitives.requireEnum(
        indexDoc["status"], "index.yml status",
        setOf("draft", "active", "rejected", "deleted", "verified"),
    )
    // description / url / logo are optional in the dataloader; only validate
    // format when present. URL fields go through java.net.URI for the same
    // shape check the dataloader performs via `new URL(...)`.
    indexDoc["description"]?.let {
        SchemaPrimitives.requireNonBlankString(it, "index.yml description")
    }
    indexDoc["url"]?.let { requireUrlFormat(it, "index.yml url", tag) }
    indexDoc["logo"]?.let { requireUrlFormat(it, "index.yml logo", tag) }
    // cpeVendors / aliases must be Arrays if present (dataloader: throws if not).
    indexDoc["cpeVendors"]?.let { SchemaPrimitives.requireStringList(it, "index.yml cpeVendors") }
    indexDoc["aliases"]?.let    { SchemaPrimitives.requireStringList(it, "index.yml aliases") }
    // tags items must be UUIDs (dataloader maps each tag through `new UUID(tag)`).
    indexDoc["tags"]?.let { tagsValue ->
        require(tagsValue is List<*>) { "$tag index.yml tags must be a list" }
        tagsValue.forEachIndexed { i, item ->
            SchemaPrimitives.requireUuid(item, "index.yml tags[$i]")
        }
    }

    val code = indexDoc["code"] as String
    SchemaPrimitives.requireCodeMatchesDir(code, projectDir.name, "index.yml code")
    // Mirrors the dataloader's VendorFileHandler code regex (lowercase letters,
    // digits, underscores). Fast-fail at gate time instead of failing on prod.
    require(Regex("^[\\d_a-z]+\$").matches(code)) {
        "$tag code='$code' must match ^[\\d_a-z]+\$ (lowercase alphanumeric with underscores) — see com/platform/dataloader VendorFileHandler"
    }

    // package.json schema — vendor-specific npm name + zerobias block
    val pkgDoc = SchemaPrimitives.parseJson(projectDir.resolve("package.json"))
    val expectedName = "@zerobias-org/vendor-${code.replace('.', '-')}"
    require(pkgDoc["name"] == expectedName) {
        "$tag package.json name is '${pkgDoc["name"]}' but expected '$expectedName' (derived from code=$code)"
    }
    SchemaPrimitives.requireNonBlankString(pkgDoc["description"], "package.json description")

    // zerobias block (also accepts legacy auditmation key for transitional packages)
    val artifact = SchemaPrimitives.getPath(pkgDoc, "zerobias.import-artifact")
        ?: SchemaPrimitives.getPath(pkgDoc, "auditmation.import-artifact")
    require(artifact == "vendor") {
        "$tag expected zerobias.import-artifact='vendor', got '$artifact'"
    }
    val pkgField = SchemaPrimitives.getPath(pkgDoc, "zerobias.package")
        ?: SchemaPrimitives.getPath(pkgDoc, "auditmation.package")
    require(pkgField == code) {
        "$tag zerobias.package='$pkgField' must match index.yml code '$code'"
    }
    val dataloaderVersion = SchemaPrimitives.getPath(pkgDoc, "zerobias.dataloader-version")
        ?: SchemaPrimitives.getPath(pkgDoc, "auditmation.dataloader-version")
    SchemaPrimitives.requireNonBlankString(dataloaderVersion, "zerobias.dataloader-version")

    proj.logger.lifecycle("$tag: code=$code")
}

val projectPaths by tasks.registering {
    group = "info"
    description = "Output project-to-directory mappings for tooling (used by zbb CLI)"
    doLast {
        subprojects.filter { it.buildFile.exists() }.forEach { p ->
            println("${p.path}=${p.projectDir.relativeTo(rootDir)}")
        }
    }
}

val changedModules by tasks.registering {
    group = "info"
    description = "List vendors changed since last version tag"
    doLast {
        val lastTag = try {
            providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
            }.standardOutput.asText.get().trim()
        } catch (e: Exception) {
            logger.warn("No version tags found -- listing all vendors as changed")
            null
        }

        val diffArgs = if (lastTag != null) {
            listOf("git", "diff", "--name-only", lastTag, "HEAD")
        } else {
            listOf("git", "ls-files")
        }

        val result = providers.exec {
            commandLine(diffArgs)
        }.standardOutput.asText.get()

        val changed = result.lines()
            .filter { it.startsWith("package/") }
            .map { it.split("/").drop(1).dropLast(1).joinToString("/") }
            .distinct()
            .filter { it.isNotEmpty() }

        changed.forEach { println(it) }
    }
}
