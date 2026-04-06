<script lang="ts">
  import { Button, type ButtonProps } from "$lib/components/ui/button";
  import { cn } from "$lib/utils/cn.utils";

  let { children, class: className, ...props }: ButtonProps = $props();
</script>

<Button class={cn("font-dm-sans enabled:cursor-pointer disabled:pointer-events-auto disabled:cursor-not-allowed", className)} {...props}>
  {@render children?.()}
</Button>
