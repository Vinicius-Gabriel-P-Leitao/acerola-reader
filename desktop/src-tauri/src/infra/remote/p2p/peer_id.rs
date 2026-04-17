use std::fmt;

/// Identificador de um peer na rede P2P.
/// Newtype sobre String para evitar uso genérico acidental.
#[derive(Debug)]
pub struct PeerId {
    pub id: String,
}

impl fmt::Display for PeerId {
    fn fmt(&self, format: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(format, "{}", self.id)
    }
}
