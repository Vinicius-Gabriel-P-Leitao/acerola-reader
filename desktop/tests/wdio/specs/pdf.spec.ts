import path from 'node:path';
import fs from 'node:fs';
import {
	firstDisplayed,
	getPathname,
	invokeTauriCommand,
	navigateTo,
	waitForAppReady,
} from '../helpers/app';

describe('PDF to CBZ E2E', () => {
	it('scans PDF, converts to CBZ, and reads it', async () => {
		const pdfFolder = path.resolve(process.cwd(), 'tests/wdio/comic/pdf');
		const cbzFile = path.join(pdfFolder, 'witchcraft.cbz');

		if (fs.existsSync(cbzFile)) {
			fs.rmSync(cbzFile);
		}

		await waitForAppReady();
		await navigateTo('/home');

		// Dispara a conversao e scan
		await invokeTauriCommand('refresh_library', { path: pdfFolder });

		// Espera a conversao terminar e o mangá aparecer (tempo maior devido a conversao pdf)
		const comicCard = await firstDisplayed('//*[normalize-space()="pdf"]', 30_000);
		await comicCard.click();

		await browser.waitUntil(async () => (await getPathname()) === '/comic/pdf', {
			timeout: 5_000,
			timeoutMsg: 'Nao navegou para a página do comic pdf.'
		});

		// Clica no capitulo convertido
		const chapterItem = await firstDisplayed('//*[contains(normalize-space(), "witchcraft")]', 10_000);
		await chapterItem.click();

		await browser.waitUntil(async () => (await getPathname()) === '/reader', {
			timeout: 5_000,
			timeoutMsg: 'Nao navegou para o reader.'
		});

		// Confirma que a página 1 abriu
		const image = await firstDisplayed('img[alt="Página 1"]', 10_000);
		const src = await image.getAttribute('src');
		expect(src).toMatch(/^blob:|asset:\/\/|tauri:\/\//i);
	});
});
