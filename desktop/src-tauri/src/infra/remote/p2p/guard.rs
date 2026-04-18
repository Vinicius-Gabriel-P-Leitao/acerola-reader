use crate::infra::error::messages::connection_error::ConnectionError;
use crate::infra::remote::p2p::connection_context::ConnectionContext;
use std::future::Future;
use std::pin::Pin;

pub type BoxFuture<'a, T> = Pin<Box<dyn Future<Output = T> + Send + 'a>>;

pub type BoxedValidator = Box<
    dyn for<'a> Fn(&'a ConnectionContext<()>) -> BoxFuture<'a, Result<(), ConnectionError>>
        + Send
        + Sync,
>;
