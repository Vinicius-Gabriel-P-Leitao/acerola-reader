use std::sync::Arc;

use async_trait::async_trait;
use tokio::{
    io::{AsyncRead, AsyncWrite},
    sync::RwLock,
};
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use super::{read_byte, read_device_info, write_byte, write_device_info, Recv, Writer, PING, PONG};
use crate::{
    core::network::state::NetworkState,
    data::{
        identity::device_info::DeviceInfo,
        protocol::{EventEmitter, ProtocolHandler},
    },
    infra::{error::ConnectionError, peer::PeerId},
};

pub struct RpcServerHandler {
    emit: EventEmitter,
    local_info: DeviceInfo,
    state: Arc<RwLock<NetworkState>>,
}

#[async_trait]
impl ProtocolHandler for RpcServerHandler {
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError> {
        let mut framed_recv = FramedRead::new(recv, LengthDelimitedCodec::new());
        let mut framed_send = FramedWrite::new(send, LengthDelimitedCodec::new());

        self.perform_handshake(peer, &mut framed_send, &mut framed_recv).await?;
        self.exchange_device_info(peer, &mut framed_send, &mut framed_recv).await?;
        self.protocol_loop(&mut framed_send, &mut framed_recv).await?;

        Ok(())
    }
}

impl RpcServerHandler {
    pub fn new(
        emit: EventEmitter, local_info: DeviceInfo, state: Arc<RwLock<NetworkState>>,
    ) -> Self {
        Self { emit, local_info, state }
    }

    async fn perform_handshake(
        &self, peer: &PeerId, send: &mut Writer, recv: &mut Recv,
    ) -> Result<(), ConnectionError> {
        match read_byte(recv).await {
            Ok(PING) => {
                tracing::debug!(layer = "rpc_server", peer = %peer.id, "ping received");
                (self.emit)("rpc:ping_received", peer.id.clone());

                write_byte(send, PONG).await?;
                (self.emit)("rpc:pong_sent", peer.id.clone());
                Ok(())
            },
            Ok(_) => Err(ConnectionError::StreamFailed("unexpected byte before handshake".into())),
            Err(err) => Err(ConnectionError::from(err)),
        }
    }

    async fn exchange_device_info(
        &self, peer: &PeerId, send: &mut Writer, recv: &mut Recv,
    ) -> Result<(), ConnectionError> {
        let device_info = read_device_info(recv).await?;
        write_device_info(send, &self.local_info).await?;
        (self.emit)("rpc:device_info_exchanged", peer.id.clone());

        self.state.write().await.store_device_info(peer.clone(), device_info);
        Ok(())
    }

    async fn protocol_loop(
        &self, send: &mut Writer, recv: &mut Recv,
    ) -> Result<(), ConnectionError> {
        loop {
            match read_byte(recv).await {
                Ok(PING) => {
                    write_byte(send, PONG).await?;
                },
                Ok(_) => break,
                Err(err) => return Err(ConnectionError::from(err)),
            }
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use std::sync::{Arc, Mutex};

    use tokio::{
        sync::RwLock,
        time::{sleep, Duration},
    };

    use super::*;
    use crate::{
        core::network::state::NetworkState, data::protocol::EventEmitter, infra::peer::PeerId,
    };

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string(), device_id: None }
    }

    fn make_device_info(name: &str) -> DeviceInfo {
        DeviceInfo { name: name.to_string(), os: "linux".to_string(), version: "0.0.1".to_string() }
    }

    fn make_state() -> Arc<RwLock<NetworkState>> {
        Arc::new(RwLock::new(NetworkState::new()))
    }

    fn capture_emitter() -> (EventEmitter, Arc<Mutex<Vec<String>>>) {
        let events = Arc::new(Mutex::new(Vec::new()));
        let clone = Arc::clone(&events);

        let emit: EventEmitter = Arc::new(move |event: &str, _: String| {
            clone.lock().unwrap().push(event.to_string());
        });

        (emit, events)
    }

    #[tokio::test]
    async fn servidor_responde_pong_ao_receber_ping() {
        let (client_side, server_side) = tokio::io::duplex(4096);
        let (emit, events) = capture_emitter();
        let server = RpcServerHandler::new(emit, make_device_info("server"), make_state());

        let peer = make_peer("peer-1");
        let peer_s = peer.clone();
        tokio::spawn(async move {
            let (read, write) = tokio::io::split(server_side);
            let _ = server.handle(&peer_s, Box::new(write), Box::new(read)).await;
        });

        let (read, write) = tokio::io::split(client_side);
        use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

        use super::{read_byte, write_byte, write_device_info, PONG};
        let mut recv = FramedRead::new(
            Box::new(read) as Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            LengthDelimitedCodec::new(),
        );
        let mut send = FramedWrite::new(
            Box::new(write) as Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
            LengthDelimitedCodec::new(),
        );

        write_byte(&mut send, super::PING).await.unwrap();
        let byte = read_byte(&mut recv).await.unwrap();
        assert_eq!(byte, PONG);

        write_device_info(&mut send, &make_device_info("client")).await.unwrap();
        sleep(Duration::from_millis(20)).await;

        assert!(events.lock().unwrap().iter().any(|e| e == "rpc:ping_received"));
        assert!(events.lock().unwrap().iter().any(|e| e == "rpc:pong_sent"));
    }

    #[tokio::test]
    async fn servidor_armazena_device_info_do_cliente() {
        let (client_side, server_side) = tokio::io::duplex(4096);
        let (emit, _) = capture_emitter();
        let state = make_state();
        let server = RpcServerHandler::new(emit, make_device_info("server"), Arc::clone(&state));

        let peer = make_peer("peer-1");
        let peer_s = peer.clone();
        tokio::spawn(async move {
            let (read, write) = tokio::io::split(server_side);
            let _ = server.handle(&peer_s, Box::new(write), Box::new(read)).await;
        });

        let (read, write) = tokio::io::split(client_side);
        use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

        use super::{read_byte, write_byte, write_device_info};
        let mut recv = FramedRead::new(
            Box::new(read) as Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            LengthDelimitedCodec::new(),
        );
        let mut send = FramedWrite::new(
            Box::new(write) as Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
            LengthDelimitedCodec::new(),
        );

        write_byte(&mut send, super::PING).await.unwrap();
        read_byte(&mut recv).await.unwrap();
        write_device_info(&mut send, &make_device_info("client-pc")).await.unwrap();

        sleep(Duration::from_millis(30)).await;

        let stored = state.read().await.get_device_info(&peer).map(|d| d.name.clone());
        assert_eq!(stored, Some("client-pc".to_string()));
    }

    #[tokio::test]
    async fn servidor_encerra_se_nao_receber_ping() {
        let (client_side, server_side) = tokio::io::duplex(4096);
        let (emit, _) = capture_emitter();
        let server = RpcServerHandler::new(emit, make_device_info("server"), make_state());

        let peer = make_peer("peer-1");
        let server_task = tokio::spawn(async move {
            let (read, write) = tokio::io::split(server_side);
            server.handle(&peer, Box::new(write), Box::new(read)).await
        });

        drop(client_side);
        sleep(Duration::from_millis(20)).await;
        assert!(server_task.is_finished());
    }
}
