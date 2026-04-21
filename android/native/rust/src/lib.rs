mod bridge;
mod state;

use std::sync::Arc;
use acerola_p2p::api::{guard::open_guard, AcerolaP2P};
use jni::objects::{JClass, JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use state::{AppState, INSTANCE};
use tokio::runtime::Runtime;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_br_acerola_comic_infrastructure_p2p_P2PNode_initNode(
    env: JNIEnv,
    _class: JClass,
    callback: JObject,
) {
    let runtime = Runtime::new().expect("Failed to create Tokio runtime");
    let jvm = env.get_java_vm().expect("Failed to get JVM");
    let global_callback = env.new_global_ref(callback).expect("Failed to create global ref");

    let node = runtime.block_on(async {
        let emit: acerola_p2p::api::protocol::EventEmitter =
            Arc::new(|event, data| bridge::event_emitter_callback(event, data));

        AcerolaP2P::builder(emit)
            .guard(Box::new(|ctx| Box::pin(open_guard(ctx))))
            .build()
            .await
            .expect("Failed to start the p2p node.")
    });

    let state = AppState {
        node: Arc::new(node),
        runtime,
        callback: global_callback,
        jvm,
    };

    INSTANCE.set(state).expect("Failed to set state");
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_br_acerola_comic_infrastructure_p2p_P2PNode_getLocalId(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let state = INSTANCE.get().expect("Node not initialized");
    let local_id = state.node.local_id().to_string();
    bridge::to_jstring(&mut env, &local_id)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_br_acerola_comic_infrastructure_p2p_P2PNode_connect(
    mut env: JNIEnv,
    _class: JClass,
    peer_id: JString,
    alpn: JString,
) {
    let state = INSTANCE.get().expect("Node not initialized");
    let peer_id_rust = bridge::to_rust_string(&mut env, peer_id);
    let alpn_rust = bridge::to_rust_string(&mut env, alpn).into_bytes();

    let node = Arc::clone(&state.node);
    state.runtime.spawn(async move {
        let _ = node.connect(&peer_id_rust, &alpn_rust).await;
    });
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_br_acerola_comic_infrastructure_p2p_P2PNode_shutdown(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Some(state) = INSTANCE.get() {
        let node = Arc::clone(&state.node);
        state.runtime.block_on(async move {
            let _ = node.shutdown().await;
        });
    }
}
