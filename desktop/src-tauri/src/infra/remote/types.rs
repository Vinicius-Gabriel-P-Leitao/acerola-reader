use std::fmt;

/// A tipagem aqui é para especificar que é um Id do peer, usar string direto fica generico
#[derive(Debug)]
pub struct PeerId{
    pub id: String
}

impl fmt::Display for PeerId {
    fn fmt(&self, format: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(format, "{}", self.id)
    }
}
