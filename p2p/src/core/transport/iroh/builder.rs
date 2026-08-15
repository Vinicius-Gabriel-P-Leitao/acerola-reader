use async_trait::async_trait;
use iroh::{
    endpoint::{self, presets},
    Endpoint, RelayConfig, RelayMap, RelayUrl, SecretKey,
};
use iroh_mdns_address_lookup as mdns;
use secrecy::{ExposeSecret, SecretBox};

use super::transport::IrohTransport;
use crate::{core::transport::TransportP2pBuilder, infra::error::ConnectionError};

const IDENTITY_DERIVE_CONTEXT: &str = "acerola-p2p 2026 node identity";

/// Construtor configurável para o `IrohTransport`.
#[derive(Default)]
pub struct IrohTransportBuilder {
    relay_urls: Vec<String>,
    seed: Option<SecretBox<[u8; 32]>>,
}

impl IrohTransportBuilder {
    /// Adiciona uma URL de relay ao pool gerenciado pelo Iroh.
    pub fn relay(mut self, url: &str) -> Self {
        self.relay_urls.push(url.to_string());
        self
    }

    /// Define a seed bruta de 32 bytes para a geração de identidade criptográfica do nó.
    pub fn seed(mut self, seed: [u8; 32]) -> Self {
        self.seed = Some(SecretBox::new(Box::new(seed)));
        self
    }
}

#[async_trait]
impl TransportP2pBuilder for IrohTransportBuilder {
    type Output = IrohTransport;

    fn set_seed(&mut self, seed: [u8; 32]) {
        self.seed = Some(SecretBox::new(Box::new(seed)));
    }

    fn get_seed(&self) -> Option<[u8; 32]> {
        self.seed.as_ref().map(|s| *s.expose_secret())
    }

    async fn build(self, alpns: Vec<Vec<u8>>) -> Result<IrohTransport, ConnectionError> {
        tracing::debug!(
            layer = "iroh_transport",
            alpns = ?alpns.iter().map(|it| String::from_utf8_lossy(it)).collect::<Vec<_>>(),
            "building iroh transport"
        );

        let mut builder = self.build_mode(alpns)?;
        builder = self.apply_secret(builder);

        let endpoint = builder.bind().await?;

        tracing::info!(
            layer = "iroh_transport",
            local_id = %endpoint.id(),
            "iroh transport bound successfully"
        );

        Ok(IrohTransport::new(endpoint))
    }
}

#[rustfmt::skip]
impl IrohTransportBuilder {
    fn build_mode(&self, alpns: Vec<Vec<u8>>) -> Result<endpoint::Builder, ConnectionError> {
        let mdns = mdns::MdnsAddressLookup::builder();
        let mut builder = Endpoint::builder(presets::N0).address_lookup(mdns).alpns(alpns);

        builder = if self.relay_urls.is_empty() {
            builder.relay_mode(iroh::RelayMode::Disabled)
        } else {
            let relay_configs: Vec<RelayConfig> = self.relay_urls.iter()
                .map(|url| url.parse::<RelayUrl>().map(
                    RelayConfig::from
                )).collect::<Result<_, iroh::RelayUrlParseError>>()?;

            builder.relay_mode(iroh::RelayMode::Custom(RelayMap::from_iter(relay_configs)))
        };

        Ok(builder)
    }

    fn apply_secret(&self, mut builder: endpoint::Builder) -> endpoint::Builder {
        if let Some(secret) = self.seed.as_ref() {
            let derive = blake3::derive_key(IDENTITY_DERIVE_CONTEXT, secret.expose_secret());
            builder = builder.secret_key(SecretKey::from_bytes(&derive));
        }

        builder
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn validate_build_returns_transport() {
        let transport = IrohTransportBuilder::default()
            .relay("https://relay.test.local")
            .build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_ok());
    }

    #[tokio::test]
    async fn validate_build_without_relay_returns_transport() {
        let transport = IrohTransportBuilder::default().build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_ok());
    }

    #[tokio::test]
    async fn validate_build_invalid_relay_returns_error() {
        let transport = IrohTransportBuilder::default()
            .relay("nao-sou-uma-url-valida")
            .build(vec![b"test/proto".to_vec()]);
        assert!(transport.await.is_err());
    }

    #[test]
    fn get_seed_returns_none_when_unconfigured_and_some_when_set() {
        let builder_unconfigured = IrohTransportBuilder::default();
        assert!(builder_unconfigured.get_seed().is_none());

        let target_seed = [0x55u8; 32];
        let mut builder_configured = IrohTransportBuilder::default();
        builder_configured.set_seed(target_seed);
        assert_eq!(builder_configured.get_seed(), Some(target_seed));
    }
}
