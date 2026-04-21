import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
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
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    implementation(libs.jna)
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)
}


tasks.withType<Test> {
    systemProperty("java.library.path", "${project.projectDir}/src/main/jniLibs/arm64-v8a")
    systemProperty("jna.library.path", "${project.projectDir}/src/main/jniLibs/arm64-v8a")

    testLogging {
        events("passed", "skipped", "failed")
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("p2p") }
        exclude { it.file.path.contains("generated") }
    }
}