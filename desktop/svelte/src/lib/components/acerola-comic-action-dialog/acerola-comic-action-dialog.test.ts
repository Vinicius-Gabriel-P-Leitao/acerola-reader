import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
import type { Category } from '$lib/contracts/bookmarks/bookmarks.payloads';

// Mock do toast
vi.mock('svelte-sonner', () => ({
	toast: {
		success: vi.fn(),
		error: vi.fn()
	}
}));

// Mock do error do Tauri
vi.mock('@tauri-apps/plugin-log', () => ({
	error: vi.fn()
}));

// Mock do element.animate para jsdom
if (!window.HTMLElement.prototype.animate) {
	window.HTMLElement.prototype.animate = function () {
		const self = {} as Animation;
		return {
			finished: Promise.resolve(self),
			cancel: () => {},
			finish: () => {},
			play: () => {},
			pause: () => {},
			reverse: () => {}
		} as Animation;
	};
}

describe('AcerolaComicActionDialog', () => {
	const mockBookmarks: Category[] = [
		{ id: 1, name: 'Favoritos', color: 0xff0000 },
		{ id: 2, name: 'Lendo', color: 0x00ff00 }
	];

	const mockOnHide = vi.fn();
	const mockOnDelete = vi.fn();
	const mockOnBookmark = vi.fn();
	const mockOnClose = vi.fn();
	const mockOnSelectAll = vi.fn();

	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('renderiza nada quando open é false', () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: false,
				selectedIds: [],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		expect(screen.queryByText(/Comic Actions|Ações do Quadrinho/i)).not.toBeInTheDocument();
	});

	it('renderiza o dialog quando open é true', () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2, 3],
				totalCount: 10,
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onSelectAll: mockOnSelectAll,
				onClose: mockOnClose
			}
		});

		expect(screen.getByText(/Comic Actions|Ações do Quadrinho/i)).toBeInTheDocument();
		expect(screen.getByText(/3 (comic\(s\) selected|quadrinho\(s\) selecionado\(s\))/i)).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Hide|Ocultar/i })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Delete|Deletar/i })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Bookmark|Adicionar Bookmark/i })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Select all|Selecionar todos/i })).toBeInTheDocument();
	});

	it('chama onSelectAll ao clicar no botão selecionar todos dentro do dialog', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1],
				totalCount: 10,
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onSelectAll: mockOnSelectAll,
				onClose: mockOnClose
			}
		});

		const selectAllBtn = screen.getByRole('button', { name: /Select all|Selecionar todos/i });
		await fireEvent.click(selectAllBtn);

		expect(mockOnSelectAll).toHaveBeenCalled();
	});

	it('abre dialog de confirmação e chama onHide quando confirmado', async () => {
		mockOnHide.mockResolvedValueOnce(undefined);

		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const hideButton = screen.getByRole('button', { name: /Hide|Ocultar/i });
		await fireEvent.click(hideButton);

		expect(screen.getByText(/Hide Comics|Ocultar Quadrinhos/i)).toBeInTheDocument();

		const confirmButtons = screen.getAllByRole('button', { name: /Hide|Ocultar/i });
		await fireEvent.click(confirmButtons[confirmButtons.length - 1]);

		expect(mockOnHide).toHaveBeenCalledWith([1, 2]);
	});

	it('cancela dialog de hide quando Cancel é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const hideButton = screen.getByRole('button', { name: /Hide|Ocultar/i });
		await fireEvent.click(hideButton);

		const cancelButton = screen.getByRole('button', { name: /Cancel|Cancelar/i });
		await fireEvent.click(cancelButton);

		expect(mockOnHide).not.toHaveBeenCalled();
	});

	it('abre dialog de confirmação quando Delete é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const deleteButton = screen.getByRole('button', { name: /Delete|Deletar/i });
		await fireEvent.click(deleteButton);

		expect(screen.getByText(/Delete Comics|Excluir Quadrinhos/i)).toBeInTheDocument();
	});

	it('chama onDelete quando confirmado no dialog', async () => {
		mockOnDelete.mockResolvedValueOnce(undefined);

		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const deleteButton = screen.getByRole('button', { name: /Delete|Deletar/i });
		await fireEvent.click(deleteButton);

		const confirmButtons = screen.getAllByRole('button', { name: /Delete|Excluir|Deletar/i });
		await fireEvent.click(confirmButtons[confirmButtons.length - 1]);

		expect(mockOnDelete).toHaveBeenCalledWith([1, 2]);
	});

	it('exibe menu de bookmarks quando o botão Bookmark é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const bookmarkButton = screen.getByRole('button', { name: /Bookmark|Adicionar Bookmark/i });
		await fireEvent.click(bookmarkButton);

		expect(screen.getByText('Favoritos')).toBeInTheDocument();
		expect(screen.getByText('Lendo')).toBeInTheDocument();
	});

	it('chama onBookmark com o categoryId correto', async () => {
		mockOnBookmark.mockResolvedValueOnce(undefined);

		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const bookmarkButton = screen.getByRole('button', { name: /Bookmark|Adicionar Bookmark/i });
		await fireEvent.click(bookmarkButton);

		const favoritosOption = screen.getByText('Favoritos');
		await fireEvent.click(favoritosOption);

		expect(mockOnBookmark).toHaveBeenCalledWith([1, 2], 1);
	});

	it('cancela dialog de delete quando Cancel é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				open: true,
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const deleteButton = screen.getByRole('button', { name: /Delete|Deletar/i });
		await fireEvent.click(deleteButton);

		const cancelButton = screen.getByRole('button', { name: /Cancel|Cancelar/i });
		await fireEvent.click(cancelButton);

		expect(mockOnDelete).not.toHaveBeenCalled();
	});
});
