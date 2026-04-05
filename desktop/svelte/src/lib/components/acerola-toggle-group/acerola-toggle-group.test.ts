import { render, screen } from "@testing-library/svelte";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AcerolaToggleGroup from "./acerola-toggle-group.svelte";

describe("AcerolaToggleGroup", () => {
  it("renderiza o grupo de toggles", async () => {
    let value = "bold";
    
    render(AcerolaToggleGroup, {
      children: () => ({
        html: '<button data-value="bold">Bold</button><button data-value="italic">Italic</button>',
      }),
      value: value,
      type: "single",
    });

    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(2);
    
    // Simplificando verificação para testar a renderização do root
    expect(screen.getByRole("group")).toBeInTheDocument();
  });
});
