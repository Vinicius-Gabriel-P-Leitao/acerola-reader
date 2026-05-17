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
        entries: [
          "../svelte/src/**/*.stories.@(js|ts|svelte)",
          "../svelte/src/**/*.svelte",
        ],
        holdUntilResolved: true,
        include: [
          "svelte",
          "svelte/internal",
          "svelte/internal/client",
          "svelte/internal/disclose-version",
          "esm-env",
          "devalue",
          "@storybook/sveltekit/internal/mocks/app/state.svelte.js",
          "@storybook/sveltekit/internal/mocks/app/navigation",
          "@storybook/sveltekit/internal/mocks/app/stores",
          "tailwind-merge",
          "tailwind-variants",
          "svelte-sonner",
          "bits-ui",
          "mode-watcher",
          "clsx",
          "@internationalized/date"
        ],
        exclude: [
          "@lucide/svelte",
          "@tauri-apps/api",
          "@tauri-apps/plugin-store",
          "@tauri-apps/plugin-dialog",
          "@tauri-apps/plugin-fs",
          "@tauri-apps/plugin-log",
          "@tauri-apps/plugin-opener",
          "@tauri-apps/plugin-sql",
        ]
      },
    });
  },
};

export default config;
