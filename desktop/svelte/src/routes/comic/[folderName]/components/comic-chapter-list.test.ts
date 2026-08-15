import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { tick } from 'svelte';
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

	it('renderiza a lista de capítulos quando fornecida', async () => {
		render(ComicChapterList, {
			props: { data: { pagesData, totalChapters: 2, pageSize: 2 } }
		});

		// O bloco só monta o conteúdo real (AcerolaHeroButton) quando a página
		// está dentro da janela de renderização — em produção o
		// IntersectionObserver reporta isso pouco depois do mount.
		const firstPage = document.querySelector<HTMLElement>('[data-page="0"]');
		intersectionCallback(
			[{ target: firstPage!, isIntersecting: true } as unknown as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		await tick();

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

	it('monta o conteúdo real só quando a página entra na janela de renderização', async () => {
		render(ComicChapterList, {
			props: { data: { pagesData, totalChapters: 2, pageSize: 2 } }
		});

		expect(screen.queryByText('Capítulo 1: Test')).not.toBeInTheDocument();

		const firstPage = document.querySelector<HTMLElement>('[data-page="0"]');
		intersectionCallback(
			[{ target: firstPage!, isIntersecting: true } as unknown as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		await tick();

		expect(screen.getByText('Capítulo 1: Test')).toBeInTheDocument();
	});

	it('desmonta o conteúdo real ao sair da janela de renderização', async () => {
		render(ComicChapterList, {
			props: { data: { pagesData, totalChapters: 2, pageSize: 2 } }
		});

		const firstPage = document.querySelector<HTMLElement>('[data-page="0"]');
		intersectionCallback(
			[{ target: firstPage!, isIntersecting: true } as unknown as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		await tick();
		expect(screen.getByText('Capítulo 1: Test')).toBeInTheDocument();

		intersectionCallback(
			[{ target: firstPage!, isIntersecting: false } as unknown as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		await tick();

		expect(screen.queryByText('Capítulo 1: Test')).not.toBeInTheDocument();
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

		const firstPage = document.querySelector<HTMLElement>('[data-page="0"]');
		intersectionCallback(
			[{ target: firstPage!, isIntersecting: true } as unknown as IntersectionObserverEntry],
			{} as IntersectionObserver
		);
		await tick();

		await user.click(screen.getByText('Capítulo 2: Another Test'));

		expect(onOpenChapter).toHaveBeenCalledWith(pagesData[0].items[1]);
	});
});
