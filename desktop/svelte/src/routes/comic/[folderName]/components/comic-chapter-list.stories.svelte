<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import ComicChapterList, { type Chapter } from "./comic-chapter-list.svelte";

  const { Story } = defineMeta({
    title: "Pages/Comic/ComicChapterList",
    component: ComicChapterList,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Lista de capítulos do comic.",
        },
      },
    },
  });
</script>

<script lang="ts">
  const chapters: Chapter[] = [
    {
      id: "c1",
      title: "Capítulo 1: O Início",
      date: "12 Out 2023",
      fileName: "001.cbz",
      isRead: true,
    },
    {
      id: "c2",
      title: "Capítulo 2: A Descoberta",
      date: "19 Out 2023",
      fileName: "002.cbz",
      isRead: false,
    },
  ];
</script>

<Story name="With Chapters" asChild>
  <ComicChapterList {chapters} />
</Story>

<Story name="Empty" asChild>
  <ComicChapterList chapters={[]} />
</Story>
