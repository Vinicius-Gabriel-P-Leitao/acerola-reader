//! Abstração de armazenamento e transferência de blobs content-addressed (`P2pBlobStore`).
//!
//! Mesmo tratamento de adapter que `core::transport` já dá ao `iroh`: a trait `P2pBlobStore` é
//! transporte-agnóstica, e o adapter concreto do `iroh-blobs` (atrás da feature
//! `iroh-blobs-adapter`) vive em `core::blobs::iroh`.

mod hash;
mod mem;
mod store;

#[cfg(feature = "iroh-blobs-adapter")]
pub mod iroh;

pub use hash::{BlobHash, BlobHashParseError};
pub use mem::InMemoryBlobStore;
pub use store::P2pBlobStore;
