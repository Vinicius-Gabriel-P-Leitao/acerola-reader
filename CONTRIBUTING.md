# Como Contribuir

O Acerola é um monorepo — cada plataforma tem sua própria stack, seus próprios comandos de build/teste e seu próprio guia de contribuição detalhado. Este arquivo só existe para te apontar para o guia certo.

| Plataforma | Guia completo |
| --- | --- |
| Desktop (Rust + Tauri + Svelte) | [desktop/CONTRIBUTING.md](desktop/CONTRIBUTING.md) |
| P2P — biblioteca compartilhada (Rust) | [p2p/CONTRIBUTING.md](p2p/CONTRIBUTING.md) |
| Android (Kotlin) | sem guia próprio ainda — abra uma issue antes de um PR |
| Relay (Rust) | sem guia próprio ainda — abra uma issue antes de um PR |

## Regras que valem para o monorepo inteiro

- **Escopo por PR**: um PR deve tocar uma única plataforma (`android/`, `desktop/`, `p2p/` ou `relay/`), salvo mudanças de fato compartilhadas (docs raiz, `LICENSE`, `PRIVACY_POLICY.md`).
- **Convenção de commit**: `[tag](plataforma): descrição`, por exemplo `[fix](desktop): corrige leak de conexão no reader`. O `tag` segue os mesmos prefixos documentados nos guias específicos (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `merge`); a `plataforma` é `android`, `desktop`, `p2p`, `relay` ou `monorepo` para mudanças na raiz.
- **Testes**: cada plataforma tem seu próprio runner e sua própria automação via `cargo-make`/Gradle/Vitest — veja o guia específico antes de rodar testes manualmente.

Para o resto (arquitetura interna, padrões de código, setup do ambiente), siga o guia da plataforma que você está mexendo.
