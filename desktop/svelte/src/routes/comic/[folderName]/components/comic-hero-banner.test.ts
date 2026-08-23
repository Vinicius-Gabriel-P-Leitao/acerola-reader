import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import ComicHeroBanner from './comic-hero-banner.svelte';

describe('ComicHeroBanner', () => {
	it('renderiza imagem do banner quando a url é fornecida', () => {
		const { container } = render(ComicHeroBanner, {
			props: { data: { banner: 'https://test.com/banner.jpg' } }
		});
		const img = container.querySelector('img');
		expect(img).toBeInTheDocument();
		expect(img?.getAttribute('src')).toBe('https://test.com/banner.jpg');
	});

	it('renderiza placeholder de ícone quando o banner não é fornecido', () => {
		const { container } = render(ComicHeroBanner, { props: { data: { banner: null } } });
		const img = container.querySelector('img');
		expect(img).not.toBeInTheDocument();
	});

	it('exibe a nota real quando fornecida', () => {
		render(ComicHeroBanner, { props: { data: { banner: null, rating: 9.8 } } });
		expect(screen.getByText('9.8')).toBeInTheDocument();
	});

	it('oculta o badge de nota quando não há rating, mas mostra o total de capítulos', () => {
		render(ComicHeroBanner, { props: { data: { banner: null, rating: null, chapterCount: 10 } } });
		expect(screen.queryByText('9.8')).not.toBeInTheDocument();
		expect(screen.getByText('10 Caps')).toBeInTheDocument();
	});
});
