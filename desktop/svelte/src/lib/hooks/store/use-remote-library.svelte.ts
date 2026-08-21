import { invoke } from '@tauri-apps/api/core';
import { listen, type UnlistenFn } from '@tauri-apps/api/event';
import { SvelteMap, SvelteSet } from 'svelte/reactivity';
import { NETWORK_COMMANDS } from '$lib/contracts/network/network.commands';
import { NETWORK_EVENTS } from '$lib/contracts/network/network.events';
import type {
	ComicSummary,
	LibraryQueryResultPayload
} from '$lib/contracts/network/network.payloads';

/// Espelha o payload de erro dos outros eventos `sync:*:error` (ver
/// `use-network-sync.svelte.ts::parseErrorPayload`) — `library:query:error` carrega
/// `{ peerId, message }` no mesmo formato.
function parseErrorPayload(payload: string): { peerId?: string; message: string } {
	try {
		const parsed = JSON.parse(payload);
		if (parsed && typeof parsed === 'object' && typeof parsed.message === 'string') {
			return { peerId: parsed.peerId, message: parsed.message };
		}
	} catch {
		// payload inesperado sem JSON — trata como mensagem crua.
	}
	return { message: payload };
}

/// Consulta e mantém em cache (por `peerId`) a lista de quadrinhos de bibliotecas remotas —
/// usado pra "puxar" um quadrinho individual que só existe em outro dispositivo (ver
/// `sync_comic` em `use-network-sync.svelte.ts`). A consulta é única por clique (não
/// re-dispara a cada tecla do campo de busca): o filtro por título é reativo no frontend sobre
/// o resultado já em cache, mesmo padrão do "Buscar quadrinhos por título" da Home.
export function useRemoteLibrary() {
	const libraries = new SvelteMap<string, ComicSummary[]>();
	const loading = new SvelteSet<string>();
	const errors = new SvelteMap<string, string>();
	const unlisten: UnlistenFn[] = [];

	async function startListening() {
		unlisten.push(
			await listen<string>(NETWORK_EVENTS.libraryQueryResult, (event) => {
				const payload = JSON.parse(event.payload) as LibraryQueryResultPayload;
				loading.delete(payload.peerId);
				errors.delete(payload.peerId);
				libraries.set(payload.peerId, payload.comics);
			}),
			await listen<string>(NETWORK_EVENTS.libraryQueryError, (event) => {
				const { peerId, message } = parseErrorPayload(event.payload);
				if (!peerId) return;
				loading.delete(peerId);
				errors.set(peerId, message);
			})
		);
	}

	function stopListening() {
		unlisten.forEach((fn) => fn());
		unlisten.length = 0;
	}

	async function queryRemoteLibrary(peerId: string, addrs: number[]) {
		loading.add(peerId);
		errors.delete(peerId);
		try {
			await invoke(NETWORK_COMMANDS.queryRemoteLibrary, { peerId, addrs });
		} catch (err) {
			loading.delete(peerId);
			errors.set(peerId, String(err));
			throw err;
		}
	}

	function comicsFor(peerId: string): ComicSummary[] {
		return libraries.get(peerId) ?? [];
	}

	function isLoading(peerId: string): boolean {
		return loading.has(peerId);
	}

	function errorFor(peerId: string): string | undefined {
		return errors.get(peerId);
	}

	return {
		startListening,
		stopListening,
		queryRemoteLibrary,
		comicsFor,
		isLoading,
		errorFor
	};
}
