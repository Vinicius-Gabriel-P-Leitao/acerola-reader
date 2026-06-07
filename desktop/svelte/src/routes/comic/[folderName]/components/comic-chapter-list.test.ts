import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ComicChapterList from './comic-chapter-list.svelte';

describe('ComicChapterList', () => {
	let intersectionCallback: IntersectionObserverCallback;

	beforeEach(() => {
		globalThis.IntersectionObserver = class {
			constructor(callback: IntersectionObserverCallback) {
				intersectionCallback = callback;
			}

			observe = vi.fn();
			unobserve = vi.fn();
			disconnect = vi.fn();
		} as unknown as typeof IntersectionObserver;
	});

	const pagesData = [
		{
			page: 0,
			items: [
				{
					id: 'c1',
					title: 'Capítulo 1: Test',
					chapterSort: '1',
					fileName: '001.cbz',
					isRead: true
				},
				{
					id: 'c2',
					title: 'Capítulo 2: Another Test',
					chapterSort: '2',
					fileName: '002.cbz',
					isRead: false
				}
			]
		}
	];

	it('renderiza a lista de capítulos quando fornecida', () => {
		render(ComicChapterList, {
			props: { data: { pagesData, totalChapters: 2, pageSize: 2 } }
		});

		expect(screen.getByText('Capítulo 1: Test')).toBeInTheDocument();
		expect(screen.getByText('001.cbz')).toBeInTheDocument();
		expect(screen.getByText('Capítulo 2: Another Test')).toBeInTheDocument();
		expect(screen.getByText('002.cbz')).toBeInTheDocument();
	});

	it('renderiza empty state quando a lista está vazia', () => {
		render(ComicChapterList, {
			props: { data: { pagesData: [], totalChapters: 0, pageSize: 2 } }
		});

		expect(screen.getByText('Carregando...')).toBeInTheDocument();
	});

	it('notifica a próxima página visível para paginação', () => {
		const onVisiblePages = vi.fn();
		render(ComicChapterList, {
			props: {
				data: { pagesData, totalChapters: 4, pageSize: 2 },
				events: { onVisiblePages }
			}
		});

		const nextPage = document.querySelector<HTMLElement>('[data-page="1"]');
		expect(nextPage).toBeInTheDocument();

		intersectionCallback(
			[{ target: nextPage!, isIntersecting: true } as IntersectionObserverEntry],
			{} as IntersectionObserver
		);

		expect(onVisiblePages).toHaveBeenLastCalledWith([1]);
	});

	it('remove página da lista visível ao sair do viewport', () => {
		const onVisiblePages = vi.fn();
		render(ComicChapterList, {
			props: {
				data: { pagesData, totalChapters: 4, pageSize: 2 },
				events: { onVisiblePages }
			}
		});

		const firstPage = document.querySelector<HTMLElement>('[data-page="0"]');
		expect(firstPage).toBeInTheDocument();

		intersectionCallback(
			[{ target: firstPage!, isIntersecting: true } as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		intersectionCallback(
			[{ target: firstPage!, isIntersecting: false } as IntersectionObserverEntry],
			{} as IntersectionObserver
		);

		expect(onVisiblePages).toHaveBeenLastCalledWith([]);
	});

	it('abre capítulo ao clicar no item', async () => {
		const user = userEvent.setup();
		const onOpenChapter = vi.fn();
		render(ComicChapterList, {
			props: {
				data: { pagesData, totalChapters: 2, pageSize: 2 },
				events: { onOpenChapter }
			}
		});

		await user.click(screen.getByText('Capítulo 2: Another Test'));

		expect(onOpenChapter).toHaveBeenCalledWith(pagesData[0].items[1]);
	});
});
