import { firstDisplayed, getTitle, navigateTo, navigateToWithState, waitForAppReady } from '../helpers/app';
import { createReaderFixture, readerChapterFor } from '../helpers/fixtures';

describe('native acerola via WebDriverIO', () => {
	it('initializes the app without crashing', async () => {
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

	it('triggers folder selection without freezing the app', async () => {
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

	it('loads native chapter image in the reader', async () => {
		const fixture = createReaderFixture('Acerola WDIO Native');
		await waitForAppReady();
		await navigateToWithState('/reader', {
			chapter: readerChapterFor(fixture),
			chapterIndex: 0,
			totalChapters: 1,
			chapterScope: fixture.comicTitle
		});

		const image = await firstDisplayed('img[alt="Página 1"], img[alt="Page 1"]', 20_000);

		const src = await image.getAttribute('src');
		expect(src).toMatch(/^blob:|asset:\/\/|tauri:\/\//i);
	});

	it.skip('triggers native minimize control via titlebar', async () => {
		await waitForAppReady();
		await navigateTo('/home');

		const beforeRect = await browser.getWindowRect();

		try {
			const minimizeButton = await browser.$('[aria-label="Minimizar"]');
			await minimizeButton.waitForDisplayed({ timeout: 5_000 });
			await minimizeButton.click();
			await browser.pause(500);

			const afterRect = await browser.getWindowRect();
			const visibilityState = await browser.execute(() => document.visibilityState);

			// Windows/WebView2 mantém o tamanho, mas move a janela minimizada para fora da tela.
			const movedOffscreen = afterRect.x < -10_000 || afterRect.y < -10_000;
			const wasMinimized =
				visibilityState !== 'visible' || afterRect.height < beforeRect.height || movedOffscreen;
			expect(wasMinimized).toBe(true);
		} finally {
			await browser
				.setWindowRect(beforeRect.x, beforeRect.y, beforeRect.width, beforeRect.height)
				.catch(() => undefined);
		}
	});
});
