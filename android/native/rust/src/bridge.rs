use jni::objects::JString;
use jni::sys::jstring;
use jni::JNIEnv;

use crate::state::INSTANCE;

pub fn event_emitter_callback(event: &str, data: String) {
    let state = INSTANCE.get().expect("Node not initialized");
    let mut env = state
        .jvm
        .attach_current_thread()
        .expect("Failed to attach thread");

    let event_str = env
        .new_string(event)
        .expect("Failed to create event string");

    // The data is already a string, so just pass it through.
    let data_str = env.new_string(data).expect("Failed to create data string");

    env.call_method(
        state.callback.as_obj(),
        "onEvent",
        "(Ljava/lang/String;Ljava/lang/String;)V",
        &[(&event_str).into(), (&data_str).into()],
    )
    .expect("Failed to call onEvent");
}

pub fn to_rust_string(env: &mut JNIEnv, j_string: JString) -> String {
    env.get_string(&j_string)
        .expect("Couldn't get java string!")
        .into()
}

pub fn to_jstring(env: &mut JNIEnv, rust_string: &str) -> jstring {
    env.new_string(rust_string)
        .expect("Couldn't create java string!")
        .into_raw()
}
