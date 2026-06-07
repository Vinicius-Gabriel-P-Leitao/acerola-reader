import { render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import ReaderPages from './reader-pages.svelte';
import type { ReaderPageTracker } from './reader-pages.svelte';

function trackPage(): ReaderPageTracker {
	return vi.fn(() => ({
		destroy: vi.fn()
	}));
}

function page(index: number) {
	return {
		index,
		total: 5,
		mimeType: 'image/jpeg',
		url: `blob:page-${index + 1}`,
		cacheHit: false
	};
}

function props(overrides = {}) {
	return {
		data: {
			mode: 'vertical',
			pageCount: 3,
			currentPage: 1,
			openFailed: false,
			chapterAvailable: true
		},
		services: {
			pageAt: (index: number) => page(index),
			trackPage: trackPage()
		},
		...overrides
	} as const;
}

describe('ReaderPages', () => {
	it('renderiza mensagem quando capitulo esta indisponivel', () => {
		render(ReaderPages, {
			props: props({
				data: {
					...props().data,
					mode: 'vertical',
					pageCount: 0,
					currentPage: 0,
					openFailed: true,
					chapterAvailable: false
				},
				services: {
					pageAt: vi.fn(),
					trackPage: trackPage()
				}
			})
		});

		expect(screen.getByText('Capítulo indisponível')).toBeInTheDocument();
	});

	it('prioriza fallback de indisponivel sobre loading', () => {
		render(ReaderPages, {
			props: props({
				data: {
					...props().data,
					pageCount: 0,
					openFailed: false,
					chapterAvailable: false
				}
			})
		});

		expect(screen.getByText('Capítulo indisponível')).toBeInTheDocument();
		expect(screen.queryByRole('img')).not.toBeInTheDocument();
	});

	it('renderiza loading quando capitulo esta disponivel sem paginas', () => {
		const { container } = render(ReaderPages, {
			props: props({
				data: {
					...props().data,
					pageCount: 0
				}
			})
		});

		expect(container.querySelector('.animate-spin')).toBeInTheDocument();
		expect(container.querySelector('section')).not.toBeInTheDocument();
	});

	it('renderiza uma secao por pagina e aplica rastreamento', () => {
		const tracker = trackPage();

		const { container } = render(ReaderPages, {
			props: props({
				services: {
					pageAt: (index: number) => (index === 1 ? page(index) : undefined),
					trackPage: tracker
				},
				data: {
					...props().data,
					mode: 'horizontal',
					pageCount: 3,
					currentPage: 1
				}
			})
		});

		const sections = container.querySelectorAll('section');
		expect(sections).toHaveLength(3);
		expect(tracker).toHaveBeenCalledTimes(3);
		expect(screen.getByRole('img', { name: 'Página 2' })).toHaveAttribute('src', 'blob:page-2');
		expect(container.querySelectorAll('.animate-spin')).toHaveLength(2);
	});

	it('aplica classes corretas para cada modo de leitura', () => {
		const cases = [
			{ mode: 'horizontal', containerClass: 'w-max', sectionClass: 'w-screen' },
			{ mode: 'vertical', containerClass: 'max-w-6xl', sectionClass: 'snap-center' },
			{ mode: 'webtoon', containerClass: 'max-w-3xl', sectionClass: 'items-start' }
		];

		for (const entry of cases) {
			const { container, unmount } = render(ReaderPages, {
				props: props({
					data: {
						...props().data,
						mode: entry.mode
					}
				})
			});

			expect(container.firstElementChild?.className).toContain(entry.containerClass);
			expect(container.querySelector('section')?.className).toContain(entry.sectionClass);
			unmount();
		}
	});

	it('marca imagens proximas como eager e distantes como lazy', () => {
		render(ReaderPages, {
			props: props({
				data: {
					...props().data,
					pageCount: 5,
					currentPage: 0
				}
			})
		});

		expect(screen.getByRole('img', { name: 'Página 1' })).toHaveAttribute('loading', 'eager');
		expect(screen.getByRole('img', { name: 'Página 3' })).toHaveAttribute('loading', 'eager');
		expect(screen.getByRole('img', { name: 'Página 4' })).toHaveAttribute('loading', 'lazy');
	});

	it('usa placeholder de webtoon para paginas ausentes', () => {
		const { container } = render(ReaderPages, {
			props: props({
				data: {
					...props().data,
					mode: 'webtoon',
					pageCount: 1
				},
				services: {
					pageAt: () => undefined,
					trackPage: trackPage()
				}
			})
		});

		expect(container.querySelector('.min-h-\\[64vh\\]')).toBeInTheDocument();
	});
});
