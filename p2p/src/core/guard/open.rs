use super::BoxedValidator;

pub struct OpenGuard;

impl OpenGuard {
    pub fn into_validator() -> BoxedValidator {
        Box::new(|_ctx| Box::pin(async { Ok(()) }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{api::guard::ConnectionContext, infra::peer::PeerId};

    #[tokio::test]
    async fn open_guard_allows_any_connection() {
        let validator = OpenGuard::into_validator();
        let ctx = ConnectionContext {
            peer_id: PeerId { id: "test-peer".to_string(), device_id: None },
            data: (),
        };
        assert!(validator(&ctx).await.is_ok());
    }
}
