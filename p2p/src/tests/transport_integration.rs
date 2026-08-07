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
    async fn two_nodes_exchange_data_on_custom_alpn() {
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
                .inbound(
                    b"test/echo",
                    Arc::new(ReceiverHandler { received: Arc::clone(&received) }),
                )
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
    async fn data_arrives_intact_with_large_payload() {
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
                .inbound(
                    b"test/bulk",
                    Arc::new(ReceiverHandler { received: Arc::clone(&received) }),
                )
                .build()
                .await
                .unwrap();

        node_a.connect(node_b.local_addr().clone(), b"test/bulk").await.unwrap();
        sleep(Duration::from_millis(2000)).await;

        assert_eq!(*received.lock().await, payload);
    }

    #[tokio::test]
    async fn peer_appears_in_node_b_state_after_real_connection() {
        let node_a = build_node("a").await;
        let node_b = build_node("b").await;

        let id_a = node_a.local_id().to_string();
        node_a.connect(node_b.local_addr().clone(), b"acerola/handshake/1").await.unwrap();
        sleep(Duration::from_millis(500)).await;

        let peers = node_b.connected_peers().await;
        assert!(peers.keys().any(|p| p.id == id_a), "node A did not appear in node B state");
    }

    #[tokio::test]
    async fn alpn_without_handler_on_receiver_does_not_break_sender() {
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

        assert!(result.is_ok(), "connect should not fail on sender");
        assert!(
            node_b.connected_peers().await.is_empty(),
            "node B should not have connected peers"
        );
    }

    #[tokio::test]
    async fn real_iroh_nodes_latency_monitoring_integration() {
        crate::tests::init_tracing();

        let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel();
        let emitter_a: EventEmitter = Arc::new(move |event: &str, payload: String| {
            let _ = tx.send((event.to_string(), payload));
        });

        let node_a = AcerolaP2p::builder(emitter_a, IrohTransportBuilder::default(), device("a"))
            .build()
            .await
            .unwrap();

        let node_b = build_node("b").await;
        let id_b = node_b.local_id().to_string();

        node_a.connect(node_b.local_addr().clone(), b"acerola/handshake/1").await.unwrap();
        sleep(Duration::from_millis(1500)).await;

        let peers = node_a.connected_peers().await;
        assert!(peers.keys().any(|p| p.id == id_b), "node B did not appear in node A state");

        let mut events = Vec::new();
        while let Ok(evt) = rx.try_recv() {
            events.push(evt);
        }

        // Verifica que eventos RPC do handshake foram emitidos na conexão real
        assert!(events.iter().any(|(ev, _)| ev == "rpc:ping_sent" || ev == "rpc:device_info_sent"));

        let _ = node_a.shutdown().await;
        let _ = node_b.shutdown().await;
    }
}
