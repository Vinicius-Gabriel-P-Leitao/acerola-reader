package br.acerola.comic.util.file

/**
 * Kotlin equivalent of Rust's `std::path::Path::file_stem()`: strips everything
 * from (and including) the last '.', except when that '.' is the first
 * character of the name (dotfiles like ".gitignore" are left unchanged).
 */
fun String.fileStem(): String {
    val lastDot = lastIndexOf('.')
    if (lastDot <= 0) return this
    return substring(0, lastDot)
}
