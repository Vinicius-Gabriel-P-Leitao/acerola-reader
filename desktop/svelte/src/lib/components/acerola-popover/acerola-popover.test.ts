import { render, screen } from "@testing-library/svelte";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AcerolaPopover from "./acerola-popover.svelte";

describe("AcerolaPopover", () => {
  it("renderiza o trigger e abre o conteúdo ao clicar", async () => {
    const { container } = render(AcerolaPopover, {
      trigger: () => ({
        html: '<button>Abrir Popover</button>',
      }),
      content: () => ({
        html: '<div>Conteúdo do Popover</div>',
      }),
    });

    const trigger = screen.getByRole("button");
    expect(trigger).toBeInTheDocument();
    
    // Conteúdo não deve estar visível inicialmente
    expect(screen.queryByText("Conteúdo do Popover")).not.toBeVisible();

    const user = userEvent.setup();
    await user.click(trigger);

    // Conteúdo deve aparecer
    expect(screen.getByText("Conteúdo do Popover")).toBeVisible();
  });
});
