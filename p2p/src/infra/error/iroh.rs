use iroh::{
    endpoint::{
        BindError as IrohBindError, ConnectError as IrohConnectError,
        ConnectingError as IrohConnectingError, ConnectionError as IrohConnectionError,
    },
    RelayUrlParseError,
};

use super::ConnectionError;

impl From<IrohBindError> for ConnectionError {
    fn from(err: IrohBindError) -> Self {
        match err {
            IrohBindError::Sockets { meta, .. } => {
                log::debug!("[IrohTransport::BindError] failed to bind sockets — meta: {:?}", meta);
                ConnectionError::StartupFailed("port unavailable".into())
            },
            IrohBindError::CreateQuicEndpoint { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] failed to create QUIC endpoint — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("failed to create QUIC endpoint".into())
            },
            IrohBindError::CreateNetmonMonitor { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] failed to create network monitor — meta: {:?}",
                    meta
                );
                ConnectionError::StreamFailed("network monitor unavailable".into())
            },
            IrohBindError::InvalidTransportConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] invalid transport configuration — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("invalid transport configuration".into())
            },
            IrohBindError::InvalidCaRootConfig { meta, .. } => {
                log::debug!(
                    "[IrohTransport::BindError] invalid CA root configuration — meta: {:?}",
                    meta
                );
                ConnectionError::StartupFailed("invalid certificate configuration".into())
            },
            err => {
                log::debug!("[IrohTransport::BindError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectError> for ConnectionError {
    fn from(err: IrohConnectError) -> Self {
        match err {
            IrohConnectError::Connection { source, .. } => ConnectionError::from(source),
            IrohConnectError::Connect { meta, .. } => {
                log::debug!(
                    "[IrohTransport::ConnectError] failed to initiate connection — meta: {:?}",
                    meta
                );
                ConnectionError::PeerDisconnected
            },
            IrohConnectError::Connecting { meta, .. } => {
                log::debug!("[IrohTransport::ConnectError] handshake failed — meta: {:?}", meta);
                ConnectionError::PeerDisconnected
            },
            err => {
                log::debug!("[IrohTransport::ConnectError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectingError> for ConnectionError {
    fn from(err: IrohConnectingError) -> Self {
        match err {
            IrohConnectingError::ConnectionError { source, .. } => ConnectionError::from(source),
            IrohConnectingError::LocallyRejected { .. } => {
                log::warn!("[IrohTransport::ConnectingError] connection rejected locally by guard");
                ConnectionError::AuthDenied
            },
            IrohConnectingError::HandshakeFailure { .. } => {
                log::warn!(
                    "[IrohTransport::ConnectingError] handshake failed — invalid or untrusted peer"
                );
                ConnectionError::AuthDenied
            },
            IrohConnectingError::InternalConsistencyError { .. } => {
                log::debug!("[IrohTransport::ConnectingError] internal consistency error");
                ConnectionError::StreamFailed("internal error".into())
            },
            err => {
                log::debug!("[IrohTransport::ConnectingError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<IrohConnectionError> for ConnectionError {
    fn from(err: IrohConnectionError) -> Self {
        match err {
            IrohConnectionError::TimedOut => {
                log::warn!("[IrohTransport::ConnectionError] connection timed out");
                ConnectionError::Timeout
            },
            IrohConnectionError::Reset => {
                log::warn!("[IrohTransport::ConnectionError] connection reset by peer");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::ConnectionClosed(_) => {
                log::debug!("[IrohTransport::ConnectionError] connection closed by peer");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::ApplicationClosed(_) => {
                log::debug!("[IrohTransport::ConnectionError] connection closed by application");
                ConnectionError::PeerDisconnected
            },
            IrohConnectionError::VersionMismatch => {
                log::warn!("[IrohTransport::ConnectionError] incompatible protocol version");
                ConnectionError::IncompatibleVersion
            },
            IrohConnectionError::LocallyClosed => {
                log::debug!("[IrohTransport::ConnectionError] connection closed locally");
                ConnectionError::Shutdown
            },
            err => {
                log::debug!("[IrohTransport::ConnectionError] unmapped error: {:?}", err);
                ConnectionError::StreamFailed(err.to_string())
            },
        }
    }
}

impl From<RelayUrlParseError> for ConnectionError {
    fn from(relay_err: RelayUrlParseError) -> Self {
        log::debug!(
            "[IrohTransport::RelayUrlParseError] failed to parse relay URL — error: {:?}",
            relay_err
        );

        ConnectionError::StartupFailed("invalid relay URL".into())
    }
}

impl From<getrandom::Error> for ConnectionError {
    fn from(err: getrandom::Error) -> Self {
        log::error!("[generate_seed] system failed to provide secure entropy — error: {:?}", err);
        ConnectionError::StartupFailed("system cannot provide secure entropy".into())
    }
}
