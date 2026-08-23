use std::cmp::Ordering;

/// Compara dois nomes de arquivo/entrada em ordem natural (`page2` antes de `page10`).
pub fn natural_cmp(left: &str, right: &str) -> Ordering {
    natural_key(left).cmp(&natural_key(right))
}

/// Gera uma chave de ordenação onde sequências numéricas são normalizadas por tamanho fixo,
/// para que a comparação lexicográfica resultante respeite a ordem numérica natural.
pub fn natural_key(value: &str) -> String {
    let mut output = String::with_capacity(value.len());
    let mut digits = String::new();

    for character in value.chars() {
        if character.is_ascii_digit() {
            digits.push(character);
            continue;
        }

        flush_digits(&mut output, &mut digits);
        output.push(character.to_ascii_lowercase());
    }

    flush_digits(&mut output, &mut digits);
    output
}

fn flush_digits(output: &mut String, digits: &mut String) {
    if digits.is_empty() {
        return;
    }

    let trimmed = digits.trim_start_matches('0');
    let normalized = if trimmed.is_empty() { "0" } else { trimmed };

    output.push_str(&format!("{normalized:0>20}"));
    digits.clear();
}

#[cfg(test)]
mod tests {
    use super::natural_key;

    #[test]
    fn test_sorts_numbered_pages_in_natural_order() {
        let mut pages = vec!["page10.jpg", "page2.jpg", "page001.jpg"];
        pages.sort_by_key(|page| natural_key(page));

        assert_eq!(pages, vec!["page001.jpg", "page2.jpg", "page10.jpg"]);
    }
}
