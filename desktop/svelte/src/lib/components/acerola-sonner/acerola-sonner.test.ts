import { render } from "@testing-library/svelte";
import { describe, expect, it } from "vitest";
import AcerolaSonner from "./acerola-sonner.svelte";

describe("AcerolaSonner", () => {
  it("renderiza o toaster", () => {
    // Sonner renderiza um elemento com a classe 'toaster'
    render(AcerolaSonner);
    expect(document.querySelector(".toaster")).toBeInTheDocument();
  });
});
