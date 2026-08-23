import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { describe, expect, it } from 'vitest';
import type { ComicSummaryItemPayload } from '$lib/contracts/home/home.payloads';
import { ActiveComicState } from './comic-context.svelte';
import ProviderHarness from '../../../tests/harness/state/comic-context-provider.svelte';
import MissingHarness from '../../../tests/harness/state/comic-context-missing.svelte';

function comic(): ComicSummaryItemPayload {
	return {
		relations: { directoryId: 'dir-1', metadataId: null },
		filesystem: { folderName: 'Acerola' },
		metadata: {
			title: 'Acerola',
			externalSync: false,
			activeSource: null,
			chapterCount: 10
		},
		artwork: { cover: null, banner: null }
	};
}

describe('ActiveComicState', () => {
	it('stores active comic and cover', () => {
		const state = new ActiveComicState();
		const item = comic();

		state.set(item, 'asset://cover.jpg');

		expect(state.item).toEqual(item);
		expect(state.coverUrl).toBe('asset://cover.jpg');
	});

	it('clears active comic', () => {
		const state = new ActiveComicState();
		state.set(comic(), 'asset://cover.jpg');

		state.clear();

		expect(state.item).toBeNull();
		expect(state.coverUrl).toBeNull();
	});
});

describe('useComicContext', () => {
	it('returns context created by the provider', async () => {
		let context: ActiveComicState | undefined;
		const item = comic();

		render(ProviderHarness, {
			props: {
				comic: item,
				cover: 'asset://cover.jpg',
				onReady: (value) => {
					context = value;
				}
			}
		});

		await tick();

		expect(context?.item).toEqual(item);
		expect(context?.coverUrl).toBe('asset://cover.jpg');
	});

	it('throws error when used outside provider', () => {
		expect(() => render(MissingHarness)).toThrow(
			'useComicContext must be used within a layout that calls setComicContext'
		);
	});
});
