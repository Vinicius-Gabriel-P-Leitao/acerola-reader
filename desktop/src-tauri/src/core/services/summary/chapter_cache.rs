use std::{
    num::NonZeroUsize,
    sync::{Arc, Mutex},
};

use lru::LruCache;

use crate::cmd::events::summary::ChapterDto;

/// Mesma origem de dado (SQLite) muda de custo dependendo do padrão de acesso:
/// reabrir um volume/ordenação/busca já visto não precisa rodar a query de
/// novo. Espelha o `LruCache` do leitor (`core/services/reader/mod.rs`), só
/// que aqui a "página cara" é o resultado de uma query em vez de bytes
/// decodificados de um arquivo.
const CHAPTER_CACHE_CAPACITY: usize = 32;

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ChapterCacheKey {
    pub comic_directory_fk: i64,
    pub volume_id: Option<i64>,
    pub sort_by: String,
    pub search_query: Option<String>,
    // INFO: use-reader-navigation.svelte.ts busca um capítulo específico por
    // posição via page=índice/page_size=1 (mesma comic_directory_fk/volume_id/
    // sort_by/search_query de uma busca "tudo de uma vez"). Sem page/page_size
    // na chave, essa chamada colidiria com o cache de outra paginação e
    // devolveria o capítulo errado.
    pub page: i32,
    pub page_size: i32,
}

#[derive(Clone)]
pub struct ChapterCacheService {
    cache: Arc<Mutex<LruCache<ChapterCacheKey, ChapterDto>>>,
}

impl ChapterCacheService {
    pub fn new() -> Self {
        Self {
            cache: Arc::new(Mutex::new(LruCache::new(
                NonZeroUsize::new(CHAPTER_CACHE_CAPACITY).unwrap(),
            ))),
        }
    }

    pub fn get(&self, key: &ChapterCacheKey) -> Option<ChapterDto> {
        let mut cache = self.cache.lock().unwrap();
        cache.get(key).cloned()
    }

    pub fn put(&self, key: ChapterCacheKey, value: ChapterDto) {
        let mut cache = self.cache.lock().unwrap();
        cache.put(key, value);
    }

    /// Chamado após rescan/deep rescan — as únicas operações que realmente
    /// mudam quais capítulos/volumes existem no disco. Sync de metadata
    /// (título, descrição, capa) não invalida, porque não muda identidade
    /// de capítulo.
    pub fn invalidate_comic(&self, comic_directory_fk: i64) {
        let mut cache = self.cache.lock().unwrap();

        let stale_keys: Vec<ChapterCacheKey> = cache
            .iter()
            .filter(|(key, _)| key.comic_directory_fk == comic_directory_fk)
            .map(|(key, _)| key.clone())
            .collect();

        for key in stale_keys {
            cache.pop(&key);
        }
    }
}

impl Default for ChapterCacheService {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample_dto() -> ChapterDto {
        use crate::cmd::events::summary::{ChapterPageDto, VolumeViewType};

        ChapterDto {
            archive: ChapterPageDto {
                items: vec![],
                volumes: vec![],
                page_size: 1_000_000,
                page: 0,
                total: 0,
                volume_sections: vec![],
            },
            show_volume_headers: false,
            has_volume_structure: false,
            effective_view_mode: VolumeViewType::Chapter,
        }
    }

    fn key(comic_directory_fk: i64, volume_id: Option<i64>) -> ChapterCacheKey {
        ChapterCacheKey {
            comic_directory_fk,
            volume_id,
            sort_by: "number_asc".to_string(),
            search_query: None,
            page: 0,
            page_size: 1_000_000,
        }
    }

    #[test]
    fn teste_page_e_page_size_diferentes_nao_colidem_no_cache() {
        let service = ChapterCacheService::new();

        let full_list_key = key(1, None);
        let single_chapter_key = ChapterCacheKey { page: 5, page_size: 1, ..key(1, None) };

        service.put(full_list_key.clone(), sample_dto());

        assert!(service.get(&full_list_key).is_some());
        assert!(service.get(&single_chapter_key).is_none());
    }

    #[test]
    fn teste_get_retorna_none_quando_nao_ha_entrada() {
        let service = ChapterCacheService::new();
        assert!(service.get(&key(1, None)).is_none());
    }

    #[test]
    fn teste_put_e_get_retornam_o_mesmo_valor() {
        let service = ChapterCacheService::new();
        let cache_key = key(1, None);

        service.put(cache_key.clone(), sample_dto());

        assert!(service.get(&cache_key).is_some());
    }

    #[test]
    fn teste_invalidate_comic_remove_so_as_entradas_daquele_comic() {
        let service = ChapterCacheService::new();
        let key_comic_1 = key(1, None);
        let key_comic_2 = key(2, None);

        service.put(key_comic_1.clone(), sample_dto());
        service.put(key_comic_2.clone(), sample_dto());

        service.invalidate_comic(1);

        assert!(service.get(&key_comic_1).is_none());
        assert!(service.get(&key_comic_2).is_some());
    }
}
