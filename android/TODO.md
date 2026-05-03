# Acerola — Leitor de Quadrinhoss

Acerola é um aplicativo Android focado em entusiastas de quadrinhos que preferem gerenciar sua própria biblioteca local. Ele oferece uma maneira
fluida, bonita e eficiente de escanear, organizar e ler arquivos (`.cbz`, `.cbr`), enriquecendo a coleção com metadados de fontes online populares.

## 🚀 Funcionalidades Principais

* **Gerenciamento Local Automático:** Escaneia e organiza pastas e arquivos de quadrinhos diretamente do armazenamento do dispositivo.
* **Sincronização de Metadados:** Busca dados ricos (capas, sinopses, autores) em provedores como MangaDex e AniList.
* **Leitor Nativo Integrado:** Experiência de leitura fluida, customizável e com suporte nativo a arquivos compactados.
* **Interface Adaptativa:** Design moderno (Material 3), personalizável com diversos temas e responsivo (suporte a modo paisagem).

## 💻 Stack de Tecnologia

* **Linguagem:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Persistência:** Room Database e DataStore
* **Assincronicidade:** Kotlin Coroutines & Flow
* **Imagens:** Coil
* **Injeção de Dependências:** Manual (via ViewModelFactories)

---

## 🚧 Tarefas Pendentes (TODO)

### Criar nova estrutura para volumes

#### 1. Core: Motor de Normalização e Inteligência (`SortNormalizer`)

- [x] **Criação do `SortNormalizer.kt`**:
    - [x] Extrair a lógica do `ChapterArchiveEngine` e `NormalizeChapterSort.kt` para um serviço centralizado.
    - [x] Implementar parsing unificado que retorna um `SortResult` (Inteiro, Decimal, IsSpecial, Tipo: Volume/Chapter).
    - [x] **Regex Avançado**: Adicionar suporte a padrões de Volume (`Vol.`, `V-`, `Volume`, `Edição`) mantendo compatibilidade com os padrões de
      capítulos já existentes.
- [x] **Lógica de Propagação (`is_special`)**:
    - [x] Criar função de resolução: `isChapterSpecial = (parentVolume.is_special || currentFile.is_special)`.

#### 2. Persistência: Room, Migrations e Unicidade

- [x] **Nova Entidade `volume_archive`**:
    - [x] Campos: `id`, `comic_directory_fk` (FK CASCADE), `name`, `path`, `volume_sort`, `is_special`.
    - [x] **Índice Único**: `UNIQUE(comic_directory_fk, volume_sort)` para evitar volumes duplicados.
- [x] **Refatoração da `chapter_archive`**:
    - [x] Adicionar `volume_id_fk: Long?` (Nullable para capítulos na raiz).
    - [x] Adicionar `is_special: Boolean`.
    - [x] **SEGURANÇA DE METADADOS**: **Manter** `UNIQUE(comic_directory_fk, chapter)`. Isso impede que o "Capítulo 1" exista em dois volumes
      diferentes, protegendo o vínculo com a API do MangaDex.
- [x] **Migration 1.0**:
    - [x] Script de criação da tabela de volumes.
    - [x] Script de alteração da tabela de capítulos com valores default.

#### 3. Data Engine: O Novo Fluxo do Scanner

- [x] **Detecção de Subpastas (Recursividade)**:
    - [x] Alterar `scanRecursive` para identificar subpastas que seguem o padrão de Volume.
    - [x] Aplicar o `SortNormalizer` no nome da pasta para gerar o `volume_sort`.
- [x] **Lógica de Vínculo Estrito**:
    - [x] Se o arquivo estiver dentro de um volume: vincular ao `volume_id_fk`.
    - [x] Se o arquivo estiver na raiz: `volume_id_fk = NULL`.
- [x] **Tratamento de Conflitos**:
    - [x] Implementar `try-catch` específico no scanner para o erro de `UniqueConstraint`. Se dois volumes diferentes tentarem registrar o mesmo
      número de capítulo, o sistema deve ignorar o segundo e emitir um log de "Nome de Capítulo Duplicado na Obra".

#### 4. Contrato e Mappers: A "Grande Quebra" (Breaking Changes)

- [x] **Refatoração de DTOs**:
    - [x] `ChapterFileDto`: Incluir `volumeId`, `volumeName` e `isSpecial`.
    - [x] `ChapterArchivePageDto`: Incluir um mapa ou lista de metadados dos volumes presentes naquela página (necessário para a paginação da UI).
