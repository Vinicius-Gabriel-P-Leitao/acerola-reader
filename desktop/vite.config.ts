import { paraglideVitePlugin } from "@inlang/paraglide-js";
import { sveltekit } from "@sveltejs/kit/vite";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import svg from "@poppanator/sveltekit-svg";
import { defineConfig } from "vite";

const host = process.env.TAURI_DEV_HOST;

export default defineConfig(async () => ({
  clearScreen: false,
  plugins: [
    sveltekit(),
    tailwindcss(),
    svg({
      includePaths: ["./svelte/src/lib/assets/"],
      svgoOptions: {
        multipass: true,
        plugins: ["preset-default"],
      },
    }),
    paraglideVitePlugin({
      project: "./svelte/project.inlang",
      outdir: "./svelte/src/lib/paraglide",
    }),
  ],
  resolve: { alias: { $theme: path.resolve("./svelte/src/theme") } },
  server: {
    port: 1420,
    strictPort: true,
    host: host || "127.0.0.1",
    watch: { ignored: ["**/src-tauri/**"] },
    hmr: host ? { protocol: "ws", host, port: 1421 } : undefined,
    fs: {
      allow: ["svelte/src", "svelte/static", ".svelte-kit", "node_modules"],
    },
  },
}));
