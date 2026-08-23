import {
	expandDock,
	firstDisplayed,
	getPathname,
	isTextDisplayed,
	navigateTo,
	waitForAppReady,
	waitForText
} from '../helpers/app';

describe('native home', () => {
	it('renders navigation, search and library state without freezing', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		await waitForText('Acerola');

		// O dock só expõe os links depois de expandido (modo hover) — os
		// rótulos "Início"/"Histórico"/"Configurações" só existem como
		// aria-label, não como texto visível (ver acerola-dock.svelte).
		await expandDock();
		expect(await (await browser.$('[aria-label="Início"]')).isExisting()).toBe(true);
		expect(await (await browser.$('[aria-label="Histórico"]')).isExisting()).toBe(true);
		expect(await (await browser.$('[aria-label="Configurações"]')).isExisting()).toBe(true);

		// Abre o dialog de busca
		const searchButton = await firstDisplayed('button[aria-label="Buscar quadrinho..."]');
		await searchButton.click();

		// Verifica se o dialog de busca abriu
		await browser.waitUntil(
			async () => {
				const searchDialog = await browser.$('[role="dialog"]');
				return searchDialog.isDisplayed();
			},
			{
				timeout: 5_000,
				interval: 100,
				timeoutMsg: 'Dialog de busca não abriu.'
			}
		);

		expect(await getPathname()).toBe('/home');

		await browser.waitUntil(
			async () => {
				const hasEmptyState = await isTextDisplayed('Nenhum quadrinho encontrado.');
				const comicCards = await browser.$$('main h3');
				const hasComicCard = (await comicCards.length) > 0;
				const hasLoading = await isTextDisplayed('Carregando...');

				return hasEmptyState || hasComicCard || hasLoading;
			},
			{
				timeout: 10_000,
				interval: 100,
				timeoutMsg: 'Home não exibiu loading, estado vazio ou cards.'
			}
		);
	});

	it('navigates between Home and Settings via sidebar', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		await expandDock();
		await (await firstDisplayed('a[href="/config"]')).click();
		await browser.waitUntil(async () => (await getPathname()) === '/config', {
			timeout: 10_000,
			interval: 100,
			timeoutMsg: 'Sidebar não navegou para Configurações.'
		});
		await waitForText('Configurações');

		await expandDock();
		await (await firstDisplayed('a[href="/home"]')).click();
		await browser.waitUntil(async () => (await getPathname()) === '/home', {
			timeout: 10_000,
			interval: 100,
			timeoutMsg: 'Sidebar não voltou para Home.'
		});
		expect(await (await browser.$('[aria-label="Início"]')).isExisting()).toBe(true);
	});
});
