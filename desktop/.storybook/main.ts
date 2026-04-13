import type { StorybookConfig } from '@storybook/sveltekit';
import path from 'path';
import { mergeConfig } from 'vite';

const config: StorybookConfig = {
  stories: [
    "../svelte/src/**/*.mdx",
    "../svelte/src/**/*.stories.@(js|ts|svelte)",
  ],
  addons: [
    "@storybook/addon-svelte-csf",
    "@chromatic-com/storybook",
    "@storybook/addon-vitest",
    "@storybook/addon-a11y",
    "@storybook/addon-docs",
  ],
  framework: "@storybook/sveltekit",
  staticDirs: ["../svelte/static"],
  viteFinal(config) {
    return mergeConfig(config, {
      server: {
        fs: {
          allow: [path.resolve("svelte/src/theme")],
        },
      },
      optimizeDeps: {
        include: [
          "@lucide/svelte/icons/moon",
          "@lucide/svelte/icons/sun",
          "@lucide/svelte/icons/settings",
          "@lucide/svelte/icons/library",
          "@lucide/svelte/icons/history",
          "@lucide/svelte/icons/monitor",
          "@lucide/svelte/icons/alert-triangle",
          "@lucide/svelte/icons/bell",
          "@lucide/svelte/icons/check-circle-2",
          "@lucide/svelte/icons/info",
          "@lucide/svelte/icons/x",
          "@lucide/svelte/icons/x-circle",
          "@lucide/svelte/icons/loader-2",
          "@lucide/svelte/icons/circle-check",
          "@lucide/svelte/icons/octagon-x",
          "@lucide/svelte/icons/triangle-alert",
          "@lucide/svelte/icons/panel-left",
          "@lucide/svelte/icons/folder",
          "@lucide/svelte/icons/play",
          "@lucide/svelte/icons/check",
          "@lucide/svelte/icons/chevron-down",
          "@lucide/svelte/icons/chevron-up",
          "@lucide/svelte/icons/search",
          "@lucide/svelte/icons/more-vertical",
          "@lucide/svelte/icons/book-open",
          "tailwind-merge",
          "tailwind-variants",
          "svelte-sonner",
          "bits-ui",
          "mode-watcher",
          "@tauri-apps/api/window",
          "@tauri-apps/plugin-store",
        ],
      },
    });
  },
};

export default config;
