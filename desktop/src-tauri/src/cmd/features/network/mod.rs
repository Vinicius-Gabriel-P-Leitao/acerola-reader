use std::sync::Arc;

use tauri::{AppHandle, Emitter, Runtime, State};

use crate::{
    cmd::events::network::NetworkStatusPayload, core::services::network::NetworkServiceApi,
};

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
