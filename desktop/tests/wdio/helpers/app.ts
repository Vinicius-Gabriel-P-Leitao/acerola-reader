const APP_ORIGIN = process.platform === 'win32' ? 'http://tauri.localhost' : 'tauri://localhost';
const APP_READY_SELECTOR = '[data-tauri-drag-region]';
const APP_ORIGINS = ['http://tauri.localhost', 'https://tauri.localhost', 'tauri://localhost'];

type AppState = {
	href: string;
	pathname: string;
	readyState: DocumentReadyState;
	bodyChildCount: number;
	hasAppLayout: boolean;
};

async function getAppState() {
	return browser.execute((readySelector) => {
		return {
			href: window.location.href,
			pathname: window.location.pathname,
			readyState: document.readyState,
			bodyChildCount: document.body?.children.length ?? 0,
			hasAppLayout: Boolean(document.querySelector(readySelector))
		} satisfies AppState;
	}, APP_READY_SELECTOR);
}

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

export async function navigateTo(route: string) {
	// WebView2/Tauri precisam da origem customizada; URL relativa quebra em about:blank.
	const normalizedRoute = route.startsWith('/') ? route : `/${route}`;
	const fullUrl = `${APP_ORIGIN}${normalizedRoute}`;

	await browser.url(fullUrl);

	await browser.waitUntil(
		async () => {
			const state = await getAppState();
			return state.pathname === normalizedRoute;
		},
		{
			timeout: 5_000,
			interval: 100,
			timeoutMsg: `WebView não navegou para ${normalizedRoute}.`
		}
	);
}

export async function waitForAppReady() {
	// __TAURI_INTERNALS__ fica disponivel ainda em about:blank; carregue a rota real primeiro.
	await waitForTauriReady();

	const state = await getAppState();
	if (!state.hasAppLayout && !APP_ORIGINS.some((origin) => state.href.startsWith(origin))) {
		await navigateTo('/home');
	}

	try {
		await browser.waitUntil(
			async () => {
				const currentState = await getAppState();
				return currentState.readyState === 'complete' && currentState.hasAppLayout;
			},
			{
				timeout: 5_000,
				interval: 100,
				timeoutMsg: 'DOM do app não renderizou após __TAURI_INTERNALS__ disponível.'
			}
		);
	} catch {
		const currentState = await getAppState();
		throw new Error(
			`DOM do app não renderizou após __TAURI_INTERNALS__ disponível. Estado: ${JSON.stringify(
				currentState
			)}`
		);
	}
}

export async function getTitle() {
	return browser.execute(() => document.title);
}
