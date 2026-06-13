CREATE TABLE IF NOT EXISTS mangadex_source (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mangadex_id TEXT NOT NULL,
  anilist_id TEXT,
  amazon_url TEXT,
  ebookjapan_url TEXT,
  raw_url TEXT,
  engtl_url TEXT,
  comic_metadata_fk INTEGER NOT NULL UNIQUE,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE
);
