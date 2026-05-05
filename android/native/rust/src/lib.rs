#[path = "lib/mode.rs"]
pub(crate) mod mode;

#[path = "lib/singleton.rs"]
pub(crate) mod singleton;

pub mod api;

uniffi::setup_scaffolding!();

#[cfg(target_os = "android")]
mod android_init {
    use jni::objects::GlobalRef;
    use once_cell::sync::OnceCell;

    static CONTEXT: OnceCell<GlobalRef> = OnceCell::new();

    #[allow(non_snake_case)]
    #[no_mangle]
    pub extern "system" fn JNI_OnLoad(
        vm: jni::JavaVM,
        _: *mut std::ffi::c_void,
    ) -> jni::sys::jint {
        let mut env = vm.attach_current_thread().expect("Failed to attach JVM thread");

        let activity_thread = env
            .find_class("android/app/ActivityThread")
            .expect("ActivityThread not found");

        let context = env
            .call_static_method(activity_thread, "currentApplication", "()Landroid/app/Application;", &[])
            .expect("currentApplication failed")
            .l()
            .expect("Expected object");

        let global = env.new_global_ref(context).expect("Failed to create global ref");

        unsafe {
            ndk_context::initialize_android_context(
                vm.get_java_vm_pointer().cast(),
                global.as_obj().as_raw().cast(),
            );
        }

        CONTEXT.set(global).ok();

        jni::sys::JNI_VERSION_1_6
    }
}
