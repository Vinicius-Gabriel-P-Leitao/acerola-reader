import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockStoreMethods = vi.hoisted(() => ({
	get: vi.fn(() => Promise.resolve(null)),
	set: vi.fn(() => Promise.resolve()),
	save: vi.fn(() => Promise.resolve())
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn(() => mockStoreMethods),
	LazyStore: vi.fn().mockImplementation(() => mockStoreMethods)
}));

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

import Onboarding from './onboarding.svelte';

describe('Onboarding Component', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('renderiza o primeiro passo (Boas-vindas) por padrão', () => {
		render(Onboarding);

		expect(screen.getByText('Acerola')).toBeInTheDocument();
		expect(screen.getByText('Bem-vindo ao Acerola, seu leitor de quadrinhos.')).toBeInTheDocument();
		expect(screen.getByText('Começar')).toBeInTheDocument();
	});

	it('avança para o passo de Idioma ao clicar em Começar', async () => {
		const user = userEvent.setup();
		render(Onboarding);

		await user.click(screen.getByText('Começar'));

		expect(screen.getByText('Selecionar Idioma')).toBeInTheDocument();
		expect(screen.getByText('Escolha o idioma preferido para a aplicação.')).toBeInTheDocument();
	});

	it('avança pelos passos de Idioma até Formatos ao clicar em Próximo', async () => {
		const user = userEvent.setup();
		render(Onboarding);

		await user.click(screen.getByText('Começar'));
		await user.click(screen.getByText('Próximo'));

		expect(screen.getByText('Formatos Suportados')).toBeInTheDocument();
		expect(screen.getByText('CBZ')).toBeInTheDocument();
		expect(screen.getByText('CBR')).toBeInTheDocument();
		expect(screen.getByText('PDF')).toBeInTheDocument();
	});
});
