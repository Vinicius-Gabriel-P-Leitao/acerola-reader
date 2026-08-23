# WebDriverIO Native E2E

Esta suite testa o binário Tauri nativo via `tauri-driver`. Ela é separada dos specs Playwright em `tests/e2e/`, que rodam contra o WebView/Vite.

## Build

Antes de rodar, gere o binário de release:

```sh
cargo tauri build
```

O config espera:

- Windows: `src-tauri/target/release/acerola.exe`
- Linux: `src-tauri/target/release/acerola`

## Windows

Instale o `tauri-driver`:

```sh
cargo install tauri-driver --locked
```

Instale o Edge Driver compatível com o WebView2/Edge 148.x:

```sh
cargo install --git https://github.com/chippers/msedgedriver-tool
msedgedriver-tool
```

Garanta que `msedgedriver.exe` esteja no `PATH` ou coloque o binário em:

```text
.bin/windows/msedgedriver.exe
```

## Linux CI

Instale `tauri-driver`, `webkit2gtk-driver` e `xvfb`:

```sh
cargo install tauri-driver --locked
sudo apt-get update
sudo apt-get install -y webkit2gtk-driver xvfb
```

## Rodar

O projeto usa npm, detectado por `package-lock.json`:

```sh
npm run test:wdio
```

No Linux CI, rode esse comando dentro de `xvfb-run -a`.

## Fixtures

O teste de protocolo asset espera um fixture em:

```text
tests/wdio/fixtures/reader.cbz
```

Esse arquivo deve existir antes da execução. Se o reader depender de estado de biblioteca persistido, prepare também o banco/configuração local do app antes de rodar a suite.

## Limitações

- macOS não é suportado pelo `tauri-driver` para desktop porque não há driver WebDriver para WKWebView.
- WebDriver não controla dialogs nativos de seleção de pasta; o teste apenas valida que o clique no botão não trava o app.
- O teste de minimizar depende do comportamento do gerenciador de janelas. Em Linux, rode sob Xvfb.
