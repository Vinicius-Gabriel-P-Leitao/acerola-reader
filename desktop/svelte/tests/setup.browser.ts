import { vi } from "vitest";

vi.mock("@tauri-apps/api/window", () => ({
  getCurrentWindow: vi.fn(() => ({
    theme: vi.fn().mockResolvedValue("light"),
    setTheme: vi.fn().mockResolvedValue(undefined),
    onThemeChanged: vi.fn().mockResolvedValue({ unlisten: vi.fn() }),
  })),
}));

vi.mock("@tauri-apps/plugin-store", () => ({
  LazyStore: vi.fn().mockImplementation(function () {
    return {
      get: vi.fn().mockResolvedValue(null),
      set: vi.fn().mockResolvedValue(undefined),
    };
  }),
}));
