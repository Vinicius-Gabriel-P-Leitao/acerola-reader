CREATE TABLE IF NOT EXISTS chapter_metadata (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT,
  chapter TEXT NOT NULL,
  page_count INTEGER,
  scanlation TEXT,
  comic_metadata_fk INTEGER NOT NULL,
  FOREIGN KEY(comic_metadata_fk) REFERENCES comic_metadata(id) ON DELETE CASCADE,
  UNIQUE(chapter, comic_metadata_fk)
);
