# STRUCTURE.md — Directory Layout & Organization

## Root Layout

```
acerola/
├── app/                    # Entry point module
├── ui/                     # Presentation layer (Compose UI + ViewModels)
├── core/                   # Use case + WorkManager workers
├── data/                   # Repositories, Room, Retrofit, Apollo, Services
├── infra/                  # Cross-cutting: errors, logging, preferences, utils
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── mise.toml
├── CLAUDE.md
└── TODO.md
```

## Module: `:app`

```
app/src/main/java/br/acerola/manga/
├── AcerolaApplication.kt   # @HiltAndroidApp, WorkManager + Coil setup
└── MainActivity.kt         # Main nav host (Home/History/Search/Config)
```

## Module: `:ui`

```
ui/src/main/java/br/acerola/manga/
├── common/
│   ├── activity/
│   │   └── BaseActivity.kt          # Compose host + theme + snackbar + nav setup
│   ├── navigation/
│   │   └── Destination.kt           # Route enum
│   ├── ux/
│   │   ├── Acerola.kt               # Root namespace object
│   │   ├── component/               # Reusable UI components (Button, Card, Dialog, etc.)
│   │   ├── layout/                  # Structural layouts (Scaffold, TopBar, BottomBar, etc.)
│   │   ├── modifier/                # Custom Modifiers (Glass.kt)
│   │   └── theme/
│   │       ├── Theme.kt             # AcerolaTheme entry point
│   │       ├── color/               # Color palettes (Catppuccin, Dracula, Nord)
│   │       └── local/               # CompositionLocals (LocalSnackbarHostState)
│   └── viewmodel/
│       ├── archive/                 # FileSystemAccessViewModel
│       ├── library/archive/         # ChapterArchiveViewModel
│       ├── metadata/                # MetadataSettingsViewModel
│       └── theme/                   # ThemeViewModel
└── module/
    ├── main/                        # Main namespace (Home/History/Search/Config)
    │   ├── Main.kt                  # Namespace object definition
    │   ├── config/
    │   │   ├── layout/              # Config screen layouts
    │   │   ├── component/           # Config components
    │   │   └── state/               # ConfigUiState, ConfigAction
    │   ├── history/
    │   │   ├── layout/
    │   │   ├── component/
    │   │   └── state/               # HistoryUiState, HistoryAction
    │   ├── home/
    │   │   ├── layout/
    │   │   ├── component/
    │   │   └── state/               # HomeUiState, HomeAction
    │   └── search/
    │       ├── SearchScreen.kt
    │       └── state/               # SearchAction
    ├── manga/                       # Single manga detail
    │   ├── Manga.kt
    │   ├── layout/                  # ChapterSection, ConfigSection, Tabs, Header
    │   ├── component/               # ChapterItem, PaginationPreference, etc.
    │   └── state/                   # MangaUiState, MangaAction
    ├── reader/                      # Manga reader
    │   ├── Reader.kt
    │   ├── ReaderActivity.kt        # Separate Activity for full-screen
    │   ├── component/               # HorizontalPagedReader, VerticalPagedReader, WebtoonReader
    │   ├── gesture/                 # ZoomablePageImage
    │   ├── layout/                  # BottomControls, PageContent, TopBar, SettingsSheet
    │   └── state/                   # ReaderUiState, ReaderAction
    └── download/
        ├── Download.kt
        ├── component/
        └── state/                   # DownloadAction
```

## Module: `:core`

```
core/src/main/java/br/acerola/manga/core/
├── usecase/
│   ├── AnilistCaseModule.kt
│   ├── ComicInfoCaseModule.kt
│   ├── DirectoryCaseModule.kt
│   ├── MangadexCaseModule.kt
│   ├── chapter/                     # ObserveChaptersUseCase, GetChapterCountUseCase
│   ├── download/                    # DownloadChaptersUseCase
│   ├── history/                     # ObserveHistoryUseCase, TrackReadingProgressUseCase
│   ├── library/                     # SyncLibraryUseCase, RescanMangaUseCase, RescanMangaChaptersUseCase
│   ├── manga/                       # ObserveLibraryUseCase, ExtractCoverFromChapterUseCase
│   ├── metadata/                    # SyncMangaMetadataUseCase, ManageCategoriesUseCase
│   ├── search/                      # SearchMangaUseCase
│   └── template/                    # AddTemplateUseCase, RemoveTemplateUseCase, ObserveTemplatesUseCase
└── worker/
    ├── LibrarySyncWorker.kt
    ├── MetadataSyncWorker.kt
    ├── ChapterDownloadWorker.kt
    └── WorkManagerModule.kt
```

## Module: `:data`

