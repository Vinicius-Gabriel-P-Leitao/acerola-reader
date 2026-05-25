import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ComicMetadataPanel from './comic-metadata-panel.svelte';

describe('ComicMetadataPanel', () => {
	const defaultProps = {
		title: 'Test Manga',
		author: 'Test Author',
		category: 'Manga',
		chaptersCount: 15,
		description: 'Test description',
		cover: 'https://test.com/cover.jpg',
		onBack: vi.fn()
	};

	it('renderiza o título, autor, categoria e contagem de capítulos', () => {
		render(ComicMetadataPanel, defaultProps);
		expect(screen.getByRole('heading', { name: 'Test Manga', level: 1 })).toBeInTheDocument();
		expect(screen.getByText('Test Author')).toBeInTheDocument();
		expect(screen.getByText('Manga')).toBeInTheDocument();
		expect(screen.getByText('15 Caps')).toBeInTheDocument();
		expect(screen.getByText('Test description')).toBeInTheDocument();
	});

	it('chama onBack quando o botão de voltar é clicado', async () => {
		const user = userEvent.setup();
		const onBack = vi.fn();
		const { container } = render(ComicMetadataPanel, { ...defaultProps, onBack });

		const backButton = container.querySelector('button');
		expect(backButton).toBeInTheDocument();
		if (backButton) {
			await user.click(backButton);
			expect(onBack).toHaveBeenCalledOnce();
		}
	});
});
