export const NETWORK_EVENTS = {
	status: 'network:status',
	historyStarted: 'sync:history:started',
	historyComplete: 'sync:history:complete',
	historyError: 'sync:history:error',
	filesStarted: 'sync:files:started',
	filesProgress: 'sync:files:progress',
	filesComplete: 'sync:files:complete',
	filesError: 'sync:files:error'
} as const;
