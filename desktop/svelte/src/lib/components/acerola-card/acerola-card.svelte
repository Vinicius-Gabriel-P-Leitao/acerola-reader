<script lang="ts">
  import * as Card from "$lib/components/ui/card";
  import type { AcerolaCardProps } from "./acerola-card.types";

  let {
    title,
    description,
    children,
    footer,
    size = "default",
    class: className,
  }: AcerolaCardProps = $props();
</script>

<Card.Root {size} class={className}>
  <Card.Header>
    <Card.Title class="text-foreground font-semibold">{title}</Card.Title>
    {#if description}
      <Card.Description>{description}</Card.Description>
    {/if}
  </Card.Header>

  {#if children}
    <Card.Content>
      {@render children()}
    </Card.Content>
  {/if}

  {#if footer}
    <Card.Footer>
      {@render footer()}
    </Card.Footer>
  {/if}
</Card.Root>
