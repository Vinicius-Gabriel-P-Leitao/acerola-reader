import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AcerolaSwitch from "./acerola-switch.svelte";

describe("AcerolaSwitch", () => {
  it("renderiza corretamente", () => {
    render(AcerolaSwitch);
    expect(screen.getByRole("switch")).toBeInTheDocument();
  });

  it("muda de estado ao ser clicado", async () => {
    const user = userEvent.setup();
    let checked = false;
    render(AcerolaSwitch, { checked });

    const switchEl = screen.getByRole("switch");
    await user.click(switchEl);
    
    expect(switchEl.getAttribute("aria-checked")).toBe("true");
  });
});
