<script lang="ts">
    import { cn } from '$lib/utils/cn.utils';

    /**
     * Props for the BookmarkRibbon component.
     */
    export type BookmarkRibbonProps = {
        /** The ARGB integer representing the color (e.g., 0xFFF44336) */
        color: number;
        /** Optional CSS classes to extend or override default styles */
        class?: string;
    };

    let { color, class: className }: BookmarkRibbonProps = $props();

    // Remove the alpha channel 'FF' at the beginning if present, or just use as is
    // But since it's 0xFFF44336, we can bitwise AND with 0xFFFFFF to get RGB.
    // In JS, numbers are double precision, bitwise operates on 32-bit signed integers.
    // 0xFFF44336 & 0xFFFFFF = 0xF44336
    const hexColor = '#' + (color & 0xFFFFFF).toString(16).padStart(6, '0');
</script>

<div
    class={cn("absolute top-0 left-3 w-4 h-7 z-10", className)}
    style="background-color: {hexColor}; clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 75%, 0 100%);"
></div>