- [x] **Revisão Total de Mappers**:
    - [x] Atualizar `ArchivePersistenceMapper.kt` e `ChapterArchiveMapper.kt`.
    - [x] Atualizar os tradutores de UI (`toViewDto`, `toViewPageDto`).

#### 5. DAO & SQL: Ordenação Hierárquica Nativa

- [x] **Refatorar `getChaptersByDirectoryPaged`**:
    - [x] Realizar `LEFT JOIN volume_archive ON volume_id_fk = volume.id`.
- [x] **Implementar Order By Multi-Nível**:
    1. `COALESCE(volume.is_special, 0)` (Especiais/Extras por último ou conforme filtro).
    2. `CAST(volume_sort AS INTEGER)` (Volumes em ordem numérica).
    3. `CAST(SUBSTR... AS INTEGER)` (Parte decimal do volume).
    4. `chapter_archive.is_special` (Capítulos especiais dentro do volume).
    5. `CAST(chapter_sort AS INTEGER)` (Capítulos em ordem numérica).
    6. `CAST(SUBSTR... AS INTEGER)` (Parte decimal do capítulo).

#### 6. UI: Jetpack Compose & Agrupamento Visual

- [x] **ViewModel**:
    - [x] **Remover a ordenação in-memory**: O ViewModel agora confia 100% no SQL para não bagunçar a hierarquia.
    - [x] Adicionar lógica de estado: `val hasVolumes = chapters.any { it.volumeId != null }`.
- [x] **Implementação do `stickyHeader`**:
    - [x] No `LazyColumn`, injetar headers dinamicamente quando o `volumeId` mudar entre um item e outro da lista.
    - [x] Criar `VolumeHeaderComponent` com suporte a indicadores de "Especial" e contagem de capítulos.
- [x] **Fallback de UI**: Se `hasVolumes` for falso (obra flat), ocultar os headers e manter o layout atual para não poluir a tela.

#### 5. Camada de Dados & UI (Compose)

- [x] **DTOs & Mappers**:
    - [x] Atualizar `ChapterDto` e `ChapterFileDto` para conter informações do Volume pai.
    - [x] Atualizar mappers em `ArchivePersistenceMapper.kt`.
- [x] **ViewModel (Agrupamento)**:
    - [x] Criar lógica para detectar se há múltiplos volumes ativos.
    - [x] Adaptar `ComicViewModel` para não desfazer a ordenação do SQL (remover ordenação redundante em Kotlin).
- [x] **Interface (Jetpack Compose)**:
    - [x] Criar `VolumeHeaderComponent`.
    - [x] Implementar `stickyHeader` na `LazyColumn`.
    - [x] **Lógica de Visibilidade**: Ocultar o Header se houver apenas 1 volume ou se os capítulos forem "órfãos" (Root).
- [x] **Transformar formato de volume em cards colapsáveis**: 
  - [x] Quando tiver volumes ao invés de paginar os chapter vai criar cards colapsáveis e validar para ele não buscar todos os chapters por 
    volume se tiver 2 volumes mas cada um com 500 chapter isso vai explodir a memoria, tem que ser feito de uma forma otimizada visualmente e no sql
- [x] **Criar visualização com cover do volume** Essa visualização é simples e dá para extrair a primeira capa do volume como capa da listagem é 
  possível também.
- [x] **Criar nova UserPreference** A nova preferência vai ser para olhar volumes como capas ou uma lista de cards dropaveis (metodo atual), só 
  sera feito um novo e dar o direito de escolha.

### Corrigir problema com zoom e ocultar layout de leitor quando clicar no meio da tela ou pinça ou dois cliques

- [x] **Suposto lugar** Acredito que seja o problema no código de gesture por que funciona somente no método de webtoom, nos outros não funcionam.

### Correções (Bugfixes)

- [x] **Falso-positivo na conclusão de leitura (Android):**
    - O app está marcando quadrinhos como concluídos automaticamente durante o carregamento das páginas.
    - **Causa provável:** A lógica da "regra dos 70%" está sendo disparada erroneamente no momento do pré-carregamento (*preload*) das imagens, e não
      na visualização ativa pelo usuário.

### Validar se os metadados dos chapters estão sendo carregados

