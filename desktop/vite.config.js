import { paraglideVitePlugin } from "@inlang/paraglide-js";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import { sveltekit } from "@sveltejs/kit/vite";

// @ts-expect-error process is a nodejs global
const host = process.env.TAURI_DEV_HOST;

// https://vite.dev/config/
export default defineConfig(async () => ({
  plugins: [
    sveltekit(),
    tailwindcss(),
    paraglideVitePlugin({ project: "./project.inlang", outdir: "./src/lib/paraglide" })
  ],
  clearScreen: false,
  // NOTE: O tauri espera uma porta fixa
  server: {
    port: 1420,
    strictPort: true,
    host: host || "127.0.0.1",
    hmr: host ? { protocol: "ws", host, port: 1421 } : undefined,
    watch: {
      // NOTE: Vite ignore watching `src-tauri`
      ignored: ["**/src-tauri/**"]
    }
  }
}));
