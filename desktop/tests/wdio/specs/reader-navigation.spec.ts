import path from 'node:path';
import fs from 'node:fs';
import {
	firstDisplayed,
	getPathname,
	invokeTauriCommand,
	navigateTo,
	waitForAppReady
} from '../helpers/app';

describe('Reader Navigation E2E', () => {
	it('tests navigation across chapters and history resuming', async function () {
		this.timeout(90_000);
		const cbzFolder = path.resolve(process.cwd(), 'tests/wdio/comic/cbz');
		const cbzFile = path.join(cbzFolder, 'witchcraft.cbz');
		
		// Create multiple chapters for testing
		const cbzChapter2 = path.join(cbzFolder, 'witchcraft2.cbz');
		const cbzChapter3 = path.join(cbzFolder, 'witchcraft3.cbz');

		if (!fs.existsSync(cbzChapter2)) {
			fs.copyFileSync(cbzFile, cbzChapter2);
		}
		if (!fs.existsSync(cbzChapter3)) {
			fs.copyFileSync(cbzFile, cbzChapter3);
		}

		await waitForAppReady();
		await navigateTo('/home');

		// 1. Scan the library
		await invokeTauriCommand('refresh_library', { path: cbzFolder });

		// Wait for comic to appear
		const comicCard = await firstDisplayed('//a[contains(@href, "/comic/")]', 15_000);
		await comicCard.click();

		await browser.waitUntil(async () => (await getPathname()).includes('/comic/'), {
			timeout: 5_000,
			timeoutMsg: 'Nao navegou para a página do comic cbz.'
		});

		// 2. Open Chapter 1
		const chapterItem = await firstDisplayed(
			'//*[contains(normalize-space(), "witchcraft.cbz")]',
			10_000
		);
		await chapterItem.click();

		await browser.waitUntil(async () => (await getPathname()) === '/reader', {
			timeout: 5_000,
			timeoutMsg: 'Nao navegou para o reader.'
		});

		// Wait for image to render
		const image = await firstDisplayed('img[alt="Página 1"]', 10_000);
		expect(await image.getAttribute('src')).toMatch(/^blob:|asset:\/\/|tauri:\/\//i);

		// 3. Open Command Palette / Footer to see Next Button and click it
		const nextBtn = await firstDisplayed('//*[normalize-space()="PRÓXIMO"]', 5_000);
		await nextBtn.click();

		// Wait for the next chapter to load
		await browser.pause(3000);
		
		// The reader should now be in chapter 2.
		// Let's verify we have a previous button now.
		const prevBtn = await firstDisplayed('//*[normalize-space()="ANTERIOR"]', 5_000);
		expect(await prevBtn.isExisting()).toBe(true);

		// 4. Leave Reader and check Comic page
		const backBtn = await firstDisplayed('button[title="Voltar"]', 5_000);
		await backBtn.click();

		await browser.waitUntil(async () => (await getPathname()).includes('/comic/'), {
			timeout: 5_000,
			timeoutMsg: 'Nao voltou para a tela do comic.'
		});

		// 5. Check History resume
		await navigateTo('/history');
		const resumeBtn = await firstDisplayed('//*[contains(normalize-space(), "Continuar")]', 10_000);
		await resumeBtn.click();

		await browser.waitUntil(async () => (await getPathname()) === '/reader', {
			timeout: 5_000,
			timeoutMsg: 'Nao navegou para o reader pelo historico.'
		});

		// Check if we are still on chapter 2 by checking for the ANTERIOR button and PRÓXIMO button
		const nextBtnHistory = await firstDisplayed('//*[normalize-space()="PRÓXIMO"]', 5_000);
		const prevBtnHistory = await firstDisplayed('//*[normalize-space()="ANTERIOR"]', 5_000);
		
		expect(await prevBtnHistory.isExisting()).toBe(true);
		expect(await nextBtnHistory.isExisting()).toBe(true);

		// 6. Test Previous Chapter
		await prevBtnHistory.click();
		await browser.pause(3000);
		
		// Back on chapter 1, previous button should be gone.
		const prevBtnGone = await browser.$('//*[normalize-space()="ANTERIOR"]');
		expect(await prevBtnGone.isExisting()).toBe(false);

		// Cleanup files
		fs.rmSync(cbzChapter2);
		fs.rmSync(cbzChapter3);
	});
});
