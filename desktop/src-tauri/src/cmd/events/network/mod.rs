use std::collections::{HashMap, HashSet};

use acerola_p2p::api::{network::NetworkMode, peer};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedPeerPayload {
    pub peer_id: String,
    pub alpn: String,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct NetworkStatusPayload {
    pub mode: String,
    pub peers: Vec<ConnectedPeerPayload>,
}

impl NetworkStatusPayload {
    pub fn from(mode: NetworkMode, peers: HashMap<peer::PeerIdentity, HashSet<Vec<u8>>>) -> Self {
        let mode_str = match mode {
            NetworkMode::Local => "local",
            NetworkMode::Relay => "relay",
        };

        let peer_list: Vec<ConnectedPeerPayload> = peers
            .into_iter()
            .flat_map(|(peer, alpns)| {
                let peer_id = peer.id;

                alpns.into_iter().map(move |alpn| ConnectedPeerPayload {
                    peer_id: peer_id.clone(),
                    alpn: String::from_utf8_lossy(&alpn).into_owned(),
                })
            })
            .collect();

        Self { mode: mode_str.to_string(), peers: peer_list }
    }
}
