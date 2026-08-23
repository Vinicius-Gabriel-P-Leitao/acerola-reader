import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import AcerolaInput from './acerola-input.svelte';

describe('AcerolaInput', () => {
	it('renderiza um input', () => {
		render(AcerolaInput);
		expect(screen.getByRole('textbox')).toBeInTheDocument();
	});

	it('exibe o placeholder', () => {
		render(AcerolaInput, { props: { ui: { placeholder: 'Digite algo...' } } });
		expect(screen.getByPlaceholderText('Digite algo...')).toBeInTheDocument();
	});

	it('exibe o valor inicial', () => {
		render(AcerolaInput, { props: { state: { value: 'texto inicial' } } });
		expect(screen.getByDisplayValue('texto inicial')).toBeInTheDocument();
	});

	it('fica desabilitado quando a prop disabled é passada', () => {
		render(AcerolaInput, { props: { ui: { disabled: true } } });
		expect(screen.getByRole('textbox')).toBeDisabled();
	});

	it('aceita digitação', async () => {
		const user = userEvent.setup();
		render(AcerolaInput, { props: { ui: { placeholder: 'Digite...' } } });
		const input = screen.getByPlaceholderText('Digite...');
		await user.type(input, 'quadrinhos');
		expect(input).toHaveValue('quadrinhos');
	});

	it('aplica classe rounded-lg', () => {
		render(AcerolaInput);
		expect(screen.getByRole('textbox')).toHaveClass('rounded-lg');
	});
});
