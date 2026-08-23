import { invoke } from '@tauri-apps/api/core';
import { load } from '@tauri-apps/plugin-store';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { NETWORK_COMMANDS } from '$lib/contracts/network/network.commands';
import type { RelayInfo } from '$lib/contracts/network/network.payloads';

export function useRelaySettings() {
	let relayInfo = $state<RelayInfo | undefined>(undefined);

	async function loadRelayInfo() {
		relayInfo = await invoke<RelayInfo>(NETWORK_COMMANDS.getRelayInfo);
	}

	/// Salva o override. Só tem efeito no próximo início do app — a conexão P2P atual
	/// continua usando o relay com o qual já subiu.
	async function setRelayUrl(value: string) {
		const store = await load(STORE_FILE);
		const trimmed = value.trim();

		if (!trimmed || (relayInfo && trimmed === relayInfo.defaultRelay)) {
			await store.delete(STORE_KEYS.relayUrl);
		} else {
			await store.set(STORE_KEYS.relayUrl, trimmed);
		}

		await store.save();
	}

	async function resetRelayUrl() {
		const store = await load(STORE_FILE);
		await store.delete(STORE_KEYS.relayUrl);
		await store.save();
	}

	return {
		loadRelayInfo,
		setRelayUrl,
		resetRelayUrl,
		get relayInfo() {
			return relayInfo;
		},
		get isOverridden() {
			return !!relayInfo && relayInfo.activeRelay !== relayInfo.defaultRelay;
		}
	};
}
