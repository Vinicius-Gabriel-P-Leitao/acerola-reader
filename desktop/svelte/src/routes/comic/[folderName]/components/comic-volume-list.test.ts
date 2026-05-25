import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ComicVolumeList from './comic-volume-list.svelte';

// Mock animate for Svelte transitions in JSDOM
if (typeof window !== 'undefined' && !window.HTMLElement.prototype.animate) {
	window.HTMLElement.prototype.animate = vi.fn().mockReturnValue({
		finished: Promise.resolve(),
		cancel: vi.fn(),
		pause: vi.fn(),
		play: vi.fn(),
		reverse: vi.fn(),
		onfinish: null,
		oncancel: null
	});
}

describe('ComicVolumeList', () => {
	const volumes = [
		{
			id: 'v1',
			title: 'Volume 1',
			chapters: [{ id: 'c1', title: 'Capítulo 1', date: '12 Out', isRead: true }]
		}
	];

	it('renderiza a lista de volumes quando fornecida', () => {
		render(ComicVolumeList, { volumes });

		expect(screen.getByText('Volume 1')).toBeInTheDocument();
		expect(screen.getByText('1 Capítulos inclusos')).toBeInTheDocument();
	});

	it('expande o volume ao clicar para mostrar os capítulos', async () => {
		const user = userEvent.setup();
		render(ComicVolumeList, { volumes });

		const volumeBtn = screen.getByText('Volume 1').closest("[data-slot='item']");
		expect(volumeBtn).toBeInTheDocument();

		if (volumeBtn) {
			await user.click(volumeBtn);
			expect(screen.getByText('Capítulo 1')).toBeInTheDocument();
		}
	});

	it('renderiza empty state quando a lista está vazia', () => {
		render(ComicVolumeList, { volumes: [] });

		expect(screen.getByText('Nenhum volume indexado ainda.')).toBeInTheDocument();
	});
});
