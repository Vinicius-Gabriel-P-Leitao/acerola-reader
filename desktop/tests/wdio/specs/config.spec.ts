import {
	exactTextSelector,
	firstDisplayed,
	getPathname,
	navigateTo,
	waitForAppReady,
	waitForText
} from '../helpers/app';

async function currentTheme() {
	return browser.execute(() => document.documentElement.getAttribute('data-theme') ?? '');
}

async function openConfig() {
	await waitForAppReady();
	await navigateTo('/config');
	await waitForText('Configurações');
}

describe('config nativa', () => {
	it('renderiza seções e controles principais', async () => {
		await openConfig();

		await waitForText('Personalize sua experiência de leitura.');
		await waitForText('Configuração dos Arquivos');
		await waitForText('Pasta dos quadrinhos');
		await waitForText('Gerar ComicInfo.xml');
		await waitForText('Biblioteca');
		await waitForText('Sincronização rápida');
		await waitForText('Sincronização profunda');
		await waitForText('Aparência');
		await waitForText('Catppuccin');
		await waitForText('Nord');
		await waitForText('Dracula');
		await waitForText('Configuração de metadados');
		await waitForText('Idioma');
		await waitForText('Sincronização com MangaDex');
		await waitForText('Sincronização com AniList');
	});

	it('troca tema visual e mantém o app na tela de configurações', async () => {
		await openConfig();

		await (await waitForText('Nord')).click();
		await browser.waitUntil(async () => (await currentTheme()).startsWith('nord'), {
			timeout: 5_000,
			interval: 100,
			timeoutMsg: 'Tema Nord não foi aplicado no documento.'
		});

		await (await waitForText('Dracula')).click();
		await browser.waitUntil(async () => ['dracula', 'alucard'].includes(await currentTheme()), {
			timeout: 5_000,
			interval: 100,
			timeoutMsg: 'Tema Dracula não foi aplicado no documento.'
		});

		expect(await getPathname()).toBe('/config');

		await (await waitForText('Catppuccin')).click();
	});

	it('abre seletor de idioma da sidebar sem alterar a rota', async () => {
		await openConfig();

		const localeTrigger = await firstDisplayed('[data-slot="select-trigger"]');
		await localeTrigger.click();

		await waitForText('English');
		await waitForText('Português');
		await browser.keys(['Escape']);

		expect(await getPathname()).toBe('/config');
	});

	it('abre popover de idiomas de metadados', async () => {
		await openConfig();

		const languageButton = await firstDisplayed(
			`${exactTextSelector('Idioma')}/ancestor::*[@data-slot="item"][1]//button`
		);
		await languageButton.click();

		await waitForText('Português (BR)');
		await waitForText('English');
		await waitForText('日本語');
		await browser.keys(['Escape']);

		expect(await getPathname()).toBe('/config');
	});
});
