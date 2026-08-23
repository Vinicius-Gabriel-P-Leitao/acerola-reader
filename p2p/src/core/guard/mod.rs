//! Middleware de autorização e filtros de conexão.
//!
//! Os `Guards` funcionam como uma barreira que valida conexões entrantes
//! (e possivelmente sainte) antes delas ocuparem recursos da aplicação,
//! permitindo implementar firewalls P2P, whitelists, blacklists ou
//! verificação de chaves com facilidade.

use std::{future::Future, pin::Pin};

use crate::infra::{error::ConnectionError, peer::PeerId};

pub mod open;
pub mod tofu;

/// Contexto passado para a função de validação (Guard) ao receber uma conexão.
///
/// Contém o `PeerId` do nó que está tentando se conectar, além de um
/// campo genérico `data` que futuramente pode abrigar metadata adicional.
pub struct ConnectionContext<T> {
    /// O identificador público do par na rede.
    pub peer_id: PeerId,
    /// Espaço reservado para dados complementares no contexto (atualmente não utilizado).
    pub data: T,
}

/// Alias para o retorno assíncrono de um Guard.
pub type BoxFuture<'a, T> = Pin<Box<dyn Future<Output = T> + Send + 'a>>;

/// O tipo da closure (validadora) que é invocada pela biblioteca para autorizar tráfego.
///
/// Um `BoxedValidator` deve consumir o contexto da conexão e retornar uma `BoxFuture`
/// assíncrona que resulta em `Ok(())` se a conexão for aceita, ou `Err(ConnectionError::AuthDenied)`
/// caso ela deva ser rejeitada precocemente.
pub type BoxedValidator = Box<
    dyn for<'a> Fn(&'a ConnectionContext<()>) -> BoxFuture<'a, Result<(), ConnectionError>>
        + Send
        + Sync,
>;

#[cfg(test)]
mod tests {
    use super::*;

    fn make_ctx() -> ConnectionContext<()> {
        ConnectionContext {
            peer_id: PeerId { id: "test-peer".to_string(), device_id: None },
            data: (),
        }
    }

    #[tokio::test]
    async fn custom_validator_denies_connection() {
        let deny: BoxedValidator = Box::new(|_ctx| {
            Box::pin(async { Err(ConnectionError::AuthDenied("test deny".into())) })
        });
        let ctx = make_ctx();
        assert!(deny(&ctx).await.is_err());
    }

    #[tokio::test]
    async fn validator_allows_or_denies_by_peer_id() {
        let allow: BoxedValidator = Box::new(|ctx| {
            let allowed = ctx.peer_id.id == "trusted-peer";
            Box::pin(async move {
                if allowed {
                    Ok(())
                } else {
                    Err(ConnectionError::AuthDenied("not trusted".into()))
                }
            })
        });

        let trusted = ConnectionContext {
            peer_id: PeerId { id: "trusted-peer".to_string(), device_id: None },
            data: (),
        };
        let unknown = ConnectionContext {
            peer_id: PeerId { id: "unknown-peer".to_string(), device_id: None },
            data: (),
        };

        assert!(allow(&trusted).await.is_ok());
        assert!(allow(&unknown).await.is_err());
    }
}
