import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";

/** @type {import('@sveltejs/kit').Config} */
const config = {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({
      fallback: "index.html",
    }),
    files: {
      assets:      "svelte/static",
      lib:         "svelte/src/lib",
      routes:      "svelte/src/routes",
      appTemplate: "svelte/src/app.html",
    },
  },
};

export default config;
