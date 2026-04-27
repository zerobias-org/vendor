pluginManagement {
    // Use local build-tools if available (dev), otherwise pull from GitHub Packages Maven (CI)
    val localBuildTools = file("../util/packages/build-tools")
    if (localBuildTools.exists()) {
        includeBuild(localBuildTools)
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/zerobias-org/util")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "zerobias-org"
                password = System.getenv("READ_TOKEN") ?: System.getenv("NPM_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
    // Resolve latest build-tools from Maven (used in CI when local composite build is absent)
    plugins {
        id("zb.workspace") version "1.+"
        id("zb.base") version "1.+"
        id("zb.content") version "1.+"
    }
}

rootProject.name = "vendors"

// Auto-discover all vendors under package/.
// A directory is a vendor if it contains build.gradle.kts.
// The per-package build.gradle.kts is a one-line marker:
//   plugins { id("zb.content") }
// — same pattern as module/collectorbot. Matches zbb's design rule that
// every publishable subproject must have its own build.gradle.kts so
// `cd package/<vendor> && zbb gate` resolves to the right project.
// Project names mirror filesystem: package/accelq → :accelq
val packageDir = file("package")
if (packageDir.exists()) {
    packageDir.walkTopDown()
        .filter { it.name == "build.gradle.kts" }
        .forEach { buildFile ->
            val moduleDir = buildFile.parentFile
            val relativePath = moduleDir.relativeTo(packageDir).path
            val projectPath = relativePath.replace(File.separatorChar, ':')

            include(projectPath)
            project(":$projectPath").projectDir = moduleDir
        }
}
