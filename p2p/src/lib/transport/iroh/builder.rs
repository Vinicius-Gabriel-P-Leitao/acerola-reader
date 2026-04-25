use async_trait::async_trait;
use iroh::address_lookup::mdns;
use iroh::endpoint::presets;
use iroh::{Endpoint, RelayConfig, RelayMap, RelayUrl};

use super::transport::IrohTransport;
use crate::error::ConnectionError;
use crate::transport::TransportP2pBuilder;

/// Construtor configurável para o `IrohTransport`.
pub struct IrohTransportBuilder {
    relay_urls: Vec<String>,
}

impl Default for IrohTransportBuilder {
    fn default() -> Self {
        Self { relay_urls: Vec::new() }
    }
}

impl IrohTransportBuilder {
    /// Adiciona uma URL de relay ao pool gerenciado pelo Iroh.
    pub fn relay(mut self, url: &str) -> Self {
        self.relay_urls.push(url.to_string());
        self
    }
}

#[async_trait]
#[rustfmt::skip]
impl TransportP2pBuilder for IrohTransportBuilder {
    type Output = IrohTransport;

    async fn build(self, alpns: Vec<Vec<u8>>) -> Result<IrohTransport, ConnectionError> {
        let mdns = mdns::MdnsAddressLookup::builder();

        let mode = if self.relay_urls.is_empty() {
            iroh::RelayMode::Disabled
        } else {
            let relay_configs: Vec<RelayConfig> = self.relay_urls.into_iter()
                .map(|url| {
                    url.parse::<RelayUrl>().map(|relay_url| RelayConfig {
                        url: relay_url, quic: None
                    })
                }).collect::<Result<_, iroh::RelayUrlParseError>>()?;
            iroh::RelayMode::Custom(RelayMap::from_iter(relay_configs))
        };

        let endpoint = Endpoint::builder(presets::N0)
            .address_lookup(mdns).relay_mode(mode).alpns(alpns)
            .bind().await?;

        Ok(IrohTransport::new(endpoint))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn valida_build_retorna_transport() {
        let transport = IrohTransportBuilder::default()
            .relay("https://relay.test.local")
            .build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_ok());
    }

    #[tokio::test]
    async fn valida_build_sem_relay_retorna_transport() {
        let transport = IrohTransportBuilder::default().build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_ok());
    }

    #[tokio::test]
    async fn valida_build_relay_invalido_retorna_erro() {
        let transport = IrohTransportBuilder::default()
            .relay("nao-sou-uma-url-valida")
            .build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_err());
    }
}
