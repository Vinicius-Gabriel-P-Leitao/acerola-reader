import { render, screen } from "@testing-library/svelte";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, beforeEach } from "vitest";
import AcerolaNotification, { notificationStore } from "./acerola-notification.svelte";

function getTrigger() {
  return document.querySelector<HTMLElement>("[data-popover-trigger]")!;
}

describe("AcerolaNotification", () => {
  beforeEach(() => {
    notificationStore.clearAll();
  });

  it("renderiza o botão de notificação", () => {
    render(AcerolaNotification);
    expect(getTrigger()).toBeInTheDocument();
  });

  it("não exibe badge quando não há notificações", () => {
    render(AcerolaNotification);
    expect(document.querySelector(".bg-primary.rounded-full")).not.toBeInTheDocument();
  });

  it("exibe badge quando há notificações", () => {
    notificationStore.notify.success("Teste");
    render(AcerolaNotification);
    expect(document.querySelector(".bg-primary.rounded-full")).toBeInTheDocument();
  });

  it("abre o popover e exibe a notificação ao clicar no botão", async () => {
    notificationStore.notify.success("Scan concluído!");
    render(AcerolaNotification);

    const user = userEvent.setup();
    await user.click(getTrigger());

    expect(screen.getByText("Scan concluído!")).toBeInTheDocument();
  });

  it("exibe estado vazio quando não há notificações", async () => {
    render(AcerolaNotification);

    const user = userEvent.setup();
    await user.click(getTrigger());

    expect(screen.getByText("Nenhuma notificação")).toBeInTheDocument();
  });

  it("remove notificação ao clicar no X", async () => {
    notificationStore.notify.error("Erro de sync");
    render(AcerolaNotification);

    const user = userEvent.setup();
    await user.click(getTrigger());

    const closeBtn = document.querySelector<HTMLElement>(".size-6.shrink-0");
    await user.click(closeBtn!);

    expect(screen.queryByText("Erro de sync")).not.toBeInTheDocument();
  });

  it("limpa todas as notificações ao clicar em limpar tudo", async () => {
    notificationStore.notify.success("Notificação 1");
    notificationStore.notify.info("Notificação 2");
    render(AcerolaNotification);

    const user = userEvent.setup();
    await user.click(getTrigger());
    await user.click(screen.getByText("Limpar tudo"));

    expect(screen.queryByText("Notificação 1")).not.toBeInTheDocument();
    expect(screen.queryByText("Notificação 2")).not.toBeInTheDocument();
  });
});
