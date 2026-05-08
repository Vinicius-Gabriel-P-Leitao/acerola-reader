use std::{collections::HashMap, sync::Arc};

use super::acerola_p2p::AcerolaP2p;
use crate::{
    core::{
        guard::BoxedValidator,
        network::manager::NetworkManager,
        transport::{P2pTransport, TransportP2pBuilder},
    },
    data::{
        identity::device_info::DeviceInfo,
        protocol::{
            rpc::{RpcClientHandler, RpcServerHandler},
            EventEmitter, ProtocolHandler,
        },
    },
    infra::error::ConnectionError,
};

const RESERVED_ALPNS: &[&[u8]] = &[b"acerola/handshake/1"];

/// Estrutura auxiliar para pré-configurar o ecossistema P2p antes da iniciação real no sistema operacional.
///
/// Através desse builder é possível injetar regras de firewall,
/// registrar portas e protocols customizados (handlers ALPN) e repassar
/// as lógicas de monitoria pro usuário.
pub struct AcerolaP2pBuilder<TB: TransportP2pBuilder>
where
    TB::Output: 'static,
{
    pub(super) transport: TB,
    pub(super) emit: EventEmitter,
    pub(super) device_info: DeviceInfo,
    pub(super) guard: BoxedValidator,
    pub(super) handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    pub(super) handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
}

impl<TB: TransportP2pBuilder> AcerolaP2pBuilder<TB> {
    pub(super) fn new(emit: EventEmitter, transport: TB, device_info: DeviceInfo) -> Self {
        Self {
            emit,
            transport,
            device_info,
            handlers_inbound: HashMap::new(),
            handlers_outbound: HashMap::new(),
            guard: Box::new(|_ctx| Box::pin(async { Ok(()) })),
        }
    }

    /// Atribui um componente ou closure Guard para checagem estrita de cada handshake na rede.
    pub fn guard(mut self, validator: BoxedValidator) -> Self {
        self.guard = validator;
        self
    }

    /// Acopla um manipulador passivo de requisições de serviço à pilha.
    /// Dispara somente quando um par iniciar conexão invocando a exata chave `alpn`.
    pub fn inbound(mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) -> Self {
        assert!(
            !RESERVED_ALPNS.contains(&alpn),
            "ALPN {:?} is reserved by the library and cannot be overridden",
            alpn
        );
        self.handlers_inbound.insert(alpn.to_vec(), handler);
        self
    }

    /// Acopla um manipulador proativo à pilha, a ser usado toda vez que o software
    /// quiser ativamente invocar um sub-serviço e processar a via dupla ativamente.
    pub fn outbound(mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) -> Self {
        assert!(
            !RESERVED_ALPNS.contains(&alpn),
            "ALPN {:?} is reserved by the library and cannot be overridden",
            alpn
        );
        self.handlers_outbound.insert(alpn.to_vec(), handler);
        self
    }

    /// Compila as configurações submetidas e consolida a interface física no sistema operacional (abre as sockets).
    ///
    /// Além de popular a estrutura do `NetworkManager`, ativa de ofício o handler base `acerola/handshake/1`.
    pub async fn build(self) -> Result<AcerolaP2p, ConnectionError> {
        let alpns: Vec<Vec<u8>> = RESERVED_ALPNS
            .iter()
            .map(|a| a.to_vec())
            .chain(self.handlers_inbound.keys().cloned())
            .chain(self.handlers_outbound.keys().cloned())
            .collect::<std::collections::HashSet<_>>()
            .into_iter()
            .collect();

        let transport = Arc::new(self.transport.build(alpns).await?);

        let local_id = transport.local_id();

        #[rustfmt::skip]
        let (mut manager, command_tx, state) = NetworkManager::new(Arc::clone(&transport) as Arc<dyn P2pTransport>, self.guard);

        manager.register_inbound(
            b"acerola/handshake/1",
            Arc::new(RpcServerHandler::new(
                Arc::clone(&self.emit),
                self.device_info.clone(),
                Arc::clone(&state),
            )),
        );

        manager.register_outbound(
            b"acerola/handshake/1",
            Arc::new(RpcClientHandler::new(
                Arc::clone(&self.emit),
                self.device_info.clone(),
                Arc::clone(&state),
            )),
        );

        for (alpn, handler) in self.handlers_inbound {
            manager.register_inbound(&alpn, handler);
        }

        for (alpn, handler) in self.handlers_outbound {
            manager.register_outbound(&alpn, handler);
        }

        tokio::spawn(manager.run());

        Ok(AcerolaP2p { command_tx, local_id, state, device_info: self.device_info })
    }
}

