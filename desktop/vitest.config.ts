import { defineConfig } from "vitest/config";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import { svelteTesting } from "@testing-library/svelte/vite";
import path from "path";

export default defineConfig({
  plugins: [svelte({ hot: false }), svelteTesting()],

  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["tests/setup.ts"],
    include: ["src/**/*.test.ts"],
  },

  resolve: {
    alias: {
      $lib:      path.resolve("./src/lib"),
      $theme:    path.resolve("./src/theme"),
      $services: path.resolve("./src/services"),
    },
  },
});
