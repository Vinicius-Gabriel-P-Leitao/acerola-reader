use acerola_p2p::api::{
    error::P2pError,
    peer::PeerIdentity,
    protocol::{EventEmitter, Handler},
};
use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::{
    core::services::sync::file_sync::FileSyncService,
    infra::{
        error::RpcError,
        sync::{
            framing::{
                framed_reader, framed_writer, read_json, receive_file_to_disk, send_file_bytes,
                write_json, FramedReader, FramedWriter,
            },
            messages::{FileHeader, FileManifest, FileWantList},
        },
    },
};

/// Envia, em sequência, os arquivos de `wanted` (o que o peer pediu de nós). Se um item não
/// existir mais localmente (corrida com uma deleção concorrente), envia um header
/// "indisponível" (`size: 0`) em vez de abortar a sessão inteira.
async fn send_files(
    writer: &mut FramedWriter, wanted: &[(String, String)], service: &FileSyncService,
    emit: &EventEmitter,
) -> Result<(), P2pError> {
    for (comic_name, chapter) in wanted {
        let resolved = service.resolve_local_file(comic_name, chapter).await?;

        let Some((path, size, checksum, file_name)) = resolved else {
            write_json(
                writer,
                &FileHeader {
                    comic_name: comic_name.clone(),
                    chapter: chapter.clone(),
                    file_name: String::new(),
                    size: 0,
                    checksum: None,
                },
            )
            .await?;
            continue;
        };

        write_json(
            writer,
            &FileHeader {
                comic_name: comic_name.clone(),
                chapter: chapter.clone(),
                file_name,
                size,
                checksum,
            },
        )
        .await?;

        send_file_bytes(writer, &path, size).await?;
        (emit)("sync:files:progress", format!("{} - {}", comic_name, chapter));
    }

    Ok(())
}

/// Recebe, em sequência, `expected_count` arquivos, gravando em streaming num arquivo
/// temporário e só movendo pro destino final depois de verificar o blake3 contra o
/// anunciado no header. Descarta silenciosamente headers "indisponíveis" (`size: 0`) ou
/// arquivos que falharem a verificação de integridade, sem abortar a sessão.
async fn receive_files(
    reader: &mut FramedReader, expected_count: usize, service: &FileSyncService,
    emit: &EventEmitter,
) -> Result<(), P2pError> {
    let incoming_dir = service.library_root().join("synced");
    tokio::fs::create_dir_all(&incoming_dir).await.map_err(RpcError::from)?;

    for _ in 0..expected_count {
        let header: FileHeader = read_json(reader).await?;

        if header.size == 0 {
            continue;
        }

        let temp_path = incoming_dir.join(format!(".incoming-{}.tmp", rand::random::<u64>()));
        let computed_checksum = receive_file_to_disk(reader, &temp_path, header.size).await?;

        if let Some(expected) = &header.checksum {
            if expected != &computed_checksum {
                tokio::fs::remove_file(&temp_path).await.ok();
                (emit)(
                    "sync:files:error",
                    format!("checksum mismatch: {} - {}", header.comic_name, header.chapter),
                );
                continue;
            }
        }

        service
            .persist_received_chapter(
                &header.comic_name,
                &header.chapter,
                &header.file_name,
                &temp_path,
                computed_checksum,
            )
            .await?;

        (emit)("sync:files:progress", format!("{} - {}", header.comic_name, header.chapter));
    }

    Ok(())
}

/// Lado que INICIA a sessão de sync de arquivos. Sequência (cada passo alterna quem
/// escreve/lê, nunca os dois escrevendo ao mesmo tempo no stream):
/// 1. escreve manifesto local → 2. lê manifesto do peer → 3. escreve o que quer → 4. lê o
/// que o peer quer → 5. lê os arquivos que pediu (o peer escreve primeiro nessa fase) →
/// 6. escreve os arquivos que o peer pediu.
pub struct FileSyncOutbound {
    emit: EventEmitter,
    service: FileSyncService,
}

impl FileSyncOutbound {
    pub fn new(emit: EventEmitter, service: FileSyncService) -> Self {
        Self { emit, service }
    }

    async fn run(&self, writer: &mut FramedWriter, reader: &mut FramedReader) -> Result<(), P2pError> {
        let local_manifest = self.service.build_manifest().await?;
        write_json(writer, &local_manifest).await?;

        let peer_manifest: FileManifest = read_json(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        // Fase 1: o peer envia primeiro o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit).await?;

        // Fase 2: eu envio o que o peer pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit).await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for FileSyncOutbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)("sync:files:started", peer.id.clone());

        match self.run(&mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)("sync:files:complete", peer.id.clone());
                Ok(())
            },
            Err(error) => {
                (self.emit)("sync:files:error", error.to_string());
                Err(error)
            },
        }
    }
}

/// Lado que RESPONDE à sessão de sync de arquivos — mesma sequência lógica do outbound,
/// com os papéis de leitura/escrita invertidos em cada passo.
pub struct FileSyncInbound {
    emit: EventEmitter,
    service: FileSyncService,
}

impl FileSyncInbound {
    pub fn new(emit: EventEmitter, service: FileSyncService) -> Self {
        Self { emit, service }
    }

    async fn run(&self, writer: &mut FramedWriter, reader: &mut FramedReader) -> Result<(), P2pError> {
        let peer_manifest: FileManifest = read_json(reader).await?;

        let local_manifest = self.service.build_manifest().await?;
        write_json(writer, &local_manifest).await?;

        let their_wanted: FileWantList = read_json(reader).await?;

        let my_wanted = self.service.diff_wanted(&peer_manifest).await?;
        write_json(writer, &FileWantList { wanted: my_wanted.clone() }).await?;

        // Fase 1: eu envio primeiro o que o peer (outbound) pediu.
        send_files(writer, &their_wanted.wanted, &self.service, &self.emit).await?;

        // Fase 2: eu recebo o que eu pedi.
        receive_files(reader, my_wanted.len(), &self.service, &self.emit).await?;

        Ok(())
    }
}

#[async_trait]
impl Handler for FileSyncInbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        let mut writer = framed_writer(send);
        let mut reader = framed_reader(recv);

        (self.emit)("sync:files:started", peer.id.clone());

        match self.run(&mut writer, &mut reader).await {
            Ok(()) => {
                (self.emit)("sync:files:complete", peer.id.clone());
                Ok(())
            },
            Err(error) => {
                (self.emit)("sync:files:error", error.to_string());
                Err(error)
            },
        }
    }
}
