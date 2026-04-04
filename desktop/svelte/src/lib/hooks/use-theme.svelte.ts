import { browser } from "$app/environment";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store";
import { THEMES } from "$lib/constants/themes";
import { LazyStore } from "@tauri-apps/plugin-store";

export type ThemeColor = keyof typeof THEMES;
export type ThemeMode = keyof (typeof THEMES)[ThemeColor];

const store = new LazyStore(STORE_FILE);

let theme = $state<ThemeColor>("catppuccin");
let mode = $state<ThemeMode>("dark");

// IIFE para carregar o tema do store e colocar na DOM
// prettier-ignore
browser && (async () => {
  const [savedTheme, savedMode] = await Promise.all([
    store.get<ThemeColor>(STORE_KEYS.theme),
    store.get<ThemeMode>(STORE_KEYS.mode),
  ]);

  if (savedTheme) theme = savedTheme;
  if (savedMode)  mode  = savedMode;

  applyTheme(theme, mode);
})();

function applyTheme(name: ThemeColor, it: ThemeMode) {
  if (!browser) return;

  const root = document.documentElement;
  root.setAttribute("data-theme", THEMES[name][it]);
  root.classList.toggle("dark", it === "dark");
}

export function useTheme() {
  async function setTheme(name: ThemeColor) {
    theme = name;
    applyTheme(theme, mode);
    await store.set(STORE_KEYS.theme, name);
  }

  async function setMode(it: ThemeMode) {
    mode = it;
    applyTheme(theme, mode);
    await store.set(STORE_KEYS.mode, it);
  }

  return {
    get theme() {
      return theme;
    },
    get mode() {
      return mode;
    },
    setTheme,
    setMode,
  };
}
