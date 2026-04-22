#[path = "lib/guard.rs"]
pub(crate) mod guard;

#[path = "lib/mode.rs"]
pub(crate) mod mode;

#[path = "lib/singleton.rs"]
pub(crate) mod singleton;

pub mod api;

uniffi::setup_scaffolding!();
