CREATE TABLE IF NOT EXISTS anilist_source (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  anilist_id INTEGER NOT NULL,
  average_score INTEGER,
  popularity INTEGER,
  trending INTEGER,
  cover_image TEXT,
  banner_image TEXT,
  comic_metadata_fk INTEGER NOT NULL UNIQUE,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE
);
