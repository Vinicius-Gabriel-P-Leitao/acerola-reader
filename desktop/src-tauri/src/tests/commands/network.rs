use std::{
    collections::HashSet,
    sync::{Arc, Mutex},
};

use acerola_p2p::api::{
    identity::DeviceInfo,
    network::NetworkMode,
    peer::{PeerAddr, PeerIdentity},
};
use anyhow::Result;
use async_trait::async_trait;
use serde_json::{json, Value};

use super::support::{
    build_webview, invoke_err, invoke_ok, invoke_ok_value, listen_event, recv_event,
};
use crate::{
    cmd::features::network as network_cmd,
    core::services::network::{ConnectedPeerInfo, NetworkServiceApi},
};

#[derive(Clone)]
struct MockNetworkService {
    state: Arc<Mutex<MockNetworkState>>,
}

struct MockNetworkState {
    local_id: String,
    mode: NetworkMode,
    peers: Vec<ConnectedPeerInfo>,
    last_connection: Option<(String, Vec<u8>)>,
    failure: Option<String>,
}

impl MockNetworkService {
    fn new() -> Self {
        Self {
            state: Arc::new(Mutex::new(MockNetworkState {
                local_id: "local-peer-id".to_string(),
                mode: NetworkMode::Local,
                peers: Vec::new(),
                last_connection: None,
                failure: None,
            })),
        }
    }

    fn fail_next(&self, message: &str) {
        self.state.lock().expect("network mock mutex should not be poisoned").failure =
            Some(message.to_string());
    }

    fn set_mode(&self, mode: NetworkMode) {
        self.state.lock().expect("network mock mutex should not be poisoned").mode = mode;
    }

    fn set_peers(&self, peers: Vec<ConnectedPeerInfo>) {
        self.state.lock().expect("network mock mutex should not be poisoned").peers = peers;
    }

    fn mode(&self) -> NetworkMode {
        self.state.lock().expect("network mock mutex should not be poisoned").mode.clone()
    }

    fn last_connection(&self) -> Option<(String, Vec<u8>)> {
        self.state
            .lock()
            .expect("network mock mutex should not be poisoned")
            .last_connection
            .clone()
    }

    fn take_failure(state: &mut MockNetworkState) -> Result<(), String> {
        if let Some(message) = state.failure.take() {
            Err(message)
        } else {
            Ok(())
        }
    }
}

#[async_trait]
impl NetworkServiceApi for MockNetworkService {
    fn local_id(&self) -> Result<String, String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        Ok(state.local_id.clone())
    }

    async fn connected_peers_with_info(&self) -> Result<Vec<ConnectedPeerInfo>, String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        Ok(state.peers.clone())
    }

    async fn switch_to_local(&self) -> Result<(), String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        state.mode = NetworkMode::Local;
        Ok(())
    }

    async fn switch_to_relay(&self) -> Result<(), String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        state.mode = NetworkMode::Relay;
        Ok(())
    }

    async fn mode(&self) -> Result<NetworkMode, String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        Ok(state.mode.clone())
    }

    async fn connect(&self, peer_addr: PeerAddr, alpn: Vec<u8>) -> Result<(), String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)?;
        state.last_connection = Some((peer_addr.id.id, alpn));
        Ok(())
    }

    async fn shutdown(&self) -> Result<(), String> {
        let mut state = self.state.lock().expect("network mock mutex should not be poisoned");
        Self::take_failure(&mut state)
    }
}

fn mock_network_service() -> Arc<MockNetworkService> {
    Arc::new(MockNetworkService::new())
}

fn build_network_app(
    service: Arc<MockNetworkService>,
) -> Result<(tauri::App<tauri::test::MockRuntime>, tauri::WebviewWindow<tauri::test::MockRuntime>)>
{
    let managed: Arc<dyn NetworkServiceApi> = service;

    build_webview(tauri::test::mock_builder().manage(managed).invoke_handler(
        tauri::generate_handler![
            network_cmd::get_network_status,
            network_cmd::switch_to_local,
            network_cmd::switch_to_relay,
            network_cmd::get_local_id,
            network_cmd::connect_to_peer,
        ],
    ))
}

