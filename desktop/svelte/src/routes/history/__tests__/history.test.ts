import { render, screen, waitFor } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HistoryPage from '../+page.svelte';

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

const { mockInvoke } = vi.hoisted(() => ({
	mockInvoke: vi.fn()
}));

vi.mock('@tauri-apps/api/core', () => ({
	invoke: (cmd: string, args: any) => mockInvoke(cmd, args),
	convertFileSrc: (path: string) => `asset://${path}`
}));

vi.mock('$lib/assets/placeholder/placeholder_manga.svg?component', () => ({
	default: () => ''
}));

const mockHistoryData = [
	{
		comicDirectoryId: '1',
		chapterArchiveId: '10',
		lastPage: 5,
		isCompleted: false,
		updatedAt: 1600000000,
		comicName: 'Comic 1',
		comicCover: '/path/to/cover1.jpg',
		chapterName: '1',
		folderName: 'Comic 1',
		chapterPath: '/path/to/chapter1.cbz',
		chapterSort: '1',
		isSpecial: false,
		lastModified: 0
	},
	{
		comicDirectoryId: '2',
		chapterArchiveId: '20',
		lastPage: 12,
		isCompleted: true,
		updatedAt: 1500000000,
		comicName: 'Comic 2',
		comicCover: null,
		chapterName: '2',
		folderName: 'Comic 2',
		chapterPath: '/path/to/chapter2.cbz',
		chapterSort: '2',
		isSpecial: false,
		lastModified: 0
	}
];

describe('HistoryPage', () => {
	beforeEach(() => {
		vi.resetAllMocks();
	});

	it('renderiza o empty state quando não há histórico', async () => {
		// Renderiza o empty state quando não houver histórico
		mockInvoke.mockResolvedValueOnce([]);
		render(HistoryPage);

		await waitFor(() => {
			expect(mockInvoke).toHaveBeenCalledWith('history_get_all', undefined);
		});
		
		expect(screen.getByText(/Nenhum histórico encontrado/i)).toBeInTheDocument();
	});

	it('renderiza itens de historico com sucesso', async () => {
		// Renderiza os itens de histórico com sucesso
		mockInvoke.mockResolvedValueOnce(mockHistoryData);
		render(HistoryPage);

		await waitFor(() => {
			expect(screen.getByText('Comic 1')).toBeInTheDocument();
			expect(screen.getByText('Comic 2')).toBeInTheDocument();
		});
	});

	it('limpa historico ao clicar no botao', async () => {
		// Limpa o histórico ao clicar no botão de limpar
		const user = userEvent.setup();
		mockInvoke.mockResolvedValueOnce(mockHistoryData);
		render(HistoryPage);

		await waitFor(() => {
			expect(screen.getByText('Comic 1')).toBeInTheDocument();
		});

		mockInvoke.mockResolvedValueOnce(undefined);
		const clearButton = screen.getByRole('button', { name: /Limpar Histórico/i });
		
		await user.click(clearButton);

		expect(mockInvoke).toHaveBeenCalledWith('history_clear', undefined);
		
		await waitFor(() => {
			expect(screen.queryByText('Comic 1')).not.toBeInTheDocument();
		});
	});
});
