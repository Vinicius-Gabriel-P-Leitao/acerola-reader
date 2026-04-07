use sqlx::{ query, query_as, FromRow, Pool, Sqlite, sqlite::{ SqliteArguments, SqliteRow } };
use std::marker::PhantomData;

pub trait Entity {
    fn columns() -> &'static [&'static str];
    fn table_name() -> &'static str;
    fn id(&self) -> i64;
}

pub struct Repository<T: Entity> {
    pool: Pool<Sqlite>,
    _marker: PhantomData<T>,
}

pub trait Bindable {
    fn bind_insert<'query>(
        &'query self,
        query: sqlx::query::Query<'query, Sqlite, SqliteArguments<'query>>
    ) -> sqlx::query::Query<'query, Sqlite, SqliteArguments<'query>>;

    fn bind_update<'q>(
        &'q self,
        query: sqlx::query::Query<'q, Sqlite, SqliteArguments<'q>>
    ) -> sqlx::query::Query<'q, Sqlite, SqliteArguments<'q>>;
}

impl<T: Entity> Repository<T> {
    pub fn new(pool: Pool<Sqlite>) -> Self {
        Self { pool, _marker: PhantomData }
    }

    // prettier-ignore
    pub async fn find_all(&self) -> Result<Vec<T>, sqlx::Error>
        where T: Entity + for<'row> FromRow<'row, SqliteRow> + Send + Unpin
    {
        let cols = T::columns().join(", ");
        let table = T::table_name();

        // prettier-ignore
        query_as::<_, T>(&format!("SELECT {} FROM {}", cols, table)).fetch_all(&self.pool).await
    }

    // prettier-ignore
    pub async fn insert(&self, entity: &T) -> Result<T, sqlx::Error>
      where T: Entity + Bindable + for<'row> FromRow<'row, SqliteRow> + Send + Unpin
    {
        let table = T::table_name();
        let cols = T::columns().join(", ");
        let placeholders = T::columns().iter().map(|_| "?").collect::<Vec<_>>().join(", ");

        let sql = format!("INSERT INTO {} ({}) VALUES ({}) RETURNING *", table, cols, placeholders);
        let row = entity.bind_insert(query(&sql)).fetch_one(&self.pool).await?;

        T::from_row(&row)
    }

    // prettier-ignore
    pub async fn update(&self, entity: &T) -> Result<T, sqlx::Error>
        where T: Entity + Bindable + for<'r> FromRow<'r, SqliteRow> + Send + Unpin
    {
        let table = T::table_name();
        let set_clause = T::columns().iter().filter(|col| **col != "id")
            .map(|col| format!("{} = ?", col)).collect::<Vec<_>>().join(", ");

        let sql = format!("UPDATE {} SET {} WHERE id = ? RETURNING *", table, set_clause);
        let row = entity.bind_update(query(&sql)).fetch_one(&self.pool).await?;

        T::from_row(&row)
    }

    // prettier-ignore
    pub async fn delete(&self, id: i64) -> Result<(), sqlx::Error> {
        let table = T::table_name();
        query(&format!("DELETE FROM {} WHERE id = ?", table)).bind(id).execute(&self.pool).await?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use crate::data::repositories::base::{ Entity, Bindable, Repository };
    use sqlx::{ FromRow, sqlite::SqliteArguments, query::Query, Sqlite };
    use crate::tests::utils::setup_test_db::setup_test_db;

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
            &'query self,
            query: Query<'query, Sqlite, SqliteArguments<'query>>
        ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
            query.bind(self.id).bind(&self.name)
        }

        fn bind_update<'query>(
            &'query self,
            query: Query<'query, Sqlite, SqliteArguments<'query>>
        ) -> Query<'query, Sqlite, SqliteArguments<'query>> {
            query.bind(&self.name).bind(self.id) // name no SET, id no WHERE
        }
    }

    async fn setup() -> (sqlx::SqlitePool, Repository<FakeEntity>) {
        let pool = setup_test_db().await;

        sqlx::query("CREATE TABLE fake_entity (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
            .execute(&pool).await
            .unwrap();

        let repo = Repository::<FakeEntity>::new(pool.clone());
        (pool, repo)
    }

    #[tokio::test]
    async fn test_find_all_returns_all_rows() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk'), (2, 'Vinland')")
            .execute(&pool).await
            .unwrap();

        let result = repo.find_all().await.unwrap();

        assert_eq!(result.len(), 2);
        assert_eq!(result[0].name, "Berserk");
        assert_eq!(result[1].name, "Vinland");
    }

    #[tokio::test]
    async fn test_insert_returns_inserted_entity() {
        let (_, repo) = setup().await;

        let entity = FakeEntity { id: 1, name: "Berserk".to_string() };
        let result = repo.insert(&entity).await.unwrap();

        assert_eq!(result.id, 1);
        assert_eq!(result.name, "Berserk");
    }

    #[tokio::test]
    async fn test_update_returns_updated_entity() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk')").execute(&pool).await.unwrap();

        let updated = FakeEntity { id: 1, name: "Vinland".to_string() };
        let result = repo.update(&updated).await.unwrap();

        assert_eq!(result.id, 1);
        assert_eq!(result.name, "Vinland");
    }

    #[tokio::test]
    async fn test_delete_removes_entity() {
        let (pool, repo) = setup().await;

        sqlx::query("INSERT INTO fake_entity VALUES (1, 'Berserk')").execute(&pool).await.unwrap();

        repo.delete(1).await.unwrap();

        let result = repo.find_all().await.unwrap();
        assert_eq!(result.len(), 0);
    }
}
