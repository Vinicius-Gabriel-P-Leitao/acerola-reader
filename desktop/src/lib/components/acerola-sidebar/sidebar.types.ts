import type { Component } from "svelte";

export type SidebarItem = {
  href: string;
  label: string;
  icon: Component;
};
