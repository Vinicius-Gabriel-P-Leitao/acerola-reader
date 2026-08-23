CREATE TABLE IF NOT EXISTS chapter_read (
  comic_directory_id INTEGER NOT NULL,
  chapter_archive_id INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  PRIMARY KEY(comic_directory_id, chapter_archive_id),
  FOREIGN KEY(comic_directory_id) REFERENCES comic_directory(id) ON DELETE CASCADE,
  FOREIGN KEY(chapter_archive_id) REFERENCES chapter_archive(id) ON DELETE CASCADE
);
