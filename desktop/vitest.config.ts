import { storybookTest } from "@storybook/addon-vitest/vitest-plugin";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import { svelteTesting } from "@testing-library/svelte/vite";
import { playwright } from "@vitest/browser-playwright";
import { fileURLToPath } from "node:url";
import path from "path";
import { defineConfig } from "vitest/config";

const dirname =
  typeof __dirname !== "undefined"
    ? __dirname
    : path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [
    svelte({
      hot: false,
    }),
    svelteTesting(),
  ],
  resolve: {
    alias: {
      $lib: path.resolve("./svelte/src/lib"),
      $theme: path.resolve("./svelte/src/theme"),
      $services: path.resolve("./svelte/src/services"),
      "$app/state": path.resolve("./svelte/tests/mocks/app-state.ts"),
      "$app/environment": path.resolve("./svelte/tests/mocks/app-environment.ts"),
    },
  },
  test: {
    projects: [
      {
        extends: true,
        test: {
          globals: true,
          environment: "jsdom",
          include: ["svelte/src/**/*.test.ts"],
          setupFiles: ["svelte/tests/setup.ts"],
        },
      },
      {
        extends: true,
        plugins: [
          storybookTest({
            configDir: path.join(dirname, ".storybook"),
          }),
        ],
        test: {
          name: "storybook",
          browser: {
            enabled: true,
            headless: true,
            provider: playwright({}),
            instances: [
              {
                browser: "chromium",
              },
            ],
          },
        },
      },
    ],
  },
});
