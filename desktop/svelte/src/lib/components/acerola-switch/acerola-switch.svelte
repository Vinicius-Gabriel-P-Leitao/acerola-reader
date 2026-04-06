<script lang="ts">
  import { Switch as SwitchPrimitive } from "bits-ui";
  import { cn } from "$lib/utils/cn.utils.js";
  import type { AcerolaSwitchProps } from "./acerola-switch.types.js";

  let {
    class: className,
    size = "default",
    ref = $bindable(null),
    checked = $bindable(false),
    ...restProps
  }: AcerolaSwitchProps = $props();
</script>

<SwitchPrimitive.Root
  bind:ref
  bind:checked
  data-slot="switch"
  data-size={size}
  class={cn(
    "peer group/switch relative inline-flex shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=unchecked]:bg-input",
    size === "default" ? "h-8 w-14" : "h-6 w-11",
    className,
  )}
  {...restProps}
>
  <SwitchPrimitive.Thumb
    class={cn(
      "pointer-events-none block rounded-full bg-background shadow-lg ring-0 transition-transform data-[state=checked]:translate-x-full data-[state=unchecked]:translate-x-0",
      size === "default" ? "h-6 w-6" : "h-5 w-5",
    )}
  />
</SwitchPrimitive.Root>
