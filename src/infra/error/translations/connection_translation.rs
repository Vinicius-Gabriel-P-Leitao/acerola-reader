use iroh::endpoint::{
    BindError as IrohBindError, ConnectError as IrohConnectError,
    ConnectingError as IrohConnectingError, ConnectionError as IrohConnectionError,
};

use crate::infra::error::messages::{connection_error::ConnectionError, rpc_error::RpcError};

impl From<IrohBindError> for ConnectionError {
    fn from(bind_err: IrohBindError) -> Self {
        match bind_err {
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
            bind_err => {
                log::debug!("[IrohTransport::BindError] unmapped error: {:?}", bind_err);
                ConnectionError::StreamFailed(bind_err.to_string())
            },
        }
    }
}

impl From<IrohConnectError> for ConnectionError {
    fn from(connect_err: IrohConnectError) -> Self {
        match connect_err {
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
            connect_err => {
                log::debug!("[IrohTransport::ConnectError] unmapped error: {:?}", connect_err);
                ConnectionError::StreamFailed(connect_err.to_string())
            },
        }
    }
}

impl From<IrohConnectingError> for ConnectionError {
    fn from(connecting_err: IrohConnectingError) -> Self {
        match connecting_err {
            IrohConnectingError::ConnectionError { source, .. } => ConnectionError::from(source),
            IrohConnectingError::LocallyRejected { .. } => {
                log::warn!(
                    "[IrohTransport::ConnectingError] connection rejected locally by guard, validate requirements"
                );
                ConnectionError::AuthDenied
            },
            IrohConnectingError::HandshakeFailure { .. } => {
                log::warn!(
                    "[IrohTransport::ConnectingError] handshake failed — invalid or untrusted peer"
                );
                ConnectionError::AuthDenied
            },
            IrohConnectingError::InternalConsistencyError { .. } => {
                log::debug!(
                    "[IrohTransport::ConnectingError] internal consistency error in connecting"
                );
                ConnectionError::StreamFailed("internal error".into())
            },
            connecting_err => {
                log::debug!(
                    "[IrohTransport::ConnectingError] unmapped error: {:?}",
                    connecting_err
                );
                ConnectionError::StreamFailed(connecting_err.to_string())
            },
        }
    }
}

impl From<IrohConnectionError> for ConnectionError {
    fn from(connection_err: IrohConnectionError) -> Self {
        match connection_err {
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
            connection_err => {
                log::debug!(
                    "[IrohTransport::ConnectionError] unmapped error: {:?}",
                    connection_err
                );
                ConnectionError::StreamFailed(connection_err.to_string())
            },
        }
    }
}

impl From<RpcError> for ConnectionError {
    fn from(rpc_err: RpcError) -> Self {
        match rpc_err {
            RpcError::Stream(msg) => ConnectionError::StreamFailed(msg),
            RpcError::Serialize(msg) => ConnectionError::StreamFailed(msg),
            RpcError::Deserialize(msg) => ConnectionError::StreamFailed(msg),
        }
    }
}
