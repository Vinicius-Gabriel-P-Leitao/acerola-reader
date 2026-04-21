//! Definição da identidade de um nó (Peer) na rede P2P.
//!
//! Este módulo provê a estrutura `PeerId`, que encapsula o identificador
//! de um nó, sendo a principal forma de referenciar destinos.

use std::fmt;

/// Representa a identidade pública de um nó na rede.
///
/// O `PeerId` contém o identificador subjacente da camada de rede (ex: Iroh PublicKey
/// formatado). Implementa traits de equivalência e hashing para uso como chave
/// no rastreamento do estado das conexões ativas.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct PeerId {
    /// Identificador único (geralmente uma string em base32).
    pub id: String,
}

impl fmt::Display for PeerId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.id)
    }
}