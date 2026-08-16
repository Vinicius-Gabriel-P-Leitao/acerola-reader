export const NETWORK_COMMANDS = {
	getNetworkStatus: 'get_network_status',
	switchToLocal: 'switch_to_local',
	switchToRelay: 'switch_to_relay',
	getLocalId: 'get_local_id',
	getLocalAddr: 'get_local_addr',
	getLocalDeviceInfo: 'get_local_device_info',
	getRelayInfo: 'get_relay_info',
	connectToPeer: 'connect_to_peer',
	syncHistory: 'sync_history',
	syncFiles: 'sync_files',
	syncAll: 'sync_all'
} as const;
