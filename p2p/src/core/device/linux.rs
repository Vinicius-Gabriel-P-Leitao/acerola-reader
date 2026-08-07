use crate::{
    data::identity::device_info::{DeviceInfo, DeviceInfoProvider},
    infra::error::DeviceInfoError,
};

pub struct DefaultDeviceInfoProvider {
    version: String,
}

impl DefaultDeviceInfoProvider {
    pub fn new(version: impl Into<String>) -> Self {
        Self { version: version.into() }
    }
}

impl DeviceInfoProvider for DefaultDeviceInfoProvider {
    fn provide(&self) -> Result<DeviceInfo, DeviceInfoError> {
        let name = std::fs::read_to_string("/etc/hostname")
            .map(|s| s.trim().to_string())
            .unwrap_or_else(|_| "unknown".to_string());

        Ok(DeviceInfo { name, os: "linux".to_string(), version: self.version.clone() })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn provide_returns_ok_with_non_empty_fields() {
        // Instancia o provedor de informações do dispositivo para Linux
        let device_provider = DefaultDeviceInfoProvider::new("2.1.0");
        let device_info_result = device_provider.provide();

        assert!(device_info_result.is_ok());
        let device_info = device_info_result.unwrap();

        assert!(!device_info.name.is_empty());
        assert!(!device_info.version.is_empty());
        assert!(!device_info.os.is_empty());
        assert_eq!(device_info.os, "linux");
        assert_eq!(device_info.version, "2.1.0");
    }
}
