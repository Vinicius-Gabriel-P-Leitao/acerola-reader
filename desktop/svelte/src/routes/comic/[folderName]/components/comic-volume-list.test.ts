import { render, screen, waitFor } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ComicVolumeList from './comic-volume-list.svelte';

vi.mock('svelte/transition', () => ({
	slide: () => ({ duration: 0 })
}));

// Mock animate for Svelte transitions in JSDOM
if (typeof window !== 'undefined' && !window.HTMLElement.prototype.animate) {
	window.HTMLElement.prototype.animate = vi.fn().mockReturnValue({
		finished: Promise.resolve(),
		cancel: vi.fn(),
		pause: vi.fn(),
		play: vi.fn(),
		reverse: vi.fn(),
		onfinish: null,
		oncancel: null
	});
}

describe('ComicVolumeList', () => {
	let intersectionCallback: IntersectionObserverCallback;
	let observedElements: Element[];

	beforeEach(() => {
		observedElements = [];

		globalThis.IntersectionObserver = class {
			constructor(callback: IntersectionObserverCallback) {
				intersectionCallback = callback;
			}

			observe = vi.fn((element: Element) => {
				observedElements.push(element);
			});
			unobserve = vi.fn();
			disconnect = vi.fn();
		} as unknown as typeof IntersectionObserver;
	});

	const volumes = [
		{
			id: 'v1',
			title: 'Volume 1',
			totalChapters: 1,
			hasMore: false,
			chapters: [{ id: 'c1', title: 'Capítulo 1', fileName: '001.cbz', isRead: true }]
		}
	];

	const pagesData = [
		{
			page: 0,
			items: [{ id: 'c1', title: 'Capítulo 1', fileName: '001.cbz', isRead: true }]
		}
	];

	it('renderiza a lista de volumes quando fornecida', () => {
		render(ComicVolumeList, {
			props: {
				data: { volumes, pagesData },
				events: { onExpand: vi.fn() }
			}
		});

		expect(screen.getByText('Volume 1')).toBeInTheDocument();
		expect(screen.getByText('1 capítulos inclusos')).toBeInTheDocument();
	});

	it('expande o volume ao clicar para mostrar os capítulos', async () => {
		const user = userEvent.setup();
		render(ComicVolumeList, {
			props: {
				data: { volumes, pagesData },
				events: { onExpand: vi.fn() }
			}
		});

		const volumeBtn = screen.getByRole('button', { name: /Volume 1/i });
		expect(volumeBtn).toBeInTheDocument();

		if (volumeBtn) {
			await user.click(volumeBtn);
			expect(screen.getByText('Capítulo 1')).toBeInTheDocument();
		}
	});

	it('colapsa o volume expandido e limpa páginas visíveis', async () => {
		const user = userEvent.setup();
		const onExpand = vi.fn();
		const onVisiblePages = vi.fn();
		render(ComicVolumeList, {
			props: {
				data: { volumes, pagesData },
				events: { onExpand, onVisiblePages }
			}
		});

		const volumeBtn = screen.getByRole('button', { name: /Volume 1/i });
		await user.click(volumeBtn);
		expect(screen.getByText('Capítulo 1')).toBeInTheDocument();

		await user.click(volumeBtn);

		expect(onExpand).toHaveBeenLastCalledWith(null);
		expect(onVisiblePages).toHaveBeenLastCalledWith([]);
		await waitFor(() => {
			expect(screen.queryByText('Capítulo 1')).not.toBeInTheDocument();
		});
	});

	it('exibe mensagem quando volume expandido não possui capítulos', async () => {
		const user = userEvent.setup();
		render(ComicVolumeList, {
			props: {
				data: {
					volumes: [
						{
							id: 'vazio',
							title: 'Volume Vazio',
							totalChapters: 0,
							hasMore: false,
							chapters: []
						}
					],
					pagesData: []
				},
				events: { onExpand: vi.fn() }
			}
		});

		await user.click(screen.getByRole('button', { name: /Volume Vazio/i }));

		expect(screen.getByText('Nenhum capítulo disponível.')).toBeInTheDocument();
	});

	it('notifica páginas visíveis para lazy load', async () => {
		const user = userEvent.setup();
		const onVisiblePages = vi.fn();
		render(ComicVolumeList, {
			props: {
				data: { volumes, pagesData },
				events: { onExpand: vi.fn(), onVisiblePages }
			}
		});

		await user.click(screen.getByRole('button', { name: /Volume 1/i }));
		expect(observedElements).toHaveLength(1);

		intersectionCallback(
			[{ target: observedElements[0], isIntersecting: true } as IntersectionObserverEntry],
			{} as IntersectionObserver
		);

		expect(onVisiblePages).toHaveBeenLastCalledWith([0]);

		intersectionCallback(
			[{ target: observedElements[0], isIntersecting: false } as IntersectionObserverEntry],
			{} as IntersectionObserver
		);

		expect(onVisiblePages).toHaveBeenLastCalledWith([]);
	});

	it('abre capítulo ao clicar no item expandido', async () => {
		const user = userEvent.setup();
		const onOpenChapter = vi.fn();
		render(ComicVolumeList, {
			props: {
				data: { volumes, pagesData },
				events: { onExpand: vi.fn(), onOpenChapter }
			}
		});

		await user.click(screen.getByRole('button', { name: /Volume 1/i }));
		await user.click(screen.getByText('Capítulo 1'));

		expect(onOpenChapter).toHaveBeenCalledWith(pagesData[0].items[0]);
	});

	it('renderiza empty state quando a lista está vazia', () => {
		render(ComicVolumeList, {
			props: {
				data: { volumes: [] },
				events: { onExpand: vi.fn() }
			}
		});

		expect(screen.getByText('Nenhum volume indexado ainda.')).toBeInTheDocument();
	});
});
