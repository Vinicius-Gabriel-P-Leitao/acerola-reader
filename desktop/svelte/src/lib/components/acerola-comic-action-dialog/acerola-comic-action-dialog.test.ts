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
	window.HTMLElement.prototype.animate = () => ({
		finished: Promise.resolve(),
		cancel: () => {},
		finish: () => {},
		play: () => {},
		pause: () => {},
		reverse: () => {},
		addEventListener: () => {},
		removeEventListener: () => {}
	});
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

	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('renderiza nada quando não há itens selecionados', () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		expect(screen.queryByText('selected')).not.toBeInTheDocument();
	});

	it('renderiza o dialog quando há itens selecionados', () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2, 3],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		expect(screen.getByText('3 selected')).toBeInTheDocument();
		expect(screen.getByText('Hide')).toBeInTheDocument();
		expect(screen.getByText('Delete')).toBeInTheDocument();
		expect(screen.getByText('Bookmark')).toBeInTheDocument();
	});

	it('exibe a contagem correta de itens selecionados', () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		expect(screen.getByText('2 selected')).toBeInTheDocument();
	});

	it('chama onHide quando o botão Hide é clicado', async () => {
		mockOnHide.mockResolvedValueOnce(undefined);
		
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const hideButton = screen.getByText('Hide');
		await fireEvent.click(hideButton);

		expect(mockOnHide).toHaveBeenCalledWith([1, 2]);
	});

	it('abre dialog de confirmação quando Delete é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
		await fireEvent.click(deleteButtons[0]);

		expect(screen.getByText('Delete Comics')).toBeInTheDocument();
		expect(screen.getByText(/Are you sure you want to delete/)).toBeInTheDocument();
	});

	it('chama onDelete quando confirmado no dialog', async () => {
		mockOnDelete.mockResolvedValueOnce(undefined);
		
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		// Abre o dialog
		const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
		await fireEvent.click(deleteButtons[0]);

		// Confirma a deleção - pega o botão dentro do dialog de confirmação (último botão Delete)
		const confirmButtons = screen.getAllByRole('button', { name: 'Delete' });
		await fireEvent.click(confirmButtons[confirmButtons.length - 1]);

		expect(mockOnDelete).toHaveBeenCalledWith([1, 2]);
	});

	it('exibe menu de bookmarks quando o botão Bookmark é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const bookmarkButton = screen.getByRole('button', { name: /Bookmark/ });
		await fireEvent.click(bookmarkButton);

		expect(screen.getByText('Favoritos')).toBeInTheDocument();
		expect(screen.getByText('Lendo')).toBeInTheDocument();
	});

	it('chama onBookmark com o categoryId correto', async () => {
		mockOnBookmark.mockResolvedValueOnce(undefined);
		
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		// Abre o menu de bookmarks
		const bookmarkButton = screen.getByRole('button', { name: /Bookmark/ });
		await fireEvent.click(bookmarkButton);

		// Clica em uma categoria
		const favoritosOption = screen.getByText('Favoritos');
		await fireEvent.click(favoritosOption);

		expect(mockOnBookmark).toHaveBeenCalledWith([1, 2], 1);
	});

	it('chama onClose quando o botão X é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		// Encontra o botão de fechar pelo ícone X
		const closeButtons = screen.getAllByRole('button', { name: '' });
		await fireEvent.click(closeButtons[0]);

		expect(mockOnClose).toHaveBeenCalled();
	});

	it('desabilita botões durante processamento', async () => {
		mockOnHide.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));
		
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		const hideButton = screen.getByRole('button', { name: 'Hide' });
		await fireEvent.click(hideButton);

		// Verifica se os botões estão desabilitados durante o processamento
		expect(hideButton).toBeDisabled();
	});

	it('cancela dialog de delete quando Cancel é clicado', async () => {
		render(AcerolaComicActionDialog, {
			props: {
				selectedIds: [1, 2],
				bookmarks: mockBookmarks,
				onHide: mockOnHide,
				onDelete: mockOnDelete,
				onBookmark: mockOnBookmark,
				onClose: mockOnClose
			}
		});

		// Abre o dialog
		const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
		await fireEvent.click(deleteButtons[0]);

		// Clica em Cancel
		const cancelButton = screen.getByRole('button', { name: 'Cancel' });
		await fireEvent.click(cancelButton);

		expect(mockOnDelete).not.toHaveBeenCalled();
	});
});
