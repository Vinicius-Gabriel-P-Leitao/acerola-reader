# TODO: Auditoria de Estrutura e Naming

## 🏷️ Nomenclatura e Semântica

- [ ] **Evitar Conflito com Compose (Typo.kt)**
    - *Arquivo:* `app/src/main/java/br/acerola/manga/ui/common/theme/Typo.kt`
    - *Sugestão:* `AppTypography.kt` ou `AppTypo.kt`.
    - *Motivo:* Evitar ambiguidade com `androidx.compose.material3.Typography` em imports e buscas.

- [ ] **Renomear DTO Remoto (API)**
    - *Arquivo:* `data/remote/mangadex/dto/manga/MangaMangadexDto.kt`
    - *Sugestão:* `MangadexMangaResponse.kt`.
    - *Motivo:* Diferenciar claramente o que é "Resposta Bruta da API" dos seus DTOs de comunicação interna (`domain/dto`).

## 🏗️ Organização de Camadas (Inversão de Dependência)

- [ ] **Mover Implementações Técnicas para Data**
    - *Origem:* `domain/service/api/mangadex/` (Classes: `MangadexFetch...Service`)
    - *Origem:* `domain/service/library/` (Classes: `...Operation`, `...Service`)
    - *Destino:* `data/remote/mangadex/` e `data/local/` respectivamente.
    - *Justificativa:* Manter o `domain` contendo apenas as **Interfaces** (Contratos) e os **DTOs de Comunicação**. As classes que tocam em APIs externas ou Sistema de Arquivos (Retrofit/File IO) são implementações de `data`.

- [ ] **Centralizar Lógica de Persistência**
    - *Arquivos:* `FolderMangaOperation.kt`, `FileChapterOperation.kt`
    - *Sugestão:* Mover para a camada de `data/local` como `LocalDataSource` ou implementações do Repositório.
    - *Motivo:* Libera o `domain/service` para focar apenas na orquestração dos DTOs que serão entregues à UI.

## 🧹 Limpeza e Padronização

- [ ] **Sincronização de Mappers**
    - *Pasta:* `data/local/mapper/`
    - *Ação:* Validar se os mappers estão convertendo corretamente entre Entities do Room, DTOs Remotos e os DTOs de Domínio que a UI consome.