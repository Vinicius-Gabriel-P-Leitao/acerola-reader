import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { THEMES } from '$lib/constants/themes';
import HookHarness from '../hook.test-harness.svelte';
import { useTheme } from './use-theme.svelte';

async function renderHook() {
	let hook: ReturnType<typeof useTheme> | undefined;

	render(HookHarness, {
		props: {
			create: useTheme,
			onReady: (value) => {
				hook = value as ReturnType<typeof useTheme>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

async function flushTheme() {
	await tick();
	await Promise.resolve();
	await tick();
}

describe('useTheme', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		document.documentElement.removeAttribute('data-theme');
		document.documentElement.classList.remove('dark');
	});

	it('aplica tema e modo claro no documento', async () => {
		const hook = await renderHook();

		await hook.setMode('light');
		await hook.setTheme('nord');
		await flushTheme();

		expect(hook.theme).toBe('nord');
		expect(hook.mode).toBe('light');
		expect(hook.resolved).toBe('light');
		expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.nord.light);
		expect(document.documentElement.classList.contains('dark')).toBe(false);
	});

	it('resolve modo system a partir da janela atual', async () => {
		const hook = await renderHook();

		await hook.setMode('system');
		await flushTheme();

		expect(hook.mode).toBe('system');
		expect(hook.resolved).toBe('light');
		expect(document.documentElement.classList.contains('dark')).toBe(false);
	});
});
