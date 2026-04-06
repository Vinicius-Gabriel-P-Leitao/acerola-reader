import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AcerolaSelect from "./acerola-select.svelte";
import type { AcerolaSelectOption } from "./acerola-select.types";

describe("AcerolaSelect", () => {
  const options: AcerolaSelectOption[] = [
    { value: "pt-br", label: "Português" },
    { value: "en", label: "Inglês" },
  ];

  it("renderiza o select com o placeholder", () => {
    render(AcerolaSelect, { options, placeholder: "Escolha o idioma" });
    
    // the underlying component from bits-ui uses a button with combobox role, 
    // but without JS full mount sometimes it's just a button
    const trigger = screen.getByText("Escolha o idioma");
    expect(trigger).toBeInTheDocument();
  });

  it("exibe o label correto de acordo com o valor", () => {
    render(AcerolaSelect, { options, value: "pt-br" });
    expect(screen.getByText("Português")).toBeInTheDocument();
  });

  it("abre as opções ao clicar e permite selecionar uma opção", async () => {
    const user = userEvent.setup();
    render(AcerolaSelect, { options, placeholder: "Selecione..." });

    const trigger = screen.getByText("Selecione...");
    await user.click(trigger);

    // bits-ui select option roles can sometimes be hard to query in JSDom
    // Let's query by text instead to be safe since it's definitely rendered
    const option = await screen.findByText("Inglês");
    expect(option).toBeInTheDocument();

    await user.click(option);

    // After clicking the option, the trigger should now show "Inglês"
    expect(screen.getByText("Inglês")).toBeInTheDocument();
  });
});
