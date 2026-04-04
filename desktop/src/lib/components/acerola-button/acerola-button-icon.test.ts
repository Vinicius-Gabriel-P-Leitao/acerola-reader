import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AcerolaButtonIcon from "./acerola-button-icon.svelte";

describe("AcerolaButtonIcon", () => {
  it("renderiza como botão", () => {
    render(AcerolaButtonIcon);
    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  it("tem dimensões quadradas via classe", () => {
    render(AcerolaButtonIcon);
    const button = screen.getByRole("button");
    expect(button.className).toContain("w-10");
    expect(button.className).toContain("h-10");
  });

  it("chama onclick ao ser clicado", async () => {
    const user = userEvent.setup();
    const onclick = vi.fn();

    render(AcerolaButtonIcon, { props: { onclick } });
    await user.click(screen.getByRole("button"));

    expect(onclick).toHaveBeenCalledOnce();
  });

  it("não chama onclick quando disabled", async () => {
    const user = userEvent.setup();
    const onclick = vi.fn();

    render(AcerolaButtonIcon, { props: { onclick, disabled: true } });
    await user.click(screen.getByRole("button"));

    expect(onclick).not.toHaveBeenCalled();
  });
});
