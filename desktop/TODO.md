# Refatorar

- [ ] Escrever mais testes para os hooks
- [ ] Fazer objetos alinhados terem contexto, evitar algo como

```ts
 let {
  title,
  totalChapters,
  viewMode = 'cover',
  coverUri = null,
  bannerUri = null,
  isExpanded = false,
  onclick
 }: {
  title: string;
  totalChapters: number;
  viewMode?: 'cover' | 'banner';
  coverUri?: string | null;
  bannerUri?: string | null;
  isExpanded?: boolean;
  onclick: () => void;
 } = $props();
```

Criar type para cada um de objeto assim e fazer parecido com o card

```ts
<script module lang="ts">
 import type { Snippet } from 'svelte';

 export type AcerolaCardImageProps = {
  data: {
   title: string;
   cover?: string | null;
   progress?: number;
   description?: string;
  };
  events?: {
   onClick?: (event: MouseEvent) => void;
  };
  ui?: {
   class?: string;
  };
 };

 export type AcerolaCardImageSnippets = {
  placeholder?: Snippet;
  overlay?: Snippet;
  footer?: Snippet;
  action?: Snippet;
 };
</script>

<script lang="ts">
 import BookOpenIcon from '@lucide/svelte/icons/book-open';
 import { AspectRatio } from '$lib/components/ui/aspect-ratio';
 import { cn } from '$lib/utils/cn.utils';

 let {
  ui,
  data,
  events,
  footer,
  action,
  overlay,
  placeholder
 }: AcerolaCardImageProps & AcerolaCardImageSnippets = $props();
</script>
```

  Manter esse padrão de código onde fica os objetos internos por contexto
