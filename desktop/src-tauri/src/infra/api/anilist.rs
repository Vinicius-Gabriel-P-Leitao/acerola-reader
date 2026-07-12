use serde::{Deserialize, Serialize};
use serde_json::json;

#[derive(Serialize, Deserialize, Debug)]
pub struct AnilistResponse<T> {
    pub data: T,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistData {
    #[serde(rename = "Media")]
    pub media: AnilistMedia,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistMedia {
    pub id: i64,
    pub title: AnilistTitle,
    pub description: Option<String>,
    pub status: Option<String>,
    pub staff: Option<AnilistStaff>,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistStaff {
    pub edges: Vec<AnilistStaffEdge>,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistStaffEdge {
    pub role: String,
    pub node: AnilistStaffNode,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistStaffNode {
    pub name: AnilistStaffName,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistStaffName {
    pub full: String,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AnilistTitle {
    pub romaji: Option<String>,
    pub english: Option<String>,
    pub native: Option<String>,
}

pub struct AnilistClient {
    client: reqwest::Client,
}

impl AnilistClient {
    pub fn new() -> Self {
        Self { client: reqwest::Client::new() }
    }

    pub async fn search_manga_by_title(&self, title: &str) -> Result<AnilistResponse<AnilistData>, String> {
        let query = "
            query ($search: String) {
              Media (search: $search, type: MANGA) {
                id
                title {
                  romaji
                  english
                  native
                }
                description
                status
                staff {
                  edges {
                    role
                    node {
                      name {
                        full
                      }
                    }
                  }
                }
              }
            }
        ";

        let body = json!({
            "query": query,
            "variables": {
                "search": title
            }
        });

        let res = self.client.post("https://graphql.anilist.co")
            .json(&body)
            .send()
            .await
            .map_err(|e| e.to_string())?
            .json::<AnilistResponse<AnilistData>>()
            .await
            .map_err(|e| e.to_string())?;
            
        Ok(res)
    }
}
