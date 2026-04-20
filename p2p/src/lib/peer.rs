use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct PeerId {
    pub id: String,
}

impl fmt::Display for PeerId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.id)
    }
}
