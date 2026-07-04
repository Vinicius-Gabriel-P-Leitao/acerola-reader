use anyhow::Result;
use serde_json::{json, Value};
use sqlx::{query, Pool, Sqlite};

use super::support::{build_webview, invoke_ok, invoke_ok_value};
use crate::{
    cmd::features::history as history_cmd,
    core::services::history::HistoryService,
    tests::utils::setup_test_db::setup_test_db_with_comic,
};

async fn setup() -> Result<(Pool<Sqlite>, tauri::App<tauri::test::MockRuntime>, tauri::WebviewWindow<tauri::test::MockRuntime>)> {
    let pool = setup_test_db_with_comic().await;
    let service = HistoryService::new(pool.clone());
    
    // Insere capitulo basico no banco para satisfazer chaves estrangeiras
    query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, comic_directory_fk, last_modified) VALUES (1, '1', 'path', '1', 0, 1, 0)").execute(&pool).await.unwrap();
    
    let builder = tauri::test::mock_builder().invoke_handler(tauri::generate_handler![
        history_cmd::history_update_reading,
        history_cmd::history_get_all,
        history_cmd::history_get_comic,
        history_cmd::history_get_read_chapters,
        history_cmd::history_clear,
    ]);
    
    let builder = builder.manage(service);
    let (app, webview) = build_webview(builder)?;
    
    Ok((pool, app, webview))
}

#[tokio::test(flavor = "multi_thread")]
async fn teste_atualiza_historico_leitura_com_sucesso() -> Result<()> {
    // Atualiza o historico de leitura com sucesso
    let (_pool, _app, webview) = setup().await?;
    
    let _: Value = invoke_ok(&webview, "history_update_reading", json!({
        "comicId": "1",
        "chapterId": "1",
        "lastPage": 5,
        "isCompleted": false
    }))?;
    
    let response = invoke_ok_value(&webview, "history_get_all", json!({}))?;
    let history = response.as_array().unwrap();
    assert_eq!(history.len(), 1);
    assert_eq!(history[0]["comicDirectoryId"], "1");
    assert_eq!(history[0]["lastPage"], 5);
    
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn teste_obtem_historico_completo() -> Result<()> {
    // Obtem todos os itens de historico corretamente
    let (_pool, _app, webview) = setup().await?;
    
    let _: Value = invoke_ok(&webview, "history_update_reading", json!({
        "comicId": "1",
        "chapterId": "1",
        "lastPage": 2,
        "isCompleted": true
    }))?;
    
    let response = invoke_ok_value(&webview, "history_get_all", json!({}))?;
    let history = response.as_array().unwrap();
    assert_eq!(history.len(), 1);
    assert_eq!(history[0]["isCompleted"], true);
    
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn teste_limpa_historico_de_leitura() -> Result<()> {
    // Limpa o historico de leitura
    let (_pool, _app, webview) = setup().await?;
    
    let _: Value = invoke_ok(&webview, "history_update_reading", json!({
        "comicId": "1",
        "chapterId": "1",
        "lastPage": 2,
        "isCompleted": true
    }))?;
    
    let _: Value = invoke_ok(&webview, "history_clear", json!({}))?;
    
    let response = invoke_ok_value(&webview, "history_get_all", json!({}))?;
    let history = response.as_array().unwrap();
    assert_eq!(history.len(), 0);
    
    Ok(())
}
