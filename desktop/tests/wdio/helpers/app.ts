export const APP_ORIGIN =
	process.platform === 'win32' ? 'http://tauri.localhost' : 'tauri://localhost';
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

export async function navigateToWithState(route: string, state: Record<string, unknown>) {
	const normalizedRoute = route.startsWith('/') ? route : `/${route}`;
	const fullUrl = `${APP_ORIGIN}${normalizedRoute}`;

	await browser.execute(
		({ targetUrl, pageState }) => {
			const currentState = window.history.state ?? {};
			const fallbackIndex = Date.now();
			const currentHistoryIndex = Number(currentState['sveltekit:history']) || fallbackIndex;
			const currentNavigationIndex = Number(currentState['sveltekit:navigation']) || fallbackIndex;

			const nextState = {
				...currentState,
				'sveltekit:history': currentHistoryIndex + 1,
				'sveltekit:navigation': currentNavigationIndex + 1,
				'sveltekit:states': pageState
			};

			window.history.pushState(nextState, '', targetUrl);
			window.dispatchEvent(new PopStateEvent('popstate', { state: nextState }));
		},
		{ targetUrl: fullUrl, pageState: state }
	);

	await browser.waitUntil(
		async () => {
			const currentState = await getAppState();
			return currentState.pathname === normalizedRoute;
		},
		{
			timeout: 5_000,
			interval: 100,
			timeoutMsg: `WebView não navegou para ${normalizedRoute} com estado de página.`
		}
	);
}

export async function setStoreValue(key: string, value: unknown) {
	const state = await getAppState();
	if (!APP_ORIGINS.some((origin) => state.href.startsWith(origin))) {
		await navigateTo('/home');
	}

	await browser.execute(
		async ({ key: k, value: v }) => {
			const tauriWindow = window as typeof window & {
				__TAURI_INTERNALS__?: {
					invoke: (cmd: string, payload?: unknown) => Promise<unknown>;
				};
			};
			if (tauriWindow.__TAURI_INTERNALS__) {
				const rid = await tauriWindow.__TAURI_INTERNALS__.invoke('plugin:store|load', {
					path: 'settings.json'
				});
				await tauriWindow.__TAURI_INTERNALS__.invoke('plugin:store|set', { rid, key: k, value: v });
				await tauriWindow.__TAURI_INTERNALS__.invoke('plugin:store|save', { rid });
			}
		},
		{ key, value }
	);
}

export async function ensureOnboardingCompleted() {
	await waitForTauriReady();
	await setStoreValue('onboarding_completed', true);
}

export async function waitForAppReady(skipOnboardingComplete = false) {
	// __TAURI_INTERNALS__ fica disponivel ainda em about:blank; carregue a rota real primeiro.
	await waitForTauriReady();

	const state = await getAppState();
	if (!state.hasAppLayout && !APP_ORIGINS.some((origin) => state.href.startsWith(origin))) {
		await navigateTo('/home');
	}

	if (!skipOnboardingComplete) {
		await setStoreValue('onboarding_completed', true);
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

export async function getPathname() {
	return browser.execute(() => window.location.pathname);
}

export async function expectPathname(pathname: string) {
	await browser.waitUntil(async () => (await getPathname()) === pathname, {
		timeout: 5_000,
		interval: 100,
		timeoutMsg: `Rota atual não ficou em ${pathname}.`
	});
}

export function xpathLiteral(value: string) {
	if (!value.includes("'")) return `'${value}'`;
	if (!value.includes('"')) return `"${value}"`;

	return `concat(${value
		.split("'")
		.map((part) => `'${part}'`)
		.join(`, "'", `)})`;
}

export function exactTextSelector(text: string) {
	return `//*[normalize-space()=${xpathLiteral(text)}]`;
}

export function containsTextSelector(text: string) {
	return `//*[contains(normalize-space(), ${xpathLiteral(text)})]`;
}

export async function waitForText(text: string, timeout = 5_000) {
	return firstDisplayed(exactTextSelector(text), timeout);
}

export async function waitForTextContaining(text: string, timeout = 5_000) {
	return firstDisplayed(containsTextSelector(text), timeout);
}

export async function isTextDisplayed(text: string) {
	const elements = await browser.$$(exactTextSelector(text));

	for (const element of elements) {
		if (await element.isDisplayed().catch(() => false)) return true;
	}

	return false;
}

export async function clickText(text: string, timeout = 5_000) {
	const element = await waitForText(text, timeout);
	await element.click();
	return element;
}

export async function firstDisplayed(selector: string, timeout = 5_000) {
	let lastCount = 0;

	await browser.waitUntil(
		async () => {
			const elements = await browser.$$(selector);
			lastCount = await elements.length;

			for (const element of elements) {
				if (await element.isDisplayed().catch(() => false)) return true;
			}

			return false;
		},
		{
			timeout,
			interval: 100,
			timeoutMsg: `Nenhum elemento visível para seletor "${selector}" (${lastCount} encontrados).`
		}
	);

	const elements = await browser.$$(selector);
	for (const element of elements) {
		if (await element.isDisplayed().catch(() => false)) return element;
	}

	throw new Error(`Nenhum elemento visível para seletor "${selector}".`);
}

export async function invokeTauriCommand<T = unknown>(command: string, args?: unknown) {
	return browser.execute(
		async ({ command: commandName, args: commandArgs }) => {
			const tauriWindow = window as typeof window & {
				__TAURI_INTERNALS__?: {
					invoke: (cmd: string, payload?: unknown) => Promise<unknown>;
				};
			};

			if (!tauriWindow.__TAURI_INTERNALS__) {
				throw new Error('window.__TAURI_INTERNALS__ indisponível.');
			}

			return tauriWindow.__TAURI_INTERNALS__.invoke(commandName, commandArgs);
		},
		{ command, args }
	) as Promise<T>;
}
