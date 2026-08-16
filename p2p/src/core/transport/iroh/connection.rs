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

/// Um stream bidirecional já aceito numa conexão, aguardando ser entregue ao `NetworkManager`.
///
/// Diferente da versão anterior, `send`/`recv` já foram obtidos via `Connection::accept_bi()`
/// *antes* deste valor existir — quem aceita repetidamente novos streams numa mesma conexão
/// (para permitir reaproveitamento, ver `IrohTransport`) é um loop de fundo dedicado por conexão,
/// não o `NetworkManager`. `accept_bi()` aqui só desempacota o que já foi aceito.
pub struct IrohIncoming {
    send: endpoint::SendStream,
    recv: endpoint::RecvStream,
    conn: Arc<endpoint::Connection>,
    addr: PeerAddr,
    peer: PeerId,
    alpn: Vec<u8>,
}

impl IrohIncoming {
    #[allow(clippy::too_many_arguments)]
    pub(crate) fn new(
        send: endpoint::SendStream, recv: endpoint::RecvStream, conn: Arc<endpoint::Connection>,
        peer: PeerId, addr: PeerAddr, alpn: Vec<u8>,
    ) -> Self {
        Self { send, recv, conn, peer, addr, alpn }
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
        Ok((
            Box::new(ConnectionWriter::new(self.send, Arc::clone(&self.conn))),
            Box::new(ConnectionReader::new(self.recv, self.conn)),
        ))
    }
}
