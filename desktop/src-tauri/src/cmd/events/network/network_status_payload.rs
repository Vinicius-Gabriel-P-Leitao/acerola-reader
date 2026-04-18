use crate::core::connection::p2p::state::network_state::NetworkMode;
use crate::infra::remote::p2p::peer_id::PeerId;
use std::collections::HashMap;
use serde::Serialize;

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkConnectionItem {
    pub peer_id: String,
    pub alpn: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkStatusPayload {
    pub mode: String,
    pub connections: Vec<NetworkConnectionItem>,
}

impl NetworkStatusPayload {
    pub fn from(mode: NetworkMode, peers: HashMap<PeerId, Vec<u8>>) -> Self {
        let connections = peers
            .into_iter()
            .map(|(peer, alpn)| NetworkConnectionItem {
                peer_id: peer.id,
                alpn: String::from_utf8_lossy(&alpn).to_string(),
            })
            .collect();

        Self {
            mode: format!("{:?}", mode),
            connections,
        }
    }
}
