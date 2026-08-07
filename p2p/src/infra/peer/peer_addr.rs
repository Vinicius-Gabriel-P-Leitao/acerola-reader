use crate::infra::peer::PeerId;

/// Endereço de conexão de um peer contendo o identificador do nó e os dados de endereçamento.
#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub struct PeerAddr {
    /// Identificador do peer.
    pub id: PeerId,
    /// Bytes de endereçamento serializado (ex: EndpointAddr do Iroh).
    pub addrs: Vec<u8>,
}
