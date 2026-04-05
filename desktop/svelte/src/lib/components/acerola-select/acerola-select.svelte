<script lang="ts">
  import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
  } from "$lib/components/ui/select";
  import { m } from "$lib/paraglide/messages";
  import { cn } from "$lib/utils/cn.utils";
  import {
    type AcerolaSelectOption,
    type AcerolaSelectProps,
  } from "./acerola-select.types";

  let {
    options = [],
    class: className,
    value = $bindable(""),
    placeholder = m["components.select.placeholder"](),
    ...props
  }: AcerolaSelectProps = $props();

  let selectedLabel = $derived(
    options.find((it: AcerolaSelectOption) => it.value === value)?.label ??
      placeholder,
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
