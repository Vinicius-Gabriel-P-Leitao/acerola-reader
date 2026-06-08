CREATE TABLE IF NOT EXISTS reading_history (
  comic_directory_id INTEGER NOT NULL PRIMARY KEY,
  chapter_archive_id INTEGER NOT NULL,
  last_page INTEGER NOT NULL,
  is_completed BOOLEAN NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY(comic_directory_id) REFERENCES comic_directory(id) ON DELETE CASCADE,
  FOREIGN KEY(chapter_archive_id) REFERENCES chapter_archive(id) ON DELETE CASCADE
);
