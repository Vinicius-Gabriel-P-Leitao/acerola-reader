import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import AcerolaBookmarkRibbon from './acerola-bookmark-ribbon.svelte';

describe('AcerolaBookmarkRibbon', () => {
	it('should render correctly with a valid color', () => {
		const { container } = render(AcerolaBookmarkRibbon, { color: 0xFFF44336 });
		
		const div = container.querySelector('div');
		expect(div).not.toBeNull();
		// O componente converte 0xFFF44336 para #f44336 e o aplica como background-color
		expect(div?.style.backgroundColor).toBe('rgb(244, 67, 54)'); // O navegador converte HEX para RGB na style property
	});

	it('should parse colors safely even with full alpha', () => {
		const { container } = render(AcerolaBookmarkRibbon, { color: 0xFFFFFFFF });
		
		const div = container.querySelector('div');
		expect(div).not.toBeNull();
		expect(div?.style.backgroundColor).toBe('rgb(255, 255, 255)');
	});

	it('should apply additional classes passed to the component', () => {
		const { container } = render(AcerolaBookmarkRibbon, { color: 0xFF000000, class: 'test-class' });
		
		const div = container.querySelector('div');
		expect(div?.classList.contains('test-class')).toBe(true);
	});
});
