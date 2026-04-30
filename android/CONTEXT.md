# Acerola Project Context: Volume Hierarchy Implementation

## Status Snapshot (2026-04-30)

This document reflects the current codebase status for the `Comic -> Volume -> Chapter` hierarchy.

## Completed

### Infra
- Sort normalization exists and is already used for chapter and volume parsing.
- Volume-aware parsing and matching infrastructure is present.

### Data
- Persistence already supports `volume_archive`.
- Chapters already link to parent volumes through `volume_id_fk`.
- Hierarchical scanning is already implemented in the local archive engine.
- Chapter DTOs and page DTOs already carry parent volume data.
- UI mappers already expose joined volume information.

### SQL and Ordering
- Chapter queries already use `LEFT JOIN volume_archive`.
- Hierarchical ordering is already delegated to SQL for the default chapter order.

### UI
- The comic module already had initial volume-aware chapter rendering.
- A `VolumeHeader` component already existed and has now been completed.

## Implemented In The Latest Pass

### ViewModel
- `ComicViewModel` no longer decides header visibility from a naive “has any volume” rule.
- Header visibility is now based on:
  - chapters having volume ids
  - more than one distinct non-null volume
  - no root/orphan chapters mixed into the same list

### Compose
- `ComicScreen` now trusts `chapterDto.showVolumeHeaders` instead of recomputing visibility locally.
- `ChapterSection` now injects `stickyHeader` blocks when `volumeId` changes between adjacent ordered items.
- Root fallback headers were removed from the grouped rendering path.

### Volume Header
- `VolumeHeader` now shows:
  - volume name
  - chapter count
  - special indicator for special volumes

## Current UI Rules
- Flat works do not show headers.
- Single-volume works do not show headers.
- Mixed root-plus-volume lists do not show headers.
- Multi-volume ordered lists show sticky headers on volume transitions.

## Remaining Follow-up
- The DAO still applies Kotlin-side reordering for non-default sort modes such as `LAST_UPDATE` and descending display. That is acceptable for now, but it is not a full SQL-only sorting pipeline for every mode.
- String resources and some older files still contain legacy encoding artifacts unrelated to the volume feature.

## Guardrails
- Keep the architecture flow `infra -> data -> core -> ui`.
- Do not reintroduce in-memory hierarchy grouping that fights SQL ordering.
- Keep user-facing text in resources.
- Prefer Material 3 patterns in Compose.
