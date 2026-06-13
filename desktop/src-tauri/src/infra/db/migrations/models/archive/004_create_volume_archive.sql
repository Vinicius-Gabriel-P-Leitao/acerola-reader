CREATE TABLE IF NOT EXISTS volume_archive (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  path TEXT NOT NULL,
  volume_sort TEXT NOT NULL,
  is_special BOOLEAN NOT NULL DEFAULT 0,
  cover TEXT,
  banner TEXT,
  comic_directory_fk INTEGER NOT NULL,
  last_modified INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(comic_directory_fk) REFERENCES comic_directory(id) ON DELETE CASCADE,
  UNIQUE(comic_directory_fk, volume_sort)
);
