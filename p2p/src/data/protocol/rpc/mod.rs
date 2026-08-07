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
pub(crate) const GOODBYE: u8 = 0x03;

pub(crate) type Recv = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
pub(crate) type Writer = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

pub(crate) async fn read_byte(recv: &mut Recv) -> Result<u8, RpcError> {
    let bytes = tokio::time::timeout(std::time::Duration::from_secs(2), recv.next())
        .await
        .map_err(|_| RpcError::Stream("read timeout".into()))?
        .ok_or(RpcError::Stream("stream closed".into()))??;
    bytes.first().copied().ok_or(RpcError::Stream("empty frame".into()))
}

pub(crate) async fn read_device_info(recv: &mut Recv) -> Result<DeviceInfo, RpcError> {
    let bytes = tokio::time::timeout(std::time::Duration::from_secs(2), recv.next())
        .await
        .map_err(|_| RpcError::Stream("read timeout".into()))?
        .ok_or(RpcError::Stream("stream closed".into()))??;
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
    fn ping_byte_different_from_pong() {
        assert_ne!(PING, PONG);
    }

    #[test]
    fn bytes_are_expected_values() {
        assert_eq!(PING, 0x01);
        assert_eq!(PONG, 0x02);
    }

    #[tokio::test]
    async fn test_write_and_read_byte() {
        let (client_io, server_io) = tokio::io::duplex(1024);
        let mut send: Writer = FramedWrite::new(Box::new(client_io), LengthDelimitedCodec::new());
        let mut recv: Recv = FramedRead::new(Box::new(server_io), LengthDelimitedCodec::new());

        write_byte(&mut send, PING).await.expect("write_byte failed");
        drop(send);
        let result = read_byte(&mut recv).await.expect("read_byte failed");
        assert_eq!(result, PING);
    }

    #[tokio::test]
    async fn test_write_and_read_device_info() {
        let (client_io, server_io) = tokio::io::duplex(1024);
        let mut send: Writer = FramedWrite::new(Box::new(client_io), LengthDelimitedCodec::new());
        let mut recv: Recv = FramedRead::new(Box::new(server_io), LengthDelimitedCodec::new());

        let dev_info = DeviceInfo {
            name: "test-device".to_string(),
            os: "linux".to_string(),
            version: "1.0.0".to_string(),
        };

        write_device_info(&mut send, &dev_info).await.expect("write_device_info failed");
        drop(send);
        let result = read_device_info(&mut recv).await.expect("read_device_info failed");
        assert_eq!(result.name, dev_info.name);
        assert_eq!(result.os, dev_info.os);
        assert_eq!(result.version, dev_info.version);
    }
}
