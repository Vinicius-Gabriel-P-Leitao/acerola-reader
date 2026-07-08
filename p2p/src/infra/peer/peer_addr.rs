use crate::infra::peer::PeerId;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PeerAddr {
    pub id: PeerId,
    pub addrs: Vec<u8>,
}
