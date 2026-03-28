# CONCERNS.md — Technical Debt & Issues

## Tech Debt

- **Hard-coded language for MangaDex API** — Portuguese (`pt-br`) hard-coded; should be user-configurable preference.
- **Incomplete error handling in SnackbarHostState provider** — Missing fallback when host is not present in composition.
- **Missing error translation for unsupported chapter formats** — Unsupported `.cbr` variants silently fail with generic error.
- **`trySend()` on buffered channel** — `ReaderViewModel.kt:247` uses `trySend()` which silently drops events under backpressure; could lose history update events.

## Known Bugs

- **Resource leak in CBR/CBZ extractors** — `InputStream` not properly closed in `finally` blocks when exceptions occur during copy operations.
- **Unsafe null dereference (`!!`)** — `ComicInfo` source uses `!!` on nullable fields without guards; crash risk on malformed XML.
- **Unvalidated content URIs** — File operations on user-supplied URIs without validation before access.

## Security Concerns

- Unvalidated content URIs passed directly to file I/O — potential path traversal if URIs are crafted.
- `!!` operator on external data (ComicInfo XML) without sanitization.

## Performance Issues

- **File system traversal without caching** — Library scan re-traverses full directory tree on every sync; no incremental diff.
- **Template detection re-evaluated on every scan** — Expensive regex/pattern matching not cached between scans.
- **Large monolithic Composable functions (300+ lines)** — Causes full recomposition on any state change; should be split into smaller Composables with stable keys.
- **Progress updates without progressive feedback** — Batch updates to UI without intermediate progress states.
- **Entire chapters loaded into bitmap cache** — No eviction or size limit observed; memory pressure on low-end devices.
- **Unbounded temp file accumulation** — Extracted pages not cleaned up reliably after reading session.

## Fragile Areas

- **Channel-based event delivery without backpressure** — `Channel(BUFFERED)` used for UI events; under load events can be dropped.
- **Non-atomic concurrent archive access** — Simultaneous reads of the same `.cbz`/`.cbr` from multiple coroutines not guarded.
- **Unprotected StateFlow updates in async blocks** — Concurrent updates to `MutableStateFlow` from multiple coroutines without synchronization.
- **WorkManager sync deduplication missing** — Multiple sync requests can enqueue simultaneously; no `ExistingWorkPolicy.KEEP` or similar guard everywhere.

## Scaling Limits

- **Database loads entire library into memory** — `getAll()` style queries return full lists; will degrade with large libraries (1000+ manga).
- **No pagination on library queries** — Room queries lack `LIMIT`/`OFFSET` or `Pager` integration.
- **Entire chapter bitmap cache** — No LRU size cap on Coil cache for reader; accumulates across chapters.

## Test Coverage Gaps

- No stress tests for rapid page navigation in reader.
- Missing concurrency tests for simultaneous library syncs.
- No archive corruption handling tests (truncated `.cbz`).
- Network timeout and retry scenarios not covered.
- Database migration paths (`v5 → v6`) not tested.
- No tests for CBR extraction edge cases (multi-part RAR).
