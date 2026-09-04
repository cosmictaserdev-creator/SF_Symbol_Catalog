import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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
    }
}

compose.desktop {
    application {
        mainClass = "com.sfsymbols.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SfSymbolsCatalog"
            packageVersion = "1.0.0"
        }
    }
}
