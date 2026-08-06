use serde::{Deserialize, Serialize};

use crate::infra::error::DeviceInfoError;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct DeviceInfo {
    pub os: String,
    pub name: String,
    pub version: String,
}

pub trait DeviceInfoProvider {
    fn provide(&self) -> Result<DeviceInfo, DeviceInfoError>;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn serialization_and_deserialization_roundtrip() {
        // Verifica se a serialização JSON e a desserialização recompõem o objeto exatamente igual
        let original_info = DeviceInfo {
            os: "linux".to_string(),
            name: "workstation-01".to_string(),
            version: "1.2.3".to_string(),
        };

        let json_representation =
            serde_json::to_string(&original_info).expect("Falha ao serializar DeviceInfo");
        let deserialized_info: DeviceInfo =
            serde_json::from_str(&json_representation).expect("Falha ao desserializar DeviceInfo");

        assert_eq!(original_info, deserialized_info);
    }

    #[test]
    fn empty_fields_serialize_correctly() {
        // Garante que campos de texto vazios são serializados corretamente sem panic ou perda de chaves
        let empty_info =
            DeviceInfo { os: String::new(), name: String::new(), version: String::new() };

        let json_representation = serde_json::to_string(&empty_info)
            .expect("Falha ao serializar DeviceInfo com campos vazios");
        assert!(json_representation.contains("\"os\":\"\""));
        assert!(json_representation.contains("\"name\":\"\""));
        assert!(json_representation.contains("\"version\":\"\""));

        let deserialized_info: DeviceInfo = serde_json::from_str(&json_representation)
            .expect("Falha ao desserializar DeviceInfo vazio");
        assert_eq!(empty_info, deserialized_info);
    }
}
