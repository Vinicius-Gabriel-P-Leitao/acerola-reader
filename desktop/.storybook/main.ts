import type { StorybookConfig } from "@storybook/sveltekit";

const config: StorybookConfig = {
  framework: "@storybook/sveltekit",

  stories: ["../src/**/*.stories.svelte"],

  addons: [
    "@storybook/addon-essentials",
    "@storybook/addon-svelte-csf",
  ],

  docs: {
    autodocs: "tag",
  },
};

export default config;
