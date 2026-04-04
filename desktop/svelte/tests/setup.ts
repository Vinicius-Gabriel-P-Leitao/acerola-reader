import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";

// Mock para resolver problemas do jsdom e pointer events do radix/bits-ui
if (typeof window !== "undefined") {
  window.HTMLElement.prototype.hasPointerCapture = vi.fn();
  window.HTMLElement.prototype.releasePointerCapture = vi.fn();
  // @ts-ignore: Mock simples para testes que resolve o hasPointerCapture
  window.PointerEvent = class PointerEvent extends Event {};
}

// Mock SvelteKit $app/environment — browser = true para testes de componente
vi.mock("$app/environment", async () => await import("./mocks/app-environment.ts"));
vi.mock("$app/state", async () => await import("./mocks/app-state.ts"));

// Mock Tauri store — comportamento padrão: retorna null (sem valor salvo)
vi.mock("@tauri-apps/plugin-store", async () => {
  const { mockLazyStore } = await import("./mocks/tauri.ts");
  return {
    LazyStore: mockLazyStore(),
  };
});

afterEach(() => {
  vi.clearAllMocks();
});