- [x] **O combine de 7 valores** Acredito ser problema do combine de 7 valores na ComicViewModel que tá fazendo isso de não carregar os métadados 
  do chapter, problema era de ordem e tratamento com novos dados no cache.

### Aplicar mudanças da lib rust acerola p2p

- [ ] **A lib está a ser feita:** Séra feito um grande refactor no campo de rust para poder montar a FFI atualizada e
  otimizada para poder salvar chaves de PeerId, DeviceInfo entre outros, poder usar o keystore para salvar dados que devem ser criptografias.

### Adicionar um worker para o conversor de pdf

- [ ] **Montar um worker:** Criar um worker para quando um pdf for virar cbz, pode demorar muito, ou se melhor como tenho uma lista de pastas e 
  arquivos na hora de converter, se possível tenta converter 2 ao mesmo tempo, se for viável. 

### Novas Funcionalidades (Features)

- [ ] **Ação de Conclusão Manual:** Implementar botão/opção para o usuário marcar um quadrinho ou capítulo como concluído manualmente.
- [ ] **Seleção Múltipla (Multi-select):** Permitir a seleção de múltiplos capítulos e quadrinhos segurando (*long press*) o card ou botão
  correspondente.

### Modificar tabelas do banco de dados para ter nomes consistentes

- [ ] **Alguns campos que deveriam ser _fk estão com _id:** Será feito refactor e migration, deixa isso para versão do banco 3.

### 🔧 Refatoração de Arquitetura (Tech Debt Pendente)

#### P3 — God Objects

- [x] `ComicDirectoryEngine` → extrair `DirectoryScanner` (service de IO puro) — **feito**
- [x] `ChapterArchiveEngine` (503 linhas) → dividir em:
    - [x] `ArchiveValidator` (interface + `DefaultArchiveValidator`) — valida formato, padrão de nome, duplicata
    - [x] `ChapterIndexer` (interface + `DefaultChapterIndexer`) — mapeia `FastFileMetadata` → `ChapterArchive`
    - [x] `ChapterArchiveEngine` — pipeline: injeta validator + indexer, orquestra o fluxo
- [x] `MangadexChapterMetadataClient` — confirmado como Retrofit interface pura, nenhuma ação necessária

#### P4 — ViewModel SRP (validar antes de executar — pode quebrar contratos)

- [ ] `ComicDirectoryViewModel` → extrair `GetComicDirectoryUseCase`
- [ ] `ComicViewModel` → extrair `GetComicDetailsUseCase` *(alto risco — validar FlowStateFlow antes)*
- [ ] `HomeViewModel` → extrair `GetLibraryUseCase` com parâmetros de filtro explícitos
- [ ] `ReaderViewModel` → extrair `ReaderPrefetchUseCase` (pré-carregamento / paginação)

``#### P6 — OCP

- [ ] `MetadataSyncWorker` → substituir `when` de sources por `Map<String, MetadataProvider>` (interface + Hilt multibinding)
- [ ] `MangadexComicEngine` → criar `sealed class MangadexResponse` com `toDomainError()` por subtipo
- [ ] `AnilistMangaInfoSource` → criar `ApolloExceptionMapper` isolando dependência do Apollo``

---

## ✅ Histórico de Implementações (Changelog)

### 📚 Biblioteca e Gerenciamento Local
- **Motor de Escaneamento e Sync:** Leitura e persistência automática de pastas e arquivos (`.cbz`, `.cbr`), com sincronização contínua de adições, renomeações e exclusões.
- **Sync Contextual e Individual:** Opção de sincronizar apenas um quadrinho específico e botões de atualização que respeitam a fonte de metadados
  ativa.
- **Ações por Quadrinhos:** Adicionar aos favoritos (Bookmark), ocultar ou deletar direto do menu de contexto.
- **Limpeza de Dados:** Ações (com confirmação) para limpar metadados ou capítulos. Um Job em background também limpa dados de quadrinhos cujas pastas
  não existem mais.
- **Informação de Armazenamento:** Exibição precisa do tamanho da biblioteca em MB ou GB.

### 📖 Leitor e Rastreamento (Tracking)
- **Motor de Leitura:** Suporte nativo a `.cbz`/`.cbr` com otimização de clique para avançar/voltar páginas e layout paginado (configurável).
- **Rastreamento de Leitura:** Função "Continuar Lendo" e marcação de capítulos como lidos/não lidos.
- **Gerenciamento de Imagens:** Opção de trocar, salvar e remover capas e banners.
- **UX do Leitor:** BottomSheet opaco e integrado ao tema.

