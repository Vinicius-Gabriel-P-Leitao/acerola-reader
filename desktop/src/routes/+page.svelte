<script lang="ts">
	import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
	import { useTheme } from "$lib/hooks/use-theme.svelte";

	const { setTheme } = useTheme();

	const themes = [
		{ label: "Catppuccin Latte", value: "catppuccin-latte" },
		{ label: "Catppuccin Mocha", value: "catppuccin-mocha" },
		{ label: "Nord Light", value: "nord-light" },
		{ label: "Nord Dark", value: "nord-dark" },
		{ label: "Alucard", value: "alucard" },
		{ label: "Dracula", value: "dracula" },
	];
</script>

{#each themes as theme}
	<AcerolaButton onclick={() => setTheme(theme.value)}>
		{theme.label}
	</AcerolaButton>
{/each}
