use crate::{
    data::identity::device_info::{DeviceInfo, DeviceInfoProvider},
    infra::error::DeviceInfoError,
};

/// Provider para Android — o nome do dispositivo e a versão do app devem ser passados
/// explicitamente pelo caller, pois não é possível auto-detectá-los sem JNI.
///
/// Exemplo de uso a partir do Kotlin:
/// ```kotlin
/// DefaultDeviceInfoProvider(Build.MODEL, BuildConfig.VERSION_NAME)
/// ```
pub struct DefaultDeviceInfoProvider {
    name: String,
    version: String,
}

impl DefaultDeviceInfoProvider {
    pub fn new(name: impl Into<String>, version: impl Into<String>) -> Self {
        Self { name: name.into(), version: version.into() }
    }
}

impl DeviceInfoProvider for DefaultDeviceInfoProvider {
    fn provide(&self) -> Result<DeviceInfo, DeviceInfoError> {
        Ok(DeviceInfo {
            name: self.name.clone(),
            os: "android".to_string(),
            version: self.version.clone(),
        })
    }
}
