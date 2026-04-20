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
