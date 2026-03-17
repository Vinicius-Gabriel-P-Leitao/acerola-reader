# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Acerola is a native Android app (Kotlin + Jetpack Compose) for managing and reading local manga files (`.cbz`/`.cbr`). It scans local library directories, enriches metadata via MangaDex API, and provides a reading interface.

## Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug

# Tests
./gradlew test                              # All unit tests
./gradlew :data:test                        # Single module
./gradlew :data:test --tests "*ClassName"   # Single test class
./gradlew connectedDebugAndroidTest         # Instrumented tests (device required)

# Clean
./gradlew clean build
```

No linting/formatting tools are configured (no Detekt or KtLint).

## Module Architecture

Four Gradle modules with a strict dependency hierarchy:

```
:app → :presentation → :data → :infrastructure
```

- **`:infrastructure`** — Cross-cutting concerns: custom exceptions, error messages, `AcerolaLogger`, OkHttp interceptors, DataStore preferences, permission utilities, regex patterns.
- **`:data`** — All business logic and data access: Room DB, Retrofit (MangaDex REST), Apollo (GraphQL), repository adapters, use cases, DTOs/mappers, background WorkManager services.
- **`:presentation`** — Jetpack Compose UI: Screens, ViewModels, UiState classes, navigation, theming (Catppuccin, Dracula, Alucard, Nord).
- **`:app`** — Entry point: `AcerolaApplication` (Hilt, WorkManager, Coil setup), `MainActivity` (navigation host), DI modules.

## Key Architectural Patterns

**UDF (Unidirectional Data Flow):** User interaction → `Screen` dispatches `Action` → `ViewModel` calls repository/use case → emits `UiState` (StateFlow) → UI recomposes.

**Port & Adapter (Repositories):** Interfaces live in `:data/repository/port/`, implementations in `:data/repository/adapter/local/` and `:data/repository/adapter/remote/`.

**Error Handling:** Arrow `Either<Error, Success>` throughout the data layer. UI errors surface as `UserMessage` via a `Channel` in ViewModels, displayed as Snackbars.

**State per Screen:** Each feature has a `*UiState` data class in `presentation/module/<feature>/state/`. All ViewModels extend a base that exposes `uiState: StateFlow<UiState>`.

**DI:** Hilt everywhere — `@HiltViewModel` for ViewModels, `@HiltAndroidApp` on Application, `@Module`/`@Provides` for repositories and services.

## Room Database (v6)

Tables: `manga_directory`, `manga_remote_info`, `chapter_archive`, `chapter_remote_info`, `chapter_download_source`, `author`, `genre`, `cover`, `reading_history`, `chapter_read`.

Remote APIs: `https://api.mangadex.org` (REST via Retrofit/Moshi) and Apollo GraphQL client.

## JVM Targets

- `:app` → Java 21
- `:data`, `:presentation`, `:infrastructure` → Java 17
