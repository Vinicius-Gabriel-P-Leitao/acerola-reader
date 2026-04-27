use crate::infra::error::DeviceInfoError;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceInfo {
    pub os: String,
    pub name: String,
    pub version: String,
}

pub trait DeviceInfoProvider {
    fn provide(&self) -> Result<DeviceInfo, DeviceInfoError>;
}
