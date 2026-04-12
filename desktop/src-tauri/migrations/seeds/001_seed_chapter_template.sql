INSERT
  OR IGNORE INTO chapter_template (id, label, pattern, is_default, priority)
VALUES
  (
    -1,
    '01.*.',
    '{chapter}{decimal}.*.{extension}',
    1,
    1
  ),
  (
    -2,
    'Ch. 01.*.',
    'Ch. {chapter}{decimal}.*.{extension}',
    1,
    2
  ),
  (
    -3,
    'Cap. 01.*.',
    'Cap. {chapter}{decimal}.*.{extension}',
    1,
    3
  ),
  (
    -4,
    'chapter 01.*.',
    'chapter {chapter}{decimal}.*.{extension}',
    1,
    4
  );