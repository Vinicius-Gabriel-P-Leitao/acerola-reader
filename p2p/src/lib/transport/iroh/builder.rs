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

        let relay_configs: Vec<RelayConfig> = self.relay_urls.into_iter().map(|url| {
                url.parse::<RelayUrl>().map(|relay_url| RelayConfig { url: relay_url, quic: None })
            }).collect::<Result<_, iroh::RelayUrlParseError>>()?;

        let endpoint = Endpoint::builder(presets::N0)
            .relay_mode(iroh::RelayMode::Custom(
                RelayMap::from_iter(relay_configs))
            ).alpns(alpns).address_lookup(mdns).bind().await?;

        Ok(IrohTransport::new(endpoint))
    }
}