fn peer(peer_id: &str, alpn: &[u8]) -> ConnectedPeerInfo {
    let mut alpns = HashSet::new();
    alpns.insert(alpn.to_vec());

    (
        PeerIdentity { id: peer_id.to_string(), device_id: None },
        alpns,
        Some(DeviceInfo {
            name: "Notebook".to_string(),
            os: "windows".to_string(),
            version: "0.0.1-test".to_string(),
        }),
    )
}

#[tokio::test(flavor = "multi_thread")]
async fn get_network_status_emite_modo_e_peers() -> Result<()> {
    let service = mock_network_service();
    service.set_mode(NetworkMode::Relay);
    service.set_peers(vec![peer("peer-1", b"acerola/handshake/1")]);
    let (app, webview) = build_network_app(service)?;
    let status_rx = listen_event(&app, "network:status");

    let _: Value = invoke_ok(&webview, "get_network_status", json!({}))?;

    let status = recv_event(status_rx, "network:status").await?;

    assert_eq!(status["mode"], "relay");
    assert_eq!(status["peers"][0]["peerId"], "peer-1");
    assert_eq!(status["peers"][0]["alpn"], "acerola/handshake/1");
    assert_eq!(status["peers"][0]["device"]["name"], "Notebook");

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn get_network_status_serializa_erro_do_servico() -> Result<()> {
    let service = mock_network_service();
    service.fail_next("status failure");
    let (_app, webview) = build_network_app(service)?;

    let error = invoke_err(&webview, "get_network_status", json!({}))?;

    assert_eq!(error, json!("status failure"));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn switch_to_local_troca_modo() -> Result<()> {
    let service = mock_network_service();
    service.set_mode(NetworkMode::Relay);
    let (_app, webview) = build_network_app(service.clone())?;

    let _: Value = invoke_ok(&webview, "switch_to_local", json!({}))?;

    assert!(matches!(service.mode(), NetworkMode::Local));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn switch_to_local_serializa_erro_do_servico() -> Result<()> {
    let service = mock_network_service();
    service.fail_next("local failure");
    let (_app, webview) = build_network_app(service)?;

    let error = invoke_err(&webview, "switch_to_local", json!({}))?;

    assert_eq!(error, json!("local failure"));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn switch_to_relay_troca_modo() -> Result<()> {
    let service = mock_network_service();
    let (_app, webview) = build_network_app(service.clone())?;

    let _: Value = invoke_ok(&webview, "switch_to_relay", json!({}))?;

    assert!(matches!(service.mode(), NetworkMode::Relay));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn switch_to_relay_serializa_erro_do_servico() -> Result<()> {
    let service = mock_network_service();
    service.fail_next("relay failure");
    let (_app, webview) = build_network_app(service)?;

    let error = invoke_err(&webview, "switch_to_relay", json!({}))?;

    assert_eq!(error, json!("relay failure"));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn get_local_id_retorna_id_local() -> Result<()> {
    let service = mock_network_service();
    let (_app, webview) = build_network_app(service)?;

    let local_id = invoke_ok_value(&webview, "get_local_id", json!({}))?;

    assert_eq!(local_id, json!("local-peer-id"));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn get_local_id_serializa_erro_do_servico() -> Result<()> {
    let service = mock_network_service();
    service.fail_next("id failure");
    let (_app, webview) = build_network_app(service)?;

    let error = invoke_err(&webview, "get_local_id", json!({}))?;

    assert_eq!(error, json!("id failure"));

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn connect_to_peer_repassa_peer_id_e_alpn_bytes() -> Result<()> {
    let service = mock_network_service();
    let (_app, webview) = build_network_app(service.clone())?;

    let _: Value = invoke_ok(
        &webview,
        "connect_to_peer",
        json!({ "peerId": "peer-2", "alpn": "acerola/handshake/1" }),
    )?;

    assert_eq!(
        service.last_connection(),
        Some(("peer-2".to_string(), b"acerola/handshake/1".to_vec()))
    );

    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn connect_to_peer_serializa_erro_do_servico() -> Result<()> {
    let service = mock_network_service();
    service.fail_next("connect failure");
    let (_app, webview) = build_network_app(service)?;

    let error = invoke_err(
        &webview,
        "connect_to_peer",
        json!({ "peerId": "peer-2", "alpn": "acerola/handshake/1" }),
    )?;

    assert_eq!(error, json!("connect failure"));

    Ok(())
}
