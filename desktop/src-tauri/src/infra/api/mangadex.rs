use std::collections::HashMap;

use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct MangadexResponse<T> {
    pub result: String,
    pub response: String,
    #[serde(default = "Vec::new")]
    pub data: Vec<T>,
    pub limit: Option<i32>,
    pub offset: Option<i32>,
    pub total: Option<i32>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct MangaData {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub attributes: MangaAttributes,
    #[serde(default = "Vec::new")]
    pub relationships: Vec<Relationship>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct MangaAttributes {
    pub title: HashMap<String, String>,
    #[serde(default = "Vec::new")]
    pub alt_titles: Vec<HashMap<String, String>>,
    #[serde(default = "HashMap::new")]
    pub description: HashMap<String, String>,
    #[serde(default)]
    pub is_locked: bool,
    pub links: Option<Links>,
    pub status: String,
    pub year: Option<i64>,
    #[serde(default = "Vec::new")]
    pub tags: Vec<Tag>,
    pub latest_uploaded_chapter: Option<String>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Links {
    pub al: Option<String>,
    pub ap: Option<String>,
    pub kt: Option<String>,
    pub mu: Option<String>,
    pub mal: Option<String>,
    pub raw: Option<String>,
    pub amz: Option<String>,
    pub ebj: Option<String>,
    pub engtl: Option<String>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Tag {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub attributes: TagAttributes,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct TagAttributes {
    pub name: HashMap<String, String>,
    pub group: String,
    pub version: i32,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Relationship {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub related: Option<String>,
    pub attributes: Option<RelationshipAttributes>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct RelationshipAttributes {
    pub name: Option<String>,
    pub volume: Option<String>,
    pub file_name: Option<String>,
    pub locale: Option<String>,
}

pub struct MangadexClient {
    client: reqwest::Client,
}

impl MangadexClient {
    pub fn new() -> Self {
        Self { client: reqwest::Client::new() }
    }

    pub async fn search_manga_by_title(
        &self, title: &str,
    ) -> Result<MangadexResponse<MangaData>, String> {
        let res = self
            .client
            .get("https://api.mangadex.org/manga")
            .header("User-Agent", "AcerolaMangaApp/1.0 (Acerola Desktop)")
            .query(&[("title", title), ("includes[]", "author"), ("includes[]", "cover_art")])
            .send()
            .await
            .map_err(|err| err.to_string())?
            .json::<MangadexResponse<MangaData>>()
            .await
            .map_err(|err| err.to_string())?;
        Ok(res)
    }

    pub async fn get_manga_chapters(
        &self, manga_id: &str, language: &str,
    ) -> Result<MangadexResponse<ChapterData>, String> {
        let res = self
            .client
            .get(format!("https://api.mangadex.org/manga/{}/feed", manga_id))
            .header("User-Agent", "AcerolaMangaApp/1.0 (Acerola Desktop)")
            .query(&[
                ("limit", "100"),
                ("includes[]", "scanlation_group"),
                ("order[chapter]", "asc"),
                ("translatedLanguage[]", language),
            ])
            .send()
            .await
            .map_err(|err| err.to_string())?
            .json::<MangadexResponse<ChapterData>>()
            .await
            .map_err(|err| err.to_string())?;
        Ok(res)
    }

    pub fn get_cover_url(manga_id: &str, file_name: &str) -> String {
        format!("https://uploads.mangadex.org/covers/{}/{}", manga_id, file_name)
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ChapterData {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub attributes: ChapterAttributes,
    #[serde(default = "Vec::new")]
    pub relationships: Vec<Relationship>,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ChapterAttributes {
    pub volume: Option<String>,
    pub chapter: Option<String>,
    pub title: Option<String>,
    pub translated_language: Option<String>,
    pub external_url: Option<String>,
    pub publish_at: Option<String>,
    pub readable_at: Option<String>,
    pub created_at: Option<String>,
    pub updated_at: Option<String>,
    pub pages: Option<i32>,
    pub version: Option<i32>,
}
