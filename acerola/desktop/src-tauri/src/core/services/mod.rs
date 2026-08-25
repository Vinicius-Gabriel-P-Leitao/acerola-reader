pub mod archive;
pub mod category;
pub mod comic;
pub mod history;
pub mod metadata;
pub mod network;
pub mod reader;
pub mod summary;
pub mod sync;

// FIXME: Por que não é uma pasta de comic/ com um mod.rs que usa pub mod comic; e não usa pub use?
pub use comic::ComicService;
