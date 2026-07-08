//! Módulo de testes unitários e de integração.
//!
//! Contém utilitários (como mocks e stubs) voltados puramente para a testabilidade
//! da biblioteca sem depender do driver físico QUIC ou DNS local.

pub mod api_integration;
pub mod mock_transport;
pub mod transport_integration;

#[cfg(test)]
#[allow(dead_code)]
pub fn init_tracing() {
    let _ = tracing_subscriber::fmt().with_test_writer().try_init();
}
