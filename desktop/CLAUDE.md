# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Acerola** is a Portuguese-language desktop application for reading and managing local comic book libraries (CBZ and CBR formats). Built with **Tauri 2** — a Rust backend exposing IPC commands to a **SvelteKit/Svelte 5** frontend.

## Commands

All commands are run from the repo root unless noted.

### Development
```bash
npm run dev              # Compile i18n + start Tauri dev (Vite on :1420)
npm run storybook        # Launch Storybook on :6006
```

### Build
```bash
npm run build            # Compile i18n + build frontend
npm run tauri build      # Full Tauri release build (Rust + frontend)
```

### Type Checking
```bash
npm run check            # svelte-kit sync + svelte-check
npm run check:watch      # Continuous type checking
```

### Testing
```bash
npm run test             # All JS/Svelte tests (vitest run)
npm run test:unit        # Unit tests only (jsdom, no browser)
npm run test:storybook   # Storybook component tests (headless Chromium)
npm run test:tauri       # Rust backend tests (cargo test)
npm run test:all         # JS + Rust tests
npm run test:coverage    # All tests with v8 coverage report
```

To run a **single test file**:
```bash
npx vitest run svelte/src/path/to/component.test.ts --project=unit
```

### i18n
```bash
npm run paraglide:compile  # Must run before dev/build if messages changed
```

## Architecture

```
Frontend (TypeScript/Svelte 5)
       ↓  Tauri invoke() / IPC
Tauri Commands  (src-tauri/src/commands/)
       ↓
Services / Engines  (src-tauri/src/core/services/)
       ↓
Repositories  (src-tauri/src/data/repositories/)
       ↓
SQLite  (tauri-plugin-sql, auto-migrated at startup)
```

### Frontend (`svelte/src/`)
- **routes/** — SvelteKit pages: `home/`, `history/`, `config/`
- **lib/components/** — Custom "acerola-*" UI components (built on shadcn-svelte / bits-ui)
- **lib/contracts/** — TypeScript interfaces shared across the frontend
- **lib/state/** — Svelte stores / app state
- **lib/paraglide/** — Auto-generated i18n output (do not edit manually)
- **theme/** — Tailwind CSS variables and base styles

### Backend (`src-tauri/src/`)
- **commands/features/** — Tauri `#[command]` handlers organized by feature (`library/`, `home/`)
- **commands/events/** — Event payload structs (JSON responses sent to frontend)
- **core/services/** — Business logic engines: `comic_scanner_engine`, `chapter_scanner_engine`, `comic_summary_engine`
- **data/models/** — Domain models: `archive/` (comic/chapter), `views/` (serializable read models)
- **data/repositories/** — SQLite data access layer
- **infra/** — DB connection setup, logging
- **migrations/** — SQLite migrations split into `models/`, `views/`, `seeds/` — auto-applied at app startup

### Key Design Notes
- **IPC pattern**: Frontend calls `invoke("command_name", args)` → Rust `#[command]` → service → repository → DB.
- **Paraglide i18n**: Translation files live in `svelte/messages/`. Compiled output is generated into `svelte/src/lib/paraglide/` — never edit that directory manually. Re-run `npm run paraglide:compile` after changing messages.
- **shadcn-svelte**: Components are generated into `svelte/src/lib/components/ui/` via `components.json`. Custom Acerola components wrap them under `lib/components/acerola-*/`.
- **Test mocks**: `$app/state`, `$app/environment`, Tauri `invoke`, `localStorage`, and `matchMedia` are all mocked in `svelte/tests/`. Paraglide is intentionally excluded from tests.
- **Database**: SQLite stored at `{app_data_dir}/acerola.db`. Migrations run on startup via tauri-plugin-sql. Test code uses `tempfile` for isolated SQLite instances.
- **Tool versions**: Managed by `mise.toml` — Node 25, Rust 1.94.1.