```
data/src/main/java/br/acerola/manga/
├── adapter/
│   ├── contract/
│   │   ├── gateway/                 # Port interfaces (ChapterGateway, MangaGateway, etc.)
│   │   └── provider/                # Provider interfaces
│   ├── history/                     # History adapter implementations
│   ├── library/                     # MangaDirectoryEngine and related
│   └── metadata/
│       ├── anilist/engine|source/   # Anilist metadata engines
│       ├── comicinfo/engine|source/ # ComicInfo XML metadata engines
│       └── mangadex/engine|source/  # MangaDex metadata engines
├── dto/
│   ├── archive/                     # ChapterArchivePageDto, MangaDirectoryDto
│   ├── history/                     # History DTOs
│   ├── metadata/category|chapter|manga/ # Metadata DTOs
│   └── view/                        # View-specific DTOs
├── local/
│   ├── converter/                   # Room TypeConverters
│   ├── dao/archive|category|history|metadata|view/  # Room DAOs
│   ├── database/                    # AcerolaDatabase
│   ├── entity/archive|category|history|metadata|relation|view/  # Room entities
│   └── translator/infra|persistence|remote|ui/  # Layer mappers
├── remote/
│   ├── anilist/                     # AnilistClient (Apollo GraphQL)
│   └── mangadex/
│       ├── api/                     # Retrofit API interfaces
│       ├── dto/chapter|manga/       # MangaDex response DTOs
│       └── interceptor/             # OkHttp interceptors
└── service/
    ├── archive/                     # CoverSaver
    ├── cache/                       # PageCacheHandler
    ├── compact/                     # CBZ/CBR extraction
    ├── download/                    # Chapter download service
    ├── file/                        # File utilities
    ├── metadata/                    # MetadataExporter, ComicInfo parser
    ├── reader/contract|di|extract/  # Reader services, ChapterSourceFactory
    └── template/                    # ChapterNameProcessor, TemplateMatcher
```

## Module: `:infra`

```
infra/src/main/java/br/acerola/manga/
├── config/
│   ├── network/                     # SafeApiCall
│   ├── permission/                  # FileSystemAccessManager
│   └── preference/                  # DataStore preferences (Theme, ReadingMode, etc.)
├── error/
│   ├── exception/                   # IntegrityException, TechnicalException, MangadexRequestException
│   ├── message/                     # Error sealed classes (ChapterError, IoError, NetworkError, etc.)
│   └── UserMessage.kt
├── logging/
│   ├── AcerolaLogger.kt
│   ├── LogEvent.kt
│   └── LogLevel.kt
├── pattern/
│   ├── MangadexPattern.kt
│   └── MediaFilePattern.kt
├── type/
│   └── UiText.kt
└── util/
    ├── DocumentFileHash.kt
    ├── NormalizeChapterSort.kt
    └── NotificationHelper.kt
```

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| UI Namespace | `object Module { object Area { object Role } }` | `Main.Home.Layout` |
| Screen Composable | `fun Module.Area.Layout.Screen()` | `Main.Home.Layout.Screen()` |
| Component Composable | `fun Module.Area.Component.Name()` | `Main.Home.Component.MangaCard()` |
| ViewModel | `<Feature>ViewModel` | `HomeViewModel` |
| UiState | `<Feature>UiState` | `ReaderUiState` |
| Action | `<Feature>Action` | `HomeAction` |
| Use Case | `<Verb><Domain>UseCase` | `SyncLibraryUseCase` |
| Gateway interface | `<Domain>Gateway` | `ChapterGateway` |
| Engine/Adapter | `<Source><Domain>Engine` | `MangadexSourceMangaEngine` |
| DAO | `<Entity>Dao` | `ChapterArchiveDao` |
| Entity | `<EntityName>` | `MangaDirectory`, `ChapterArchive` |
| DTO | `<Entity>Dto` | `MangaDirectoryDto` |
| Worker | `<Task>Worker` | `LibrarySyncWorker` |

## Key File Locations

| Purpose | Path |
|---------|------|
| App entry point | `app/src/main/java/br/acerola/manga/AcerolaApplication.kt` |
| Main navigation | `app/src/main/java/br/acerola/manga/MainActivity.kt` |
| Reader activity | `ui/src/main/java/br/acerola/manga/module/reader/ReaderActivity.kt` |
| Theme system | `ui/src/main/java/br/acerola/manga/common/ux/theme/Theme.kt` |
| Room database | `data/src/main/java/br/acerola/manga/local/database/` |
| MangaDex API | `data/src/main/java/br/acerola/manga/remote/mangadex/api/` |
| Gateway interfaces | `data/src/main/java/br/acerola/manga/adapter/contract/gateway/` |
| Error definitions | `infra/src/main/java/br/acerola/manga/error/message/` |
| DataStore prefs | `infra/src/main/java/br/acerola/manga/config/preference/` |
