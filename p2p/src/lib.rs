//! Ponto de entrada principal da biblioteca `acerola-p2p`.
//!
//! Este módulo define a árvore de diretórios do projeto,
//! ocultando os detalhes de implementação (marcados como `pub(crate)`)
//! e expondo unicamente a interface pública através do submódulo `api`.
//!
//! A `acerola-p2p` é uma biblioteca voltada para comunicação Peer-to-Peer (P2P),
//! focada especificamente em atender a base dos meus projetos de forma direta.
//! Ela simplifica o processo de descoberta, roteamento de sub-protocolos (via ALPN)
//! e aplicação de regras de segurança (guards) durante as conexões.

#[path = "lib/error.rs"]
pub(crate) mod error;

#[path = "lib/guard.rs"]
pub(crate) mod guard;

#[path = "lib/network.rs"]
pub(crate) mod network;

#[path = "lib/peer.rs"]
pub(crate) mod peer;

#[path = "lib/protocol.rs"]
pub(crate) mod protocol;

#[path = "lib/transport.rs"]
pub(crate) mod transport;

#[cfg(test)]
pub(crate) mod tests;

/// Interface pública da biblioteca.
/// Contém as estruturas essenciais e o construtor `AcerolaP2PBuilder`
/// para inicialização e gestão do nó.
pub mod api;
