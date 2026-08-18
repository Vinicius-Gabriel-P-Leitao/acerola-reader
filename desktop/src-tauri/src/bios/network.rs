use std::{
    path::{Path, PathBuf},
    sync::Arc,
};

use acerola_p2p::api::{
    guard::{TofuGuard, TrustedPeerStore},
    identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
    storage::P2PStorage,
    transport::IrohTransportBuilder,
    AcerolaP2p,
};
use tauri::{Emitter, Manager};

use crate::{
    bios::scopes::{read_library_path, read_relay_url_override},
    core::services::{
        network::{NetworkService, NetworkServiceApi},
        sync::{file_sync::FileSyncService, history_sync::HistorySyncService},
    },
    data::repositories::sync::sync_history_log_repo::SyncHistoryLogRepository,
    infra::{
        error::ComicError,
        security::{
            get_or_create_master_key,
            p2p_storage::{SecureP2pStorage, SharedP2pStorage},
            trusted_store::SecureTrustedStore,
            MasterKeySource,
        },
        sync::protocol::{
            file_handler::{FileSyncInbound, FileSyncOutbound},
            file_session_guard::FileSyncSessionGuard,
            history_handler::{HistorySyncInbound, HistorySyncOutbound},
            FILE_SYNC_ALPN, HISTORY_SYNC_ALPN,
        },
    },
};

/// Relay oficial do Acerola — default sempre disponível, sem exigir nenhuma configuração.
pub const DEFAULT_RELAY_URL: &str = "https://relay.acerola-comic.com";

/// Nome do antigo arquivo de seed em texto puro — só é lido uma vez pra migrar pra
/// `identity.enc` (ver [`migrate_plaintext_seed_if_present`]); depois disso deixa de existir.
const LEGACY_PLAINTEXT_SEED_FILE_NAME: &str = "p2p-seed.key";

/// Se `p2p-seed.key` (formato antigo, texto puro) ainda existir e `identity.enc` (formato
/// novo, criptografado) ainda não existir, migra o seed pro storage seguro e apaga o
/// arquivo antigo. Sem isso, quem já tinha pareado dispositivos perderia a identidade
/// P2P atual no primeiro update e precisaria re-parear tudo do zero.
async fn migrate_plaintext_seed_if_present(
    app_data_directory: &Path, secure_storage: &SecureP2pStorage,
) -> Result<(), ComicError> {
    let legacy_path = app_data_directory.join(LEGACY_PLAINTEXT_SEED_FILE_NAME);
    if !legacy_path.exists() {
        return Ok(());
    }

    if secure_storage.load_identity().await.ok().flatten().is_some() {
        // Já migrado numa execução anterior (ex.: apagar o arquivo antigo falhou) — só limpa.
        std::fs::remove_file(&legacy_path).ok();
        return Ok(());
    }

    let legacy_seed = std::fs::read(&legacy_path).map_err(ComicError::Io)?;
    if legacy_seed.len() != 32 {
        tracing::warn!(
            "[Bios::Network] Legacy p2p-seed.key has unexpected length ({} bytes), ignoring",
            legacy_seed.len()
        );
        return Ok(());
    }

    secure_storage.save_identity(&legacy_seed).await.map_err(|err| {
        ComicError::SystemFailure(format!("failed to migrate legacy p2p seed: {err}"))
    })?;
    std::fs::remove_file(&legacy_path).ok();
    tracing::info!(
        "[Bios::Network] Migrated legacy plaintext p2p-seed.key into encrypted identity.enc"
    );

    Ok(())
}

