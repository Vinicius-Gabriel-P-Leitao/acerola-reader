import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AcerolaButton from "./acerola-button.svelte";

describe("AcerolaButton", () => {
  it("renderiza o conteúdo passado via slot", () => {
    render(AcerolaButton, { props: { children: () => "Clique aqui" } });
    expect(screen.getByText("Clique aqui")).toBeInTheDocument();
  });

  it("chama onclick ao ser clicado", async () => {
    const user = userEvent.setup();
    const onclick = vi.fn();

    render(AcerolaButton, { props: { onclick } });
    await user.click(screen.getByRole("button"));

    expect(onclick).toHaveBeenCalledOnce();
  });

  it("não chama onclick quando disabled", async () => {
    const user = userEvent.setup();
    const onclick = vi.fn();

    render(AcerolaButton, { props: { onclick, disabled: true } });
    await user.click(screen.getByRole("button"));

    expect(onclick).not.toHaveBeenCalled();
  });
});
