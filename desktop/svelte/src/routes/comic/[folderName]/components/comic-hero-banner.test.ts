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
		// Verifica se os elementos das estatísticas ainda estão lá
		expect(screen.getByText('9.8')).toBeInTheDocument();
		expect(screen.getByText('Popular')).toBeInTheDocument();
	});
});
