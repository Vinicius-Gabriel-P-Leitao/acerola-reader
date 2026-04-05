import { render, screen } from "@testing-library/svelte";
import userEvent from "@testing-library/user-event";
import { createRawSnippet } from "svelte";
import { describe, expect, it } from "vitest";
import AcerolaPopover from "./acerola-popover.svelte";

describe("AcerolaPopover", () => {
  it("renderiza o trigger e abre o conteudo ao clicar", async () => {
    const trigger = createRawSnippet(() => ({
      render: () => "<button>Abrir Popover</button>",
    }));
    const content = createRawSnippet(() => ({
      render: () => "<div>Conteudo do Popover</div>",
    }));

    render(AcerolaPopover, {
      trigger,
      content,
    });

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
    expect(screen.queryByText("Conteudo do Popover")).not.toBeVisible();

    const user = userEvent.setup();
    await user.click(button);

    expect(screen.getByText("Conteudo do Popover")).toBeVisible();
  });
});
