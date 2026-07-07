<script lang="ts">
    import { cn } from '$lib/utils/cn.utils';

    /**
     * Props for the BookmarkRibbon component.
     */
    export type AcerolaBookmarkRibbonProps = {
        /** The color of the bookmark ribbon as an integer (e.g. 0xFF0000) */
        color: number;
        /** Optional CSS class to apply to the ribbon */
        class?: string;
    };

    let { color, class: className }: AcerolaBookmarkRibbonProps = $props();

    // Remove the alpha channel 'FF' at the beginning if present, or just use as is
    // But since it's 0xFFF44336, we can bitwise AND with 0xFFFFFF to get RGB.
    // In JS, numbers are double precision, bitwise operates on 32-bit signed integers.
    // 0xFFF44336 & 0xFFFFFF = 0xF44336
    let hexColor = $derived('#' + (color & 0xFFFFFF).toString(16).padStart(6, '0'));
</script>

<div
    class={cn("absolute -top-2.5 left-3 w-5 h-8 z-10 drop-shadow-md", className)}
    style="background-color: {hexColor}; clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 75%, 0 100%);"
></div>
