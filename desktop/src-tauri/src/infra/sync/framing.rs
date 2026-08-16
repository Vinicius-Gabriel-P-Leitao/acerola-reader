use std::{path::Path, time::Duration};

use futures::SinkExt;
use serde::{de::DeserializeOwned, Serialize};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
use tokio_stream::StreamExt;
use tokio_util::codec::{FramedRead, FramedWrite, LengthDelimitedCodec};

use crate::infra::error::RpcError;

/// Tamanho máximo de um frame (mensagens JSON e chunks de arquivo), em bytes.
pub const MAX_FRAME_LEN: usize = 1024 * 1024;
/// Tamanho de cada chunk ao transmitir um arquivo em streaming — mantém o uso de
/// memória constante independente do tamanho do cbz/cbr sendo transferido.
pub const FILE_CHUNK_SIZE: usize = 256 * 1024;

const FRAME_TIMEOUT: Duration = Duration::from_secs(30);

pub type FramedReader = FramedRead<Box<dyn AsyncRead + Send + Unpin>, LengthDelimitedCodec>;
pub type FramedWriter = FramedWrite<Box<dyn AsyncWrite + Send + Unpin>, LengthDelimitedCodec>;

fn codec() -> LengthDelimitedCodec {
    LengthDelimitedCodec::builder().max_frame_length(MAX_FRAME_LEN).new_codec()
}

pub fn framed_reader(recv: Box<dyn AsyncRead + Send + Unpin>) -> FramedReader {
    FramedRead::new(recv, codec())
}

pub fn framed_writer(send: Box<dyn AsyncWrite + Send + Unpin>) -> FramedWriter {
    FramedWrite::new(send, codec())
}

pub async fn write_json<T: Serialize>(
    writer: &mut FramedWriter, value: &T,
) -> Result<(), RpcError> {
    let bytes = serde_json::to_vec(value)?;
    writer.send(bytes.into()).await?;
    Ok(())
}

pub async fn read_json<T: DeserializeOwned>(reader: &mut FramedReader) -> Result<T, RpcError> {
    let bytes = tokio::time::timeout(FRAME_TIMEOUT, reader.next())
        .await
        .map_err(|_| RpcError::Stream("timeout waiting for frame".into()))?
        .ok_or_else(|| RpcError::Stream("stream closed".into()))??;

    Ok(serde_json::from_slice(&bytes)?)
}

pub async fn write_bytes(writer: &mut FramedWriter, bytes: &[u8]) -> Result<(), RpcError> {
    writer.send(bytes.to_vec().into()).await?;
    Ok(())
}

pub async fn read_bytes(reader: &mut FramedReader) -> Result<Vec<u8>, RpcError> {
    let bytes = tokio::time::timeout(FRAME_TIMEOUT, reader.next())
        .await
        .map_err(|_| RpcError::Stream("timeout waiting for frame".into()))?
        .ok_or_else(|| RpcError::Stream("stream closed".into()))??;

    Ok(bytes.to_vec())
}

/// Envia o conteúdo de `path` em chunks de [`FILE_CHUNK_SIZE`], sem carregar o arquivo
/// inteiro em memória — necessário pra cbz/cbr que podem ter centenas de MB.
pub async fn send_file_bytes(
    writer: &mut FramedWriter, path: &Path, expected_size: u64,
) -> Result<(), RpcError> {
    let mut file = tokio::fs::File::open(path).await?;
    let mut buffer = vec![0u8; FILE_CHUNK_SIZE];
    let mut sent: u64 = 0;

    while sent < expected_size {
        let read = file.read(&mut buffer).await?;
        if read == 0 {
            break;
        }

        write_bytes(writer, &buffer[..read]).await?;
        sent += read as u64;
    }

    Ok(())
}

/// Recebe um arquivo em streaming direto pro disco (`dest`), calculando o blake3 no
/// caminho pra não precisar reler o arquivo depois só pra verificar integridade.
/// Retorna o checksum calculado, que o chamador compara contra o anunciado no manifesto.
pub async fn receive_file_to_disk(
    reader: &mut FramedReader, dest: &Path, expected_size: u64,
) -> Result<String, RpcError> {
    let mut file = tokio::fs::File::create(dest).await?;
    let mut hasher = blake3::Hasher::new();
    let mut received: u64 = 0;

    while received < expected_size {
        let chunk = read_bytes(reader).await?;
        if chunk.is_empty() {
            break;
        }

        file.write_all(&chunk).await?;
        hasher.update(&chunk);
        received += chunk.len() as u64;
    }

    file.flush().await?;
    Ok(hasher.finalize().to_hex().to_string())
}
