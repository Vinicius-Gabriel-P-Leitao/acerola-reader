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

#[path = "lib/mod.rs"]
pub(crate) mod acerola;

#[cfg(test)]
pub(crate) mod tests;

/// Interface pública da biblioteca.
/// Contém as estruturas essenciais e o construtor `AcerolaP2PBuilder`
/// para inicialização e gestão do nó.
pub mod api;
