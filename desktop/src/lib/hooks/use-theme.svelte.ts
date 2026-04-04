import { browser } from "$app/environment";
import { LazyStore } from "@tauri-apps/plugin-store";

const DARK_THEMES = new Set(["catppuccin-mocha", "nord-dark", "dracula"]);
const DEFAULT_THEME = "catppuccin-mocha";
const STORE_KEY = "theme";

const store = new LazyStore("settings.json");

function applyTheme(name: string) {
  if (!browser) return;

  const root = document.documentElement;
  root.setAttribute("data-theme", name);
  root.classList.toggle("dark", DARK_THEMES.has(name));
}

export function useTheme() {
  let theme = $state(DEFAULT_THEME);

  $effect(() => {
    if (!browser) return;

    store.get<string>(STORE_KEY).then((saved) => {
      if (saved) theme = saved;
      applyTheme(theme);
    });
  });

  async function setTheme(name: string) {
    theme = name;
    applyTheme(name);
    await store.set(STORE_KEY, name);
  }

  return {
    get theme() {
      return theme;
    },
    setTheme,
  };
}
