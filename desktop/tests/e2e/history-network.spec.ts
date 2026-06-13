import { expect, test } from '@playwright/test';
import {
	collectConsoleErrors,
	installTauriMocks,
	mockedTauriResponse
} from '../../svelte/tests/mocks/tauri-playwright';

type NetworkMode = 'local' | 'relay';

function networkStatus(mode: NetworkMode) {
	return { mode, peers: [] };
}

test.describe('history rede', () => {
	let consoleErrors: string[];
	let mode: NetworkMode;

	test.beforeEach(async ({ page }) => {
		consoleErrors = collectConsoleErrors(page);
		mode = 'local';

		await installTauriMocks(page, {
			get_local_id: 'local-peer-id',
			get_network_status: () =>
				mockedTauriResponse({
					value: networkStatus(mode),
					events: [{ event: 'network:status', payload: networkStatus(mode) }]
				}),
			switch_to_relay: () => {
				mode = 'relay';
			},
			switch_to_local: () => {
				mode = 'local';
			},
			connect_to_peer: undefined
		});
	});

	test('mostra modo local e troca para relay no WebView', async ({ page }) => {
		test.setTimeout(10_000);

		await page.goto('/history');

		await expect(page.getByText('Modo: local')).toBeVisible();

		await page.getByRole('button', { name: 'Relay' }).click();

		await expect(page.getByText('Modo: relay')).toBeVisible();
		await expect(page).not.toHaveURL(/\/home$/);
		expect(consoleErrors).toEqual([]);
	});
});
