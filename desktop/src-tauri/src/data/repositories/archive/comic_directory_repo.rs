use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::repositories::base::{ Repository, Entity };
use sqlx::SqlitePool;

pub struct ComicRepository {
    base: Repository<ComicDirectory>,
    pool: SqlitePool,
}

impl ComicRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            base: Repository::new(pool.clone()),
            pool,
        }
    }

    pub async fn find_all(&self) -> Result<Vec<ComicDirectory>, sqlx::Error> {
        self.base.find_all().await
    }

    pub async fn find_by_name(&self, name: &str) -> Result<Option<ComicDirectory>, String> {
      let table = ComicDirectory::table_name();
        let cols = ComicDirectory::columns().join(", ");

        // prettier-ignore
        sqlx::query_as::<_, ComicDirectory>(
            &format!("SELECT {} FROM {} WHERE name = ?", cols, table)
        ).bind(name).fetch_optional(&self.pool).await.map_err(|error: sqlx::Error| error.to_string())
    }
}
