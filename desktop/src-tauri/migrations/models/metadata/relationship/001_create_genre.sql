CREATE TABLE IF NOT EXISTS genre (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  genre TEXT NOT NULL,
  comic_metadata_fk INTEGER NOT NULL,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE,
  UNIQUE(genre, comic_metadata_fk)
);
