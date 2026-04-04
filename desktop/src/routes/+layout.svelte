<script lang="ts">
  import { locales, localizeHref } from "$lib/paraglide/runtime";
  import { page } from "$app/state";
  import "$theme/layout.css";

  const { children } = $props();
</script>

{@render children()}

<div style="display:none">
  {#each locales as locale}
    <a href={localizeHref(page.url.pathname, { locale })}>{locale}</a>
  {/each}
</div>
