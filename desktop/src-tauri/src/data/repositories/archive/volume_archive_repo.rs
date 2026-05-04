use crate::data::models::archive::volume_archive::VolumeArchive;
use crate::data::repositories::Repository;
use sqlx::SqlitePool;

pub struct VolumeRepository {
    pub base: Repository<VolumeArchive>,
}

impl VolumeRepository {
    pub fn new(pool: SqlitePool) -> Self {
        Self { base: Repository::new(pool) }
    }
}

#[cfg(test)]
mod tests {
    use super::VolumeRepository;
    use crate::data::models::archive::comic_directory::ComicDirectory;
    use crate::data::models::archive::volume_archive::VolumeArchive;
    use crate::data::repositories::Repository;
    use crate::infra::error::DbError;
    use crate::tests::utils::setup_test_db::setup_test_db;

    fn berserk() -> ComicDirectory {
        ComicDirectory {
            id: 1,
            name: "Berserk".to_string(),
            path: "/quadrinhos/berserk".to_string(),
            cover: None,
            banner: None,
            last_modified: 1700000000,
            archive_template_fk: None,
            external_sync_enabled: true,
            hidden: false,
        }
    }

    fn vol1() -> VolumeArchive {
        VolumeArchive {
            id: 1,
            name: "Vol. 01".to_string(),
            path: "/quadrinhos/berserk/Vol. 01".to_string(),
            volume_sort: "1".to_string(),
            is_special: false,
            cover: None,
            banner: None,
            comic_directory_fk: 1,
            last_modified: 1700000000,
        }
    }

    async fn setup() -> VolumeRepository {
        let pool = setup_test_db().await;
        Repository::<ComicDirectory>::new(pool.clone()).insert(&berserk()).await.unwrap();
        VolumeRepository::new(pool)
    }

    #[tokio::test]
    async fn teste_inserir_e_buscar_todos() {
        let repo = setup().await;
        let inserted = repo.base.insert(&vol1()).await.unwrap();
        assert_eq!(inserted.id, 1);
        assert_eq!(inserted.name, "Vol. 01");
        assert_eq!(inserted.volume_sort, "1");
        let all = repo.base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
    }

    #[tokio::test]
    async fn teste_atualizar() {
        let repo = setup().await;
        repo.base.insert(&vol1()).await.unwrap();
        let updated = VolumeArchive { name: "Vol. 001 Special".to_string(), ..vol1() };
        let result = repo.base.update(&updated).await.unwrap();
        assert_eq!(result.name, "Vol. 001 Special");
    }

    #[tokio::test]
    async fn teste_deletar() {
        let repo = setup().await;
        repo.base.insert(&vol1()).await.unwrap();
        repo.base.delete(1).await.unwrap();
        assert_eq!(repo.base.find_all().await.unwrap().len(), 0);
    }

    #[tokio::test]
    async fn teste_erro_unique_volume_sort_por_comic() {
        let repo = setup().await;
        repo.base.insert(&vol1()).await.unwrap();
        let dup = VolumeArchive { id: 2, ..vol1() };
        let result = repo.base.insert(&dup).await;
        assert!(matches!(result, Err(DbError::UniqueViolation)));
    }

    #[tokio::test]
    async fn teste_erro_ao_atualizar_inexistente() {
        let repo = setup().await;
        let result = repo.base.update(&vol1()).await;
        assert!(matches!(result, Err(DbError::NotFound)));
    }
}
