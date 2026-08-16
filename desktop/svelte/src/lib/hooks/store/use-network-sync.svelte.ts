import { invoke } from '@tauri-apps/api/core';
import { listen, type UnlistenFn } from '@tauri-apps/api/event';
import { NETWORK_COMMANDS } from '$lib/contracts/network/network.commands';
import { NETWORK_EVENTS } from '$lib/contracts/network/network.events';

export type TransferLogEntry = {
	id: number;
	kind: 'history' | 'files';
	status: 'started' | 'progress' | 'complete' | 'error';
	message: string;
	timestamp: number;
};

const MAX_LOG_ENTRIES = 50;

/// Dispara sessões de sync (histórico/arquivos) contra um peer já pareado e mantém um
/// log reativo do progresso, alimentado pelos eventos `sync:history:*`/`sync:files:*` que
/// os protocol handlers do Rust emitem direto (ver `infra::sync::protocol` no backend).
export function useNetworkSync() {
	let log = $state<TransferLogEntry[]>([]);
	let nextId = 0;
	const unlisten: UnlistenFn[] = [];

	function push(kind: TransferLogEntry['kind'], status: TransferLogEntry['status'], message: string) {
		log = [{ id: nextId++, kind, status, message, timestamp: Date.now() }, ...log].slice(
			0,
			MAX_LOG_ENTRIES
		);
	}

	async function startListening() {
		unlisten.push(
			await listen<string>(NETWORK_EVENTS.historyStarted, (event) =>
				push('history', 'started', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.historyComplete, (event) =>
				push('history', 'complete', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.historyError, (event) =>
				push('history', 'error', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.filesStarted, (event) =>
				push('files', 'started', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.filesProgress, (event) =>
				push('files', 'progress', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.filesComplete, (event) =>
				push('files', 'complete', event.payload)
			),
			await listen<string>(NETWORK_EVENTS.filesError, (event) =>
				push('files', 'error', event.payload)
			)
		);
	}

	function stopListening() {
		unlisten.forEach((fn) => fn());
		unlisten.length = 0;
	}

	async function syncHistory(peerId: string, addrs: number[]) {
		await invoke(NETWORK_COMMANDS.syncHistory, { peerId, addrs });
	}

	async function syncFiles(peerId: string, addrs: number[]) {
		await invoke(NETWORK_COMMANDS.syncFiles, { peerId, addrs });
	}

	async function syncAll(peerId: string, addrs: number[]) {
		await invoke(NETWORK_COMMANDS.syncAll, { peerId, addrs });
	}

	return {
		startListening,
		stopListening,
		syncHistory,
		syncFiles,
		syncAll,
		get log() {
			return log;
		}
	};
}