### 📥 Downloads (Integração MangaDex)
- **Busca e Idioma:** Busca robusta (por nome, ID ou URL) com seleção de idioma, respeitando a configuração global do app.
- **Gerenciamento de Fila:** Downloads múltiplos e simultâneos, rodando em background com notificação de progresso e indicativo visual na UI.
- **Empacotamento:** Criação automática de `.cbz` diretamente na pasta da biblioteca, sem problema de extensão dupla (ex: `.cbz.zip`).
- **Pré-requisitos de Download:** Download prioritário de metadados (capa, gêneros) antes do início da fila de capítulos.
- **Paginação de Grandes Quadrinhoss:** Otimização para listar e baixar obras com mais de 300 capítulos em sequência.

### 🧠 Metadados e Fontes (Sources)
- **Fontes Suportadas:** MangaDex, AniList e leitura local via `ComicInfo.xml`.
- **Sincronização em Background:** Sync longo roda silenciosamente e gera notificações com barra de progresso.
- **Identificação Visual:** Badges informam claramente qual provedor gerou os metadados atuais da obra.
- **Resolução de Conflitos:** Lógica para priorizar dados do banco local vs `ComicInfo.xml`. Tratamento de erro 500 do AniList na UI.

### 🎛️ Filtros, Ordenação e Patterns
- **Home Screen:** Ordenação (Asc/Desc) por título, quantidade de capítulos e última atualização. Filtros rápidos (sem DataStore) por bookmark, source ou "sem metadados".
- **Capítulos:** Ordenação por número e update. Correção de lógica em capítulos decimais (ex: `0.01` sendo lido corretamente antes de `0.10`).
- **Patterns de Regex:** Interface para registrar padrões de nomenclatura (`{chapter}`, `{decimal}`, `*`). Dialogs mais descritivos e adaptáveis (BottomSheet).

### 🎨 Design System e UI/UX
- **Adaptação para Modo Paisagem (Landscape):** UX otimizada (BottomBar vira SideBar, BottomSheets viram SideSheets com alinhamento e cores consistentes).
- **Padronização de Configurações (Inspirado em Web):** Telas de configuração (`ConfigScreen`) refatoradas usando o padrão "HeroItem" (Cards arredondados, título em negrito, descrição sutil e ícone em destaque com hover/feedback tátil), similar ao padrão estrutural do Svelte/Tailwind.
- **Temas Persistentes:** Catppuccin (Padrão), Dracula, Alucard (Light) e Nord (Light/Dark). As paletas ("bolinhas" de seleção) se adaptam visualmente ao esquema do sistema.
- **Componentes Refinados:**
    - Barra Superior estilo "Apple Liquid Glass".
    - SearchScreen centralizada e SearchBar com animações corrigidas.
    - Snackbars customizadas (Vermelho para erros, Verde para sucessos).
    - Ajuste de paddings na Home para evitar que o botão flutuante (FAB) cubra os últimos itens.
- **Onboarding:** Tutorial introdutório de permissão de pasta e sync inicial.

---

### ⚙️ Arquitetura, Performance e Refatorações de Código
- **Coleções Imutáveis:** Migração de `MangaUiState` e `ReaderUiState` para `kotlinx-collections-immutable`, evitando recomposições desnecessárias.
- **Otimização de Memória:** Remoção de `Map<Int, Bitmap>` da UI State do Reader, delegando a gestão integral de cache para o Coil.
- **Desacoplamento:** Refatoração de `RemoteInfo` para `Metadata` agnóstico. Criação do `ChapterTemplateDto` para evitar vazamento de dependência do Room na camada de UI.
- **Tratamento de Exceções:** Envelopamento robusto de erros no `ChapterDownloadWorker`, `MetadataSyncWorker` e `FileSystemAccessManager`.
- **Clean Code (Tech Debt Eliminado):**
    - Strings e logs hardcoded movidos para `strings.xml` e padronizados em um Wrapper de log.
    - Correção de falhas nos contratos de UI (ex: `GlobalProgressViewModel`).
- **Testes:** Cobertura aplicada nos motores principais (ex: `MetadataSyncWorker`, `ChapterDownloadWorker`).