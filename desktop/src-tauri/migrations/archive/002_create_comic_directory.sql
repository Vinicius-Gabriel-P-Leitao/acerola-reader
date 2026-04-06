CREATE TABLE comic_directory (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  path TEXT NOT NULL,
  cover TEXT,
  banner TEXT,
  last_modified INTEGER NOT NULL,
  chapter_template_fk INTEGER,
  external_sync_enabled BOOLEAN NOT NULL DEFAULT 1,
  hidden BOOLEAN NOT NULL DEFAULT 0,
  FOREIGN KEY(chapter_template_fk) REFERENCES chapter_template(id) ON DELETE
  SET
    NULL
);