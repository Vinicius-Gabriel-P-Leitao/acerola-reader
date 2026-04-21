import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

fun capitalize(s: String): String {
    return s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

android {
    namespace = "br.acerola.comic.native"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = false
        buildConfig = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.register<Exec>("buildNativeRust") {
    group = "rust"

    environment("CARGO_NDK_PLATFORM", "26")
    commandLine(
        "cargo", "ndk", "-t", "arm64-v8a", "build", "--release"
    )
}


dependencies {
    implementation(libs.jna)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    implementation(libs.kotlinx.coroutines.core)
}
