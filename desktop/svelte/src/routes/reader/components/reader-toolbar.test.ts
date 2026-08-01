import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ReaderToolbar from './reader-toolbar.svelte';

function props(overrides = {}) {
	return {
		data: {
			title: 'Chapter 12',
			subtitle: 'Volume 2',
			zoomLevel: 1,
			zoomMode: false,
			isPaginatedMode: true,
			pageControlsDisabled: false,
			canPreviousPage: true,
			canNextPage: true
		},
		state: {
			readingMode: 'vertical'
		},
		events: {
			onBack: vi.fn(),
			onReadingModeChange: vi.fn(),
			onToggleQuickZoom: vi.fn(),
			onToggleZoomMode: vi.fn(),
			onOpenCommandPalette: vi.fn(),
			onPreviousPage: vi.fn(),
			onNextPage: vi.fn()
		},
		...overrides
	} as const;
}

describe('ReaderToolbar', () => {
	it('renderiza titulo, subtitulo e controles paginados', () => {
		render(ReaderToolbar, { props: props() });

		expect(screen.getByText('Chapter 12')).toBeInTheDocument();
		expect(screen.getByText('Volume 2')).toBeInTheDocument();
		expect(screen.getAllByRole('button')).toHaveLength(6);
	});

	it('nao renderiza subtitulo quando nao informado', () => {
		render(ReaderToolbar, {
			props: props({
				data: {
					...props().data,
					subtitle: undefined
				}
			})
		});

		expect(screen.getByText('Chapter 12')).toBeInTheDocument();
		expect(screen.queryByText('Volume 2')).not.toBeInTheDocument();
	});

	it('chama eventos dos botoes principais', async () => {
		const user = userEvent.setup();
		const toolbarProps = props();

		render(ReaderToolbar, { props: toolbarProps });

		await user.click(screen.getByTitle('Voltar'));
		await user.click(screen.getByTitle(/Aplicar zoom/));
		await user.click(screen.getByTitle(/Modo zoom/));
		await user.click(screen.getByTitle(/Comandos/));
		await user.click(screen.getByTitle(/Página anterior/));
		await user.click(screen.getByTitle(/Próxima página/));

		expect(toolbarProps.events.onBack).toHaveBeenCalledOnce();
		expect(toolbarProps.events.onToggleQuickZoom).toHaveBeenCalledOnce();
		expect(toolbarProps.events.onToggleZoomMode).toHaveBeenCalledOnce();
		expect(toolbarProps.events.onOpenCommandPalette).toHaveBeenCalledOnce();
		expect(toolbarProps.events.onPreviousPage).toHaveBeenCalledOnce();
		expect(toolbarProps.events.onNextPage).toHaveBeenCalledOnce();
	});

	it('chama mudanca de modo de leitura pelo toggle desktop', async () => {
		const user = userEvent.setup();
		const toolbarProps = props();

		render(ReaderToolbar, { props: toolbarProps });

		await user.click(screen.getByTitle('Paginado horizontal'));
		await user.click(screen.getByTitle('Webtoon'));

		expect(toolbarProps.events.onReadingModeChange).toHaveBeenCalledWith('horizontal');
		expect(toolbarProps.events.onReadingModeChange).toHaveBeenCalledWith('webtoon');
	});

	it('mostra acao de reset quando zoom esta ativo', async () => {
		const user = userEvent.setup();
		const toolbarProps = props({
			data: {
				...props().data,
				zoomLevel: 1.5
			}
		});

		render(ReaderToolbar, { props: toolbarProps });

		await user.click(screen.getByTitle(/Resetar zoom/));

		expect(toolbarProps.events.onToggleQuickZoom).toHaveBeenCalledOnce();
		expect(screen.queryByTitle(/Aplicar zoom/)).not.toBeInTheDocument();
	});

	it('oculta navegacao de paginas fora do modo paginado', () => {
		render(ReaderToolbar, {
			props: props({
				data: {
					...props().data,
					isPaginatedMode: false
				}
			})
		});

		expect(screen.queryByTitle(/Página anterior/)).not.toBeInTheDocument();
		expect(screen.queryByTitle(/Próxima página/)).not.toBeInTheDocument();
	});

	it('desabilita navegacao quando paginas estao travadas por zoom', () => {
		render(ReaderToolbar, {
			props: props({
				data: {
					...props().data,
					pageControlsDisabled: true
				}
			})
		});

		expect(screen.getAllByTitle('Desative o zoom para trocar de página')).toHaveLength(2);
		expect(screen.getAllByTitle('Desative o zoom para trocar de página')[0]).toBeDisabled();
		expect(screen.getAllByTitle('Desative o zoom para trocar de página')[1]).toBeDisabled();
	});

	it('desabilita apenas botoes sem pagina anterior ou proxima', () => {
		render(ReaderToolbar, {
			props: props({
				data: {
					...props().data,
					canPreviousPage: false,
					canNextPage: true
				}
			})
		});

		expect(screen.getByTitle(/Página anterior/)).toBeDisabled();
		expect(screen.getByTitle(/Próxima página/)).not.toBeDisabled();
	});
});
