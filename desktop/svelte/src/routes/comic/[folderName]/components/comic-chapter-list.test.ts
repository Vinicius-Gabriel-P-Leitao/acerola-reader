import { render, screen } from '@testing-library/svelte';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import ComicChapterList from './comic-chapter-list.svelte';

describe('ComicChapterList', () => {
	beforeAll(() => {
		globalThis.IntersectionObserver ??= class {
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
});
