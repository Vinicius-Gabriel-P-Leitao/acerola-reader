<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import ThemePicker from "./components/theme-picker.svelte";
  import { m } from "$lib/paraglide/messages";

  const ctx = useTheme();
</script>

<div class="max-w-5xl p-8 space-y-12">
  <!-- Header -->
  <div>
    <h1 class="text-3xl font-bold tracking-tight text-foreground">
      {m["pages.config.title"]()}
    </h1>

    <p class="text-muted-foreground mt-2">
      {m["pages.config.desc"]()}
    </p>
  </div>

  <!-- Aparência (Componente Existente) -->
  <ThemePicker theme={ctx.theme} mode={ctx.mode} onselect={ctx.setTheme} />
</div>
