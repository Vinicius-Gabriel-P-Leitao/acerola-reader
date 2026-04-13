CREATE TABLE IF NOT EXISTS chapter_page (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  page_number INTEGER NOT NULL,
  image_url TEXT NOT NULL,
  downloaded BOOLEAN NOT NULL DEFAULT 0,
  chapter_fk INTEGER NOT NULL,
  FOREIGN KEY(chapter_fk) REFERENCES chapter_metadata(id) ON DELETE CASCADE,
  UNIQUE(chapter_fk, page_number)
);
