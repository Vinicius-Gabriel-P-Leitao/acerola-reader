use std::{
    pin::Pin,
    sync::Arc,
    task::{Context, Poll},
};

use async_trait::async_trait;
use iroh::endpoint;
use tokio::io::{AsyncRead, AsyncWrite, ReadBuf};

use crate::{
    core::transport::IncomingConnection,
    infra::{
        error::ConnectionError,
        peer::{PeerAddr, PeerId},
    },
};

/// Embalagem da estrutura de conexão transitória `iroh::endpoint::Connection`.
pub struct IrohIncoming {
    pub(crate) conn: endpoint::Connection,
    pub(crate) addr: PeerAddr,
    pub(crate) peer: PeerId,
    pub(crate) alpn: Vec<u8>,
}

impl IrohIncoming {
    pub(crate) fn new(
        conn: endpoint::Connection, peer: PeerId, addr: PeerAddr, alpn: Vec<u8>,
    ) -> Self {
        Self { conn, peer, addr, alpn }
    }
}

/// Wrapper que mantém a instância do `iroh::endpoint::Connection` viva enquanto a stream de escrita for utilizada.
pub struct ConnectionWriter {
    inner: iroh::endpoint::SendStream,
    _conn: Arc<iroh::endpoint::Connection>,
}

impl ConnectionWriter {
    pub(crate) fn new(
        inner: iroh::endpoint::SendStream, conn: Arc<iroh::endpoint::Connection>,
    ) -> Self {
        Self { inner, _conn: conn }
    }
}

#[rustfmt::skip]
impl AsyncWrite for ConnectionWriter {
    fn poll_write(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>, buf: &[u8],
    ) -> Poll<Result<usize, std::io::Error>> {
        Pin::new(&mut self.inner).poll_write(cx, buf).map_err(std::io::Error::other)
    }

    fn poll_flush(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner).poll_flush(cx).map_err(std::io::Error::other)
    }

    fn poll_shutdown(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner).poll_shutdown(cx).map_err(std::io::Error::other)
    }
}

/// Wrapper que mantém a instância do `iroh::endpoint::Connection` viva enquanto a stream de leitura for utilizada.
pub struct ConnectionReader {
    inner: iroh::endpoint::RecvStream,
    _conn: Arc<iroh::endpoint::Connection>,
}

impl ConnectionReader {
    pub(crate) fn new(
        inner: iroh::endpoint::RecvStream, conn: Arc<iroh::endpoint::Connection>,
    ) -> Self {
        Self { inner, _conn: conn }
    }
}

#[rustfmt::skip]
impl AsyncRead for ConnectionReader {
    fn poll_read(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>, buf: &mut ReadBuf<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner).poll_read(cx, buf).map_err(std::io::Error::other)
    }
}

#[async_trait]
impl IncomingConnection for IrohIncoming {
    fn alpn(&self) -> &[u8] {
        &self.alpn
    }

    fn peer(&self) -> &PeerId {
        &self.peer
    }

    fn addr(&self) -> &PeerAddr {
        &self.addr
    }

    async fn accept_bi(
        self: Box<Self>,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let (send, recv) = self.conn.accept_bi().await?;
        let shared_conn = Arc::new(self.conn);

        Ok((
            Box::new(ConnectionWriter::new(send, Arc::clone(&shared_conn))),
            Box::new(ConnectionReader::new(recv, shared_conn)),
        ))
    }
}
