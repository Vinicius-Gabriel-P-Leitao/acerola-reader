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
  viteFinal(config) {
    return mergeConfig(config, {
      server: {
        fs: {
          allow: [path.resolve("svelte/src/theme")],
        },
      },
    });
  },
};

export default config;