#[cfg(all(test, feature = "iroh"))]
mod tests {
    use tokio::io::{AsyncRead, AsyncWrite};

    use super::*;
    use crate::{
        core::transport::iroh::IrohTransportBuilder, data::identity::device_info::DeviceInfo,
        infra::peer::PeerId,
    };

    fn no_op_emitter() -> EventEmitter {
        Arc::new(|_event: &str, _payload: String| {})
    }

    fn test_device_info() -> DeviceInfo {
        DeviceInfo {
            name: "test-device".to_string(),
            os: "linux".to_string(),
            version: "0.0.1".to_string(),
        }
    }

    struct NoOpHandler;

    #[async_trait::async_trait]
    impl ProtocolHandler for NoOpHandler {
        async fn handle(
            &self, _peer: &PeerId, _send: Box<dyn AsyncWrite + Send + Unpin>,
            _recv: Box<dyn AsyncRead + Send + Unpin>,
        ) -> Result<(), ConnectionError> {
            Ok(())
        }
    }

    #[tokio::test]
    async fn build_retorna_no_valido() {
        assert!(AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default(),
            test_device_info()
        )
        .build()
        .await
        .is_ok());
    }

    #[test]
    #[should_panic(expected = "reserved by the library")]
    fn inbound_com_alpn_reservado_causa_panic() {
        AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), test_device_info())
            .inbound(b"acerola/handshake/1", Arc::new(NoOpHandler));
    }

    #[test]
    #[should_panic(expected = "reserved by the library")]
    fn outbound_com_alpn_reservado_causa_panic() {
        AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), test_device_info())
            .outbound(b"acerola/handshake/1", Arc::new(NoOpHandler));
    }

    #[tokio::test]
    async fn build_com_handler_customizado_nao_falha() {
        let result = AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default(),
            test_device_info(),
        )
        .inbound(b"meu/protocolo", Arc::new(NoOpHandler))
        .outbound(b"meu/protocolo", Arc::new(NoOpHandler))
        .build()
        .await;

        assert!(result.is_ok());
    }

    fn capture_emitter() -> (EventEmitter, Arc<std::sync::Mutex<Vec<String>>>) {
        let events = Arc::new(std::sync::Mutex::new(Vec::new()));
        let clone = Arc::clone(&events);
        let emit: EventEmitter = Arc::new(move |event: &str, _: String| {
            clone.lock().unwrap().push(event.to_string());
        });
        (emit, events)
    }

    #[tokio::test]
    async fn handshake_reservado_completa_entre_dois_nos() {
        let (emit_a, events_a) = capture_emitter();
        let (emit_b, events_b) = capture_emitter();

        let node_a =
            AcerolaP2p::builder(emit_a, IrohTransportBuilder::default(), test_device_info())
                .build()
                .await
                .unwrap();

        let node_b =
            AcerolaP2p::builder(emit_b, IrohTransportBuilder::default(), test_device_info())
                .build()
                .await
                .unwrap();

        let id_b = node_b.local_id().to_string();

        // Aguarda mDNS descobrir o peer antes de tentar conectar
        tokio::time::sleep(tokio::time::Duration::from_millis(1500)).await;
        node_a.connect(&id_b, b"acerola/handshake/1").await.unwrap();

        // Aguarda handshake completar
        tokio::time::sleep(tokio::time::Duration::from_millis(1000)).await;

        let ev_a = events_a.lock().unwrap();
        let ev_b = events_b.lock().unwrap();

        assert!(ev_a.iter().any(|e| e == "rpc:ping_sent"), "node A: ping enviado");
        assert!(ev_a.iter().any(|e| e == "rpc:pong_received"), "node A: pong recebido");
        assert!(
            ev_a.iter().any(|e| e == "rpc:device_info_received"),
            "node A: device info recebida"
        );

        assert!(ev_b.iter().any(|e| e == "rpc:ping_received"), "node B: ping recebido");
        assert!(ev_b.iter().any(|e| e == "rpc:pong_sent"), "node B: pong enviado");
        assert!(
            ev_b.iter().any(|e| e == "rpc:device_info_exchanged"),
            "node B: device info trocada"
        );
    }
}
