<script lang="ts">
  import { type AcerolaSelectOption } from "./acerola-select.types";
  import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
  } from "$lib/components/ui/select";
  import { cn } from "$lib/utils/cn.utils";
  import { m } from "$lib/paraglide/messages";

  let {
    options = [],
    class: className,
    value = $bindable(""),
    placeholder = m["components.select.placeholder"](),
    ...props
  }: {
    value?: string;
    options: AcerolaSelectOption[];
    placeholder?: string;
    class?: string;
    [key: string]: any;
  } = $props();

  let selectedLabel = $derived(
    options.find((it) => it.value === value)?.label ?? placeholder,
  );
</script>

<Select type="single" bind:value {...props}>
  <SelectTrigger class={cn("w-auto min-w-48 justify-between", className)}>
    {selectedLabel}
  </SelectTrigger>

  <SelectContent>
    {#each options as option (option.value)}
      <SelectItem value={option.value} label={option.label}>
        {option.label}
      </SelectItem>
    {/each}
  </SelectContent>
</Select>
