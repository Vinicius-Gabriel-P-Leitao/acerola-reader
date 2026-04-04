<script lang="ts">
  import { Button, type ButtonProps } from "$lib/components/ui/button";

  let { children, ...props }: ButtonProps = $props();
</script>

<Button class="w-10 h-10 p-0 rounded-xl cursor-pointer {props.class}" {...props}>
  {@render children?.()}
</Button>
