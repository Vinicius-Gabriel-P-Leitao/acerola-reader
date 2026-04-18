use crate::infra::remote::p2p::peer_id::PeerId;

pub struct ConnectionContext<T> {
    pub peer_id: PeerId,
    pub data: T,
}
