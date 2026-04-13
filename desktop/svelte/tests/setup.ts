import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";

// Mock para resolver problemas do jsdom e pointer events do radix/bits-ui
if (typeof window !== "undefined") {
  window.HTMLElement.prototype.hasPointerCapture = vi.fn();
  window.HTMLElement.prototype.releasePointerCapture = vi.fn();
  // @ts-ignore: Mock simples para testes que resolve o hasPointerCapture
  window.PointerEvent = class PointerEvent extends Event {};
  
  // Mock localStorage
  Object.defineProperty(window, 'localStorage', {
    value: {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    },
    writable: true,
  });

  // Mock matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(), // Deprecated
      removeListener: vi.fn(), // Deprecated
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

// Mock SvelteKit $app/environment — browser = true para testes de componente
vi.mock("$app/environment", async () => await import("./mocks/app-environment.ts"));
vi.mock("$app/state", async () => await import("./mocks/app-state.ts"));

// Mock Tauri window — getCurrentWindow não existe em jsdom
vi.mock("@tauri-apps/api/window", () => ({
  getCurrentWindow: vi.fn(() => ({
    theme: vi.fn().mockResolvedValue("light"),
    setTheme: vi.fn().mockResolvedValue(undefined),
    onThemeChanged: vi.fn().mockResolvedValue({ unlisten: vi.fn() }),
  })),
}));

// Mock Tauri store — comportamento padrão: retorna null (sem valor salvo)
vi.mock("@tauri-apps/plugin-store", () => {
  return {
    LazyStore: class {
      constructor() {
        return {
          get: vi.fn().mockResolvedValue(null),
          set: vi.fn().mockResolvedValue(undefined),
        };
      }
    },
  };
});

afterEach(() => {
  vi.clearAllMocks();
});
