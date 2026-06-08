export async function waitForTauriReady() {
	await browser.waitUntil(
		async () =>
			browser.execute(() => {
				const tauriWindow = window as typeof window & {
					__TAURI_INTERNALS__?: unknown;
				};

				return Boolean(tauriWindow.__TAURI_INTERNALS__);
			}),
		{
			timeout: 5_000,
			interval: 100,
			timeoutMsg: 'window.__TAURI_INTERNALS__ não ficou disponível no WebView.'
		}
	);
}

export async function navigateTo(path: string) {
	await browser.execute((targetPath) => {
		window.location.href = targetPath;
	}, path);

	await browser.waitUntil(async () => browser.execute(() => window.location.pathname === path), {
		timeout: 5_000,
		interval: 100,
		timeoutMsg: `WebView não navegou para ${path}.`
	});
}

export async function getTitle() {
	return browser.execute(() => document.title);
}
