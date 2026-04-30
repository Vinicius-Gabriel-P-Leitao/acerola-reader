# Acerola Project Context: Volume Hierarchy Implementation

Este documento serve como o roteiro técnico e conjunto de regras para a implementação da hierarquia `Comic -> Volume -> Chapter`. Ele deve ser seguido estritamente após cada comando de `/clear`.

## 🛠️ Tasks (Order of Implementation)

### 1. Infra Layer: Numerical Normalization
- [ ] **Task 1.1**: Create `br.acerola.comic.util.SortNormalizer`.
    - [ ] Extract logic from `ChapterArchiveEngine` and `NormalizeChapterSort.kt`.
    - [ ] Implement `SortResult` data class (integerPart, decimalPart, isSpecial, type).
    - [ ] Add support for Volume prefixes (`Vol`, `V`, `Volume`).
    - [ ] **Validation**: Unit tests for all normalization cases (0.1, 0.10, Oneshot, etc).

### 2. Data Layer: Persistence & Schema
- [ ] **Task 2.1**: Create `volume_archive` Entity.
    - [ ] Fields: `id`, `comic_directory_fk` (FK CASCADE), `name`, `path`, `volume_sort`, `is_special`.
    - [ ] Unique Constraint: `(comic_directory_fk, volume_sort)`.
- [ ] **Task 2.2**: Update `chapter_archive` Entity.
    - [ ] Add `volume_id_fk: Long?` (FK CASCADE).
    - [ ] Add `is_special: Boolean`.
    - [ ] **Strict Constraint**: Maintain `UNIQUE(comic_directory_fk, chapter)` to protect metadata integrity.
- [ ] **Task 2.3**: Database Migration.
    - [ ] Implement migration script to create the new table and update existing chapter records.
    - [ ] **Validation**: Run database tests to ensure no data loss.

### 3. Data Layer: Scanning Engine
- [ ] **Task 3.1**: Hierarchical Scanning.
    - [ ] Update `scanRecursive` in `ComicDirectoryEngine` to detect Volume subfolders.
    - [ ] Implement Root Volume logic: chapters in the comic root folder get `volume_id_fk = null`.
- [ ] **Task 3.2**: Conflict Resolution.
    - [ ] Implement strict duplicate detection: if a chapter number exists in two volumes, log error via `AcerolaLogger` and skip.
- [ ] **Task 3.3**: Metadata Propagation.
    - [ ] Ensure `is_special` from Volume is inherited by Chapters during scan.

### 4. Data/Core Layer: Breaking Contract Changes
- [ ] **Task 4.1**: DTO Updates.
    - [ ] Update `ChapterDto`, `ChapterFileDto`, and `ChapterArchivePageDto` with volume fields.
- [ ] **Task 4.2**: Mapper Refactoring.
    - [ ] Update all persistence and UI mappers in `ArchivePersistenceMapper.kt` and related files.
    - [ ] **Validation**: Fix all compilation errors caused by DTO changes.

### 5. Data Layer: DAO & Hierarchical SQL
- [ ] **Task 5.1**: SQL Order By Logic.
    - [ ] Refactor `ChapterArchiveDao` queries with `LEFT JOIN volume_archive`.
    - [ ] Implement 6-level sorting: `Vol.is_special -> Vol.Sort -> Ch.is_special -> Ch.Sort`.

### 6. Core/UI Layer: ViewModel & UseCases
- [ ] **Task 6.1**: Business Logic Update.
    - [ ] Update UseCases/Workers to handle volume data.
    - [ ] Remove redundant in-memory sorting from `ComicViewModel`.
- [ ] **Task 6.2**: State Mapping.
    - [ ] Update `ComicUiState` to handle volume headers visibility.

### 7. UI Layer: Compose Implementation
- [ ] **Task 7.1**: Volume Header Component.
    - [ ] Create Material3 compliant header for volumes.
- [ ] **Task 7.2**: StickyHeaders.
    - [ ] Implement `stickyHeader` in `LazyColumn` for volume grouping.
    - [ ] **Validation**: Visual check for single-volume vs multi-volume scenarios.

---

## 📜 Strict Rules

1.  **Architecture Pipeline**: Follow `infra -> data -> core -> ui`. **NEVER** allow `data` to depend on `ui`.
2.  **Language**: All code, comments, and documentation must be in **English**. No Portuguese.
3.  **Imports**: No ambiguous or wildcard imports. Clean up unused imports.
4.  **Functional Pipeline**: Always use `arrow-kt` (`Either`, `flatMap`, etc.) for logic flow.
5.  **String Management**: 
    - Use `strings.xml` for all user-facing text.
    - Services/Engines must not know about `R.string.**`. Centralize errors and pass error objects.
6.  **ViewModel Pattern**: `Service/Engine -> UseCase or Worker -> ViewModel`. ViewModels **MUST NOT** use Services/Engines directly.
7.  **Theming**: Strictly follow **Material 3** and existing project theme patterns. No arbitrary styling.
8.  **Logging**: Use `br.acerola.comic.logging.AcerolaLogger` exclusively.
9.  **Dependency Injection**: Use standard **Hilt** patterns. No arbitrary names.
10. **Build & Safety**:
    - No modifying `build.gradle.kts` without explicit approval.
    - No adding libraries without permission.
    - **MUST** pass all quality checks:
        - `./gradlew ktlintFormat`
        - `./gradlew ktlintCheck`
        - `./gradlew testDebugUnitTest`
        - `./gradlew connectedAndroidTest`
11. **Workflow**:
    - Do not skip tasks.
    - Document any feature-level changes discovered during a task (e.g., "new DB field needed").
    - If unsure about code, **STOP** and ask.

---

## 📂 Reference Logs
- **Project Structure Root**: `C:\Users\vinicius\Desktop\acerola-android\acerola`
- **Logger**: `@infra/src/main/java/br/acerola/comic/logging/AcerolaLogger.kt`
