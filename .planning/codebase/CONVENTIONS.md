# Coding Conventions

**Analysis Date:** 2026-03-23

## Naming Patterns

### Files

- **ViewModels:** `[Feature]ViewModel.kt` (e.g., `HomeViewModel.kt`, `ReaderViewModel.kt`)
- **Screens/Layouts:** `[Feature]Screen.kt` or `[Feature]Layout.kt` (e.g., `HomeScreen.kt`, `MangasDirectoryAccess.kt`)
- **Components:** `[Component]Component.kt` (e.g., `SearchBar.kt`, `ChapterItem.kt`)
- **Test files:** `[Class]Test.kt` (e.g., `ArchiveMapperTest.kt`, `HomeViewModelTest.kt`)
- **Fixtures:** `[Entity]Fixtures.kt` (e.g., `MangaDirectoryFixtures.kt`, `MangaFixtures.kt`)
- **State classes:** `[Feature]UiState.kt` (e.g., `HomeUiState.kt`, `ReaderUiState.kt`)
- **Action classes:** `[Feature]Action.kt` (e.g., `HomeAction.kt`, `ReaderAction.kt`)
- **Data Access Objects:** `[Entity]Dao.kt` (e.g., `ReadingHistoryDao.kt`, `ChapterArchiveDao.kt`)
- **Entities:** `[Entity].kt` (e.g., `ChapterArchive.kt`, `MangaDirectory.kt`)
- **Repositories/Engines:** `[Source][Entity]Engine.kt` (e.g., `MangadexSourceMangaEngine.kt`, `LocalHistoryEngine.kt`)

### Functions and Methods

- **camelCase:** All functions use camelCase (e.g., `observeLibrary()`, `openChapter()`, `upsertHistory()`)
- **Use case functions:** `invoke()` method with operator syntax (e.g., `useCase()` calls `invoke()`)
- **Private state mutations:** Prefix with underscore (e.g., `_uiState`, `_uiEvents`, `_selectedHomeLayout`)
- **Event channels:** Named `uiEvents` (public), `_uiEvents` (private) for user-facing messages
- **Suspend functions:** Names describe the operation (e.g., `openChapter()`, `markChapterAsRead()`)
- **Extension functions:** Named descriptively by action (e.g., `toDto()`, `toModel()`, `toPageDto()`)

### Variables and Properties

- **StateFlow private backing field:** `private val _fieldName = MutableStateFlow(...)` → `val fieldName: StateFlow<T> = _fieldName.asStateFlow()`
- **Flow/Channel private backing field:** `private val _channelName = Channel<T>()` → `val channelName: Flow<T> = _channelName.receiveAsFlow()`
- **Mutable state in ViewModels:** Use `MutableStateFlow` with private backing field
- **Immutable exposure:** Always expose via `StateFlow<T>` or `Flow<T>` (never mutable)
- **Local variables:** camelCase (e.g., `val result`, `var isLoading`, `val historyMap`)
- **Constants:** Defined in companion objects or at file level, UPPER_SNAKE_CASE (rarely used; mostly rely on Hilt DI)
- **Test property setup:** Use `lateinit var` for lazy initialization (e.g., `private lateinit var repository: LocalHistoryEngine`)

### Types

- **Data classes:** `[Entity]Dto.kt` suffix for DTOs (e.g., `MangaDirectoryDto`, `ChapterFileDto`)
- **Entity classes:** Room entities without suffix (e.g., `MangaDirectory`, `ChapterArchive`)
- **State classes:** `[Feature]UiState` (e.g., `ReaderUiState`)
- **Action/Event classes:** `[Feature]Action` (e.g., `HomeAction`)
- **Interfaces:** Descriptive name, often `[Entity]Gateway` or `[Entity]Repository` (e.g., `MangaGateway<T>`, `HistoryGateway`)
- **Error types:** `[Domain]Error` sealed class hierarchy (e.g., `ChapterError`, `SyncError`)

## Code Style

### Formatting

