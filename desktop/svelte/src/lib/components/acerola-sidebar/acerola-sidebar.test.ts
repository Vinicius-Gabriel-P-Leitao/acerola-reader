import { render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import HistoryIcon from '@lucide/svelte/icons/history';
import HouseIcon from '@lucide/svelte/icons/house';
import AcerolaSidebar from './acerola-sidebar.svelte';

vi.mock('$app/state', () => ({
	page: { url: new URL('http://localhost/home') }
}));

const items = [
	{ href: '/home', label: 'Home', icon: HouseIcon },
	{ href: '/history', label: 'History', icon: HistoryIcon }
];

describe('AcerolaSidebar', () => {
	it('renderiza os itens sempre visíveis com o href correto', () => {
		render(AcerolaSidebar, { props: { data: { items } } });

		expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/home');
		expect(screen.getByRole('link', { name: 'History' })).toHaveAttribute('href', '/history');
	});

	it('destaca o item ativo pela rota atual', () => {
		render(AcerolaSidebar, { props: { data: { items } } });

		expect(screen.getByRole('link', { name: 'Home' }).className).toContain('bg-primary');
		expect(screen.getByRole('link', { name: 'History' }).className).not.toContain('bg-primary');
		expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('aria-current', 'page');
		expect(screen.getByRole('link', { name: 'History' })).not.toHaveAttribute('aria-current');
	});

	it('renderiza a navegação sempre visível, sem estado colapsado', () => {
		render(AcerolaSidebar, { props: { data: { items } } });

		expect(screen.getByRole('navigation')).toBeInTheDocument();
		expect(screen.getAllByRole('link')).toHaveLength(items.length);
	});
});
