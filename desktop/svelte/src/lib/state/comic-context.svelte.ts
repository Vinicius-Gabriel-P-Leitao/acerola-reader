import type { ComicSummaryItemPayload } from '$lib/contracts/home/home.payloads';
import { CONTEXT_KEYS } from '$lib/constants/context-keys';
import { getContext, setContext } from 'svelte';

export class ActiveComicState {
	#current = $state<ComicSummaryItemPayload | null>(null);
	#cover = $state<string | null>(null);

	get item() {
		return this.#current;
	}

	get coverUrl() {
		return this.#cover;
	}

	set(item: ComicSummaryItemPayload, cover: string | null) {
		this.#current = item;
		this.#cover = cover;
	}

	clear() {
		this.#current = null;
		this.#cover = null;
	}
}

export function setComicContext() {
	return setContext(CONTEXT_KEYS.activeComic, new ActiveComicState());
}

export function useComicContext() {
	const context = getContext<ActiveComicState>(CONTEXT_KEYS.activeComic);
	if (!context) {
		throw new Error('useComicContext must be used within a layout that calls setComicContext');
	}
	return context;
}
