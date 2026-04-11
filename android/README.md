# Acerola

Acerola é um leitor de mangá para Android focado em coleções locais. Você aponta para uma pasta no seu dispositivo, o app encontra os arquivos e monta sua biblioteca automaticamente.

---

## Galeria

Aqui estão alguns exemplos de como o Acerola funciona:

<table>
  <tr>
    <td align="center" width="33%">
      <strong>Home</strong><br>
      <img src="docs/home-screen.png" width="120">
    </td>
    <td align="center" width="33%">
      <strong>Configuração Manga</strong><br>
      <img src="docs/config-manga-screen.png" width="120">
    </td>
    <td align="center" width="33%">
      <strong>Configuração Geral</strong><br>
      <img src="docs/config-screen.png" width="120">
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <strong>Histórico</strong><br>
      <img src="docs/history-screen.png" width="120">
    </td>
    <td align="center" width="33%">
      <strong>Modos de leitura</strong><br>
      <img src="docs/reader-screen-modes.png" width="120">
    </td>
    <td align="center" width="33%">
      <strong>Webtoon</strong><br>
      <img src="docs/webtom-reader-mode.png" width="120">
    </td>
  </tr>
</table>

---

```mermaid
flowchart LR
    Pasta[Sua pasta\nno dispositivo] --> Scan[Acerola escaneia]
    Scan --> Biblioteca[Biblioteca organizada]
    Biblioteca --> Metadados[Busca capa e\ninformações online]
    Biblioteca --> Leitura[Você lê]
```

---

## Funcionalidades

- **Biblioteca**: Escaneia pastas do dispositivo, detecta novos arquivos automaticamente, exibe em grade/lista, permite busca e organização por categorias.
- **Metadados**: Busca capa, sinopse, autor e gênero automaticamente (MangaDex, AniList, ComicInfo). Permite trocar fontes e editar manualmente.
- **Leitura**: Abre `.cbz` e `.cbr` diretamente. Converte `.pdf` para `.cbz`. Possui paginação configurável e salva o progresso automaticamente.
- **Histórico**: Mostra mangás lidos recentemente.
- **Temas**: Várias opções (Catppuccin, Dracula, Alucard, Nord).

---

## Como usar

```mermaid
flowchart TD
    A[Abrir o app] --> B[Conceder permissão de armazenamento]
    B --> C[Escolher a pasta da sua coleção]
    C --> D[Aguardar o escaneamento]
    D --> E[Biblioteca pronta]
    E --> F{O que fazer?}
    F --> G[Sincronizar metadados\npara buscar capas e infos]
    F --> H[Tocar num mangá\npara ver os capítulos]
    H --> I[Tocar num capítulo\npara ler]
```

1. Na primeira abertura, conceda permissão de acesso ao armazenamento.
2. Configure a pasta onde seus mangás estão.
3. O app escaneia e monta a biblioteca.
4. Sincronize os metadados para o app buscar as informações online.
5. Leia.

---

## Formatos suportados

| Formato | Descrição |
|---------|-----------|
| `.cbz` | Comic Book ZIP — arquivo zip com imagens dentro |
| `.cbr` | Comic Book RAR — arquivo rar com imagens dentro |
| `.pdf` | Convertido automaticamente para `.cbz` na primeira leitura |
