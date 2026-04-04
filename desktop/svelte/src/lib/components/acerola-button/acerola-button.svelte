<script lang="ts">
  import { Button, type ButtonProps } from "$lib/components/ui/button";

  let { children, ...props }: ButtonProps = $props();
</script>

<Button class="flex-1 px-3 py-2 rounded-lg font-dm-sans cursor-pointer {props.class}" {...props}>
  {@render children?.()}
</Button>
