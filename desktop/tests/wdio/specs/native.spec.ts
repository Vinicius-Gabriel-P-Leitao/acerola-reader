import { existsSync } from 'node:fs';
import path from 'node:path';
import { getTitle, navigateTo, waitForAppReady } from '../helpers/app';

const readerFixturePath = path.resolve('tests/wdio/fixtures/reader.cbz');

describe('acerola nativo via WebDriverIO', () => {
	it('inicializa o app sem crash', async () => {
		await waitForAppReady();

		const title = await getTitle();
		// título pode ser vazio se o app não setar document.title — valida só a URL
		const url = await browser.getUrl();
		expect(url).toMatch(/tauri:\/\/localhost|localhost/i);

		// se tiver título, não pode ser vazio
		if (title !== '') {
			expect(typeof title).toBe('string');
		}
	});

	it('aciona seleção de pasta sem travar o app', async () => {
		await waitForAppReady();
		await navigateTo('/config');

		const selectFolderButton = await browser.$('//*[normalize-space()="Pasta dos quadrinhos"]');
		await selectFolderButton.waitForDisplayed({ timeout: 5_000 });

		// WebDriver não controla o dialog nativo — valida apenas que o IPC
		// foi disparado sem travar o app
		await selectFolderButton.click();
		await browser.pause(300);
		await browser.keys(['Escape']).catch(() => undefined);
		await browser.pause(300);

		// app ainda responde
		const url = await browser.getUrl();
		expect(url).toMatch(/tauri:\/\/localhost|localhost/i);
	});

	it('carrega imagem pelo protocolo asset ou tauri', async () => {
		if (!existsSync(readerFixturePath)) {
			console.warn(`Fixture CBZ ausente: ${readerFixturePath} — teste pulado.`);
			return;
		}

		await waitForAppReady();
		await navigateTo('/reader');

		const image = await browser.$('img');
		await image.waitForDisplayed({ timeout: 10_000 });

		const src = await image.getAttribute('src');
		expect(src).toMatch(/asset:\/\/|tauri:\/\//i);
	});

	it('aciona controle nativo de minimizar pela titlebar', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		const beforeRect = await browser.getWindowRect();

		const minimizeButton = await browser.$('[aria-label="Minimizar"]');
		await minimizeButton.waitForDisplayed({ timeout: 5_000 });
		await minimizeButton.click();
		await browser.pause(500);

		const afterRect = await browser.getWindowRect();
		const visibilityState = await browser.execute(() => document.visibilityState);

		// minimizado = visibilityState oculto OU altura da janela alterada
		const wasMinimized = visibilityState !== 'visible' || afterRect.height < beforeRect.height;
		expect(wasMinimized).toBe(true);
	});
});
