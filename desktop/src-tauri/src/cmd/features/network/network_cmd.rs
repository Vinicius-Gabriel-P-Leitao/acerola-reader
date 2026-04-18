use crate::{
    cmd::events::network::network_status_payload::NetworkStatusPayload,
    core::services::network::network_service::NetworkService, infra::remote::p2p::peer_id::PeerId,
};

use tauri::{AppHandle, Emitter, State};

#[tauri::command]
pub async fn get_network_status(
    app: AppHandle,
    service: State<'_, NetworkService>,
) -> Result<(), String> {
    let mode = service.mode().await;
    let peers = service.connected_peers().await;

    app.emit("network:status", NetworkStatusPayload::from(mode, peers)).unwrap();

    Ok(())
}

#[tauri::command]
pub async fn switch_to_local(service: State<'_, NetworkService>) -> Result<(), String> {
    service.switch_to_local();
    Ok(())
}

#[tauri::command]
pub async fn switch_to_relay(service: State<'_, NetworkService>) -> Result<(), String> {
    service.switch_to_relay();
    Ok(())
}

#[tauri::command]
pub async fn get_local_id(service: State<'_, NetworkService>) -> Result<String, String> {
    Ok(service.local_id().id)
}

#[tauri::command]
pub async fn connect_to_peer(
    service: State<'_, NetworkService>,
    peer_id: String,
    alpn: String,
) -> Result<(), String> {
    service.connect(PeerId { id: peer_id }, alpn.into_bytes());
    Ok(())
}
