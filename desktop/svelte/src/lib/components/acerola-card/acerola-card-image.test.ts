import { render, screen } from "@testing-library/svelte";
import { describe, expect, it } from "vitest";
import AcerolaCardImage from "./acerola-card-image.svelte";

describe("AcerolaCardImage", () => {
  it("renderiza o título", () => {
    render(AcerolaCardImage, { title: "Berserk" });
    expect(screen.getByText("Berserk")).toBeInTheDocument();
  });

  it("renderiza a imagem quando cover é passado", () => {
    render(AcerolaCardImage, { title: "Berserk", cover: "/capas/berserk.jpg" });
    const img = screen.getByRole("img", { name: "Berserk" });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute("src", "/capas/berserk.jpg");
  });

  it("renderiza fallback quando cover é null", () => {
    render(AcerolaCardImage, { title: "Berserk", cover: null });
    expect(screen.queryByRole("img", { name: "Berserk" })).not.toBeInTheDocument();
  });

  it("renderiza a barra de progresso quando progress é passado", () => {
    const { container } = render(AcerolaCardImage, { title: "Berserk", progress: 50 });
    const bar = container.querySelector(".bg-primary");
    expect(bar).toBeInTheDocument();
    expect(bar).toHaveStyle("width: 50%");
  });

  it("não renderiza a barra de progresso quando progress não é passado", () => {
    const { container } = render(AcerolaCardImage, { title: "Berserk" });
    const bar = container.querySelector(".h-1.bg-surface");
    expect(bar).not.toBeInTheDocument();
  });

  it("clipa o progresso entre 0 e 100", () => {
    const { container } = render(AcerolaCardImage, { title: "Berserk", progress: 150 });
    const bar = container.querySelector(".bg-primary");
    expect(bar).toHaveStyle("width: 100%");
  });

  it("aplica w-36 por default", () => {
    const { container } = render(AcerolaCardImage, { title: "Berserk" });
    expect(container.firstChild).toHaveClass("w-36");
  });
});
