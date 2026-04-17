use std::marker::PhantomData;

use crate::infra::{
    error::messages::connection_error::ConnectionError, remote::p2p::peer_id::PeerId,
};

// TODO: Talvez mover isso para infra, não penso em lugar bom para deixar isso, por que vai ser um trait que quando eu montar uma conexão ela vai ter que satisfazer essa struct
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
