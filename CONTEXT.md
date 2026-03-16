# Project Context: Acerola (Manga Reader)

## Purpose
Acerola is an Android application designed for manga enthusiasts to manage their local library of manga files (`.cbz`, `.cbr`). It provides automated scanning, metadata enrichment (via MangaDex), and a modern reading experience.

## Modules & Responsibilities
- **`:app`**: Application entry point, Hilt dependency injection setup, and navigation graph.
- **`:presentation`**: UI layer using Jetpack Compose, stateless components, and Screens managing UDF.
- **`:data`**: Data layer implementation including Room database, Retrofit/Apollo for remote APIs (MangaDex), Repository implementations, and Use Cases.
- **`:infrastructure`**: Common utilities, network configurations, error handling, and shared types.

## Key Dependencies & Versions
- **Kotlin**: `2.2.21`
- **Compose BOM**: `2025.12.01`
- **Material 3**: `1.4.0`
- **Room**: `2.8.4`
- **Hilt**: `2.57.2`
- **Retrofit**: `3.0.0`
- **Coil**: `2.7.0`
- **Coroutines**: `1.10.2`
- **Arrow**: `2.2.1.1` 

## Architecture
The project follows **Clean Architecture** principles with a strict **Unidirectional Data Flow (UDF)** in the presentation layer:
- **Separation of Concerns**: Clear split between UI (`:presentation`), Business Logic/Data (`:data`), and Utilities (`:infrastructure`).
- **Unidirectional Data Flow (UDF)**:
    - **Screens**: Centralize `ViewModel` instantiation via `hiltViewModel()`, collect all state flows, and manage UI event collection (Snackbars).
    - **State Management**: UI state is consolidated into `UiState` data classes located within `module/<feature>/state/`.
    - **Action Dispatching**: User interactions are modeled as sealed interfaces (`Action`) and dispatched from Screens to ViewModels.
    - **Stateless UI**: Components and Layouts are stateless, receiving only the required data and action callbacks.
- **Dependency Injection**: Managed by Hilt across all modules.
- **Reactive Programming**: Extensive use of Kotlin Coroutines and Flow for asynchronous data streams and event propagation.

## Technical Constraints
- **Min SDK**: `26` (Android 8.0 Oreo)
- **Target/Compile SDK**: `36`
- **Java/JVM Version**: `21`
- **Compose Support**: Using modern Compose versions allowing for advanced effects like `Modifier.blur()` (available since Android 12+, but requiring fallbacks for lower versions).
