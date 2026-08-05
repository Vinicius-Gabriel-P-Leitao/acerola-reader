#[cfg(all(test, feature = "iroh"))]
mod run_in_isolation {
    use std::sync::Arc;

    use async_trait::async_trait;
    use tokio::{
        io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt},
        sync::Mutex,
        time::{sleep, Duration},
    };

    use crate::{
        api::{identity::DeviceInfo, AcerolaP2p},
        core::transport::iroh::IrohTransportBuilder,
        data::protocol::{EventEmitter, ProtocolHandler},
        infra::{error::ConnectionError, peer::PeerId},
    };

    fn emitter() -> EventEmitter {
        Arc::new(|_: &str, _: String| {})
    }

    fn device(name: &str) -> DeviceInfo {
        DeviceInfo { name: name.into(), os: "linux".into(), version: "0.0.1".into() }
    }

    async fn build_node(name: &str) -> AcerolaP2p {
        AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device(name))
            .build()
            .await
            .unwrap()
    }

    struct SenderHandler {
        payload: Vec<u8>,
    }

    #[async_trait]
    impl ProtocolHandler for SenderHandler {
        async fn handle(
            &self, _peer: &PeerId, mut send: Box<dyn AsyncWrite + Send + Unpin>,
            mut recv: Box<dyn AsyncRead + Send + Unpin>,
        ) -> Result<(), ConnectionError> {
            send.write_all(&self.payload)
                .await
                .map_err(|e| ConnectionError::StreamFailed(e.to_string()))?;
            send.shutdown().await.map_err(|e| ConnectionError::StreamFailed(e.to_string()))?;
            let mut dummy = Vec::new();
            let _ = recv.read_to_end(&mut dummy).await;
            Ok(())
        }
    }

    struct ReceiverHandler {
        received: Arc<Mutex<Vec<u8>>>,
    }

    #[async_trait]
    impl ProtocolHandler for ReceiverHandler {
        async fn handle(
            &self, _peer: &PeerId, mut send: Box<dyn AsyncWrite + Send + Unpin>,
            mut recv: Box<dyn AsyncRead + Send + Unpin>,
        ) -> Result<(), ConnectionError> {
            let mut buf = Vec::new();
            recv.read_to_end(&mut buf)
                .await
                .map_err(|e| ConnectionError::StreamFailed(e.to_string()))?;
            *self.received.lock().await = buf;
            let _ = send.shutdown().await;
            Ok(())
        }
    }

    #[tokio::test]
    async fn dois_nos_trocam_dados_em_alpn_customizado() {
        crate::tests::init_tracing();
        let received = Arc::new(Mutex::new(Vec::new()));
        let payload = b"hello acerola".to_vec();

        let node_a: AcerolaP2p =
            AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device("a"))
                .outbound(b"test/echo", Arc::new(SenderHandler { payload: payload.clone() }))
                .build()
                .await
                .unwrap();

        let node_b: AcerolaP2p =
            AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device("b"))
                .inbound(b"test/echo", Arc::new(ReceiverHandler { received: Arc::clone(&received) }))
                .build()
                .await
                .unwrap();

        tracing::debug!(node_a_id = %node_a.local_id(), node_b_id = %node_b.local_id(), "initiating test connection");
        let connect_result = node_a.connect(node_b.local_addr().clone(), b"test/echo").await;
        tracing::debug!(result = ?connect_result, "connect completed");
        sleep(Duration::from_millis(1500)).await;

        let received_data = received.lock().await.clone();
        tracing::debug!(len = received_data.len(), "data received");
        assert_eq!(received_data, payload);
    }

    #[tokio::test]
    async fn dados_chegam_integros_com_payload_grande() {
        let received = Arc::new(Mutex::new(Vec::new()));
        let payload = vec![0xABu8; 64 * 1024];

        let node_a: AcerolaP2p =
            AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device("a"))
                .outbound(b"test/bulk", Arc::new(SenderHandler { payload: payload.clone() }))
                .build()
                .await
                .unwrap();

        let node_b: AcerolaP2p =
            AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device("b"))
                .inbound(b"test/bulk", Arc::new(ReceiverHandler { received: Arc::clone(&received) }))
                .build()
                .await
                .unwrap();

        node_a.connect(node_b.local_addr().clone(), b"test/bulk").await.unwrap();
        sleep(Duration::from_millis(2000)).await;

        assert_eq!(*received.lock().await, payload);
    }

    #[tokio::test]
    async fn peer_aparece_no_state_de_node_b_apos_conexao_real() {
        let node_a = build_node("a").await;
        let node_b = build_node("b").await;

        let id_a = node_a.local_id().to_string();
        node_a.connect(node_b.local_addr().clone(), b"acerola/handshake/1").await.unwrap();
        sleep(Duration::from_millis(500)).await;

        let peers = node_b.connected_peers().await;
        assert!(peers.keys().any(|p| p.id == id_a), "node A não apareceu no state de node B");
    }

    #[tokio::test]
    async fn alpn_sem_handler_no_receiver_nao_quebra_sender() {
        let node_a: AcerolaP2p =
            AcerolaP2p::builder(emitter(), IrohTransportBuilder::default(), device("a"))
                .outbound(b"test/ghost", Arc::new(SenderHandler { payload: b"ignored".to_vec() }))
                .build()
                .await
                .unwrap();

        let node_b = build_node("b").await;

        let result: Result<(), ConnectionError> =
            node_a.connect(node_b.local_addr().clone(), b"test/ghost").await;
        sleep(Duration::from_millis(300)).await;

        assert!(result.is_ok(), "connect não deveria falhar no sender");
        assert!(node_b.connected_peers().await.is_empty(), "node B não deveria ter peers conectados");
    }
}
