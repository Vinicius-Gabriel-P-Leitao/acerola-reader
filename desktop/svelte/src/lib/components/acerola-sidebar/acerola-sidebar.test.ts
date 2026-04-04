import { render, screen } from "@testing-library/svelte";
import { describe, expect, it, vi } from "vitest";
import LibraryIcon from "@lucide/svelte/icons/library";
import SettingsIcon from "@lucide/svelte/icons/settings";
import AcerolasSidebar from "./acerola-sidebar.svelte";

vi.mock("$app/state", () => ({
  page: { url: new URL("http://localhost/home") },
}));

vi.mock("$lib/components/ui/sidebar/context.svelte.ts", () => ({
  useSidebar: () => ({
    isMobile: false,
    open: true,
    openMobile: false,
    state: "expanded",
    setOpen: vi.fn(),
    setOpenMobile: vi.fn(),
    toggle: vi.fn(),
    handleShortcutKeydown: vi.fn(),
  }),
  setSidebar: vi.fn(),
}));

const items = [
  { href: "/home", label: "Biblioteca", icon: LibraryIcon },
  { href: "/config", label: "Configurações", icon: SettingsIcon },
];

describe("AcerolasSidebar", () => {
  it("renderiza o logo Acerola", () => {
    render(AcerolasSidebar, { items });
    expect(screen.getByText("Acerola")).toBeInTheDocument();
  });

  it("renderiza todos os itens de navegação", () => {
    render(AcerolasSidebar, { items });
    expect(screen.getByText("Biblioteca")).toBeInTheDocument();
    expect(screen.getByText("Configurações")).toBeInTheDocument();
  });

  it("renderiza links com href correto", () => {
    render(AcerolasSidebar, { items });
    expect(screen.getByRole("link", { name: /Biblioteca/i })).toHaveAttribute("href", "/home");
    expect(screen.getByRole("link", { name: /Configurações/i })).toHaveAttribute("href", "/config");
  });

  it("não renderiza o footer quando não é passado", () => {
    const { container } = render(AcerolasSidebar, { items });
    expect(container).toBeInTheDocument();
  });
});
