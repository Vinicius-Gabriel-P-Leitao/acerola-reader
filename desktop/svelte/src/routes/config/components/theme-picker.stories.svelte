<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import ThemePicker from "./theme-picker.svelte";

  const { Story } = defineMeta({
    title: "Pages/Config/ThemePicker",
    component: ThemePicker,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Seletor de paleta de cores da aplicação. Componente de página.",
        },
      },
    },
  });
</script>

<script lang="ts">
  import { THEMES } from "$lib/constants/themes";
  import type { ThemeColor, ThemeMode } from "$lib/hooks/use-theme.svelte";

  let theme = $state<ThemeColor>("catppuccin");

  const mode = $derived<ThemeMode>(
    document.documentElement.classList.contains("dark") ? "dark" : "light",
  );

  function onselect(name: ThemeColor) {
    theme = name;
    document.documentElement.setAttribute("data-theme", THEMES[name][mode]);
    document.documentElement.classList.toggle("dark", mode === "dark");
  }
</script>

<Story name="Default" asChild>
  <ThemePicker {theme} {mode} {onselect} />
</Story>
