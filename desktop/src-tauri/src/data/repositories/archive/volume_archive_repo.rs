use crate::data::models::archive::volume_archive::VolumeArchive;
use crate::data::repositories::{Entity, Repository};
use crate::infra::error::DbError;
use sqlx::SqlitePool;

pub struct VolumeRepository {
    pub base: Repository<VolumeArchive>,
    pub pool: SqlitePool,
}

impl VolumeRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool.clone()), pool }
    }

    pub async fn find_by_comic(&self, comic_directory_fk: i64) -> Result<Vec<VolumeArchive>, DbError> {
        let cols = VolumeArchive::columns().join(", ");
        let result = sqlx::query_as::<_, VolumeArchive>(&format!(
            "SELECT {} FROM volume_archive WHERE comic_directory_fk = ? ORDER BY CAST(volume_sort AS INTEGER) ASC",
            cols
        ))
        .bind(comic_directory_fk)
        .fetch_all(&self.pool)
        .await?;

        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::VolumeRepository;
    use crate::data::models::archive::volume_archive::VolumeArchive;
    use crate::infra::error::DbError;
    use crate::tests::utils::setup_test_db::setup_test_db_with_comic;

    fn vol1() -> VolumeArchive {
        VolumeArchive {
            id: 1,
            name: "Vol. 01".to_string(),
            path: "/test/Vol. 01".to_string(),
            volume_sort: "1".to_string(),
            is_special: false,
            cover: None,
            banner: None,
            comic_directory_fk: 1,
            last_modified: 1700000000,
        }
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        let inserted = repo.base.insert(&vol1()).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.name, "Vol. 01");
        assert_eq!(inserted.volume_sort, "1");
        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        repo.base.insert(&vol1()).await.unwrap();
        let updated = VolumeArchive { name: "Vol. 001 Special".to_string(), ..vol1() };
        let result = repo.base.update(&updated).await.unwrap();
        assert_eq!(result.name, "Vol. 001 Special");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        repo.base.insert(&vol1()).await.unwrap();
        repo.base.delete(1).await.unwrap();
        assert_eq!(repo.base.find_all().await.unwrap().len(), 0);
    }

    #[tokio::test]
    async fn teste_buscar_por_comic() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        repo.base.insert(&vol1()).await.unwrap();
        repo.base.insert(&VolumeArchive { id: 2, name: "Vol. 02".to_string(), volume_sort: "2".to_string(), ..vol1() }).await.unwrap();
        
        let volumes = repo.find_by_comic(1).await.unwrap();
        assert_eq!(volumes.len(), 2);
        assert_eq!(volumes[0].volume_sort, "1");
        assert_eq!(volumes[1].volume_sort, "2");
    }

    #[tokio::test]
    async fn teste_erro_unique_volume_sort_por_comic() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        repo.base.insert(&vol1()).await.unwrap();
        let dup = VolumeArchive { id: 2, ..vol1() };
        let result = repo.base.insert(&dup).await;
        assert!(matches!(result, Err(DbError::UniqueViolation)));
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let pool = setup_test_db_with_comic().await;
        let repo = VolumeRepository::new(pool);
        let result = repo.base.update(&vol1()).await;
        assert!(matches!(result, Err(DbError::NotFound)));
    }
}
