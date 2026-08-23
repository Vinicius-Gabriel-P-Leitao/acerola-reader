CREATE TABLE IF NOT EXISTS mangadex_source (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mangadex_id TEXT NOT NULL,
  comic_metadata_fk INTEGER NOT NULL UNIQUE,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE
);
