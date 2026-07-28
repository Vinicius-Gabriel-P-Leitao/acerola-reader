import {
	firstDisplayed,
	getPathname,
	isTextDisplayed,
	navigateTo,
	waitForAppReady,
	waitForText
} from '../helpers/app';

describe('home nativa', () => {
	it('renderiza navegação, busca e estado da biblioteca sem travar', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		await waitForText('Acerola');
		await waitForText('Início');
		await waitForText('Histórico');
		await waitForText('Configurações');

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

	it('navega entre Home e Configurações pela sidebar', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		await (await firstDisplayed('a[href="/config"]')).click();
		await browser.waitUntil(async () => (await getPathname()) === '/config', {
			timeout: 10_000,
			interval: 100,
			timeoutMsg: 'Sidebar não navegou para Configurações.'
		});
		await waitForText('Configurações');

		await (await firstDisplayed('a[href="/home"]')).click();
		await browser.waitUntil(async () => (await getPathname()) === '/home', {
			timeout: 10_000,
			interval: 100,
			timeoutMsg: 'Sidebar não voltou para Home.'
		});
		await waitForText('Início');
	});
});
