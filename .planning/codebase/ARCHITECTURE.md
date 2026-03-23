# ARCHITECTURE.md — System Design & Patterns

## Architectural Pattern

**Clean Architecture + Port & Adapter (Hexagonal) + UDF (Unidirectional Data Flow)**

The codebase combines Clean Architecture's layer separation with the Port & Adapter pattern for data access, and UDF for all UI state management.

## Module Dependency Hierarchy

```
:app
  └── :ui          (Jetpack Compose UI, ViewModels, Screens)
  └── :core         (Use cases, WorkManager workers)
        └── :data   (Repositories, Room DB, Retrofit/Apollo, Services)
              └── :infra  (Cross-cutting: logging, errors, preferences, utils)
```

Strict one-way dependency: no module may depend on a module above it in the hierarchy.

**JVM targets:**
- `:app` → Java 21
- `:data`, `:ui`, `:infra`, `:core` → Java 17

## Layers

### 1. Entry Points — `:app`
- `AcerolaApplication` — Hilt entry point (`@HiltAndroidApp`), configures WorkManager with `HiltWorkerFactory`, and sets up Coil `ImageLoader` with SVG support.
- `MainActivity` — Main nav host; extends `BaseActivity`, defines the `NavHost` graph for Home/History/Search/Config destinations.
- `ReaderActivity` (`:ui`) — Separate activity for the manga reader, used for full-screen reading with no bottom bar.

### 2. UI Layer — `:ui`
- **Screens** are Composable functions declared as extensions of namespace objects (e.g., `Main.Home.Layout.Screen()`).
- **ViewModels** extend a base class exposing `uiState: StateFlow<UiState>` and a `Channel<UserMessage>` for one-off Snackbar events.
- **Actions** are sealed classes/interfaces per feature (e.g., `HomeAction`, `ReaderAction`) dispatched from Screen to ViewModel.
- **UiState** are data classes per feature in `module/<feature>/state/` (e.g., `ReaderUiState`).
- `BaseActivity` sets up `AcerolaTheme`, `NavHost`, `SnackbarHostState` via `CompositionLocalProvider`, and edge-to-edge display.

### 3. Use Case Layer — `:core`
- Single-responsibility use cases in `core/usecase/<domain>/` (e.g., `SyncLibraryUseCase`, `ObserveLibraryUseCase`).
- Workers: `LibrarySyncWorker`, `MetadataSyncWorker`, `ChapterDownloadWorker` — all Hilt-injected WorkManager workers.
- DI modules per domain: `DirectoryCaseModule`, `MangadexCaseModule`, `ComicInfoCaseModule`, `AnilistCaseModule`.

### 4. Data Layer — `:data`
- **Port & Adapter pattern**: gateway interfaces in `adapter/contract/gateway/`, implementations in `adapter/library/`, `adapter/history/`, `adapter/metadata/`.
- **Room DB** (`AcerolaDatabase`, v6): DAOs in `local/dao/`, entities in `local/entity/`.
- **Remote**: Retrofit for MangaDex REST API (`remote/mangadex/`), Apollo GraphQL for Anilist (`remote/anilist/`).
- **Services**: domain services for archive extraction, page caching, template matching, metadata export, cover saving (`service/`).
- **Translators**: mappers between layers in `local/translator/` (infra, persistence, remote, ui sub-packages).
- **DTOs**: data transfer objects in `dto/` for archive, metadata, history, and view data.

### 5. Infrastructure Layer — `:infra`
- `AcerolaLogger` — structured logging.
- Custom exceptions: `IntegrityException`, `MangadexRequestException`, `TechnicalException`.
- Error messages: sealed classes per domain (`LibrarySyncError`, `ChapterError`, `NetworkError`, `IoError`, etc.).
- DataStore preferences: `MangaDirectoryPreference`, `ThemePreference`, `ReadingModePreference`, `HomeLayoutPreference`, `MetadataPreference`, `ChapterPerPagePreference`.
- `SafeApiCall` — OkHttp/Retrofit call wrapper returning `Either`.
- Utilities: `DocumentFileHash`, `NormalizeChapterSort`, `NotificationHelper`.
- Regex patterns: `MangadexPattern`, `MediaFilePattern`.

## UDF Data Flow

```
User taps → Screen dispatches Action → ViewModel handles Action
  → calls UseCase / Repository → result returned as Either<Error, T>
  → ViewModel updates MutableStateFlow<UiState>
  → UI recomposes via collectAsState()
  → On error: ViewModel sends UserMessage to Channel → Snackbar displayed
```

## Error Handling

Arrow `Either<Error, Success>` is used throughout the data layer. Errors are typed sealed classes from `:infra`. ViewModels unwrap Either and emit success state or push `UserMessage` events via `Channel`.

```kotlin
// Pattern in ViewModel
viewModelScope.launch {
    when (val result = useCase.execute()) {
        is Either.Right -> _uiState.update { it.copy(data = result.value) }
        is Either.Left -> userMessageChannel.send(UserMessage(result.value.message))
    }
}
```

## Dependency Injection

Hilt is used throughout:
- `@HiltAndroidApp` on `AcerolaApplication`
- `@AndroidEntryPoint` on Activities
- `@HiltViewModel` on all ViewModels
- `@Module` / `@Provides` / `@Singleton` for repositories and services
- `@HiltWorker` on WorkManager workers

## Navigation

Jetpack Compose Navigation with string routes from `Destination` enum:
- `Destination.HOME`, `HISTORY`, `SEARCH`, `CONFIG` — within `MainActivity`
- `ReaderActivity` — separate activity launched for reading (avoids nav complexity for full-screen)

Transitions: scale + fade animations (300ms) on enter/exit/popEnter/popExit.
