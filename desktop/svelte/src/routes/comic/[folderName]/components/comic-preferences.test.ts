import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import ComicPreferences from './comic-preferences.svelte';

describe('ComicPreferences', () => {
	it('renderiza as opções de preferência', () => {
		render(ComicPreferences, {
			displayMode: 'Lista',
			chaptersPerPage: '100',
			mediaType: 'Manga'
		});

		expect(screen.getByText('Modo de Exibição')).toBeInTheDocument();
		expect(screen.getByText('Lista')).toBeInTheDocument();
		expect(screen.getByText('Grade')).toBeInTheDocument();
		expect(screen.getByText('Capítulos por página')).toBeInTheDocument();
		expect(screen.getByText('Tipo de Mídia')).toBeInTheDocument();
		expect(screen.getByText('Manga')).toBeInTheDocument();
		expect(screen.getByText('Hq')).toBeInTheDocument();
	});
});
