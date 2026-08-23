# Acerola

Ecossistema de leitura de quadrinhos/mangás locais com sincronização 100% P2P entre dispositivos — sem servidor central, sem conta, sem nuvem.

Este é um monorepo: cada plataforma vive isolada na sua própria pasta, com sua própria stack e seu próprio README completo. Nenhuma plataforma depende diretamente de outra — todas consomem as bibliotecas compartilhadas em `lib/`.

| Pasta | Plataforma | Stack | Leia mais |
| --- | --- | --- | --- |
| [`android/`](android/README.md) | App Android nativo | Kotlin + Jetpack Compose | [android/README.md](android/README.md) |
| [`desktop/`](desktop/README.md) | App Desktop | Rust + Tauri + Svelte 5 | [desktop/README.md](desktop/README.md) |
| [`dashboard/`](dashboard/) | Serviço + UI de monitoramento do relay (deployável na VPS) | Rust — ainda não iniciado | — |
| [`lib/p2p/`](lib/p2p/README.md) | Biblioteca P2P compartilhada | Rust (iroh / QUIC) | [lib/p2p/README.md](lib/p2p/README.md) |
| [`lib/relay/`](lib/relay/) | Código do relay iroh vendorizado, base para o `dashboard/` | Rust | — |

## Como as peças se conectam

```mermaid
flowchart LR
    Android["android/"] --> P2P["lib/p2p/\n(biblioteca compartilhada)"]
    Desktop["desktop/"] --> P2P
    Dashboard["dashboard/"] --> Relay["lib/relay/\n(vendorizado do iroh)"]
    P2P -.->|"acesso remoto\nsem abrir portas"| Dashboard
```

`android/` e `desktop/` nunca se enxergam diretamente — cada um implementa sua própria FFI/binding para consumir `lib/p2p/`. Toda comunicação entre dispositivos é P2P direta (LAN via mDNS) ou via o relay (`dashboard/`, construído sobre `lib/relay/`) quando fora da mesma rede.

## Documentação

- Como contribuir: [CONTRIBUTING.md](CONTRIBUTING.md)
- Política de privacidade: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- Licença: [LICENSE](LICENSE) (MPL-2.0, para todo o monorepo)
- TODO de cada plataforma: `android/TODO.md`, `desktop/TODO.md`, `lib/p2p/TODO.md`
- Assets usados nos READMEs (GitHub): [`docs/github/`](docs/github/)
- Futuro site de documentação: [`docs/web/`](docs/web/) (ainda vazio)
