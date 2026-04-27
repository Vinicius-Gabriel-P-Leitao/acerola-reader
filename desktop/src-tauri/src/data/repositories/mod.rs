use crate::infra::error::DbError;
use sqlx::{
    query, query_as,
    sqlite::{SqliteArguments, SqliteRow},
    FromRow, Pool, Sqlite,
};
use std::marker::PhantomData;

pub mod archive {
    pub mod chapter_archive_repo;
    pub mod chapter_template_repo;
    pub mod comic_directory_repo;
}

pub mod views {
    pub mod comic_summary_repo;
}

pub trait Entity {
    fn columns() -> &'static [&'static str];
    fn table_name() -> &'static str;
    fn id(&self) -> i64;
}

pub trait Bindable {
    fn bind_insert<'query>(
        &'query self, query: query::Query<'query, Sqlite, SqliteArguments<'query>>,
    ) -> query::Query<'query, Sqlite, SqliteArguments<'query>>;

    fn bind_update<'q>(
        &'q self, query: query::Query<'q, Sqlite, SqliteArguments<'q>>,
    ) -> query::Query<'q, Sqlite, SqliteArguments<'q>>;
}

pub struct Repository<T: Entity> {
    pool: Pool<Sqlite>,
    _marker: PhantomData<T>,
}

impl<T: Entity> Clone for Repository<T> {
    fn clone(&self) -> Self {
        Self { pool: self.pool.clone(), _marker: PhantomData }
    }
}

impl<T: Entity> Repository<T> {
    pub fn new(pool: Pool<Sqlite>) -> Self {
        Self { pool, _marker: PhantomData }
    }

    pub async fn count(&self) -> Result<i64, DbError> {
        let table = T::table_name();
        let result = query_as::<_, (i64,)>(&format!("SELECT COUNT(*) FROM {}", table))
            .fetch_one(&self.pool)
            .await?;
        Ok(result.0)
    }

    pub async fn find_all(&self) -> Result<Vec<T>, DbError>
    where
        T: Entity + for<'row> FromRow<'row, SqliteRow> + Send + Unpin,
    {
        let cols = T::columns().join(", ");
        let table = T::table_name();

        let result = query_as::<_, T>(&format!("SELECT {} FROM {}", cols, table))
            .fetch_all(&self.pool).await?;

        Ok(result)
    }

    pub async fn insert(&self, entity: &T) -> Result<T, DbError>
    where
        T: Entity + Bindable + for<'row> FromRow<'row, SqliteRow> + Send + Unpin,
    {
        let table = T::table_name();
        let cols = T::columns().join(", ");
        let placeholders = T::columns().iter().map(|_| "?").collect::<Vec<_>>().join(", ");

        let sql = format!("INSERT INTO {} ({}) VALUES ({}) RETURNING *", table, cols, placeholders);
        let row = entity.bind_insert(query(&sql)).fetch_one(&self.pool).await?;

        Ok(T::from_row(&row)?)
    }

    pub async fn update(&self, entity: &T) -> Result<T, DbError>
    where
        T: Entity + Bindable + for<'r> FromRow<'r, SqliteRow> + Send + Unpin,
    {
        let table = T::table_name();
        let set_clause = T::columns()
            .iter()
            .filter(|col| **col != "id")
            .map(|col| format!("{} = ?", col))
            .collect::<Vec<_>>()
            .join(", ");

        let sql = format!("UPDATE {} SET {} WHERE id = ? RETURNING *", table, set_clause);

        let row = entity.bind_update(query(&sql)).fetch_one(&self.pool).await?;

        Ok(T::from_row(&row)?)
    }

    pub async fn delete(&self, id: i64) -> Result<(), DbError> {
        let table = T::table_name();
        query(&format!("DELETE FROM {} WHERE id = ?", table)).bind(id).execute(&self.pool).await?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use crate::{
        data::repositories::{Bindable, Entity, Repository},
        infra::error::DbError,
        tests::utils::setup_test_db::setup_test_db,
    };
    use sqlx::{query::Query, sqlite::SqliteArguments, FromRow, Sqlite};

    #[derive(Debug, FromRow, PartialEq)]
    struct FakeEntity {
        id: i64,
        name: String,
    }

    impl Entity for FakeEntity {
        fn columns() -> &'static [&'static str] {
            &["id", "name"]
        }
        fn table_name() -> &'static str {
            "fake_entity"
        }
        fn id(&self) -> i64 {
            self.id
        }
    }

    impl Bindable for FakeEntity {
        fn bind_insert<'query>(
            &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
        ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
            query.bind(self.id).bind(&self.name)
        }

        fn bind_update<'query>(
            &'query self, query: Query<'query, Sqlite, SqliteArguments<'query>>,
        ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
            query.bind(&self.name).bind(self.id) // name no SET, id no WHERE
        }
    }

    async fn setup() -> (sqlx::SqlitePool, Repository<FakeEntity>) {
        let pool = setup_test_db().await;

        sqlx::query("CREATE TABLE fake_entity (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
            .execute(&pool)
            .await
            .unwrap();

        let repo = Repository::<FakeEntity>::new(pool.clone());
        (pool, repo)
    }

    /// INFO: Casos de sucesso, testes perfeitos
    #[tokio::test]
    async fn teste_buscar_todos() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk'), (2, 'Vinland')")
            .execute(&pool)
            .await
            .unwrap();

        let result = repo.find_all().await.unwrap();

        assert_eq!(result.len(), 2);
        assert_eq!(result[0].name, "Berserk");
        assert_eq!(result[1].name, "Vinland");
    }

    #[tokio::test]
    async fn teste_inserir() {
        let (_, repo) = setup().await;

        let entity = FakeEntity { id: 1, name: "Berserk".to_string() };
        let result = repo.insert(&entity).await.unwrap();

        assert_eq!(result.id, 1);
        assert_eq!(result.name, "Berserk");
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk')").execute(&pool).await.unwrap();

        let updated = FakeEntity { id: 1, name: "Vinland".to_string() };
        let result = repo.update(&updated).await.unwrap();

        assert_eq!(result.id, 1);
        assert_eq!(result.name, "Vinland");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk')").execute(&pool).await.unwrap();

        repo.delete(1).await.unwrap();

        let result = repo.find_all().await.unwrap();
        assert_eq!(result.len(), 0);
    }

    /// INFO: Casos de erros
    #[tokio::test]
    async fn teste_erro_ao_inserir_duplicado() {
        let (_, repo) = setup().await;

        let entity = FakeEntity { id: 1, name: "Berserk".to_string() };
        repo.insert(&entity).await.unwrap();

        let result = repo.insert(&entity).await;

        assert!(
            matches!(result, Err(DbError::UniqueViolation)),
            "Deveria ter retornado UniqueViolation, mas veio: {:?}",
            result
        );
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let (_, repo) = setup().await;

        let entity = FakeEntity { id: 999, name: "Inexistente".to_string() };
        let result = repo.update(&entity).await;

        assert!(
            matches!(result, Err(DbError::NotFound)),
            "Deveria ter retornado NotFound, mas veio: {:?}",
            result
        );
    }
}
