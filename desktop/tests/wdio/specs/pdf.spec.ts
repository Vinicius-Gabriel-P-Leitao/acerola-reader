import path from 'node:path';
import fs from 'node:fs';
import {
	firstDisplayed,
	invokeTauriCommand,
	navigateTo,
	navigateToWithState,
	waitForAppReady
} from '../helpers/app';

describe('PDF to CBZ E2E', () => {
	it('scans PDF, converts to CBZ, and reads it', async function () {
		this.timeout(90_000);
		const pdfFolder = path.resolve(process.cwd(), 'tests/wdio/comic/pdf');
		const cbzFile = path.join(pdfFolder, 'witchcraft.cbz');

		if (fs.existsSync(cbzFile)) {
			fs.rmSync(cbzFile);
		}

		await waitForAppReady();
		await navigateTo('/home');
		// Aguarda o onMount registrar o listener scan:complete
		await browser.pause(500);

		// Dispara a conversao e scan
		await invokeTauriCommand('refresh_library', { path: pdfFolder });

		// Espera o card 'pdf' aparecer — confirma que a conversao PDF→CBZ terminou
		await firstDisplayed('//main//h3[normalize-space()="pdf"]', 60_000);

		// Obtem o directoryId do comic para o estado do reader
		const comic = await invokeTauriCommand<any>('get_comic_by_folder_name', {
			folderName: 'pdf'
		});

		// Navega direto para o reader com o capitulo convertido
		await navigateToWithState('/reader', {
			chapter: {
				id: 'pdf-witchcraft-ch1',
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
			totalChapters: 1,
			chapterScope: 'pdf'
		});

		// Confirma que a página 1 abriu
		const image = await firstDisplayed('img[alt="Página 1"], img[alt="Page 1"]', 20_000);
		const src = await image.getAttribute('src');
		expect(src).toMatch(/^blob:|asset:\/\/|tauri:\/\//i);
	});
});
