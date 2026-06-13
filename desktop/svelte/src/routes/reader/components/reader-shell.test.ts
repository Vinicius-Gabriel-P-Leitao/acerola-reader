import { render, screen } from '@testing-library/svelte';
import { createRawSnippet } from 'svelte';
import { describe, expect, it } from 'vitest';
import ReaderShell from './reader-shell.svelte';

function snippet(testId: string, label: string) {
	return createRawSnippet(() => ({
		render: () => `<div data-testid="${testId}">${label}</div>`
	}));
}

function props(overrides = {}) {
	return {
		toolbar: snippet('toolbar', 'Toolbar'),
		viewport: snippet('viewport', 'Viewport'),
		footer: snippet('footer', 'Footer'),
		...overrides
	};
}

describe('ReaderShell', () => {
	it('renderiza toolbar, viewport e footer obrigatorios', () => {
		const { container } = render(ReaderShell, { props: props() });

		expect(screen.getByTestId('toolbar')).toHaveTextContent('Toolbar');
		expect(screen.getByTestId('viewport')).toHaveTextContent('Viewport');
		expect(screen.getByTestId('footer')).toHaveTextContent('Footer');
		expect(container.firstElementChild?.className).toContain('fixed');
		expect(container.firstElementChild?.className).toContain('flex-col');
	});

	it('renderiza command opcional quando informado', () => {
		render(ReaderShell, {
			props: props({
				command: snippet('command', 'Command')
			})
		});

		expect(screen.getByTestId('command')).toHaveTextContent('Command');
	});

	it('nao renderiza command quando nao informado', () => {
		render(ReaderShell, { props: props() });

		expect(screen.queryByTestId('command')).not.toBeInTheDocument();
	});
});
