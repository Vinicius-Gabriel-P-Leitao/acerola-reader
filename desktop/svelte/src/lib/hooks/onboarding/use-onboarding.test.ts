import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { load, LazyStore } from '@tauri-apps/plugin-store';
import { STORE_KEYS } from '$lib/constants/store-plugin';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';

const mockStoreMethods = vi.hoisted(() => ({
	get: vi.fn((_key: string) => Promise.resolve(null)),
	set: vi.fn((_key: string, _value: unknown) => Promise.resolve()),
	save: vi.fn(() => Promise.resolve())
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn(),
	LazyStore: vi.fn().mockImplementation(() => mockStoreMethods)
}));

import { useOnboarding } from './use-onboarding.svelte';

const loadMock = vi.mocked(load);

async function renderHook() {
	let hook: ReturnType<typeof useOnboarding> | undefined;

	render(HookHarness, {
		props: {
			create: useOnboarding,
			onReady: (value) => {
				hook = value as ReturnType<typeof useOnboarding>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

describe('useOnboarding', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('inicia com o passo 0 e gerencia o avanço e recuo de passos', async () => {
		const hook = await renderHook();

		expect(hook.currentStep).toBe(0);

		hook.nextStep();
		expect(hook.currentStep).toBe(1);

		hook.nextStep();
		expect(hook.currentStep).toBe(2);

		hook.prevStep();
		expect(hook.currentStep).toBe(1);

		hook.setStep(4);
		expect(hook.currentStep).toBe(4);
	});

	it('marca o onboarding como concluído e salva na store', async () => {
		const hook = await renderHook();

		await hook.complete();

		expect(mockStoreMethods.set).toHaveBeenCalledWith(STORE_KEYS.onboardingCompleted, true);
		expect(mockStoreMethods.save).toHaveBeenCalledOnce();
		expect(hook.isCompleted).toBe(true);
	});
});
