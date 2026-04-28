import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.acerola.comic.rust"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    packaging {
        jniLibs {
            pickFirsts.add("**/libjnidispatch.so")
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)
    implementation("net.java.dev.jna:jna:5.12.0@aar")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("p2p") }
        exclude { it.file.path.contains("generated") }
    }
}

tasks.withType<Test> {
    systemProperty("java.library.path", "${project.projectDir}/src/main/jniLibs/arm64-v8a")
    systemProperty("jna.library.path", "${project.projectDir}/src/main/jniLibs/arm64-v8a")

    testLogging {
        events("passed", "skipped", "failed")
    }
}

val cargo: String = run {
    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { localProps.load(it) }
    }

    project.findProperty("cargo.dir") as? String
        ?: System.getenv("CARGO")
        ?: localProps.getProperty("cargo.dir")
        ?: "cargo"
}

tasks.register<Exec>("buildRust") {
    workingDir = file("rust")

    commandLine(
        cargo,
        "ndk",
        "-t",
        "aarch64-linux-android",
        "build",
        "--release",
    )
}

tasks.register<Exec>("generateBindings") {
    workingDir = file("rust")

    val soPath = file("rust/target/aarch64-linux-android/release/libacerola.so").absolutePath

    commandLine(
        cargo,
        "run",
        "--bin",
        "uniffi-bindgen",
        "--",
        "generate",
        "--config",
        "uniffi.toml",
        "--library",
        soPath,
        "--language",
        "kotlin",
        "--out-dir",
        file("src/main/java/br/acerola/comic").absolutePath,
    )

    dependsOn("buildRust")
}

tasks.register<Copy>("copyRustLib") {
    from("rust/target/aarch64-linux-android/release/libacerola.so")
    into("src/main/jniLibs/arm64-v8a")

    dependsOn("buildRust")
}

tasks.named("preBuild") {
    dependsOn("generateBindings", "copyRustLib")
}
