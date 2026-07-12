use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "PascalCase")]
pub struct ComicInfo {
    pub title: Option<String>,
    pub series: Option<String>,
    pub number: Option<String>,
    pub summary: Option<String>,
    pub writer: Option<String>,
    pub penciller: Option<String>,
    pub inker: Option<String>,
    pub colorist: Option<String>,
    pub letterer: Option<String>,
    pub cover_artist: Option<String>,
    pub editor: Option<String>,
    pub publisher: Option<String>,
    pub genre: Option<String>,
    pub web: Option<String>,
    pub page_count: Option<i32>,
    pub language_iso: Option<String>,
    pub format: Option<String>,
    pub black_and_white: Option<String>,
    pub manga: Option<String>,
    pub characters: Option<String>,
    pub teams: Option<String>,
    pub locations: Option<String>,
    pub scan_information: Option<String>,
    pub story_arc: Option<String>,
    pub series_group: Option<String>,
    pub age_rating: Option<String>,
    pub pages: Option<Pages>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub struct Pages {
    #[serde(rename = "Page")]
    pub pages: Vec<Page>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "PascalCase")]
pub struct Page {
    #[serde(rename = "@Image")]
    pub image: i32,
    #[serde(rename = "@Type", default)]
    pub page_type: String,
    #[serde(rename = "@DoublePage", default)]
    pub double_page: bool,
    #[serde(rename = "@ImageSize", default)]
    pub image_size: i64,
    #[serde(rename = "@Key", default)]
    pub key: String,
    #[serde(rename = "@Bookmark", default)]
    pub bookmark: String,
    #[serde(rename = "@ImageWidth", default)]
    pub image_width: i32,
    #[serde(rename = "@ImageHeight", default)]
    pub image_height: i32,
}
