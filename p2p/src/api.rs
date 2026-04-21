//! Fachada pública e Builder do nó Acerola.
//!
//! Este módulo exporta ordenadamente todos os primitivos consumidos pelas aplicações finais
//! de modo a ocultar o path complexo das pastas internas. Ele centraliza
//! o padrão de construção (Builder pattern) usado para inicializar uma instância P2P completa.

use std::{collections::{HashMap, HashSet}, sync::Arc};

/// Renomeação transparente dos tipos de exceções manipulados no ecossistema P2P.
pub mod error {
    pub use crate::error::ConnectionError as P2PError;
}
/// Utilitários ligados ao sistema de middlewares (Guards) de rede.
pub mod guard {
    pub use crate::guard::{open_guard, BoxedValidator as Guard, ConnectionContext};
}
/// Encapsulamento da identificação de instâncias ligadas ao P2P.
pub mod peer {
    pub use crate::peer::PeerId as PeerIdentity;
}
/// Interfaces essenciais e contratos que descrevem lógicas customizadas.
pub mod protocol {
    pub use crate::protocol::{EventEmitter, ProtocolHandler as Handler};
}
/// Enums descritivos de tipologias do protocolo.
pub mod network {
    pub use crate::network::state::NetworkMode;
}

use tokio::sync::RwLock;

use crate::{
    api::network::NetworkMode,
    error::ConnectionError,
    guard::BoxedValidator,
    network::{state::NetworkState, NetworkCommand, NetworkManager},
    peer::PeerId,
    protocol::{
        rpc::{RpcClientHandler, RpcServerHandler},
        EventEmitter, ProtocolHandler,
    },
    transport::{iroh::IrohTransport, P2PTransport},
};

/// Estrutura auxiliar para pré-configurar o ecossistema P2P antes da iniciação real no sistema operacional.
///
/// Através desse builder é possível injetar regras de firewall,
/// registrar portas e protocols customizados (handlers ALPN) e repassar 
/// as lógicas de monitoria pro usuário.
pub struct AcerolaP2PBuilder {
    emit: EventEmitter,
    guard: BoxedValidator,
    handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
}

impl AcerolaP2PBuilder {
    /// Gera o molde base definindo handlers padrão vazios e um fallback que não emite erros.
    fn new(emit: EventEmitter) -> Self {
        Self {
            emit,
            // Permite passagem livre global se nenhuma restrição for registrada posteriormente.
            guard: Box::new(|_ctx| Box::pin(async { Ok(()) })),
            handlers_inbound: HashMap::new(),
            handlers_outbound: HashMap::new(),
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
    pub async fn build(self) -> Result<AcerolaP2P, ConnectionError> {
        let transport = Arc::new(IrohTransport::new().await?);
        let local_id = transport.local_id();

        let (mut manager, command_tx, state) =
            NetworkManager::new(Arc::clone(&transport) as Arc<dyn P2PTransport>, self.guard);

        manager.register_inbound(
            b"acerola/rpc",
            Arc::new(RpcServerHandler::new(Arc::clone(&self.emit))),
        );

        manager.register_outbound(
            b"acerola/rpc",
            Arc::new(RpcClientHandler::new(Arc::clone(&self.emit))),
        );

        for (alpn, handler) in self.handlers_inbound {
            manager.register_inbound(&alpn, handler);
        }

        for (alpn, handler) in self.handlers_outbound {
            manager.register_outbound(&alpn, handler);
        }

        tokio::spawn(manager.run());

        Ok(AcerolaP2P { command_tx, local_id, state })
    }
}

/// A Instância consolidada e o Controlador do Nó rodando em background.
///
/// É com este objeto instanciado que a aplicação cliente irá de fato provocar a rede,
/// discando ativamente para outros nós, requisitando o estado global ou mandando
/// comandos administrativos de desligamento ou reorientação (SwitchGuard).
pub struct AcerolaP2P {
    /// O canal atrelado à Worker Thread de Event Loop (Manager).
    command_tx: tokio::sync::mpsc::Sender<NetworkCommand>,
    /// Provisão de leitura dos contadores e views instantâneas de peers.
    state: Arc<RwLock<NetworkState>>,
    /// Identity cacheada desta cópia do servidor para acesso leve (sem Mutex).
    local_id: PeerId,
}

impl AcerolaP2P {
    /// Único ponto de partida da API, devolve uma estrutura passível de configuração.
    pub fn builder(emit: EventEmitter) -> AcerolaP2PBuilder {
        AcerolaP2PBuilder::new(emit)
    }

    /// Retorna a string legível (Base32/Base64, dependendo do backend Iroh) do nó residente.
    pub fn local_id(&self) -> &str {
        &self.local_id.id
    }

    /// Pede ativamente ao daemon de gerência para abrir um pipe QUIC até um determinado Nó.
    ///
    /// Se a resposta for exitosa, as transmissões vão direto pro handler do protocolo (`alpn`) mapeado.
    pub async fn connect(&self, peer_id: &str, alpn: &[u8]) -> Result<(), ConnectionError> {
        let peer = PeerId { id: peer_id.to_string() };
        self.command_tx
            .send(NetworkCommand::Connect { peer, alpn: alpn.to_vec() })
            .await
            .map_err(|_| ConnectionError::Shutdown)
    }

     /// Captura um Snapshot/Cópia pesada dos nós que estão trafegando e seus marcadores de sub-protocolo atrelados.
     pub async fn connected_peers(&self) -> HashMap<PeerId, HashSet<Vec<u8>>> {
        self.state.read().await.peers().clone()
    }

    /// Extrai o modo da interface (Local/Relay) operando no momento.
    pub async fn mode(&self) -> NetworkMode {
        self.state.read().await.mode().clone()
    }

    /// Solicita em runtime ao Daemon a permuta de middleware de restrição sem cair nenhuma thread.
    ///
    /// Ao receber o novo validator o gerente recusa/aceita instantes novas conexões sem derrubar os sockets mantidos.
    pub async fn switch_guard(
        &self, validator: crate::guard::BoxedValidator, mode: NetworkMode,
    ) -> Result<(), ConnectionError> {
        self.command_tx
            .send(NetworkCommand::SwitchGuard { validator, mode })
            .await
            .map_err(|_| ConnectionError::Shutdown)
    }

    /// Aciona a sequência final de drenagem forçando o cancelamento do background Event Loop 
    /// e do serviço nativo na memória.
    pub async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.command_tx.send(NetworkCommand::Shutdown).await.map_err(|_| ConnectionError::Shutdown)
    }
}