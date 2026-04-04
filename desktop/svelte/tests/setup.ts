import "@testing-library/jest-dom/vitest";
import { vi, afterEach } from "vitest";

// Mock SvelteKit $app/environment — browser = true para testes de componente
vi.mock("$app/environment", () => ({
  browser:  true,
  dev:      true,
  building: false,
  version:  "test",
}));

// Mock Tauri store — comportamento padrão: retorna null (sem valor salvo)
vi.mock("@tauri-apps/plugin-store", () => ({
  LazyStore: vi.fn().mockImplementation(() => ({
    get: vi.fn().mockResolvedValue(null),
    set: vi.fn().mockResolvedValue(undefined),
  })),
}));

afterEach(() => {
  vi.clearAllMocks();
});
