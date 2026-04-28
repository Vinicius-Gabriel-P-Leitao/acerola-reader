use acerola_p2p::api::{error::P2PError, guard::ConnectionContext};

pub async fn token_guard<T>(_ctx: &ConnectionContext<T>) -> Result<(), P2PError> {
    Ok(())
}
