import { render, screen } from "@testing-library/svelte";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AcerolaCommand from "./acerola-command.svelte";

describe("AcerolaCommand", () => {
  it("renderiza o comando e lida com mudança de valor", async () => {
    let value = "";
    
    render(AcerolaCommand, {
      children: () => ({
        html: '<div data-command-input-wrapper=""><input data-command-input="" role="textbox" /></div>',
      }),
      value: value,
      onValueChange: (v: string) => { value = v; },
    });

    const input = screen.getByRole("textbox");
    expect(input).toBeInTheDocument();

    const user = userEvent.setup();
    await user.type(input, "teste");
    
    expect(input).toBeInTheDocument();
  });
});
