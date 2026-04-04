<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import ThemePicker from "./components/theme-picker.svelte";

  const ctx = useTheme();
</script>

<section class="p-8">
  <ThemePicker theme={ctx.theme} mode={ctx.mode} onselect={ctx.setTheme} />
</section>
