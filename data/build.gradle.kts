import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.acerola.manga.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "MANGADEX_BASE_URL", "\"https://api.mangadex.org\"")
        buildConfigField("String", "MANGADEX_UPLOAD_URL", "\"https://uploads.mangadex.org\"")
        buildConfigField("String", "GITHUB_USER_AGENT", "\"github.com/Vinicius-Gabriel-P-Leitao/acerola\""
        )
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

room {
    schemaDirectory(path = "$projectDir/schema")
}

dependencies {
    implementation(project(":infrastructure"))

    // --- Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // --- Compose Runtime (Only for @Immutable) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    // --- DI ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- Database (Room) ---
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // --- Networking (Retrofit + OkHttp + GraphQL) ---
    ksp(libs.moshi.codegen)
    implementation(libs.apollo.runtime)
    implementation(libs.bundles.retrofit)
    implementation(libs.apollo.normalized.cache)

    // --- File & Utilities ---
    implementation(libs.junrar)
    implementation(libs.androidx.documentfile)

    // --- Quality code ---
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    // --- Testing ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
