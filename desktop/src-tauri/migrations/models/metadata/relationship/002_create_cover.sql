CREATE TABLE IF NOT EXISTS cover (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  file_name TEXT NOT NULL,
  url TEXT NOT NULL,
  comic_metadata_fk INTEGER NOT NULL,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE,
  UNIQUE(file_name, comic_metadata_fk)
);
