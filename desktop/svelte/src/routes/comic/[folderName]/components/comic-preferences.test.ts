import { render, screen } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ComicPreferences from './comic-preferences.svelte';

describe('ComicPreferences', () => {
	function defaultProps() {
		return {
			state: {
				volumeViewMode: 'cover' as const,
				bookmarkId: null,
				externalSyncEnabled: true
			},
			events: {
				onVolumeViewModeChange: vi.fn(),
				onBookmarkChange: vi.fn(),
				onExternalSyncChange: vi.fn()
			}
		};
	}

	it('renderiza as opções de preferência', () => {
		render(ComicPreferences, { props: defaultProps() });

		expect(screen.getByText('Leitura')).toBeInTheDocument();
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
});
