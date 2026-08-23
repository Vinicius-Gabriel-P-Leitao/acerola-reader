use std::collections::HashSet;

use acerola_p2p::api::{identity::DeviceInfo, network::NetworkMode, peer};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct DeviceInfoPayload {
    pub name: String,
    pub os: String,
    pub version: String,
}

impl From<DeviceInfo> for DeviceInfoPayload {
    fn from(d: DeviceInfo) -> Self {
        Self { name: d.name, os: d.os, version: d.version }
    }
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedPeerPayload {
    pub peer_id: String,
    pub alpn: String,
    pub device: Option<DeviceInfoPayload>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct NetworkStatusPayload {
    pub mode: String,
    pub peers: Vec<ConnectedPeerPayload>,
}

impl NetworkStatusPayload {
    pub fn from(
        mode: NetworkMode, peers: Vec<(peer::PeerIdentity, HashSet<Vec<u8>>, Option<DeviceInfo>)>,
    ) -> Self {
        let mode_str = match mode {
            NetworkMode::Local => "local",
            NetworkMode::Relay => "relay",
        };

        let peer_list: Vec<ConnectedPeerPayload> = peers
            .into_iter()
            .flat_map(|(peer, alpns, device_info)| {
                let peer_id = peer.id;
                let device = device_info.map(DeviceInfoPayload::from);

                alpns.into_iter().map(move |alpn| ConnectedPeerPayload {
                    peer_id: peer_id.clone(),
                    alpn: String::from_utf8_lossy(&alpn).into_owned(),
                    device: device.clone(),
                })
            })
            .collect();

        Self { mode: mode_str.to_string(), peers: peer_list }
    }
}
