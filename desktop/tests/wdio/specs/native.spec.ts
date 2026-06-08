import { existsSync } from 'node:fs';
import path from 'node:path';
import { getTitle, navigateTo, waitForTauriReady } from '../helpers/app';

const readerFixturePath = path.resolve('tests/wdio/fixtures/reader.cbz');

describe('acerola nativo via WebDriverIO', () => {
	it('inicializa o app sem crash', async () => {
		await waitForTauriReady();

		const title = await getTitle();
		expect(title).not.toBe('');

		const url = await browser.getUrl();
		expect(url).toMatch(/tauri:\/\/localhost|localhost/i);
	});

	it('aciona seleção de pasta sem travar o app', async () => {
		await waitForTauriReady();
		await navigateTo('/config');

		const selectFolderButton = await browser.$('//*[normalize-space()="Pasta dos quadrinhos"]');
		await selectFolderButton.waitForDisplayed({ timeout: 5_000 });

		// WebDriver não controla o dialog nativo de pasta; este teste valida que o IPC real foi
		// disparado sem travar o app. O dialog deve ser fechado manualmente ou por Escape.
		await selectFolderButton.click();
		await browser.keys('Escape').catch(() => undefined);

		await waitForTauriReady();
		expect(await getTitle()).not.toBe('');
	});

	it('carrega imagem pelo protocolo asset ou tauri', async () => {
		expect(
			existsSync(readerFixturePath),
			`Fixture CBZ ausente. Crie ${readerFixturePath} antes de rodar este teste.`
		).toBe(true);

		await waitForTauriReady();
		await navigateTo('/reader');

		const image = await browser.$('img');
		await image.waitForDisplayed({ timeout: 10_000 });

		const src = await image.getAttribute('src');
		expect(src).toMatch(/asset:\/\/|tauri:\/\//i);
	});

	it('aciona controle nativo de minimizar pela titlebar', async () => {
		await waitForTauriReady();
		await navigateTo('/home');

		const beforeRect = await browser.getWindowRect();
		const minimizeButton = await browser.$('[aria-label="Minimizar"]');
		await minimizeButton.waitForDisplayed({ timeout: 5_000 });

		await minimizeButton.click();
		await browser.pause(500);

		const visibilityState = await browser.execute(() => document.visibilityState);
		const afterRect = await browser.getWindowRect();

		expect(visibilityState !== 'visible' || afterRect.height !== beforeRect.height).toBe(true);
	});
});
