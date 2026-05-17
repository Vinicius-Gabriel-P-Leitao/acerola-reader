<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import ComicPreferences from "./comic-preferences.svelte";

  const { Story } = defineMeta({
    title: "Pages/Comic/ComicPreferences",
    component: ComicPreferences,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Painel de preferências da página do comic.",
        },
      },
    },
  });
</script>

<script lang="ts">
  let displayMode = $state("Lista");
  let chaptersPerPage = $state("100");
  let mediaType = $state("Manga");
</script>

<Story name="Default" asChild>
  <ComicPreferences bind:displayMode bind:chaptersPerPage bind:mediaType />
</Story>
