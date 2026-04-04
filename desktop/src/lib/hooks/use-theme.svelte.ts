import { browser } from "$app/environment";
import { LazyStore } from "@tauri-apps/plugin-store";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store";
import { THEMES } from "$lib/constants/themes";

export type ThemeColor = keyof typeof THEMES;
export type ThemeMode = keyof (typeof THEMES)[ThemeColor];

const store = new LazyStore(STORE_FILE);

// Estado no nível do módulo — compartilhado entre todos os componentes
let theme = $state<ThemeColor>("catppuccin");
let mode = $state<ThemeMode>("dark");
let initialized = false;

function applyTheme(name: ThemeColor, it: ThemeMode) {
  if (!browser) return;

  const root = document.documentElement;
  root.setAttribute("data-theme", THEMES[name][it]);
  root.classList.toggle("dark", it === "dark");
}

export function useTheme() {
  $effect(() => {
    if (!browser || initialized) return;
    initialized = true;

    Promise.all([
      store.get<ThemeColor>(STORE_KEYS.theme),
      store.get<ThemeMode>(STORE_KEYS.mode),
    ]).then(([savedTheme, savedMode]) => {
      if (savedTheme) theme = savedTheme;
      if (savedMode) mode = savedMode;
      
      applyTheme(theme, mode);
    });
  });

  async function setTheme(name: ThemeColor) {
    theme = name;
    applyTheme(theme, mode);
    await store.set(STORE_KEYS.theme, name);
  }

  async function setMode(m: ThemeMode) {
    mode = m;
    applyTheme(theme, mode);
    await store.set(STORE_KEYS.mode, m);
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
