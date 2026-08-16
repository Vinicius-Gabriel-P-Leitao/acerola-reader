export const NETWORK_EVENTS = {
	status: 'network:status',
	historyStarted: 'sync:history:started',
	historyComplete: 'sync:history:complete',
	historyError: 'sync:history:error',
	filesStarted: 'sync:files:started',
	filesProgress: 'sync:files:progress',
	filesComplete: 'sync:files:complete',
	filesError: 'sync:files:error',
	/** Emitido uma vez na inicialização se o keyring do SO não estiver disponível (ver
	 *  `infra::security::MasterKeySource::FallbackFile` no backend) — a chave mestra caiu
	 *  pra um arquivo local sem a proteção extra do SO. Não é um erro fatal (tudo continua
	 *  funcionando, ainda criptografado com AES-GCM), mas o usuário precisa saber. */
	keyringUnavailable: 'security:keyring_unavailable'
} as const;
