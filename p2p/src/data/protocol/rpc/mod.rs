pub(crate) mod client;
pub(crate) mod server;

pub use client::RpcClientHandler;
use futures::sink::SinkExt;
pub use server::RpcServerHandler;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use crate::{data::identity::device_info::DeviceInfo, infra::error::RpcError};

pub(crate) const PING: u8 = 0x01;
pub(crate) const PONG: u8 = 0x02;

pub(crate) type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
pub(crate) type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

pub(crate) async fn read_byte(recv: &mut Recv) -> Result<u8, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    bytes.first().copied().ok_or(RpcError::Stream("empty frame".into()))
}

pub(crate) async fn read_device_info(recv: &mut Recv) -> Result<DeviceInfo, RpcError> {
    let bytes = recv.next().await.ok_or(RpcError::Stream("stream closed".into()))??;
    Ok(serde_json::from_slice(&bytes)?)
}

pub(crate) async fn write_byte(send: &mut Writer, byte: u8) -> Result<(), RpcError> {
    send.send(vec![byte].into()).await?;
    Ok(())
}

pub(crate) async fn write_device_info(
    send: &mut Writer, device: &DeviceInfo,
) -> Result<(), RpcError> {
    let bytes = serde_json::to_vec(device)?;
    send.send(bytes.into()).await?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ping_byte_diferente_de_pong() {
        assert_ne!(PING, PONG);
    }

    #[test]
    fn bytes_sao_valores_esperados() {
        assert_eq!(PING, 0x01);
        assert_eq!(PONG, 0x02);
    }
}
