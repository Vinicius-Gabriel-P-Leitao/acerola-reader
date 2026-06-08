CREATE TABLE IF NOT EXISTS comic_info_source (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  local_hash TEXT NOT NULL,
  comic_metadata_fk INTEGER NOT NULL UNIQUE,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE
);
