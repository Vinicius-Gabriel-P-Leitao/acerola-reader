import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "br.acerola.comic.native"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += "arm64-v8a"
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
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
}

val buildRust = tasks.register<Exec>("buildRust") {
    workingDir = file("rust")
    environment("CARGO_NDK_PLATFORM", "26")
    commandLine(
        "cargo", "ndk",
        "--target", "aarch64-linux-android",
        "--output-dir", "../src/main/jniLibs",
        "build", "--release"
    )
}

tasks.named("preBuild") {
    dependsOn(buildRust)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}