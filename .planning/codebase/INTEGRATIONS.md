# External Integrations

**Analysis Date:** 2026-03-23

## APIs & External Services

**MangaDex (Manga Metadata & Downloads):**
- Service: REST API for manga metadata, chapters, author/artist info, covers
  - Endpoints: `https://api.mangadex.org` (metadata), `https://uploads.mangadex.org` (cover/file downloads)
  - SDK/Client: Retrofit 3.0.0
  - Auth: GitHub user agent injection via `MangadexInterceptor`
  - BuildConfig: `MANGADEX_BASE_URL`, `MANGADEX_UPLOAD_URL`, `GITHUB_USER_AGENT`
  - Implementation: `br.acerola.manga.remote.mangadex` package in `:data` module
  - Key clients: `MangadexMangaMetadataClient`, `MangadexChapterMetadataClient`, `MangadexMangaDownloadClient`
  - DTOs: Moshi-generated from response types (manga, chapter, entities)
  - Mapper: `MangadexRemoteMapper` translates API DTOs to local Room entities

**AniList (Manga Search & Supplemental Metadata):**
- Service: GraphQL API for manga search, titles, genres, staff, status, scores
  - Endpoint: `https://graphql.anilist.co`
  - SDK/Client: Apollo Client 4.1.0
  - Auth: None (public queries)
  - BuildConfig: `ANILIST_BASE_URL`
  - Implementation: `br.acerola.manga.remote.anilist` package in `:data` module
  - Module: `AnilistModule` (Hilt-managed singleton ApolloClient)
  - GraphQL Queries: `MediaSearch.graphql`, `MediaDetails.graphql`
  - Code generation: Apollo codegen produces Kotlin types (Java 17 target in `:data`)
  - Mapper: `AnilistRemoteMapper` transforms GraphQL responses to Room entities

## Data Storage

**Databases:**
- **Local SQLite (Room):**
  - Provider: Bundled SQLite 2.6.2 via androidx.sqlite
  - Client: Room 2.8.4 ORM
  - Location: Auto-managed by Android (internal app database directory)
  - Tables: manga_directory, manga_metadata, chapter_archive, chapter_metadata, author, genre, cover, banner, reading_history, chapter_read, mangadex_source, anilist_source, comicinfo_source, category, manga_category
  - Schema versioning: Version 2 (exported to `schema/` in `:data` module)
  - Connection: Automatic via Room, no manual JDBC
  - DAOs: 14 data access objects covering all entities and relationships

**File Storage:**
- **Local Filesystem:** Manga archive files (`.cbz`, `.cbr`) stored in user-selected directories via DocumentFile API
  - Accessed via: `androidx.documentfile.documentfile` 1.1.0
  - Pattern: Scoped file access (SAF) for Android 11+ compliance
  - Extraction: junrar 7.5.7 reads CBR/CBZ archives

**Caching:**
- **Apollo Normalized Cache (SQLite):** Client-side GraphQL query caching
  - Package: `apollo-normalized-cache-sqlite`
  - Used for: AniList query results (MediaSearch, MediaDetails)
  - Reduces redundant network calls for frequently accessed metadata

## Authentication & Identity

**Auth Provider:**
- Custom (None) - Application operates without user authentication
  - Public APIs only (MangaDex, AniList don't require user login)
  - User-Agent-based identification: `github.com/Vinicius-Gabriel-P-Leitao/acerola` sent to MangaDex

**File Access Permissions:**
- Manifest permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
- Runtime: Scoped File Access (DocumentFile) for manga directory access
- No user credentials stored locally

## Monitoring & Observability

**Error Tracking:**
- None detected - Application uses in-app error handling only
  - Error responses mapped to custom exception types (`MangadexRequestException`, `IntegrityException`, `TechnicalException`)
  - Errors surfaced to UI via `UserMessage` objects through ViewModel channels

**Logs:**
- OkHttp Logging Interceptor: HTTP request/response logging in debug builds
- Custom Logger: `AcerolaLogger` in `:infra` for application-level logging
- Console output only (no remote logging service)

## Background Jobs & Sync

**WorkManager:**
- Framework: androidx.work 2.10.0 + Hilt WorkManager 1.2.0 integration
- Workers: `ChapterDownloadWorker`, `LibrarySyncWorker`, `MetadataSyncWorker` in `:core` module
- Configuration: Hilt-managed via `HiltWorkerFactory` in `AcerolaApplication`
- Services: Foreground service for data sync (type: dataSync)
- Manifest: `androidx.work.impl.foreground.SystemForegroundService` configured

## CI/CD & Deployment

**Hosting:**
- Native Android app (APK/AAB)
- No cloud hosting (local-only execution)

**CI Pipeline:**
- None detected - Manual build via Gradle
- Build commands: `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew installDebug`

**Release Build:**
- ProGuard obfuscation + resource shrinking enabled
- Signing: Debug keystore (development only - should be replaced for production)

## Environment Configuration

**Required Env Vars:**
- None - All configuration via BuildConfig fields (compile-time constants):
  - `MANGADEX_BASE_URL` = `https://api.mangadex.org`
  - `MANGADEX_UPLOAD_URL` = `https://uploads.mangadex.org`
  - `ANILIST_BASE_URL` = `https://graphql.anilist.co`
  - `GITHUB_USER_AGENT` = `github.com/Vinicius-Gabriel-P-Leitao/acerola`

**Secrets Location:**
- No secrets file - Application has no authentication tokens or API keys
- All endpoints public (no authorization required)

**Runtime Preferences:**
- DataStore preferences stored in app-specific directory:
  - `chapter_per_page_prefs`
  - `home_layout_prefs`
  - `manga_directory_prefs`
  - `metadata_prefs`
  - `theme_prefs`
  - `reading_mode_prefs`

## Webhooks & Callbacks

**Incoming:**
- None detected - Application is read-only from external services

**Outgoing:**
- None detected - Application performs queries only (no mutations to external systems)

## Network Interceptors & Middleware

**MangaDex Interceptor:**
- Location: `br.acerola.manga.remote.mangadex.interceptor.MangadexInterceptor`
- Adds User-Agent header: `github.com/Vinicius-Gabriel-P-Leitao/acerola`
- Handles request/response logging via OkHttp LoggingInterceptor
- Timeout configuration: 30 seconds (connect, write, read)

**OkHttp Configuration:**
- Main API client: Managed OkHttpClient with MangadexInterceptor
- Download API client: Separate Retrofit instance (no interceptor) for file downloads
- Dependency injection: Hilt qualifiers (`@MainApi`, `@DownloadApi`)

## Serialization & Data Mapping

**JSON Serialization (Retrofit/MangaDex):**
- Framework: Moshi 1.15.2
- Code generation: Moshi Kotlin Codegen (KSP-based)
- DTOs: Auto-generated from response structures
- Location: `br.acerola.manga.remote.mangadex.dto` package

**GraphQL Serialization (AniList):**
- Framework: Apollo Client built-in serialization
- Code generation: Apollo codegen for Kotlin
- Location: `br.acerola.manga.remote.anilist` package
- Schema: `src/main/graphql/anilist/schema.graphqls`

**Data Mapping:**
- Mappers: `MangadexRemoteMapper`, `AnilistRemoteMapper` translate remote models to Room entities
- Pattern: DTO → Entity conversion before storage
- Location: `br.acerola.manga.local.translator.remote` package

---

*Integration audit: 2026-03-23*
