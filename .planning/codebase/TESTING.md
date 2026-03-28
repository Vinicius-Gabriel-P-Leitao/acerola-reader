# TESTING.md — Test Structure & Practices

## Framework & Libraries

| Tool | Purpose |
|------|---------|
| JUnit 4 (`@Test`, `@Before`, `@After`) | Test runner for all unit and integration tests |
| MockK (`io.mockk`) | Mocking framework — `@MockK`, `coEvery`, `coVerify`, `mockkStatic`, `mockkObject` |
| Google Truth | Assertion library (some tests use `org.junit.Assert` as well) |
| Kotlin Coroutines Test | `runTest`, `StandardTestDispatcher`, `Dispatchers.setMain/resetMain` |
| Room in-memory DB | Integration tests for DAOs (`Room.inMemoryDatabaseBuilder`) |
| AndroidX Test (`ApplicationProvider`, `@RunWith(AndroidJUnit4::class)`) | Instrumented tests |
| Compose UI Test | `createComposeRule()` for Composable component tests |

## Test Types & Locations

### Unit Tests (JVM)
- **`:data/src/test/`** — Repository adapters, mappers, services, engines
- **`:core/src/test/`** — Use cases
- **`:infra/src/test/`** — Pattern matching, template utilities
- **`:ui/src/test/`** — ViewModels

### Instrumented Tests (Device/Emulator)
- **`:data/src/androidTest/`** — DAO tests (Room in-memory), ComicInfo XML parser
- **`:ui/src/androidTest/`** — Compose UI component and screen tests

## Test Naming Convention

Tests are named in Portuguese using snake_case describing behavior:

```kotlin
@Test
fun getChaptersByMangaDirectory_deve_ordenar_capitulos_numericamente_e_decimais()

@Test
fun sync_deve_retornar_erro_quando_diretorio_nao_encontrado()
```

Pattern: `<method>_deve_<behavior>_quando_<condition>`

## Structure Pattern — Arrange / Act / Assert

All tests follow AAA with Portuguese comments:

```kotlin
@Test
fun example() = runTest {
    // Arrange
    coEvery { dao.findById(1L) } returns MangaDirectoryFixtures.createMangaDirectory()

    // Act
    val result = engine.load(1L)

    // Assert
    assertTrue(result.isRight())
}
```

## Fixtures

Fixture objects in `data/src/test/java/br/acerola/manga/fixtures/` provide factory functions with sensible defaults:

- `MangaDirectoryFixtures` — `createMangaDirectory()`, `createMangaDirectoryDto()`
- `LookupFixtures` — MangaDex lookup stubs
- `MetadataFixtures` — Metadata entity stubs

All fixture parameters have defaults, so tests only override what is relevant:

```kotlin
MangaDirectoryFixtures.createMangaDirectory(name = "One Piece")
```

## ViewModel Testing Pattern

ViewModels use `StandardTestDispatcher` with `Dispatchers.setMain`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

## Integration Tests — DAOs

DAO tests use Room in-memory database with `allowMainThreadQueries()`:

```kotlin
@RunWith(AndroidJUnit4::class)
@SmallTest
class ChapterArchiveDaoTest {
    private lateinit var db: AcerolaDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AcerolaDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }
}
```

Locations:
- `data/src/androidTest/java/br/acerola/manga/local/database/dao/archive/ChapterArchiveDaoTest.kt`
- `data/src/androidTest/java/br/acerola/manga/local/database/dao/metadata/MangaMetadataDaoTest.kt`
- `data/src/androidTest/java/br/acerola/manga/local/database/dao/metadata/author/AuthorDaoTest.kt`

## Compose UI Tests

Compose tests use `createComposeRule()` and test rendered output + interactions:

```kotlin
@get:Rule val composeTestRule = createComposeRule()

@Test
fun component_deve_exibir_titulo() {
    composeTestRule.setContent { MyComponent(title = "Hello") }
    composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
}
```

Locations: `ui/src/androidTest/java/br/acerola/manga/`
- Common UX components: `common/ux/component/`, `common/ux/layout/`
- Module screens: `module/main/`, `module/manga/`, `module/reader/`

## Async Testing

Coroutine-based code is tested with `runTest` and `StandardTestDispatcher`:

```kotlin
@Test
fun example() = runTest {
    coEvery { repository.fetch() } returns Either.Right(data)
    val result = useCase.execute()
    assertEquals(Either.Right(data), result)
}
```

## Commands

```bash
./gradlew test                           # All unit tests
./gradlew :data:test                     # Single module
./gradlew :data:test --tests "*ClassName" # Single class
./gradlew connectedDebugAndroidTest      # Instrumented (device required)
```

## Coverage Gaps

- No stress tests for rapid reader page navigation
- No concurrency tests for parallel library syncs
- No archive corruption tests (truncated `.cbz`)
- Network timeout / retry scenarios not covered
- Room DB migration tests (`v5 → v6`) missing
- CBR multi-part RAR edge cases not tested
