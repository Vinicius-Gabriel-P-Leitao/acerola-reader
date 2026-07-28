use acerola_p2p::api::{
    guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
    identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
    transport::IrohTransportBuilder,
    AcerolaP2p,
};
use rand::RngCore;
use std::path::PathBuf;
use std::sync::Arc;
use tauri::{Emitter, Manager};

use crate::{
    core::services::network::{NetworkService, NetworkServiceApi},
    infra::error::ComicError,
};

/// Obtém ou gera um seed de 32 bytes dinamicamente e o persiste em `p2p_seed.key`
fn get_or_create_p2p_seed(app_data_directory: &PathBuf) -> Result<[u8; 32], ComicError> {
    let seed_file_path = app_data_directory.join("p2p_seed.key");

    if seed_file_path.exists() {
        if let Ok(file_bytes) = std::fs::read(&seed_file_path) {
            if file_bytes.len() == 32 {
                let mut existing_seed = [0u8; 32];
                existing_seed.copy_from_slice(&file_bytes);

                tracing::info!(
                    "[Bios::Network] Loaded existing P2P seed from {:?}",
                    seed_file_path
                );
                return Ok(existing_seed);
            }
        }
    }

    let mut new_generated_seed = [0u8; 32];
    rand::thread_rng().fill_bytes(&mut new_generated_seed);

    if let Err(write_error) = std::fs::write(&seed_file_path, &new_generated_seed) {
        tracing::error!("[Bios::Network] Failed to save p2p_seed.key: {}", write_error);
    } else {
        tracing::info!("[Bios::Network] Generated and saved new P2P seed at {:?}", seed_file_path);
    }

    Ok(new_generated_seed)
}

pub async fn setup_network(app_handle: &tauri::AppHandle) -> Result<(), ComicError> {
    let app_handle_clone = app_handle.clone();

    let event_emitter: acerola_p2p::api::protocol::EventEmitter =
        Arc::new(move |event_name, event_data| {
            app_handle_clone.emit(event_name, event_data).ok();
        });

    let trusted_store = Arc::new(InMemoryTrustedStore::new());

    let app_data_directory =
        app_handle.path().app_data_dir().unwrap_or_else(|_| PathBuf::from("."));
    let p2p_seed = get_or_create_p2p_seed(&app_data_directory)?;

    let transport_builder =
        IrohTransportBuilder::default().seed(p2p_seed).relay("https://relay.acerola-comic.com");

    let device_information =
        DefaultDeviceInfoProvider::new("0.0.1-beta").provide().map_err(|device_error| {
            ComicError::SystemFailure(format!("Failed to read device info: {:?}", device_error))
        })?;

    let p2p_node = match tokio::time::timeout(
        std::time::Duration::from_secs(10),
        AcerolaP2p::builder(event_emitter, transport_builder, device_information)
            .guard(
                TofuGuard::new(Arc::clone(&trusted_store) as Arc<dyn TrustedPeerStore>)
                    .into_validator(),
            )
            .build(),
    )
    .await
    {
        Ok(Ok(node_instance)) => node_instance,
        Ok(Err(start_error)) => {
            tracing::error!("[Bios::Network] Failed to start P2P node: {:?}", start_error);
            return Err(ComicError::SystemFailure(format!(
                "Failed to start p2p node: {:?}",
                start_error
            )));
        },
        Err(timeout_error) => {
            tracing::error!(
                "[Bios::Network] Timeout waiting for AcerolaP2p::build(): {:?}",
                timeout_error
            );
            return Err(ComicError::SystemFailure(
                "TIMEOUT waiting for AcerolaP2p::build()!".to_string(),
            ));
        },
    };

    let network_service: Arc<dyn NetworkServiceApi> =
        Arc::new(NetworkService::new(Arc::new(p2p_node)));
    app_handle.manage(network_service);

    tracing::info!("[Bios::Network] P2P network service initialized successfully");

    Ok(())
}
