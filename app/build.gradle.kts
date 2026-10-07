import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// A standalone stdio launcher needs the complete runtime classpath. The normal
// jvmJar is not executable with `java -jar` and does not bundle dependencies.
tasks.register<Sync>("assembleMcp") {
    group = "distribution"
    description = "Assemble the headless MCP server and its runtime dependencies"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    into(layout.buildDirectory.dir("mcp"))
    from(tasks.named("jvmJar")) { into("lib") }
    from(requireNotNull(kotlin.targets.getByName("jvm").compilations.getByName("main").runtimeDependencyFiles)) {
        into("lib")
        // Different artifacts can share a filename; retain both on the wildcard classpath.
        eachFile { name = "${file.parentFile.name}-$name" }
    }
    from(rootProject.file("scripts/mcp.cmd"))
    from(rootProject.file("scripts/mcp.sh"))
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.reflect)
                implementation(libs.compose.macos.ui)
                implementation(libs.haze)
                implementation(libs.haze.materials)
                implementation(libs.backdrop)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.sfsymbols.MainKt"
        javaHome = System.getProperty("java.home")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SfSymbolsCatalog"
            packageVersion = providers.gradleProperty("appVersion").orElse(
                providers.environmentVariable("GITHUB_REF_NAME").map { ref ->
                    ref.removePrefix("v").takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+")) } ?: "1.0.0"
                }
            ).orElse("1.0.0").get()
            description = "Browse, search, and copy SF Symbols for Compose projects"
            vendor = "cosmicTaser"
            modules("java.prefs", "java.management", "jdk.unsupported", "jdk.crypto.ec")

            windows {
                iconFile.set(project.file("src/jvmMain/resources/app_icon.ico"))
                shortcut = true
                menu = true
                menuGroup = "SF Symbols Catalog"
                perUserInstall = true
                dirChooser = true
                // Keep this ID stable so newer package versions upgrade the app.
                upgradeUuid = "E09297C4-1F95-4BDB-B14C-60F3114F05BD"
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/app_icon.png"))
                shortcut = true
            }

            macOS {
                bundleID = "com.sfsymbols.catalog"
                iconFile.set(project.file("src/jvmMain/resources/app_icon.icns"))
                // Signing + notarization switch on only when CI provides the
                // Developer ID secrets; local and fork builds stay unsigned.
                val identity = System.getenv("MACOS_SIGNING_IDENTITY")
                if (!identity.isNullOrBlank()) {
                    signing {
                        sign.set(true)
                        this.identity.set(identity)
                    }
                    notarization {
                        appleID.set(System.getenv("NOTARIZATION_APPLE_ID"))
                        password.set(System.getenv("NOTARIZATION_PASSWORD"))
                        teamID.set(System.getenv("NOTARIZATION_TEAM_ID"))
                    }
                }
            }
        }
    }
}
