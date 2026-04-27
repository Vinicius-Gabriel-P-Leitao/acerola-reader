use getrandom::{rand_core::TryRng, SysRng};
use secrecy::SecretBox;
use crate::infra::error::ConnectionError;

pub fn generate_seed() -> Result<SecretBox<[u8; 32]>, ConnectionError> {
    let mut bytes = [0u8; 32];
    SysRng.try_fill_bytes(&mut bytes)?;

    Ok(SecretBox::new(Box::new(bytes)))
}
