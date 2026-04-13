import { render, screen } from "@testing-library/svelte";
import { describe, expect, it } from "vitest";
import AcerolaCard from "./acerola-card.svelte";

describe("AcerolaCard", () => {
  it("renderiza o título", () => {
    render(AcerolaCard, { title: "Berserk" });
    expect(screen.getByText("Berserk")).toBeInTheDocument();
  });

  it("renderiza a descrição quando passada", () => {
    render(AcerolaCard, { title: "Berserk", description: "Kentaro Miura" });
    expect(screen.getByText("Kentaro Miura")).toBeInTheDocument();
  });

  it("não renderiza a descrição quando não passada", () => {
    render(AcerolaCard, { title: "Berserk" });
    expect(screen.queryByText("Kentaro Miura")).not.toBeInTheDocument();
  });

  it("aplica o data-size correto no card", () => {
    const { container } = render(AcerolaCard, { title: "Berserk", size: "sm" });
    const card = container.querySelector("[data-slot='card']");
    expect(card).toHaveAttribute("data-size", "sm");
  });

  it("aplica class customizada", () => {
    const { container } = render(AcerolaCard, { title: "Berserk", class: "minha-classe" });
    const card = container.querySelector("[data-slot='card']");
    expect(card).toHaveClass("minha-classe");
  });
});
