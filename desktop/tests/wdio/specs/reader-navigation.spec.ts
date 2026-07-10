import path from 'node:path';
import fs from 'node:fs';
import {
	firstDisplayed,
	getPathname,
	invokeTauriCommand,
	navigateTo,
	navigateToWithState,
	waitForAppReady
} from '../helpers/app';

const cbzFolder = path.resolve(process.cwd(), 'tests/wdio/comic/cbz');
const cbzFile = path.join(cbzFolder, 'witchcraft.cbz');
const cbzChapter2 = path.join(cbzFolder, 'witchcraft2.cbz');
const cbzChapter3 = path.join(cbzFolder, 'witchcraft3.cbz');

// Garante limpeza mesmo se o teste falhar
after(() => {
	if (fs.existsSync(cbzChapter2)) fs.rmSync(cbzChapter2);
	if (fs.existsSync(cbzChapter3)) fs.rmSync(cbzChapter3);
});

describe('Reader Navigation E2E', () => {
	it('tests navigation across chapters and history resuming', async function () {
		this.timeout(90_000);

		if (!fs.existsSync(cbzChapter2)) {
			fs.copyFileSync(cbzFile, cbzChapter2);
		}
		if (!fs.existsSync(cbzChapter3)) {
			fs.copyFileSync(cbzFile, cbzChapter3);
		}

		await waitForAppReady();
		await navigateTo('/home');
		// Aguarda o onMount registrar o listener scan:complete
		await browser.pause(500);

		// 1. Scan the library
		await invokeTauriCommand('refresh_library', { path: cbzFolder });

		// Espera o card 'cbz' aparecer — confirma que o scan terminou
		await firstDisplayed('//main//h3[normalize-space()="cbz"]', 15_000);

		// Obtem o directoryId do comic para o estado do reader
		const comic = await invokeTauriCommand<any>('get_comic_by_folder_name', {
			folderName: 'cbz'
		});

		// 2. Navega direto para o reader com o capitulo 1
		await navigateToWithState('/reader', {
			chapter: {
				id: 'cbz-witchcraft-ch1',
				name: 'witchcraft',
				path: cbzFile,
				chapterSort: '0',
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			},
			comicDirectoryId: comic?.relations?.directoryId ?? null,
			chapterIndex: 0,
			totalChapters: 3,
			chapterScope: 'cbz'
		});

		// Confirma que a página 1 abriu no capitulo 1
		const image = await firstDisplayed('img[alt="Página 1"]', 10_000);
		expect(await image.getAttribute('src')).toMatch(/^blob:|asset:\/\/|tauri:\/\//i);

		// 3. Navega para o próximo capítulo
		const nextBtn = await firstDisplayed('//*[normalize-space()="Próximo capítulo"]', 5_000);
		await nextBtn.click();

		// Aguarda o botao de capitulo anterior aparecer (confirma que esta no cap 2)
		await firstDisplayed('//*[normalize-space()="Capítulo anterior"]', 10_000);

		// 4. Volta via o botao Voltar
		const backBtn = await firstDisplayed('button[title="Voltar"]', 5_000);
		await backBtn.click();

		await browser.waitUntil(async () => !(await getPathname()).startsWith('/reader'), {
			timeout: 5_000,
			timeoutMsg: 'Nao saiu do reader.'
		});

		// 5. Vai para o historico e verifica que a entrada existe
		await navigateTo('/history');
		const resumeBtn = await firstDisplayed('//*[contains(normalize-space(), "Continuar")]', 10_000);
		expect(await resumeBtn.isExisting()).toBe(true);

		// Simula o resume navegando direto para o capitulo 2
		await navigateToWithState('/reader', {
			chapter: {
				id: 'cbz-witchcraft-ch2',
				name: 'witchcraft2',
				path: cbzChapter2,
				chapterSort: '1',
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			},
			comicDirectoryId: comic?.relations?.directoryId ?? null,
			chapterIndex: 1,
			totalChapters: 3,
			chapterScope: 'cbz'
		});

		// 6. Verifica que esta no capitulo 2 (tem ambos os botoes)
		await firstDisplayed('//*[normalize-space()="Próximo capítulo"]', 5_000);
		await firstDisplayed('//*[normalize-space()="Capítulo anterior"]', 5_000);

		// 7. Volta ao capitulo 1 via state — verifica que botao anterior nao aparece no capitulo 1
		await navigateToWithState('/reader', {
			chapter: {
				id: 'cbz-witchcraft-ch1',
				name: 'witchcraft',
				path: cbzFile,
				chapterSort: '0',
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			},
			comicDirectoryId: comic?.relations?.directoryId ?? null,
			chapterIndex: 0,
			totalChapters: 3,
			chapterScope: 'cbz'
		});

		// No capitulo 1 (index 0), hasPreviousChapter = false — botao deve ser removido do DOM
		await browser.waitUntil(
			async () => {
				const btn = await browser.$('//*[normalize-space()="Capítulo anterior"]');
				return !(await btn.isExisting());
			},
			{
				timeout: 5_000,
				interval: 100,
				timeoutMsg: '"Capítulo anterior" aparece indevidamente no capitulo 1.'
			}
		);
	});
});