pub async fn setup_network(app_handle: &tauri::AppHandle) -> Result<(), ComicError> {
    let app_handle_clone = app_handle.clone();

    let event_emitter: acerola_p2p::api::protocol::EventEmitter =
        Arc::new(move |event_name, event_data| {
            app_handle_clone.emit(event_name, event_data).ok();
        });

    let app_data_directory =
        app_handle.path().app_data_dir().unwrap_or_else(|_| PathBuf::from("."));

    let (master_key, master_key_source) = get_or_create_master_key(&app_data_directory)?;
    // Guardado como estado gerenciado (não só emitido como evento) porque `setup_network`
    // roda numa task assíncrona separada (`bios/mod.rs::setup_runtime`) que pode terminar
    // antes ou depois do frontend montar a tela de Rede — um evento puro seria perdido se
    // emitido antes de qualquer listener existir. `get_security_status` (comando) consulta
    // isso sob demanda; o evento abaixo cobre o caso do frontend já estar ouvindo.
    app_handle.manage(master_key_source);
    if master_key_source == MasterKeySource::FallbackFile {
        // Sem keyring de verdade disponível (ex.: Linux/Hyprland sem gnome-keyring/kwallet
        // rodando) — nunca falha silenciosamente, mesmo princípio do VS Code: avisa o
        // usuário que a chave mestra caiu pro disco sem a proteção extra do SO.
        app_handle.emit("security:keyring_unavailable", ()).ok();
    }

    // Registrado ANTES de abrir trust/peer storage de propósito: `get_sync_history_log` é
    // um recurso independente da identidade/confiança P2P (é só o log local de sessões de
    // sync já ocorridas) — se ele ficasse depois, uma falha real de decrypt em
    // `trusted.enc`/`peers.enc` (`?` abaixo) derrubaria esse comando também como dano
    // colateral, mesmo sem relação nenhuma com o motivo real da falha.
    let database_pool = app_handle.state::<sqlx::SqlitePool>().inner().clone();
    let sync_log_repo = SyncHistoryLogRepository::new(database_pool.clone());
    app_handle.manage(sync_log_repo.clone());

    let trusted_store = Arc::new(
        SecureTrustedStore::open(&app_data_directory, master_key).map_err(ComicError::Io)?,
    );
    let secure_p2p_storage =
        Arc::new(SecureP2pStorage::open(&app_data_directory, master_key).map_err(ComicError::Io)?);
    migrate_plaintext_seed_if_present(&app_data_directory, &secure_p2p_storage).await?;

    // O relay próprio (`relay.acerola-comic.com`) é o default; usuários avançados podem
    // apontar pra outro relay via a tela de Rede, persistido em `settings.json` como
    // `relay_url`. Só é lido na inicialização — trocar em runtime não é suportado.
    let relay_url =
        read_relay_url_override(&app_data_directory).unwrap_or(DEFAULT_RELAY_URL.to_string());
    tracing::info!("[Bios::Network] Using relay: {}", relay_url);

    // Sem `.seed(...)` aqui — o builder resolve a identidade sozinho a partir do
    // `.storage(...)` abaixo (`acerola_builder.rs::resolve_identity`): carrega o seed
    // salvo em `identity.enc` se existir, ou gera um novo e já persiste criptografado.
    let transport_builder = IrohTransportBuilder::default().relay(&relay_url);

    let device_information =
        DefaultDeviceInfoProvider::new("0.0.1-beta").provide().map_err(|device_error| {
            ComicError::SystemFailure(format!("Failed to read device info: {:?}", device_error))
        })?;

    // `database_pool`/`sync_log_repo` já resolvidos mais acima, antes da abertura do
    // trust/peer storage.
    let library_root = read_library_path(&app_data_directory)
        .unwrap_or_else(|| app_data_directory.join("library"));

    let history_sync_service = HistorySyncService::new(database_pool.clone());
    let file_sync_service = FileSyncService::new(database_pool, library_root);
    // Compartilhado entre inbound e outbound: garante que só uma sessão de sync-files por
    // peer rode por vez, nos dois sentidos (ver `file_session_guard.rs`).
    let file_sync_session_guard = FileSyncSessionGuard::new();

    let p2p_node = match tokio::time::timeout(
        std::time::Duration::from_secs(10),
        AcerolaP2p::builder(Arc::clone(&event_emitter), transport_builder, device_information)
            .guard(
                TofuGuard::new(Arc::clone(&trusted_store) as Arc<dyn TrustedPeerStore>)
                    .into_validator(),
            )
            .storage(SharedP2pStorage(Arc::clone(&secure_p2p_storage)))
            .inbound(
                HISTORY_SYNC_ALPN,
                Arc::new(HistorySyncInbound::new(
                    Arc::clone(&event_emitter),
                    history_sync_service.clone(),
                    sync_log_repo.clone(),
                )),
            )
            .outbound(
                HISTORY_SYNC_ALPN,
                Arc::new(HistorySyncOutbound::new(
                    Arc::clone(&event_emitter),
                    history_sync_service,
                    sync_log_repo.clone(),
                )),
            )
            .inbound(
                FILE_SYNC_ALPN,
                Arc::new(FileSyncInbound::new(
                    Arc::clone(&event_emitter),
                    file_sync_service.clone(),
                    sync_log_repo.clone(),
                    Arc::clone(&file_sync_session_guard),
                )),
            )
            .outbound(
                FILE_SYNC_ALPN,
                Arc::new(FileSyncOutbound::new(
                    Arc::clone(&event_emitter),
                    file_sync_service,
                    sync_log_repo,
                    file_sync_session_guard,
                )),
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
        Arc::new(NetworkService::new(Arc::new(p2p_node), secure_p2p_storage));
    app_handle.manage(network_service);

    tracing::info!("[Bios::Network] P2P network service initialized successfully");

    Ok(())
}
