pub const SPECIAL_KEYWORDS: &[&str] = &["special", "extra", "oneshot", "especial"];

pub fn is_special_name(name: &str) -> bool {
    if name.trim().is_empty() {
        return false;
    }
    
    let lower = name.to_lowercase();
    SPECIAL_KEYWORDS.iter().any(|kw| lower.contains(kw))
}

pub fn special_pattern() -> String {
    SPECIAL_KEYWORDS.join("|")
}
