import { render, screen, fireEvent } from '@testing-library/svelte';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import HistoryIcon from '@lucide/svelte/icons/history';
import HouseIcon from '@lucide/svelte/icons/house';
import AcerolaDock from './acerola-dock.svelte';

vi.mock('$app/state', () => ({
	page: { url: new URL('http://localhost/home') }
}));

// jsdom não roda animações CSS/WAAPI de verdade, então as transições in/out do Svelte
// (fade/scale) nunca terminam sozinhas em teste — sem esse mock, a remoção do item do
// DOM ao colapsar a dock fica presa aguardando um "animationend" que nunca chega.
vi.mock('svelte/transition', () => ({
	fade: () => ({ duration: 0 }),
	scale: () => ({ duration: 0 })
}));

const items = [
	{ href: '/home', label: 'Home', icon: HouseIcon },
	{ href: '/history', label: 'History', icon: HistoryIcon }
];

describe('AcerolaDock', () => {
	beforeEach(() => {
		vi.useFakeTimers();
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('modo fixo: renderiza os itens sempre visíveis com o href correto', () => {
		render(AcerolaDock, { props: { data: { items }, state: { mode: 'fixed' } } });

		expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/home');
		expect(screen.getByRole('link', { name: 'History' })).toHaveAttribute('href', '/history');
	});

	it('modo fixo: destaca o item ativo pela rota atual', () => {
		render(AcerolaDock, { props: { data: { items }, state: { mode: 'fixed' } } });

		expect(screen.getByRole('link', { name: 'Home' }).className).toContain('bg-primary');
		expect(screen.getByRole('link', { name: 'History' }).className).not.toContain('bg-primary');
	});

	it('modo hover: começa colapsado, sem os itens visíveis', () => {
		render(AcerolaDock, { props: { data: { items }, state: { mode: 'hover' } } });

		expect(screen.queryByRole('link', { name: 'Home' })).not.toBeInTheDocument();
	});

	it('modo hover: expande ao passar o mouse', async () => {
		render(AcerolaDock, { props: { data: { items }, state: { mode: 'hover' } } });

		await fireEvent.mouseEnter(screen.getByRole('navigation'));

		expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
	});

	it('modo hover: colapsa de volta após o mouse sair', async () => {
		render(AcerolaDock, { props: { data: { items }, state: { mode: 'hover' } } });

		await fireEvent.mouseEnter(screen.getByRole('navigation'));
		expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();

		await fireEvent.mouseLeave(screen.getByRole('navigation'));
		await vi.advanceTimersByTimeAsync(300);

		expect(screen.queryByRole('link', { name: 'Home' })).not.toBeInTheDocument();
	});
});
