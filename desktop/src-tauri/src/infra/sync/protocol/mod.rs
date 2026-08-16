pub mod file_handler;
pub mod history_handler;

pub const HISTORY_SYNC_ALPN: &[u8] = b"acerola/sync-history/1";
pub const FILE_SYNC_ALPN: &[u8] = b"acerola/sync-files/1";
