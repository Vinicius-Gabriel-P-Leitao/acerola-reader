use crate::infra::error::translations::comic_error::ComicError;
use serde::Serialize;

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorPayload {
    pub error_type: String,
    pub message: String,
}

impl From<&ComicError> for ErrorPayload {
    fn from(err: &ComicError) -> Self {
        ErrorPayload {
            error_type: format!("{:?}", err),
            message: err.to_string(),
        }
    }
}
