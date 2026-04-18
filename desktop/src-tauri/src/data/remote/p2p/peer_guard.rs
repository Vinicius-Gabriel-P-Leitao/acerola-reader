use std::future::Future;

use crate::infra::{
    error::messages::connection_error::ConnectionError,
    remote::p2p::connection_context::ConnectionContext,
};

pub async fn guard<T, F, Fut>(
    ctx: &ConnectionContext<T>, validate: F,
) -> Result<(), ConnectionError>
where
    F: Fn(&ConnectionContext<T>) -> Fut,
    Fut: Future<Output = Result<(), ConnectionError>>,
{
    validate(ctx).await
}
