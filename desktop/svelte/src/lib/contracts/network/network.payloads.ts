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

export type RelayInfo = {
	defaultRelay: string;
	activeRelay: string;
};
