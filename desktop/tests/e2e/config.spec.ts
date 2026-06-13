import { expect, test } from '@playwright/test';
import { HOME_COMMANDS } from '../../svelte/src/lib/contracts/home/home.commands';
import { HOME_EVENTS } from '../../svelte/src/lib/contracts/home/home.events';
import {
	DIRECTORY_SCAN_COMMANDS,
	LIBRARY_COMMANDS
} from '../../svelte/src/lib/contracts/library/library.commands';
import { LIBRARY_EVENTS } from '../../svelte/src/lib/contracts/library/library.events';
import { e2eComicSummary } from '../../svelte/tests/mocks/acerola-e2e-data';
import {
	collectConsoleErrors,
	emitTauriEvent,
	installTauriMocks,
	mockedTauriResponse
} from '../../svelte/tests/mocks/tauri-playwright';

test.describe('configuração de biblioteca', () => {
	let consoleErrors: string[];

	test.beforeEach(async ({ page }) => {
		consoleErrors = collectConsoleErrors(page);

		await installTauriMocks(page, {
			[LIBRARY_COMMANDS.selectFolder]: 'C:\\Comics',
			[DIRECTORY_SCAN_COMMANDS.refreshLibrary]: undefined,
			[HOME_COMMANDS.getComicSummary]: mockedTauriResponse({
				events: [{ event: HOME_EVENTS.homeData, payload: e2eComicSummary() }]
			})
		});
	});

	test('seleciona pasta, executa sincronização rápida e volta para a biblioteca', async ({
		page
	}) => {
		test.setTimeout(10_000);

		await page.goto('/config');

		await page.getByText('Pasta dos quadrinhos').click();
		await expect(page).not.toHaveURL(/\/home$/);
		await expect(page.getByText(/C:\\Comics/)).toBeVisible();

		await page.getByText('Sincronização rápida').click();
		await expect(page).not.toHaveURL(/\/home$/);

		await emitTauriEvent(page, LIBRARY_EVENTS.scanComplete);

		await page.goto('/home');

		await expect(page.getByRole('heading', { name: 'Acerola' })).toBeVisible();
		expect(consoleErrors).toEqual([]);
	});
});
