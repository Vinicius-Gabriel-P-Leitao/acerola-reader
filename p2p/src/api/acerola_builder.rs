use std::{collections::HashMap, sync::Arc};

use crate::{
    core::guard::BoxedValidator,
    core::network::manager::NetworkManager,
    core::transport::{iroh::IrohTransportBuilder, P2pTransport, TransportP2pBuilder},
    data::identity::device_info::DeviceInfo,
    data::protocol::{
        rpc::{RpcClientHandler, RpcServerHandler},
        {EventEmitter, ProtocolHandler},
    },
    infra::error::ConnectionError,
};

use super::acerola_p2p::AcerolaP2p;

/// Estrutura auxiliar para pré-configurar o ecossistema P2p antes da iniciação real no sistema operacional.
///
/// Através desse builder é possível injetar regras de firewall,
/// registrar portas e protocols customizados (handlers ALPN) e repassar
/// as lógicas de monitoria pro usuário.
pub struct AcerolaP2pBuilder<TB: TransportP2pBuilder = IrohTransportBuilder>
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
        self.handlers_inbound.insert(alpn.to_vec(), handler);
        self
    }

    /// Acopla um manipulador proativo à pilha, a ser usado toda vez que o software
    /// quiser ativamente invocar um sub-serviço e processar a via dupla ativamente.
    pub fn outbound(mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) -> Self {
        self.handlers_outbound.insert(alpn.to_vec(), handler);
        self
    }

    /// Compila as configurações submetidas e consolida a interface física no sistema operacional (abre as sockets).
    ///
    /// Além de popular a estrutura do `NetworkManager`, ativa de ofício o handler base `acerola/rpc`.
    pub async fn build(self) -> Result<AcerolaP2p, ConnectionError> {
        #[rustfmt::skip]
        let transport = Arc::new(
            self.transport.build(
                self.handlers_inbound.keys().chain(self.handlers_outbound.keys()).cloned().collect(),
            ).await?,
        );

        let local_id = transport.local_id();

        #[rustfmt::skip]
        let (mut manager, command_tx, state) = NetworkManager::new(Arc::clone(&transport) as Arc<dyn P2pTransport>, self.guard);

        manager.register_inbound(
            b"acerola/rpc",
            Arc::new(RpcServerHandler::new(
                Arc::clone(&self.emit),
                self.device_info.clone(),
                Arc::clone(&state),
            )),
        );

        manager.register_outbound(
            b"acerola/rpc",
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::core::transport::iroh::IrohTransportBuilder;
    use crate::data::identity::device_info::DeviceInfo;
    use crate::infra::peer::PeerId;
    use tokio::io::{AsyncRead, AsyncWrite};

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
}
