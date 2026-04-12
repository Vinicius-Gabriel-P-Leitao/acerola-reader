use crate::infra::error::translations::comic_error::ComicError;
use serde::Serialize;

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ScanErrorPayload {
    pub error_type: String,
    pub message: String,
}

impl From<&ComicError> for ScanErrorPayload {
    fn from(err: &ComicError) -> Self {
        ScanErrorPayload {
            error_type: format!("{:?}", err),
            message: err.to_string(),
        }
    }
}
