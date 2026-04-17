use iroh::endpoint::{
    BindError as IrohBindError,
    ConnectError as IrohConnectError,
    ConnectionError as IrohConnectionError,
};

use crate::infra::error::messages::connection_error::ConnectionError;

impl From<IrohBindError> for ConnectionError {
    fn from(bind_err: IrohBindError) -> Self {
        match bind_err {
            IrohBindError::Sockets { meta, .. } => {
                log::debug!("[IrohTransport::BindError] failed to bind sockets — meta: {:?}", meta);
                ConnectionError::StartupFailed("port unavailable".into())
            }
            IrohBindError::CreateQuicEndpoint { meta, .. } => {
                log::debug!("[IrohTransport::BindError] failed to create QUIC endpoint — meta: {:?}", meta);
                ConnectionError::StartupFailed("failed to create QUIC endpoint".into())
            }
            IrohBindError::CreateNetmonMonitor { meta, .. } => {
                log::debug!("[IrohTransport::BindError] failed to create network monitor — meta: {:?}", meta);
                ConnectionError::StreamFailed("network monitor unavailable".into())
            }
            IrohBindError::InvalidTransportConfig { meta, .. } => {
                log::debug!("[IrohTransport::BindError] invalid transport configuration — meta: {:?}", meta);
                ConnectionError::StartupFailed("invalid transport configuration".into())
            }
            IrohBindError::InvalidCaRootConfig { meta, .. } => {
                log::debug!("[IrohTransport::BindError] invalid CA root configuration — meta: {:?}", meta);
                ConnectionError::StartupFailed("invalid certificate configuration".into())
            }
            bind_err => {
                log::debug!("[IrohTransport::BindError] unmapped error: {:?}", bind_err);
                ConnectionError::StreamFailed(bind_err.to_string())
            }
        }
    }
}

impl From<IrohConnectError> for ConnectionError {
    fn from(connect_err: IrohConnectError) -> Self {
        match connect_err {
            IrohConnectError::Connection { source, .. } => ConnectionError::from(source),
            IrohConnectError::Connect { meta, .. } => {
                log::debug!("[IrohTransport::ConnectError] failed to initiate connection — meta: {:?}", meta);
                ConnectionError::PeerDisconnected
            }
            IrohConnectError::Connecting { meta, .. } => {
                log::debug!("[IrohTransport::ConnectError] handshake failed — meta: {:?}", meta);
                ConnectionError::PeerDisconnected
            }
            connect_err => {
                log::debug!("[IrohTransport::ConnectError] unmapped error: {:?}", connect_err);
                ConnectionError::StreamFailed(connect_err.to_string())
            }
        }
    }
}

impl From<IrohConnectionError> for ConnectionError {
    fn from(connection_err: IrohConnectionError) -> Self {
        match connection_err {
            IrohConnectionError::TimedOut => ConnectionError::Timeout,
            IrohConnectionError::Reset => ConnectionError::PeerDisconnected,
            IrohConnectionError::ConnectionClosed(_) => ConnectionError::PeerDisconnected,
            IrohConnectionError::ApplicationClosed(_) => ConnectionError::PeerDisconnected,
            IrohConnectionError::VersionMismatch => ConnectionError::IncompatibleVersion,
            IrohConnectionError::LocallyClosed => ConnectionError::Shutdown,
            connection_err => ConnectionError::StreamFailed(connection_err.to_string()),
        }
    }
}
