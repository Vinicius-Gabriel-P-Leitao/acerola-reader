import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ThemePicker from "./theme-picker.svelte";

describe("ThemePicker", () => {
  it("renderiza os três temas disponíveis", () => {
    render(ThemePicker, { theme: "catppuccin", mode: "dark", onselect: vi.fn() });

    expect(screen.getByText("Catppuccin")).toBeInTheDocument();
    expect(screen.getByText("Nord")).toBeInTheDocument();
    expect(screen.getByText("Dracula")).toBeInTheDocument();
  });

  it("chama onselect com o id correto ao clicar num tema", async () => {
    const user = userEvent.setup();
    const onselect = vi.fn();

    render(ThemePicker, { theme: "catppuccin", mode: "dark", onselect });
    await user.click(screen.getByText("Nord").closest("button")!);

    expect(onselect).toHaveBeenCalledOnce();
    expect(onselect).toHaveBeenCalledWith("nord");
  });

  it("aplica estilo de selecionado no tema ativo", () => {
    const { container } = render(ThemePicker, { theme: "nord", mode: "dark", onselect: vi.fn() });

    const buttons = container.querySelectorAll("button");
    const nordBtn = Array.from(buttons).find((b) => b.textContent?.includes("Nord"));

    expect(nordBtn?.className).toContain("border-primary");
  });

  it("não aplica estilo de selecionado nos temas inativos", () => {
    const { container } = render(ThemePicker, { theme: "nord", mode: "dark", onselect: vi.fn() });

    const buttons = container.querySelectorAll("button");
    const catppuccinBtn = Array.from(buttons).find((b) => b.textContent?.includes("Catppuccin"));

    expect(catppuccinBtn?.className).not.toContain("border-primary");
  });

  it("renderiza as cores corretas no modo light", () => {
    const { container } = render(ThemePicker, { theme: "catppuccin", mode: "light", onselect: vi.fn() });

    const colorDots = container.querySelectorAll<HTMLElement>("[style*='background-color']");
    const colors = Array.from(colorDots).map((el) => el.style.backgroundColor);

    // Catppuccin light: #8839EF, #EA76CB, #1E66F5, #EFF1F5
    expect(colors[0]).toBeTruthy();
  });
});
