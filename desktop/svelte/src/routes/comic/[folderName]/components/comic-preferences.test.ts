import { render, screen } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ComicPreferences from './comic-preferences.svelte';

describe('ComicPreferences', () => {
	function defaultProps() {
		return {
			state: {
				chaptersPerPage: '100',
				volumeViewMode: 'cover' as const,
				bookmarkId: null
			},
			events: {
				onChaptersPerPageChange: vi.fn(),
				onVolumeViewModeChange: vi.fn(),
				onBookmarkChange: vi.fn()
			}
		};
	}

	it('renderiza as opções de preferência', () => {
		render(ComicPreferences, { props: defaultProps() });

		expect(screen.getByText('Leitura')).toBeInTheDocument();
		expect(screen.getByText('Capítulos por página')).toBeInTheDocument();
		expect(screen.getByText('Número de capítulos exibidos por página.')).toBeInTheDocument();
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
				chaptersPerPage: '25'
			}
		});

		expect(screen.getByText('25')).toBeInTheDocument();
	});
});
