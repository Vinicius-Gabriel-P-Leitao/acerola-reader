import type { Snippet } from "svelte";
import type { Component } from "svelte";

export type SidebarItem = {
  href: string;
  label: string;
  icon: Component;
};

export type AcerolaSidebarProps = {
  items: SidebarItem[];
  footer?: Snippet;
};
