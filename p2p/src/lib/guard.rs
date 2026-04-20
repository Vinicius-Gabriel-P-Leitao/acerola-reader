use std::future::Future;
use std::pin::Pin;

use crate::error::ConnectionError;
use crate::peer::PeerId;

pub struct ConnectionContext<T> {
    pub peer_id: PeerId,
    pub data: T,
}

pub type BoxFuture<'a, T> = Pin<Box<dyn Future<Output = T> + Send + 'a>>;

pub type BoxedValidator = Box<
    dyn for<'a> Fn(&'a ConnectionContext<()>) -> BoxFuture<'a, Result<(), ConnectionError>>
        + Send
        + Sync,
>;

pub async fn open_guard<T>(_ctx: &ConnectionContext<T>) -> Result<(), ConnectionError> {
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_ctx() -> ConnectionContext<()> {
        ConnectionContext { peer_id: PeerId { id: "test-peer".to_string() }, data: () }
    }

    #[tokio::test]
    async fn open_guard_permite_qualquer_conexao() {
        let ctx = make_ctx();
        assert!(open_guard(&ctx).await.is_ok());
    }

    #[tokio::test]
    async fn validator_customizado_nega_conexao() {
        let deny: BoxedValidator =
            Box::new(|_ctx| Box::pin(async { Err(ConnectionError::AuthDenied) }));
        let ctx = make_ctx();
        assert!(deny(&ctx).await.is_err());
    }

    #[tokio::test]
    async fn validator_permite_ou_nega_por_peer_id() {
        let allow: BoxedValidator = Box::new(|ctx| {
            let allowed = ctx.peer_id.id == "trusted-peer";
            Box::pin(async move {
                if allowed { Ok(()) } else { Err(ConnectionError::AuthDenied) }
            })
        });

        let trusted = ConnectionContext { peer_id: PeerId { id: "trusted-peer".to_string() }, data: () };
        let unknown = ConnectionContext { peer_id: PeerId { id: "unknown-peer".to_string() }, data: () };

        assert!(allow(&trusted).await.is_ok());
        assert!(allow(&unknown).await.is_err());
    }
}
