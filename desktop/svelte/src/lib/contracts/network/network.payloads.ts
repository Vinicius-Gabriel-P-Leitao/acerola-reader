export type DeviceInfoPayload = {
	name: string;
	os: string;
	version: string;
};

export type ConnectedPeerPayload = {
	peerId: string;
	alpn: string;
	device: DeviceInfoPayload | null;
};

export type NetworkStatusPayload = {
	mode: 'local' | 'relay';
	peers: ConnectedPeerPayload[];
};

/** Peer já pareado alguma vez, com o último endereço conhecido — sobrevive a restart e
 *  independe de estar conectado agora (ver `NetworkServiceApi::paired_peers` no backend). */
export type PairedPeerPayload = {
	peerId: string;
	addrs: number[];
};

export type RelayInfo = {
	defaultRelay: string;
	activeRelay: string;
};
