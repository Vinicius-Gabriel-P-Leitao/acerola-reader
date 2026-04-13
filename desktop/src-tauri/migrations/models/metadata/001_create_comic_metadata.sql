CREATE TABLE IF NOT EXISTS comic_metadata (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  romanji TEXT NOT NULL,
  status TEXT NOT NULL,
  publication INTEGER,
  sync_source TEXT,
  has_comic_info BOOLEAN NOT NULL DEFAULT 0,
  comic_directory_fk INTEGER UNIQUE,
  FOREIGN KEY(comic_directory_fk) REFERENCES comic_directory(id) ON DELETE CASCADE
);
