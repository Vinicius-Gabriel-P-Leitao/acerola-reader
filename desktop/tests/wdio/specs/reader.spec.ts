import {
	firstDisplayed,
	getPathname,
	navigateTo,
	navigateToWithState,
	waitForAppReady,
	waitForText,
	waitForTextContaining
} from '../helpers/app';
import { createReaderFixture, readerChapterFor } from '../helpers/fixtures';

async function openReaderWithFixture(title = 'Acerola WDIO Reader') {
	const fixture = createReaderFixture(title);

	await waitForAppReady();
	await navigateTo('/home');
	await navigateToWithState('/reader', {
		chapter: readerChapterFor(fixture),
		chapterIndex: 0,
		totalChapters: 1,
		chapterScope: fixture.comicTitle
	});

	return fixture;
}

describe('reader nativo', () => {
	it('exibe fallback seguro quando não há capítulo no estado da navegação', async () => {
		await waitForAppReady();
		await navigateTo('/reader');

		await waitForText('Capítulo indisponível');
		await waitForText('0% lido');
		await waitForText('Capítulos restantes indisponíveis');

		const progress = await firstDisplayed('[role="progressbar"]');
		expect(await progress.getAttribute('aria-valuenow')).toBe('0');

		try {
			const paginadoBtn = await firstDisplayed('[title="Paginado horizontal"]', 2000);
			if ((await paginadoBtn.getAttribute('data-state')) !== 'on') {
				await paginadoBtn.click();
			}
		} catch (e) {}

		const previous = await firstDisplayed('[title="Página anterior"]');
		const next = await firstDisplayed('[title="Próxima página"]');
		expect(await previous.isEnabled()).toBe(false);
		expect(await next.isEnabled()).toBe(false);
	});

	it('abre command palette, alterna modos e aplica zoom sem capítulo aberto', async () => {
		await waitForAppReady();
		await navigateTo('/reader');

		await (await firstDisplayed('[title="Comandos"]')).click();
		await waitForText('Aumentar zoom');
		await waitForText('Reduzir zoom');
		await waitForText('Paginado horizontal');
		await browser.keys(['Escape']);

		await (await firstDisplayed('[title="Paginado horizontal"]')).click();
		await waitForTextContaining('Paginado horizontal - Zoom 100%');

		await (await firstDisplayed('[title="Webtoon"]')).click();
		await waitForTextContaining('Webtoon - Zoom 100%');

		await (await firstDisplayed('[title="Aplicar zoom"]')).click();
		await waitForTextContaining('Zoom 165%');
		await (await firstDisplayed('[title="Resetar zoom"]')).click();
		await waitForTextContaining('Zoom 100%');
	});

	it('abre capítulo fixture, navega páginas e bloqueia paginação enquanto zoom está ativo', async () => {
		const fixture = await openReaderWithFixture();

		await waitForText('Ch. 1', 10_000);
		await waitForTextContaining(`${fixture.comicTitle} - 1 / 3 páginas`, 10_000);
		await waitForText('33% lido', 10_000);

		const firstPage = await browser.$('img[alt="Página 1"]');
		await firstPage.waitForDisplayed({ timeout: 10_000 });

		// Ensure we are in Paginated mode (in case a previous test left it in Webtoon)
		try {
			const paginadoBtn = await firstDisplayed('[title="Paginado horizontal"]', 2000);
			if ((await paginadoBtn.getAttribute('data-state')) !== 'on') {
				await paginadoBtn.click();
			}
		} catch (e) {}

		await (await firstDisplayed('[title="Próxima página"]')).click();
		await waitForTextContaining(`${fixture.comicTitle} - 2 / 3 páginas`, 10_000);
		await waitForText('67% lido', 10_000);

		await (await firstDisplayed('[title="Página anterior"]')).click();
		await waitForTextContaining(`${fixture.comicTitle} - 1 / 3 páginas`, 10_000);

		await (await firstDisplayed('[title="Aplicar zoom"]')).click();
		await waitForTextContaining('Zoom 165%');

		const lockedNavigation = await firstDisplayed(
			'[title="Desative o zoom para trocar de página"]'
		);
		expect(await lockedNavigation.isEnabled()).toBe(false);

		await (await firstDisplayed('[title="Resetar zoom"]')).click();
		await waitForTextContaining('Zoom 100%');

		await (await firstDisplayed('[title="Voltar"]')).click();
		await browser.waitUntil(async () => (await getPathname()) === '/home', {
			timeout: 5_000,
			interval: 100,
			timeoutMsg: 'Botão Voltar do reader não retornou para Home.'
		});
	});
});
