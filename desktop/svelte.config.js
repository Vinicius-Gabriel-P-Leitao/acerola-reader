import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";
import { resolve } from "path";

/** @type {import('@sveltejs/kit').Config} */
const config = {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({
      fallback: "index.html",
    }),
    files: {
      lib: "svelte/src/lib",
      assets: "svelte/static",
      routes: "svelte/src/routes",
      appTemplate: "svelte/src/app.html",
    },
  },
};

export default config;
