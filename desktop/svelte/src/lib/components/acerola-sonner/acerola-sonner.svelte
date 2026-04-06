<script lang="ts">
  import { Toaster as AcerolaToaster } from "$lib/components/ui/sonner/index.js";
  import type { ToasterProps as SonnerProps } from "svelte-sonner";

  let { ...restProps }: SonnerProps = $props();
</script>

<AcerolaToaster {...restProps} />
