import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import ComicPreferences from './comic-preferences.svelte';

describe('ComicPreferences', () => {
	it('renderiza as opções de preferência', () => {
		render(ComicPreferences, {
			props: {
				state: {
					displayMode: 'Lista',
					chaptersPerPage: '100',
					mediaType: 'Manga',
					volumeViewMode: 'cover'
				},
				events: {
					onDisplayModeChange: () => {},
					onChaptersPerPageChange: () => {},
					onMediaTypeChange: () => {},
					onVolumeViewModeChange: () => {}
				}
			}
		});

		expect(screen.getByText('Modo de Exibição')).toBeInTheDocument();
		expect(screen.getByText('Lista')).toBeInTheDocument();
		expect(screen.getByText('Grade')).toBeInTheDocument();
		expect(screen.getByText('Capítulos por página')).toBeInTheDocument();
		expect(screen.getByText('Tipo de Mídia')).toBeInTheDocument();
		expect(screen.getByText('Mangá')).toBeInTheDocument();
		expect(screen.getByText('HQ')).toBeInTheDocument();
	});
});
