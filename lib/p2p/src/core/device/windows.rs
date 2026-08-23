use crate::{
    data::identity::device_info::{DeviceInfo, DeviceInfoProvider},
    infra::error::DeviceInfoError,
};

/// Provedor padrão de informações do dispositivo para sistemas Windows.
pub struct DefaultDeviceInfoProvider {
    version: String,
}

impl DefaultDeviceInfoProvider {
    /// Cria um novo provedor de informações com a versão fornecida.
    pub fn new(version: impl Into<String>) -> Self {
        Self { version: version.into() }
    }
}

impl DeviceInfoProvider for DefaultDeviceInfoProvider {
    fn provide(&self) -> Result<DeviceInfo, DeviceInfoError> {
        let name = std::env::var("COMPUTERNAME").unwrap_or_else(|_| "unknown".to_string());

        Ok(DeviceInfo { name, os: "windows".to_string(), version: self.version.clone() })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn provide_returns_ok_with_non_empty_fields() {
        // Instancia o provedor de informações do dispositivo para Windows
        let device_provider = DefaultDeviceInfoProvider::new("3.0.0");
        let device_info_result = device_provider.provide();

        assert!(device_info_result.is_ok());
        let device_info = device_info_result.unwrap();

        assert!(!device_info.name.is_empty());
        assert!(!device_info.version.is_empty());
        assert!(!device_info.os.is_empty());
        assert_eq!(device_info.os, "windows");
        assert_eq!(device_info.version, "3.0.0");
    }
}
