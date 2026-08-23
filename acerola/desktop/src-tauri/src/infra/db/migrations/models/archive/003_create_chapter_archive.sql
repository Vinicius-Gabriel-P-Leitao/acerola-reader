CREATE TABLE IF NOT EXISTS chapter_archive (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chapter TEXT NOT NULL,
  path TEXT NOT NULL,
  chapter_sort TEXT NOT NULL,
  is_special BOOLEAN NOT NULL DEFAULT 0,
  checksum TEXT,
  comic_directory_fk INTEGER NOT NULL,
  volume_id_fk INTEGER,
  last_modified INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(comic_directory_fk) REFERENCES comic_directory(id) ON DELETE CASCADE,
  FOREIGN KEY(volume_id_fk) REFERENCES volume_archive(id) ON DELETE CASCADE,
  UNIQUE(comic_directory_fk, chapter)
);