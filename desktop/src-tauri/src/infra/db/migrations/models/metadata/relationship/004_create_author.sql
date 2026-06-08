CREATE TABLE IF NOT EXISTS author (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  comic_metadata_fk INTEGER NOT NULL,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE,
  UNIQUE(name, comic_metadata_fk)
);
