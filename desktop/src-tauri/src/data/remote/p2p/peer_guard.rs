use std::marker::PhantomData;

use crate::infra::{
    error::messages::connection_error::ConnectionError, remote::p2p::peer_id::PeerId,
};

pub struct ConnectionContext<T, F>
where
    F: Fn(&T) -> Result<(), ConnectionError>,
{
    _marker: PhantomData<T>,
    peer_id: PeerId,
    validate: F,
}

pub trait ConnectionGuard: Send + Sync {
    fn is_allowed<T, F>(
        &self,
        data: &T,
        ctx: ConnectionContext<T, F>,
    ) -> Result<(), ConnectionError>
    where
        F: Fn(&T) -> Result<(), ConnectionError>;
}

pub struct OpenGuard;

impl ConnectionGuard for OpenGuard {
    fn is_allowed<T, F>(
        &self,
        data: &T,
        ctx: ConnectionContext<T, F>,
    ) -> Result<(), ConnectionError>
    where
        F: Fn(&T) -> Result<(), ConnectionError>,
    {
        Ok(())
    }
}
