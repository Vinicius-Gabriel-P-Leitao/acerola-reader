CREATE TABLE IF NOT EXISTS manga_category (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  comic_directory_fk INTEGER NOT NULL UNIQUE,
  category_id INTEGER NOT NULL,
  FOREIGN KEY(comic_directory_fk) REFERENCES comic_directory(id) ON DELETE CASCADE,
  FOREIGN KEY(category_id) REFERENCES category(id) ON DELETE CASCADE
);
