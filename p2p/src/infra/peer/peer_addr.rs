use crate::infra::peer::PeerId;

#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub struct PeerAddr {
    pub id: PeerId,
    pub addrs: Vec<u8>,
}
