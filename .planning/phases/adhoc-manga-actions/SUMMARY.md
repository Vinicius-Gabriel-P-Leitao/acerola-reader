# Phase adhoc Plan manga-actions: Ações do botão ... de cada mangá

**One-liner:** ModalBottomSheet com hide/delete/bookmark para cards de mangá, com persistência via Room (hidden column, cascade delete) e Arrow Either para error handling.

---

## Objective

Implement hide, delete, and bookmark actions for manga cards on the Home screen, accessible via the `...` (MoreHoriz) button on each card.

---

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Entity: add hidden field + bump DB version | 0c2e039 | MangaDirectory.kt, AcerolaDatabase.kt, MangaDirectoryDao.kt |
| 2 | DAO: filter hidden + setHidden query | 89ccf72 | MangaDirectoryDao.kt |
| 3 | Gateway: MangaLibraryWriteGateway interface | 824c24c | MangaGateway.kt |
| 4 | Engine: implement MangaLibraryWriteGateway | e2be674 | MangaDirectoryEngine.kt, DirectoryModule.kt |
| 5 | Use cases: HideMangaUseCase + DeleteMangaUseCase | e1708d4 | HideMangaUseCase.kt, DeleteMangaUseCase.kt |
| 6 | ViewModel: inject use cases + add functions | 0ecc7c5 | MangaDirectoryViewModel.kt |
| 7 | Strings: action labels + dialog strings | 98f192d | strings.xml |
| 8 | UI: MangaActionsSheet + update items | 30819b8 | MangaActionsSheet.kt, MangaGridItem.kt, MangaListItem.kt |
| 9 | Connect in HomeScreen | 93b086a | HomeViewModel.kt, HomeScreen.kt |
| fix | Build error fixes | 11652e9 | ViewModels, UseCase annotations, Sheet |

---

## Decisions Made

1. **HomeViewModel owns hide/delete/bookmark actions** — rather than injecting `MangaDirectoryViewModel` into `HomeScreen`, the `HomeViewModel` (which already had `manageCategoriesUseCase`) received the new use cases. This follows the principle of "one ViewModel per screen".

2. **DocumentFile.fromSingleUri for FS deletion** — avoids DocumentsContract.deleteDocument which requires specific URI format; `fromSingleUri` handles both tree and document URIs gracefully.

3. **No explicit @Provides for use cases** — `HideMangaUseCase` and `DeleteMangaUseCase` use `@Inject constructor`, so Hilt provides them automatically (same pattern as `ExtractCoverFromChapterUseCase`). No changes to `DirectoryCaseModule` needed.

4. **ModalBottomSheet without rememberModalBottomSheetState** — the project's Material3 version does not include `skipPartialExpansion` parameter; the sheet is used without explicit state management (same pattern as `SettingsSheet`).

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Preserve hidden flag in upsertMangaDirectoryTransaction**
- **Found during:** Task 1 — entity + DAO review
- **Issue:** `upsertMangaDirectoryTransaction` only preserved `externalSyncEnabled` when upserting. A rescan would reset `hidden=true` back to `hidden=false`.
- **Fix:** Added `hidden = existing.hidden` to the `copy()` call in the upsert transaction.
- **Files modified:** `MangaDirectoryDao.kt`
- **Commit:** 0c2e039

**2. [Rule 3 - Build Error] Arrow `onLeft` import not needed**
- **Found during:** Task 9 compilation
- **Issue:** `import arrow.core.onLeft` is not a valid top-level import in this Arrow version; `onLeft` is called as a member extension without explicit import (pattern from `ReaderViewModel`).
- **Fix:** Removed the explicit import from both ViewModels.
- **Commit:** 11652e9

**3. [Rule 3 - Build Error] `rememberModalBottomSheetState` lacks `skipPartialExpansion`**
- **Found during:** Task 8 compilation
- **Issue:** The project's Material3 version does not expose `skipPartialExpansion` parameter on `rememberModalBottomSheetState`.
- **Fix:** Removed `rememberModalBottomSheetState` usage; `ModalBottomSheet` is used without explicit state (same as `SettingsSheet`).
- **Commit:** 11652e9

**4. [Rule 2 - Missing Qualifier] Use `@param:DirectoryEngine`**
- **Found during:** Task 5 compilation
- **Issue:** Kotlin warning about annotation target; existing codebase uses `@param:` for constructor inject qualifiers.
- **Fix:** Changed `@DirectoryEngine` to `@param:DirectoryEngine` in both new use cases.
- **Commit:** 11652e9

---

## Known Stubs

None — all actions are fully wired:
- Hide: calls `directoryDao.setHidden(mangaId, true)` → manga disappears from `getAllMangaDirectory` query (filters `hidden=0`)
- Delete: deletes folder via `DocumentFile.fromSingleUri` then removes entity from DB with cascade
- Bookmark: calls `categoryService.updateMangaCategory(directoryId, categoryId)`

---

## Files Created

- `core/src/main/java/br/acerola/manga/core/usecase/manga/HideMangaUseCase.kt`
- `core/src/main/java/br/acerola/manga/core/usecase/manga/DeleteMangaUseCase.kt`
- `ui/src/main/java/br/acerola/manga/module/main/common/component/MangaActionsSheet.kt`

## Files Modified

- `data/src/main/java/br/acerola/manga/local/entity/archive/MangaDirectory.kt`
- `data/src/main/java/br/acerola/manga/local/database/AcerolaDatabase.kt` (version 2 → 3)
- `data/src/main/java/br/acerola/manga/local/dao/archive/MangaDirectoryDao.kt`
- `data/src/main/java/br/acerola/manga/adapter/contract/gateway/MangaGateway.kt`
- `data/src/main/java/br/acerola/manga/adapter/library/MangaDirectoryEngine.kt`
- `data/src/main/java/br/acerola/manga/adapter/library/DirectoryModule.kt`
- `ui/src/main/java/br/acerola/manga/common/viewmodel/library/archive/MangaDirectoryViewModel.kt`
- `ui/src/main/java/br/acerola/manga/module/main/home/HomeViewModel.kt`
- `ui/src/main/java/br/acerola/manga/module/main/home/HomeScreen.kt`
- `ui/src/main/java/br/acerola/manga/module/main/home/component/MangaGridItem.kt`
- `ui/src/main/java/br/acerola/manga/module/main/common/component/MangaListItem.kt`
- `ui/src/main/res/values/strings.xml`

---

## Metrics

- **Completed:** 2026-03-23T23:57:11Z
- **Tasks:** 9 + 1 fix commit
- **Files created:** 3
- **Files modified:** 12
