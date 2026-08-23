# Como Contribuir

O Acerola é um monorepo — cada plataforma tem sua própria stack, seus próprios comandos de build/teste e seu próprio guia de contribuição detalhado. Este arquivo só existe para te apontar para o guia certo.

| Plataforma | Guia completo |
| --- | --- |
| Desktop (Rust + Tauri + Svelte) | [acerola/desktop/CONTRIBUTING.md](acerola/desktop/CONTRIBUTING.md) |
| P2P — biblioteca compartilhada (Rust) | [lib/p2p/CONTRIBUTING.md](lib/p2p/CONTRIBUTING.md) |
| Android (Kotlin) | sem guia próprio ainda — abra uma issue antes de um PR |
| Relay (ainda não iniciado) | sem guia próprio ainda — abra uma issue antes de um PR |
| Relay lib — vendorizado do iroh (Rust) | sem guia próprio ainda — abra uma issue antes de um PR |

## Ferramentas necessárias

- [`cargo-make`](https://github.com/sagiegurari/cargo-make) (`cargo install cargo-make`) — precisa estar instalado pra um desenvolvimento melhor. Além de ser usado internamente por cada crate Rust (veja o guia de cada plataforma), a raiz do monorepo tem seu próprio [`Makefile.toml`](Makefile.toml) com tarefas de manutenção do repo inteiro:
  - `cargo make clean` — remove o `target/` de todos os crates Rust do monorepo (`lib/p2p`, `lib/relay`, `acerola/desktop/src-tauri`, `acerola/android/native/rust`) de uma vez, sem precisar entrar em cada pasta.

## Regras que valem para o monorepo inteiro

- **Escopo por PR**: um PR deve tocar uma única plataforma (`acerola/android/`, `acerola/desktop/`, `acerola/relay/`, `lib/p2p/` ou `lib/relay/`), salvo mudanças de fato compartilhadas (docs raiz, `LICENSE`, `PRIVACY_POLICY.md`).
- **Convenção de commit**: `[tag](plataforma): descrição`, por exemplo `[fix](desktop): corrige leak de conexão no reader`. O `tag` segue os mesmos prefixos documentados nos guias específicos (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `merge`); a `plataforma` é `android`, `desktop`, `p2p`, `relay` (para `acerola/relay/`), `relay-lib` (para `lib/relay/`, o código vendorizado do iroh) ou `monorepo` para mudanças na raiz.
- **Testes**: cada plataforma tem seu próprio runner e sua própria automação via `cargo-make`/Gradle/Vitest — veja o guia específico antes de rodar testes manualmente.
- **`lib/p2p` é dependência `path` local**: `acerola/android` e `acerola/desktop` consomem `lib/p2p` por caminho relativo no `Cargo.toml`, não por `git`. Uma mudança em `lib/p2p/` já vale pros dois consumidores no mesmo PR — não precisa publicar nem rodar `cargo update` em outro repo.

Para o resto (arquitetura interna, padrões de código, setup do ambiente), siga o guia da plataforma que você está mexendo.
