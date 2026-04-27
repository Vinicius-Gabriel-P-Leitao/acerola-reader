use async_trait::async_trait;
use iroh::address_lookup::mdns;
use iroh::endpoint::{self, presets};
use iroh::{Endpoint, RelayConfig, RelayMap, RelayUrl, SecretKey};
use secrecy::{ExposeSecret, SecretBox};

use super::transport::IrohTransport;
use crate::core::transport::TransportP2pBuilder;
use crate::infra::error::ConnectionError;

const IDENTITY_DERIVE_CONTEXT: &str = "acerola-p2p 2026 node identity";

/// Construtor configurável para o `IrohTransport`.
pub struct IrohTransportBuilder {
    relay_urls: Vec<String>,
    seed: Option<SecretBox<[u8; 32]>>,
}

impl Default for IrohTransportBuilder {
    fn default() -> Self {
        Self { relay_urls: Vec::new(), seed: None }
    }
}

impl IrohTransportBuilder {
    /// Adiciona uma URL de relay ao pool gerenciado pelo Iroh.
    pub fn relay(mut self, url: &str) -> Self {
        self.relay_urls.push(url.to_string());
        self
    }

    pub fn seed(mut self, seed: [u8; 32]) -> Self {
        self.seed = Some(SecretBox::new(Box::new(seed)));
        self
    }
}

#[async_trait]
impl TransportP2pBuilder for IrohTransportBuilder {
    type Output = IrohTransport;

    async fn build(self, alpns: Vec<Vec<u8>>) -> Result<IrohTransport, ConnectionError> {
        let mut builder = self.build_mode(alpns)?;
        builder = self.apply_secret(builder);

        let endpoint = builder.bind().await?;
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
