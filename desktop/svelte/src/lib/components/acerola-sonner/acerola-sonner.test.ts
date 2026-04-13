import { render } from "@testing-library/svelte";
import { describe, expect, it } from "vitest";
import AcerolaSonner from "./acerola-sonner.svelte";

describe("AcerolaSonner", () => {
  it("renderiza o toaster", () => {
    const { container } = render(AcerolaSonner);
    expect(container).toBeInTheDocument();
  });
});
