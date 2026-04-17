use async_trait::async_trait;
use iroh::{endpoint::presets, Endpoint};
use tokio::io::{AsyncRead, AsyncWrite};

use crate::{
    core::connection::peer::transport::P2PTransport,
    infra::{error::translations::connection_error::ConnectionError, remote::types::PeerId},
};

pub struct IrohTransport {
    endpoint: Endpoint,
}

impl IrohTransport {
    pub async fn new() -> Result<Self, ConnectionError> {
        let endpoint = Endpoint::builder(presets::N0).bind().await?;

        Ok(Self { endpoint })
    }
}

#[async_trait]
impl P2PTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        todo!()
    }

    async fn open_bi(
        &self,
        alpn: &[u8],
        peer: &PeerId,
    ) -> Result<
        (
            Box<dyn AsyncWrite + Send + Unpin>,
            Box<dyn AsyncRead + Send + Unpin>,
        ),
        ConnectionError,
    > {
        todo!()
    }

    async fn shutdown(&self) -> Result<(), ConnectionError> {
        todo!()
    }
}
