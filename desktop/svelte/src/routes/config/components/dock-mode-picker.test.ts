import { render, screen } from '@testing-library/svelte';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import DockModePicker from './dock-mode-picker.svelte';

describe('DockModePicker', () => {
	it('renderiza os dois modos disponíveis', () => {
		render(DockModePicker, {
			props: {
				data: { mode: 'fixed' },
				events: { onSelect: vi.fn() }
			}
		});

		expect(screen.getByText('Fixa')).toBeInTheDocument();
		expect(screen.getByText('Com gesto')).toBeInTheDocument();
	});

	it('chama onSelect com o id correto ao clicar num modo', async () => {
		const user = userEvent.setup();
		const onSelect = vi.fn();

		render(DockModePicker, {
			props: {
				data: { mode: 'fixed' },
				events: { onSelect }
			}
		});
		await user.click(screen.getByText('Com gesto').closest('button')!);

		expect(onSelect).toHaveBeenCalledOnce();
		expect(onSelect).toHaveBeenCalledWith('hover');
	});

	it('aplica estilo de selecionado no modo ativo', () => {
		const { container } = render(DockModePicker, {
			props: {
				data: { mode: 'hover' },
				events: { onSelect: vi.fn() }
			}
		});

		const buttons = container.querySelectorAll('button');
		const hoverBtn = Array.from(buttons).find((b) => b.textContent?.includes('Com gesto'));

		expect(hoverBtn?.className).toContain('border-primary');
	});

	it('não aplica estilo de selecionado no modo inativo', () => {
		const { container } = render(DockModePicker, {
			props: {
				data: { mode: 'hover' },
				events: { onSelect: vi.fn() }
			}
		});

		const buttons = container.querySelectorAll('button');
		const fixedBtn = Array.from(buttons).find((b) => b.textContent?.includes('Fixa'));

		expect(fixedBtn?.className).not.toContain('border-primary');
	});
});
