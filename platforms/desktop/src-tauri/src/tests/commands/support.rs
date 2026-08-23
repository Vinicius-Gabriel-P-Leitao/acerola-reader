use std::{sync::mpsc, time::Duration};

use anyhow::{bail, Context, Result};
use serde::de::DeserializeOwned;
use serde_json::Value;
use tauri::{
    ipc::InvokeBody,
    test::{self, MockRuntime},
    webview::InvokeRequest,
    App, Listener, WebviewWindow, WebviewWindowBuilder,
};

pub(super) fn request(cmd: &str, body: Value) -> InvokeRequest {
    InvokeRequest {
        cmd: cmd.into(),
        callback: tauri::ipc::CallbackFn(0),
        error: tauri::ipc::CallbackFn(1),
        url: "http://tauri.localhost".parse().expect("the fixed test IPC URL must be valid"),
        body: InvokeBody::Json(body),
        headers: Default::default(),
        invoke_key: test::INVOKE_KEY.to_string(),
    }
}

pub(super) fn build_webview(
    builder: tauri::Builder<MockRuntime>,
) -> Result<(App<MockRuntime>, WebviewWindow<MockRuntime>)> {
    let app = builder.build(test::mock_context(test::noop_assets()))?;
    let webview = WebviewWindowBuilder::new(&app, "main", Default::default()).build()?;

    Ok((app, webview))
}

pub(super) fn invoke_ok<T: DeserializeOwned>(
    webview: &WebviewWindow<MockRuntime>, cmd: &str, body: Value,
) -> Result<T> {
    let response = test::get_ipc_response(webview, request(cmd, body))
        .map_err(|error| anyhow::anyhow!("IPC `{cmd}` returned unexpected error: {error}"))?;

    response.deserialize::<T>().with_context(|| format!("failed to deserialize `{cmd}`"))
}

pub(super) fn invoke_ok_value(
    webview: &WebviewWindow<MockRuntime>, cmd: &str, body: Value,
) -> Result<Value> {
    invoke_ok(webview, cmd, body)
}

pub(super) fn invoke_err(
    webview: &WebviewWindow<MockRuntime>, cmd: &str, body: Value,
) -> Result<Value> {
    match test::get_ipc_response(webview, request(cmd, body)) {
        Ok(response) => {
            let value = response.deserialize::<Value>()?;
            bail!("IPC `{cmd}` should have failed, but returned success: {value}");
        },
        Err(error) => Ok(error),
    }
}

pub(super) fn listen_event(
    app: &App<MockRuntime>, event_name: &'static str,
) -> mpsc::Receiver<String> {
    let (tx, rx) = mpsc::channel();

    app.listen_any(event_name, move |event| {
        let _ = tx.send(event.payload().to_string());
    });

    rx
}

pub(super) async fn recv_event(rx: mpsc::Receiver<String>, event_name: &str) -> Result<Value> {
    let event = event_name.to_string();
    let payload = tokio::task::spawn_blocking(move || rx.recv_timeout(Duration::from_secs(3)))
        .await
        .with_context(|| format!("event `{event}` wait task failed"))?
        .with_context(|| format!("event `{event}` was not emitted within the timeout"))?;

    serde_json::from_str(&payload)
        .with_context(|| format!("event `{event_name}` payload was not valid JSON"))
}
