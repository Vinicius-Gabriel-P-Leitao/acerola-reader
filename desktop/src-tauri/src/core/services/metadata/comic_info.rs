use serde::{de, Deserialize, Deserializer, Serialize};

fn deserialize_option_i32<'de, D>(deserializer: D) -> Result<Option<i32>, D::Error>
where
    D: Deserializer<'de>,
{
    let optional_string = Option::<String>::deserialize(deserializer)?;
    match optional_string {
        Some(raw_text) => {
            let trimmed_text = raw_text.trim();
            if trimmed_text.is_empty() {
                Ok(None)
            } else {
                trimmed_text.parse::<i32>().map(Some).map_err(de::Error::custom)
            }
        },
        None => Ok(None),
    }
}

fn deserialize_i32<'de, D>(deserializer: D) -> Result<i32, D::Error>
where
    D: Deserializer<'de>,
{
    deserialize_option_i32(deserializer).map(|optional_number| optional_number.unwrap_or(0))
}

fn deserialize_i64<'de, D>(deserializer: D) -> Result<i64, D::Error>
where
    D: Deserializer<'de>,
{
    let optional_string = Option::<String>::deserialize(deserializer)?;
    match optional_string {
        Some(raw_text) => {
            let trimmed_text = raw_text.trim();
            if trimmed_text.is_empty() {
                Ok(0)
            } else {
                trimmed_text.parse::<i64>().map_err(de::Error::custom)
            }
        },
        None => Ok(0),
    }
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Default)]
#[serde(rename_all = "PascalCase")]
pub struct ComicInfo {
    #[serde(default)]
    pub title: Option<String>,
    #[serde(default)]
    pub series: Option<String>,
    #[serde(default)]
    pub number: Option<String>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub count: Option<i32>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub volume: Option<i32>,
    #[serde(default)]
    pub summary: Option<String>,
    #[serde(default)]
    pub notes: Option<String>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub year: Option<i32>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub month: Option<i32>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub day: Option<i32>,
    #[serde(default)]
    pub writer: Option<String>,
    #[serde(default)]
    pub penciller: Option<String>,
    #[serde(default)]
    pub inker: Option<String>,
    #[serde(default)]
    pub colorist: Option<String>,
    #[serde(default)]
    pub letterer: Option<String>,
    #[serde(default)]
    pub cover_artist: Option<String>,
    #[serde(default)]
    pub editor: Option<String>,
    #[serde(default)]
    pub publisher: Option<String>,
    #[serde(default)]
    pub genre: Option<String>,
    #[serde(default)]
    pub web: Option<String>,
    #[serde(default, deserialize_with = "deserialize_option_i32")]
    pub page_count: Option<i32>,
    #[serde(default)]
    pub language_iso: Option<String>,
    #[serde(default)]
    pub format: Option<String>,
    #[serde(default)]
    pub black_and_white: Option<String>,
    #[serde(default)]
    pub manga: Option<String>,
    #[serde(default)]
    pub characters: Option<String>,
    #[serde(default)]
    pub teams: Option<String>,
    #[serde(default)]
    pub locations: Option<String>,
    #[serde(default)]
    pub scan_information: Option<String>,
    #[serde(default)]
    pub story_arc: Option<String>,
    #[serde(default)]
    pub series_group: Option<String>,
    #[serde(default)]
    pub age_rating: Option<String>,
    #[serde(default)]
    pub pages: Option<Pages>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Default)]
pub struct Pages {
    #[serde(rename = "Page", default)]
    pub pages: Vec<Page>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Default)]
#[serde(rename_all = "PascalCase")]
pub struct Page {
    #[serde(rename = "@Image", default, deserialize_with = "deserialize_i32")]
    pub image: i32,
    #[serde(rename = "@Type", default)]
    pub page_type: String,
    #[serde(rename = "@DoublePage", default)]
    pub double_page: bool,
    #[serde(rename = "@ImageSize", default, deserialize_with = "deserialize_i64")]
    pub image_size: i64,
    #[serde(rename = "@Key", default)]
    pub key: String,
    #[serde(rename = "@Bookmark", default)]
    pub bookmark: String,
    #[serde(rename = "@ImageWidth", default, deserialize_with = "deserialize_i32")]
    pub image_width: i32,
    #[serde(rename = "@ImageHeight", default, deserialize_with = "deserialize_i32")]
    pub image_height: i32,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_deserialize_empty_numeric_tags() {
        let xml_payload = r#"<ComicInfo>
            <Title>Test Comic</Title>
            <Year>2024</Year>
            <Month></Month>
            <PageCount>42</PageCount>
            <Volume></Volume>
        </ComicInfo>"#;

        let comic_info: ComicInfo =
            quick_xml::de::from_str(xml_payload).expect("Failed to parse valid XML");
        assert_eq!(comic_info.title.as_deref(), Some("Test Comic"));
        assert_eq!(comic_info.year, Some(2024));
        assert_eq!(comic_info.month, None);
        assert_eq!(comic_info.page_count, Some(42));
        assert_eq!(comic_info.volume, None);
    }

    #[test]
    fn test_deserialize_pages_with_empty_and_filled_attributes() {
        let xml_payload = r#"<ComicInfo>
            <Pages>
                <Page Image="0" Type="FrontCover" ImageWidth="1000" ImageHeight="1500" />
                <Page Image="1" Type="Story" ImageWidth="" ImageHeight="" />
            </Pages>
        </ComicInfo>"#;

        let comic_info: ComicInfo =
            quick_xml::de::from_str(xml_payload).expect("Failed to parse Pages XML");
        let pages = comic_info.pages.expect("Pages element should be present").pages;
        assert_eq!(pages.len(), 2);
        assert_eq!(pages[0].image, 0);
        assert_eq!(pages[0].image_width, 1000);
        assert_eq!(pages[0].image_height, 1500);
        assert_eq!(pages[1].image, 1);
        assert_eq!(pages[1].image_width, 0);
        assert_eq!(pages[1].image_height, 0);
    }
}
