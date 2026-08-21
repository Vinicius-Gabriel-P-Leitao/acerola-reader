use acerola_p2p::api::{error::P2pError, protocol::EventEmitter};
use serde::de::DeserializeOwned;
use tokio::io::AsyncWrite;

use crate::{
    core::services::sync::file_sync::FileSyncService,
    infra::{
        error::RpcError,
        sync::{
            framing::{
                framed_writer, read_json, receive_file_to_disk, send_file_bytes, write_json,
                FramedReader, FramedWriter,
            },
            messages::{FileHeader, SessionBusy},
        },
    },
};

/// Texto de rejeição usado tanto no evento local (`emit_busy`) quanto na mensagem `SessionBusy`
/// escrita no wire (`write_session_busy`) — mesmo motivo nos dois lugares. Compartilhado entre
/// `acerola/sync-files/1` e `acerola/sync-comic/1`: os dois competem pelo mesmo
/// `FileSyncSessionGuard` por peer (ver `file_session_guard.rs`).
pub const SESSION_BUSY_REASON: &str = "file sync session already in progress for this peer";

/// Valor do campo `error` em `SessionBusy` — mesmo literal usado pelo Android
/// (`protocol/files/exchange.rs::SESSION_BUSY_TAG`), é o que os dois lados usam pra reconhecer
/// essa mensagem específica em vez de um `FileManifest` normal.
const SESSION_BUSY_TAG: &str = "busy";

/// Emitido quando `FileSyncSessionGuard` recusa uma sessão porque já existe uma ativa pro
/// mesmo peer — reaproveita o evento `sync:files:error` já tratado pelo frontend em vez de
/// introduzir um evento novo (ver `use-network-sync.svelte.ts::parseErrorPayload`). O chamador
/// passa o nome do evento (`sync:files:error` ou `sync:comic:error`) porque cada protocolo tem
/// o seu próprio canal.
pub fn emit_busy(emit: &EventEmitter, event_name: &str, peer_id: &str) {
    (emit)(
        event_name,
        serde_json::json!({
            "peerId": peer_id,
            "message": SESSION_BUSY_REASON,
        })
        .to_string(),
    );
}

/// Escreve a mensagem de rejeição de sessão diretamente no stream, no lugar do manifesto —
/// usado quando `FileSyncSessionGuard::try_acquire` já barrou a sessão em `handle()`, antes do
/// resto do protocolo rodar. Recebe `send` cru (não um `FramedWriter` já montado) porque é
/// exatamente o estado em que `handle()` está no momento da rejeição: o stream ainda não foi
/// envolvido em nenhum framing. Melhor esforço: se a escrita falhar (peer já caiu, stream já
/// fechado), ignora — a sessão já foi rejeitada localmente de qualquer forma.
pub async fn write_session_busy(send: Box<dyn AsyncWrite + Send + Unpin>) {
    let mut writer = framed_writer(send);
    let _ = write_json(
        &mut writer,
        &SessionBusy { error: SESSION_BUSY_TAG.to_string(), reason: SESSION_BUSY_REASON.to_string() },
    )
    .await;
}

/// Lê a primeira mensagem do peer, que pode ser tanto a mensagem esperada (`T`: `FileManifest`
/// inteiro ou escopado a um único quadrinho — ver `FileSyncService::build_manifest_for_comic`
/// — ou `ComicSyncRequest` no protocolo `acerola/sync-comic/1`) quanto uma rejeição
/// `SessionBusy` — sem tag de enum no wire (mesma convenção do resto do protocolo), a única
/// forma de saber qual é espiar o JSON bruto antes de decidir o tipo concreto. Só o(s) ponto(s)
/// de leitura inicial em cada `run()` usa(m) isso: depois da primeira mensagem, uma sessão
/// aceita nunca mais manda `SessionBusy`.
pub async fn read_or_busy<T: DeserializeOwned>(reader: &mut FramedReader) -> Result<T, P2pError> {
    let value: serde_json::Value = read_json(reader).await?;

    let is_busy = value.get("error").and_then(|tag| tag.as_str()) == Some(SESSION_BUSY_TAG);
    if is_busy {
        let reason = value.get("reason").and_then(|r| r.as_str()).unwrap_or("peer is busy");
        return Err(P2pError::StreamFailed(format!("peer busy: {reason}")));
    }

    serde_json::from_value(value).map_err(|err| RpcError::Deserialize(err.to_string()).into())
}

/// Envia, em sequência, os arquivos de `wanted` (o que o peer pediu de nós). Se um item não
/// existir mais localmente (corrida com uma deleção concorrente), envia um header
/// "indisponível" (`size: 0`) em vez de abortar a sessão inteira.
pub async fn send_files(
    writer: &mut FramedWriter, wanted: &[(String, String)], service: &FileSyncService,
    emit: &EventEmitter, progress_event: &str,
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
        (emit)(progress_event, format!("{} - {}", comic_name, chapter));
    }

    Ok(())
}

/// Recebe, em sequência, `expected_count` arquivos, gravando em streaming num arquivo
/// temporário e só movendo pro destino final depois de verificar o SHA-256 contra o
/// anunciado no header. Descarta silenciosamente headers "indisponíveis" (`size: 0`) ou
/// arquivos que falharem a verificação de integridade, sem abortar a sessão.
pub async fn receive_files(
    reader: &mut FramedReader, expected_count: usize, service: &FileSyncService,
    emit: &EventEmitter, progress_event: &str, error_event: &str,
) -> Result<(), P2pError> {
    let incoming_dir = service.library_root().join("synced");
    tokio::fs::create_dir_all(&incoming_dir).await.map_err(RpcError::from)?;

    for _ in 0..expected_count {
        let header: FileHeader = read_json(reader).await?;

        tracing::debug!(
            comic_name = %header.comic_name,
            chapter = %header.chapter,
            file_name = %header.file_name,
            "[FileSync] header recebido pelo fio"
        );

        if header.size == 0 {
            continue;
        }

        let temp_path = incoming_dir.join(format!(".incoming-{}.tmp", rand::random::<u64>()));
        let computed_checksum = receive_file_to_disk(reader, &temp_path, header.size).await?;

        if let Some(expected) = &header.checksum {
            if expected != &computed_checksum {
                tokio::fs::remove_file(&temp_path).await.ok();
                (emit)(
                    error_event,
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

        (emit)(progress_event, format!("{} - {}", header.comic_name, header.chapter));
    }

    Ok(())
}
