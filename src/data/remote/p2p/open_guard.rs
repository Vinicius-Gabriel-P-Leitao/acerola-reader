use crate::infra::{
    error::messages::connection_error::ConnectionError,
    remote::p2p::connection_context::ConnectionContext,
};

pub async fn open_guard<T>(_ctx: &ConnectionContext<T>) -> Result<(), ConnectionError> {
    Ok(())
}