- **Language:** Kotlin (Java 17 for `:data`, `:presentation`, `:infra`; Java 21 for `:app`)
- **Indentation:** 4 spaces (enforced by Kotlin conventions, no explicit linter)
- **Line length:** No hard limit enforced; style guide suggests readability
- **Braces:** Allman style not used; opening brace on same line (Kotlin idiom)
- **Blank lines:** Logical grouping (one blank line between logical sections)

### Linting

- **No Detekt or KtLint configured:** No automated linting rules. Relies on IDE inspections and code review.
- **Android Lint:** `android.lint.abortOnError=false` and `android.lint.checkReleaseBuilds=false` in `gradle.properties`

### Import Organization

**Order (enforced by Kotlin convention):**
1. Android platform imports (`android.*`)
2. Androidx imports (`androidx.*`)
3. Compose imports (`androidx.compose.*`)
4. Kotlin/kotlinx imports (`kotlin.*`, `kotlinx.*`)
5. Third-party libraries (`arrow.*`, `io.mockk.*`, `com.google.common.*`, etc.)
6. Local application imports (`br.acerola.manga.*`)

**Path Aliases:**

Imports follow full package paths; no path aliases are configured. Example:

```kotlin
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
```

**Wildcard Imports:**

- **Prohibited:** NEVER use wildcard imports (e.g., `import br.acerola.manga.common.ux.component.*`)
- **Reason:** RULES.md explicitly forbids them to maintain traceability and prevent conflicts
- **Enforce:** Import each class individually

## Error Handling

**Framework:** Arrow `Either<Error, Success>` throughout the data layer

**Patterns:**

1. **Repository/Use case returns:**
   ```kotlin
   Either<Error, Success>
   ```

2. **Checking results:**
   ```kotlin
   result.isRight()  // Successful
   result.isLeft()   // Error
   result.onRight { success -> /* handle */ }
   result.onLeft { error -> /* handle */ }
   ```

3. **UI error surfacing:**
   - ViewModels catch `Either.Left` errors
   - Convert to `UserMessage` (has `uiMessage: UiText` for localization)
   - Emit via `Channel<UserMessage>` with `uiEvents: Flow<UserMessage>`
   - Snackbar displayed in UI

4. **Error types:** Sealed class hierarchies
   - `ChapterError` (sealed)
     - `ChapterError.InvalidChapterData`
     - `ChapterError.FileNotFound`
   - `SyncError` (sealed)
   - Custom domain errors per feature

5. **Logging errors:**
   ```kotlin
   AcerolaLogger.e(TAG, "Error message", LogSource.VIEWMODEL, exception)
   ```

## Logging

**Framework:** `AcerolaLogger` singleton (custom wrapper in `:infra`)

**Patterns:**

```kotlin
// Debug
AcerolaLogger.d(TAG, "Message", LogSource.VIEWMODEL)

// Info
AcerolaLogger.i(TAG, "Message", LogSource.VIEWMODEL)

// Error
AcerolaLogger.e(TAG, "Message", LogSource.VIEWMODEL, throwable)
```

**When to log:**
- State changes in ViewModels
- Lifecycle events (chapter opened, sync started)
- Errors and exceptions
- Network requests (optional, per module)

**LogSource enum:** `VIEWMODEL`, `REPOSITORY`, `USECASE`, `SERVICE`, `DATABASE`

## Comments

### When to Comment

- **Complex logic:** Explain the "why" not the "what" — code should be self-documenting
- **Non-obvious workarounds:** If there's a hack or temporary solution, explain why it's needed
- **Business rules:** If business logic is not obvious from code, add a comment
- **Performance-critical sections:** Explain optimizations or constraints

### JSDoc/KDoc

**Used sparingly** in data layer for public APIs and gateway interfaces:

```kotlin
/**
 * Fixtures reutilizáveis para testes de Mangá.
 * Siga o padrão AAA e use descrições em PT-BR nos testes.
 */
object MangaFixtures { ... }
```

**Not required** for:
- Private functions in non-library code
- Simple getters/setters
- Test functions with descriptive names

