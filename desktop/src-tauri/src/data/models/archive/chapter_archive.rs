use crate::data::repositories::base::{Bindable, Entity};
use serde::{Deserialize, Serialize};
use sqlx::{query::Query, sqlite::SqliteArguments, Sqlite};

/// Contrato com o [`crate::data::repositories::base::Repository`] genérico.
impl Entity for ChapterArchive {
    fn columns() -> &'static [&'static str] {
        &[
            "id",
            "chapter",
            "path",
            "chapter_sort",
            "fast_hash",
            "comic_directory_fk",
            "last_modified",
        ]
    }
    fn table_name() -> &'static str {
        "chapter_archive"
    }
    fn id(&self) -> i64 {
        self.id
    }
}

/// Garante que o código consiga serializar o sql para o objeto
impl Bindable for ChapterArchive {
    fn bind_insert<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(self.id)
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
    }

    fn bind_update<'query>(
        &'query self,
        query: Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
        query
            .bind(&self.chapter)
            .bind(&self.path)
            .bind(&self.chapter_sort)
            .bind(&self.fast_hash)
            .bind(self.comic_directory_fk)
            .bind(self.last_modified)
            .bind(self.id) // <- id pro WHERE id = ?
    }
}

impl ChapterArchive {
    pub fn format_sort(chapter: u64, decimal: Option<String>) -> String {
        match decimal {
            Some(d) if !d.is_empty() => format!("{chapter}.{d}"),
            _ => chapter.to_string(),
        }
    }

    pub fn fallback_sort(file_name: &str, index: usize) -> String {
        let filtered = file_name
            .chars()
            .filter(|it| it.is_ascii_digit() || *it == '.' || *it == ',')
            .collect::<String>()
            .replace(',', ".");

        if filtered.is_empty() {
            (index + 1).to_string()
        } else {
            filtered
        }
    }
}

// NOTE: Migration em src-tauri\migrations\archive
#[derive(Debug, Serialize, Deserialize, Clone, sqlx::FromRow)]
pub struct ChapterArchive {
    pub id: i64,
    pub chapter: String,
    pub path: String,
    pub chapter_sort: String,
    pub fast_hash: Option<String>,
    pub comic_directory_fk: i64,
    pub last_modified: i64,
}

#[cfg(test)]
mod tests {
    use super::ChapterArchive;

    // NOTE: Format sort

    #[test]
    fn format_template_detectado() {
        assert_eq!(ChapterArchive::format_sort(10, Some("1".to_string())), "10.1");
    }

    #[test]
    fn format_sem_decimal() {
        assert_eq!(ChapterArchive::format_sort(10, None), "10");
    }

    #[test]
    fn format_so_com_decimal() {
        assert_eq!(ChapterArchive::format_sort(0, Some("10".to_string())), "0.10");
    }

    #[test]
    fn format_preserva_leading_zero_decimal() {
        assert_eq!(ChapterArchive::format_sort(0, Some("01".to_string())), "0.01");
    }

    // NOTE: Fallback sort

    #[test]
    fn fallback_sem_template_detectado() {
        assert_eq!(ChapterArchive::fallback_sort("10,5", 0), "10.5");
    }

    #[test]
    fn fallback_arquivo_com_letas() {
        assert_eq!(ChapterArchive::fallback_sort("abc", 0), "1");
    }

    #[test]
    fn fallback_arquivo_sem_decimal() {
        assert_eq!(ChapterArchive::fallback_sort("10", 0), "10");
    }

    #[test]
    fn fallback_arquivo_so_com_decimal() {
        assert_eq!(ChapterArchive::fallback_sort("0,10", 0), "0.10");
    }
}
