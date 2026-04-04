<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import ModePicker from "./components/mode-picker.svelte";
  import ThemePicker from "./components/theme-picker.svelte";

  const ctx = useTheme();
</script>
  
<div class="p-8 space-y-10">
  <ModePicker mode={ctx.mode} onselect={ctx.setMode} />
  <ThemePicker theme={ctx.theme} mode={ctx.mode} onselect={ctx.setTheme} />
</div>
