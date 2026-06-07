import { render, screen } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ComicPreferences from './comic-preferences.svelte';

describe('ComicPreferences', () => {
	function defaultProps() {
		return {
			state: {
				displayMode: 'Lista',
				chaptersPerPage: '100',
				mediaType: 'Manga',
				volumeViewMode: 'cover' as const
			},
			events: {
				onDisplayModeChange: vi.fn(),
				onChaptersPerPageChange: vi.fn(),
				onMediaTypeChange: vi.fn(),
				onVolumeViewModeChange: vi.fn()
			}
		};
	}

	it('renderiza as opções de preferência', () => {
		render(ComicPreferences, { props: defaultProps() });

		expect(screen.getByText('Modo de Exibição')).toBeInTheDocument();
		expect(screen.getByText('Lista')).toBeInTheDocument();
		expect(screen.getByText('Grade')).toBeInTheDocument();
		expect(screen.getByText('Capítulos por página')).toBeInTheDocument();
		expect(screen.getByText('Tipo de Mídia')).toBeInTheDocument();
		expect(screen.getByText('Mangá')).toBeInTheDocument();
		expect(screen.getByText('HQ')).toBeInTheDocument();
	});

	it('oculta preferência de volume quando não existe estrutura de volume', () => {
		render(ComicPreferences, { props: defaultProps() });

		expect(screen.queryByText('Destaque do Volume')).not.toBeInTheDocument();
		expect(screen.queryByRole('radio', { name: 'Capa' })).not.toBeInTheDocument();
	});

	it('exibe preferência de volume quando existe estrutura de volume', () => {
		render(ComicPreferences, {
			props: {
				...defaultProps(),
				data: { hasVolumeStructure: true }
			}
		});

		expect(screen.getByText('Destaque do Volume')).toBeInTheDocument();
		expect(screen.getByRole('radio', { name: 'Capa' })).toBeInTheDocument();
		expect(screen.getByRole('radio', { name: 'Banner' })).toBeInTheDocument();
	});

	it('altera modo de exibição ao clicar em grade', async () => {
		const user = userEvent.setup();
		const props = defaultProps();
		render(ComicPreferences, { props });

		await user.click(screen.getByRole('radio', { name: 'Grade' }));

		expect(props.events.onDisplayModeChange).toHaveBeenCalledWith('Grade');
	});

	it('altera destaque de volume ao clicar em banner', async () => {
		const user = userEvent.setup();
		const props = defaultProps();
		render(ComicPreferences, {
			props: {
				...props,
				data: { hasVolumeStructure: true }
			}
		});

		await user.click(screen.getByRole('radio', { name: 'Banner' }));

		expect(props.events.onVolumeViewModeChange).toHaveBeenCalledWith('banner');
	});

	it('altera tipo de mídia ao clicar em webtoon', async () => {
		const user = userEvent.setup();
		const props = defaultProps();
		render(ComicPreferences, { props });

		await user.click(screen.getByRole('radio', { name: 'Webtoon' }));

		expect(props.events.onMediaTypeChange).toHaveBeenCalledWith('Webtoon');
	});

	it('altera capítulos por página pelo select', async () => {
		const user = userEvent.setup();
		const props = defaultProps();
		render(ComicPreferences, { props });

		await user.click(screen.getByText('100'));
		await user.click(await screen.findByText('50'));

		expect(props.events.onChaptersPerPageChange).toHaveBeenCalledWith('50');
	});

	it('preserva preferência controlada entre renders', async () => {
		const props = defaultProps();
		const { rerender } = render(ComicPreferences, { props });

		expect(screen.getByText('100')).toBeInTheDocument();

		await rerender({
			...props,
			state: {
				...props.state,
				chaptersPerPage: '25',
				mediaType: 'Webtoon'
			}
		});

		expect(screen.getByText('25')).toBeInTheDocument();
		expect(screen.getByRole('radio', { name: 'Webtoon' })).toBeInTheDocument();
	});
});
