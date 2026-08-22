//! Módulo de testes unitários e de integração.

/// Testes de integração da API do Acerola.
#[cfg(test)]
pub mod api_integration;

/// Testes de integração da transferência real de blobs entre nós.
#[cfg(test)]
pub mod blob_transfer;

/// Implementação de transporte simulado em memória (MockTransport).
pub mod mock_transport;

/// Testes de integração de transporte.
#[cfg(test)]
pub mod transport_integration;

/// Testes de estresse do transporte.
#[cfg(test)]
pub mod transport_stress;

/// Inicializa o assinante de logs do `tracing` para captura em testes.
#[cfg(test)]
#[allow(dead_code)]
pub fn init_tracing() {
    let _ = tracing_subscriber::fmt().with_test_writer().try_init();
}
