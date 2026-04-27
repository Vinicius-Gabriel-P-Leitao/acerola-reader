use crate::data::identity::device_info::{DeviceInfo, DeviceInfoProvider};
use crate::infra::error::DeviceInfoError;

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
