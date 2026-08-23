//! Módulo de componentes centrais da arquitetura do AcerolaP2P.

/// Abstração de armazenamento e transferência de blobs content-addressed.
pub mod blobs;
/// Detecção de informações do dispositivo hospedeiro.
pub mod device;
pub(crate) mod guard;
/// Gerenciamento de rede e estado concorrente.
pub mod network;
/// Persistência de dados e estado.
pub mod storage;
/// Camada de transporte e abstrações P2P.
pub mod transport;
