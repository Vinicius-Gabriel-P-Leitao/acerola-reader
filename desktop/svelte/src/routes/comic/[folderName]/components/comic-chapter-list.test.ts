import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import ComicChapterList from './comic-chapter-list.svelte';

describe('ComicChapterList', () => {
	const chapters = [
		{
			id: 'c1',
			title: 'Capítulo 1: Test',
			date: '12 Out 2023',
			fileName: '001.cbz',
			isRead: true
		},
		{
			id: 'c2',
			title: 'Capítulo 2: Another Test',
			date: '19 Out 2023',
			fileName: '002.cbz',
			isRead: false
		}
	];

	it('renderiza a lista de capítulos quando fornecida', () => {
		render(ComicChapterList, { chapters });

		expect(screen.getByText('Capítulo 1: Test')).toBeInTheDocument();
		expect(screen.getByText('12 Out 2023 • 001.cbz')).toBeInTheDocument();
		expect(screen.getByText('Capítulo 2: Another Test')).toBeInTheDocument();
		expect(screen.getByText('19 Out 2023 • 002.cbz')).toBeInTheDocument();
	});

	it('renderiza empty state quando a lista está vazia', () => {
		render(ComicChapterList, { chapters: [] });

		expect(screen.getByText('Sincronizando capítulos...')).toBeInTheDocument();
	});
});