## Function Design

### Size

- **Guideline:** Keep functions small and focused (single responsibility)
- **Maximum lines:** No hard limit, but prefer breaking at ~50 lines
- **Readability:** If a function needs scrolling in IDE to understand, consider refactoring

### Parameters

- **Default values:** Used liberally (e.g., test fixtures with sensible defaults)
  ```kotlin
  fun createMangaDirectory(
      id: Long = 1L,
      name: String = "Manga Test",
      cover: String? = null
  ) = MangaDirectory(...)
  ```
- **Named arguments:** Encouraged in tests and function calls for clarity
- **Destructuring:** Used in lambdas (e.g., `{ (mId, cId, idx) -> ... }`)

### Return Values

- **Either for business logic:** All repository/use case functions return `Either<Error, Success>`
- **StateFlow for UI state:** ViewModels expose state via `StateFlow<UiState>`
- **Flow for events:** Single events via `Flow<UserMessage>` or `Channel`
- **Suspend functions:** Return the actual value (no Either wrapper) and throw for errors, unless used in a repository
- **Extension functions:** Return the transformed value (e.g., `.toModel()` returns the model instance)

## Module Design

### Exports

- **ViewModel:** Injected via `@HiltViewModel`, exposed to Compose via `hiltViewModel<T>()`
- **Gateway/Repository interfaces:** Defined in `:data` contract layer, implemented with adapter pattern
- **Use cases:** Defined and implemented in `:core` or `:data`, injected into ViewModels
- **Entities:** Room `@Entity` classes live in `:data`, DTOs for transfer

### Barrel Files (Module objects)

**Not used for exports.** Instead, UI uses explicit namespace objects for component grouping:

```kotlin
object Main {
    object Common { object Component }
    object Config { object Component; object Layout }
    object Home { object Component; object Layout }
}

// Usage:
@Composable
fun Main.Home.Layout.Screen() { ... }
```

**Not barrel files in the sense of `index.ts`** — no file-level exports that re-export multiple modules.

## UI Composition (Jetpack Compose)

### State Hoisting

- **Rule:** State lives in ViewModel, passed down as immutable values + callbacks
- **Screen composables:** Receive `ViewModel` via `hiltViewModel<T>()`
- **Child composables:** Receive only data and lambdas (no ViewModel access)
- **Local UI state:** Small UI-only state (e.g., keyboard visibility) can use `remember`

### Namespace Pattern

All composables placed within namespace hierarchy (RULES.md requirement):

```kotlin
@Composable
fun Main.Home.Layout.Screen(viewModel: HomeViewModel = hiltViewModel()) { ... }

@Composable
fun Main.Home.Component.MangaCard(manga: MangaDto, onClick: (Long) -> Unit) { ... }

@Composable
fun Acerola.Component.Button(text: String, onClick: () -> Unit) { ... }
```

### Material 3 Usage

- **All components:** Must derive from `androidx.compose.material3`
- **Colors:** Use `MaterialTheme.colorScheme` (not raw colors)
- **Typography:** Use `MaterialTheme.typography` (e.g., `headlineMedium`, `bodySmall`)
- **Shapes:** Use `MaterialTheme.shapes`
- **No deprecated APIs:** All composable functions must be current (check `@Deprecated` annotations)

### Extension Functions in UI

Used to transform ViewModels data into UI-ready state:

```kotlin
fun String.toTitleCase(): String = this.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun Main.Home.Component.Header(title: String) {
    Text(
        text = title.toTitleCase(),
        style = MaterialTheme.typography.headlineMedium
    )
}
```

## Testing Conventions (See TESTING.md)

- Test method naming: Backtick-quoted descriptive names in Portuguese
- Test structure: Arrange-Act-Assert (AAA) comments
- Mocking: `mockk` for all mocks and stubs
- Fixtures: Centralized in `*Fixtures.kt` objects with sensible defaults
- Assertions: `Truth.assertThat()` for complex assertions, `org.junit.Assert` for simple ones

---

*Convention analysis: 2026-03-23*
