import { browser } from "$app/environment";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store";
import { THEMES } from "$lib/constants/themes";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { LazyStore } from "@tauri-apps/plugin-store";

export type ThemeColor = keyof typeof THEMES;
export type ThemeMode = keyof (typeof THEMES)[keyof typeof THEMES];
export type ThemeModeOption = ThemeMode | "system";

const store = new LazyStore(STORE_FILE);

let theme = $state<ThemeColor>("catppuccin");
let mode = $state<ThemeModeOption>("dark");
let resolved = $state<ThemeMode>("dark");

async function applyTheme(color: ThemeColor, modeOption: ThemeModeOption) {
  if (!browser) return;

  const window = getCurrentWindow();
  let newResolvedMode: ThemeMode = modeOption as ThemeMode;

  if (modeOption === "system") {
    newResolvedMode = (await window.theme()) ?? "light";
  }

  resolved = newResolvedMode;

  const root = document.documentElement;
  root.setAttribute("data-theme", THEMES[color][resolved]);
  root.classList.toggle("dark", resolved === "dark");

  await window.setTheme(resolved);
}

// prettier-ignore
browser && (async () => {
    const [savedTheme, savedMode] = await Promise.all([
      store.get<ThemeColor>(STORE_KEYS.theme),
      store.get<ThemeModeOption>(STORE_KEYS.mode),
    ]);

    if (savedTheme) theme = savedTheme;
    if (savedMode) mode = savedMode;

    applyTheme(theme, mode);

    const window = getCurrentWindow();
    window.onThemeChanged(async () => {
      if (mode === "system") {
        applyTheme(theme, "system");
      }
    });
  })();

export function useTheme() {
  async function setTheme(newTheme: ThemeColor) {
    theme = newTheme;
    await store.set(STORE_KEYS.theme, newTheme);
    applyTheme(theme, mode);
  }

  async function setMode(newMode: ThemeModeOption) {
    mode = newMode;
    await store.set(STORE_KEYS.mode, newMode);
    applyTheme(theme, mode);
  }

  return {
    setMode,
    setTheme,
    get resolved() {
      return resolved;
    },
    get theme() {
      return theme;
    },
    get mode() {
      return mode;
    },
  };
}
