use std::sync::Arc;

use tauri::{AppHandle, Emitter, Manager, Runtime, State};

use crate::{
    bios::{network::DEFAULT_RELAY_URL, scopes::read_relay_url_override},
    cmd::events::network::{DeviceInfoPayload, NetworkStatusPayload, RelayInfo},
    core::services::network::NetworkServiceApi,
    data::{
        models::sync::sync_history_log::SyncHistoryLogEntry,
        repositories::sync::sync_history_log_repo::SyncHistoryLogRepository,
    },
    infra::sync::protocol::{FILE_SYNC_ALPN, HISTORY_SYNC_ALPN},
};

const SYNC_HISTORY_LOG_LIMIT: i64 = 200;

#[tauri::command]
pub async fn get_network_status<R: Runtime>(
    app: AppHandle<R>, service: State<'_, Arc<dyn NetworkServiceApi>>,
) -> Result<(), String> {
    let mode = service.mode().await?;
    let peers = service.connected_peers_with_info().await?;

    app.emit("network:status", NetworkStatusPayload::from(mode, peers)).unwrap();

    Ok(())
}

#[tauri::command]
pub async fn switch_to_local(service: State<'_, Arc<dyn NetworkServiceApi>>) -> Result<(), String> {
    service.switch_to_local().await?;
    Ok(())
}

#[tauri::command]
pub async fn switch_to_relay(service: State<'_, Arc<dyn NetworkServiceApi>>) -> Result<(), String> {
    service.switch_to_relay().await?;
    Ok(())
}

#[tauri::command]
pub async fn get_local_id(
    service: State<'_, Arc<dyn NetworkServiceApi>>,
) -> Result<String, String> {
    service.local_id()
}

/// Endereço completo pra gerar o código/QR de pareamento (ver [`NetworkServiceApi::local_addr`]).
#[tauri::command]
pub async fn get_local_addr(
    service: State<'_, Arc<dyn NetworkServiceApi>>,
) -> Result<acerola_p2p::api::peer::PeerAddr, String> {
    service.local_addr()
}

/// Nome/OS/versão deste dispositivo, pra exibir algo legível na tela de Rede em vez do
/// peer id cru.
#[tauri::command]
pub async fn get_local_device_info(
    service: State<'_, Arc<dyn NetworkServiceApi>>,
) -> Result<DeviceInfoPayload, String> {
    Ok(DeviceInfoPayload::from(service.local_device_info()?))
}

/// Retorna o relay padrão do Acerola e o que está ativo de fato (que pode ter sido
/// sobrescrito nas configurações avançadas). Trocar `relay_url` só tem efeito no próximo
/// início do app, já que a lib não suporta trocar a URL do relay em runtime.
#[tauri::command]
pub async fn get_relay_info<R: Runtime>(app: AppHandle<R>) -> Result<RelayInfo, String> {
    let app_data_directory = app.path().app_data_dir().map_err(|error| error.to_string())?;
    let active_relay =
        read_relay_url_override(&app_data_directory).unwrap_or_else(|| DEFAULT_RELAY_URL.to_string());

    Ok(RelayInfo { default_relay: DEFAULT_RELAY_URL.to_string(), active_relay })
}

#[tauri::command]
pub async fn connect_to_peer(
    service: State<'_, Arc<dyn NetworkServiceApi>>, peer_id: String, addrs: Vec<u8>, alpn: String,
) -> Result<(), String> {
    use acerola_p2p::api::peer::{PeerAddr, PeerIdentity};

    let peer_identity = PeerIdentity { id: peer_id, device_id: None };
    let peer_addr = PeerAddr { id: peer_identity, addrs };
    service.connect(peer_addr, alpn.into_bytes()).await?;
    Ok(())
}

/// Dispara uma sessão de sync de histórico com um peer já pareado. O progresso e a
/// conclusão chegam pro frontend via os eventos `sync:history:*`, emitidos direto pelo
/// protocolo (ver `infra::sync::protocol::history_handler`) assim que a conexão é aceita.
#[tauri::command]
pub async fn sync_history(
    service: State<'_, Arc<dyn NetworkServiceApi>>, peer_id: String, addrs: Vec<u8>,
) -> Result<(), String> {
    use acerola_p2p::api::peer::{PeerAddr, PeerIdentity};

    let peer_addr = PeerAddr { id: PeerIdentity { id: peer_id, device_id: None }, addrs };
    service.connect(peer_addr, HISTORY_SYNC_ALPN.to_vec()).await?;
    Ok(())
}

/// Dispara uma sessão de sync de arquivos com um peer já pareado. Progresso via os
/// eventos `sync:files:*`.
#[tauri::command]
pub async fn sync_files(
    service: State<'_, Arc<dyn NetworkServiceApi>>, peer_id: String, addrs: Vec<u8>,
) -> Result<(), String> {
    use acerola_p2p::api::peer::{PeerAddr, PeerIdentity};

    let peer_addr = PeerAddr { id: PeerIdentity { id: peer_id, device_id: None }, addrs };
    service.connect(peer_addr, FILE_SYNC_ALPN.to_vec()).await?;
    Ok(())
}

/// Dispara histórico e arquivos em sequência contra o mesmo peer.
#[tauri::command]
pub async fn sync_all(
    service: State<'_, Arc<dyn NetworkServiceApi>>, peer_id: String, addrs: Vec<u8>,
) -> Result<(), String> {
    use acerola_p2p::api::peer::{PeerAddr, PeerIdentity};

    let peer_identity = PeerIdentity { id: peer_id, device_id: None };

    let history_addr = PeerAddr { id: peer_identity.clone(), addrs: addrs.clone() };
    service.connect(history_addr, HISTORY_SYNC_ALPN.to_vec()).await?;

    let files_addr = PeerAddr { id: peer_identity, addrs };
    service.connect(files_addr, FILE_SYNC_ALPN.to_vec()).await?;

    Ok(())
}

/// Últimas sessões de sync (histórico e arquivos) persistidas — sobrevive a restart,
/// diferente do log ao vivo em memória do frontend (`use-network-sync.svelte.ts`), que só
/// tem os eventos da sessão atual do app.
#[tauri::command]
pub async fn get_sync_history_log(
    repo: State<'_, SyncHistoryLogRepository>,
) -> Result<Vec<SyncHistoryLogEntry>, String> {
    repo.find_recent(SYNC_HISTORY_LOG_LIMIT).await.map_err(|error| error.to_string())
}
