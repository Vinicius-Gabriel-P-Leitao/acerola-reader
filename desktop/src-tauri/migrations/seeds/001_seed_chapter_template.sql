INSERT OR IGNORE INTO archive_template (id, label, pattern, type, is_default, priority)
VALUES
  (-1, '01.*.', '{chapter}{decimal}.*.{extension}', 'CHAPTER', 1, 0),
  (-2, 'Ch. 01.*.', 'Ch. {chapter}{decimal}.*.{extension}', 'CHAPTER', 1, 0),
  (-3, 'Cap. 01.*.', 'Cap. {chapter}{decimal}.*.{extension}', 'CHAPTER', 1, 0),
  (-4, 'chapter 01.*.', 'chapter {chapter}{decimal}.*.{extension}', 'CHAPTER', 1, 0),
  (-5, 'Vol. 01', 'Vol. {volume}{decimal}', 'VOLUME', 1, 0),
  (-6, 'Volume 01', 'Volume {volume}{decimal}', 'VOLUME', 1, 0),
  (-7, 'V01', 'V{volume}{decimal}', 'VOLUME', 1, 0),
  (-8, 'Edicao 01', 'Edicao {volume}{decimal}', 'VOLUME', 1, 0),
  (-9, 'Edição 01', 'Edição {volume}{decimal}', 'VOLUME', 1, 0);