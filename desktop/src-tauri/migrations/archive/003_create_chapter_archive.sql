CREATE TABLE chapter_archive (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chapter TEXT NOT NULL,
  path TEXT NOT NULL,
  chapter_sort TEXT NOT NULL,
  checksum TEXT,
  fast_hash TEXT,
  comic_directory_fk INTEGER NOT NULL,
  last_modified INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(comic_directory_fk) REFERENCES comic_directory(id) ON DELETE CASCADE,
  UNIQUE(comic_directory_fk, chapter)
);