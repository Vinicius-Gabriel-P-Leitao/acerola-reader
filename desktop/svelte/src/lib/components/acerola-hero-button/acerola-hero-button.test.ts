import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AcerolaHeroButton from "./acerola-hero-button.svelte";

describe("AcerolaHeroButton", () => {
  it("renderiza o titulo e descricao corretamente", () => {
    render(AcerolaHeroButton, {
      title: "Pasta de Teste",
      description: "Minha descrição de teste",
    });

    expect(screen.getByText("Pasta de Teste")).toBeInTheDocument();
    expect(screen.getByText("Minha descrição de teste")).toBeInTheDocument();
  });

  it("aplica a classe cursor-pointer e hover state se onclick for passado", async () => {
    const handleClick = vi.fn();
    const { container } = render(AcerolaHeroButton, {
      title: "Item clicável",
      onclick: handleClick,
    });

    const wrapper = container.firstElementChild as HTMLElement;
    expect(wrapper).toHaveClass("cursor-pointer");
    expect(wrapper).toHaveClass("hover:border-primary/50");

    const user = userEvent.setup();
    await user.click(wrapper);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("não aplica cursor-pointer se não for clicável", () => {
    const { container } = render(AcerolaHeroButton, {
      title: "Apenas leitura",
    });

    const wrapper = container.firstElementChild as HTMLElement;
    expect(wrapper).not.toHaveClass("cursor-pointer");
    expect(wrapper).not.toHaveClass("hover:border-primary/50");
  });
});
