# Technology Stack

**Analysis Date:** 2026-03-23

## Languages

**Primary:**
- Kotlin 2.2.21 - All application code (Android, logic, networking)

**Secondary:**
- XML - Android manifests, layouts (Material 3), themes, resource definitions

## Runtime

**Environment:**
- Android Runtime (minimum API 26, target API 36)
- Kotlin Multiplatform Support (coroutines, stdlib)

**Build System:**
- Gradle 8.13.1 (AGP)
- Kotlin Gradle Plugin 2.2.21

**JVM Target Versions:**
- `:app` module → Java 21
- `:ui`, `:core`, `:data`, `:infra` modules → Java 17

## Framework & Architecture

**Android Framework:**
- AndroidX Core 1.17.0
- AndroidX Lifecycle (Runtime KTX 2.10.0, ViewModel Compose)
- WorkManager 2.10.0 + Hilt WorkManager 1.2.0

**UI Framework:**
- Jetpack Compose 2025.12.01 (BOM-managed)
  - Material 3 1.4.0 (required - Material Design 3 only, no Material 2)
  - Material Icons Extended
  - Compose Navigation 2.9.6
  - Activity Compose 1.12.2
  - Hilt Navigation Compose 1.3.0
  - Adaptive Navigation Suite 1.4.0
  - Compose Runtime for StateFlow/immutability annotations

**Dependency Injection:**
- Hilt 2.57.2 - Singleton components across all modules
  - Code generation via KSP (Kotlin Symbol Processing)
  - `@HiltAndroidApp` on `AcerolaApplication`
  - `@HiltViewModel` on all ViewModels

## Database

**ORM & Persistence:**
- Room 2.8.4 (Type-safe SQL abstraction)
  - Schema export enabled: `schema/` directory
  - SQLite bundled 2.6.2 (native SQLite with Kotlin extensions)
  - Room KTX (flow-based DAO patterns)
  - Room Compiler (annotation processing via KSP)

**Location:**
- `br.acerola.manga.local.database.AcerolaDatabase` in `:data` module (`data/src/main/java`)
- Multiple DAOs: ChapterArchiveDao, MangaDirectoryDao, MangaMetadataDao, etc.
- Entities: MangaDirectory, MangaMetadata, ChapterArchive, ChapterMetadata, Author, Genre, Cover, Banner, ReadingHistory

## Networking

**HTTP Client:**
- Retrofit 3.0.0 - REST API abstraction
- OkHttp 5.3.2 - HTTP transport layer
  - Logging Interceptor (request/response debugging)
  - Custom Interceptors (MangaDex user agent injection)
- Moshi 1.15.2 - JSON serialization
  - Moshi Kotlin Codegen for data classes

**GraphQL:**
- Apollo Client 4.1.0 - GraphQL queries/mutations
  - Apollo Normalized Cache SQLite - client-side caching
  - Integrated in `:data` module for AniList integration

**API Configuration:**
- MangaDex API: `https://api.mangadex.org` (base) + `https://uploads.mangadex.org` (downloads)
- AniList GraphQL: `https://graphql.anilist.co`
- Configured via BuildConfig fields in data module

## Image Loading

**Framework:**
- Coil 2.7.0 - Image loading for Compose
  - SVG decoder via `coil-svg` extension
  - Configured in `AcerolaApplication` as `ImageLoaderFactory`
  - Integrated with Material 3 Compose components

## Async & Concurrency

**Coroutines:**
- kotlinx-coroutines-android 1.10.2 - Main dispatcher integration
- kotlinx-coroutines-test 1.10.2 - Testing utilities

**Error Handling & FP:**
- Arrow 2.2.1.1 - Functional programming utilities
  - arrow-core: Either type for Result modeling
  - arrow-fx-coroutines: Structured concurrency with Either

## File I/O & Document Access

**Archive Extraction:**
- junrar 7.5.7 - Reads `.cbz`/`.cbr` manga archives

**Document Access:**
- androidx.documentfile 1.1.0 - Scoped file access (Android 11+)

## User Preferences & Config

**DataStore:**
- androidx.datastore 1.2.0 - Type-safe preferences persistence
  - Used for: Theme, reading mode, chapter per-page, metadata sync preferences
  - Located in `:infra` module (`infra/src/main/java/br/acerola/manga/config/preference/`)

## Testing

**Unit Testing:**
- JUnit 4.13.2
- Mockk 1.14.7 - Kotlin-native mocking
- Turbine 1.2.1 - Flow/StateFlow testing utilities
- Truth 1.4.4 - Assertion library (Google)
- MockWebServer 5.0.0-alpha.14 - Local HTTP mock server for Retrofit testing

**Instrumentation Testing (Device/Emulator):**
- androidx.test.ext:junit 1.2.1
- androidx.espresso.core 3.7.0 - UI interaction testing
- androidx.compose.ui:ui-test-junit4 1.10.0
- Robolectric 4.14.1 - JVM Android testing

**Test Setup:**
- All modules have `testOptions.unitTests.isReturnDefaultValues = true`
- Instrumentation runner: `androidx.test.runner.AndroidJUnitRunner`

## Build Optimization

**Proguard/R8:**
- ProGuard enabled in release builds
- Resource shrinking enabled in release
- Consumer ProGuard rules in all library modules

**Code Generation:**
- KSP (Kotlin Symbol Processing) for Hilt, Room, Moshi
- Apollo code generation for GraphQL types

## Module Dependencies

Strict layered dependency hierarchy enforced:

```
:app → :ui, :data, :infra
:ui → :core, :infra
:core → :data, :infra
:data → :infra
:infra → (no dependencies)
```

**Module Purposes:**
- `:infra` - Cross-cutting concerns (exceptions, logging, DataStore, permissions)
- `:data` - Business logic (Room DB, Retrofit/Apollo clients, repositories, use cases)
- `:core` - Use case orchestration and WorkManager background jobs
- `:ui` - Compose UI, ViewModels, navigation (Material 3 only)
- `:app` - Entry point, Hilt setup, application-level config

## Platform Requirements

**Development:**
- Android SDK 36 (compileSdk)
- Kotlin 2.2.21
- Gradle 8.13.1
- Java 17+ (recommended) or Java 21 for :app

**Runtime:**
- Minimum: Android 8.0 (API 26)
- Target: Android 15 (API 36)
- Notifications support (API 33+)
- Foreground services for data sync tasks

---

*Stack analysis: 2026-03-23*
