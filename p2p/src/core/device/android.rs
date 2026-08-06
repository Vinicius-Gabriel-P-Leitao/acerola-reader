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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn provide_returns_ok_with_non_empty_fields() {
        // Instancia o provedor de informações do dispositivo para Android
        let device_provider = DefaultDeviceInfoProvider::new("Galaxy S23", "1.0.0");
        let device_info_result = device_provider.provide();

        assert!(device_info_result.is_ok());
        let device_info = device_info_result.unwrap();

        assert!(!device_info.name.is_empty());
        assert!(!device_info.version.is_empty());
        assert!(!device_info.os.is_empty());
        assert_eq!(device_info.os, "android");
        assert_eq!(device_info.name, "Galaxy S23");
        assert_eq!(device_info.version, "1.0.0");
    }
}
