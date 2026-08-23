import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';

const mockStoreMethods = vi.hoisted(() => ({
	get: vi.fn((_key: string) => Promise.resolve(null)),
	set: vi.fn((_key: string, _value: unknown) => Promise.resolve()),
	save: vi.fn(() => Promise.resolve())
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn(),
	LazyStore: vi.fn().mockImplementation(function () {
		return mockStoreMethods;
	})
}));

import { STORE_KEYS } from '$lib/constants/store-plugin';
import { useNavDockMode } from './use-nav-dock-mode.svelte';

async function renderHook() {
	let hook: ReturnType<typeof useNavDockMode> | undefined;

	render(HookHarness, {
		props: {
			create: useNavDockMode,
			onReady: (value) => {
				hook = value as ReturnType<typeof useNavDockMode>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

describe('useNavDockMode', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('usa "fixed" como modo padrão', async () => {
		const hook = await renderHook();
		expect(hook.mode).toBe('fixed');
	});

	it('persiste e reflete o modo escolhido', async () => {
		const hook = await renderHook();

		await hook.setMode('hover');
		await tick();

		expect(hook.mode).toBe('hover');
		expect(mockStoreMethods.set).toHaveBeenCalledWith(STORE_KEYS.navDockMode, 'hover');
		expect(mockStoreMethods.save).toHaveBeenCalledOnce();
	});
});
