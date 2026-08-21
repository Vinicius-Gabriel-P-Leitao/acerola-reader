# Acerola Android — TODO de Features

---

## 🏠 Home

- [x] **Exibir biblioteca em grade (grid)** - Cards com capa gerada pelo `CoverSaver` (salva como `cover.jpg` na pasta do quadrinho via SAF). O Coil carrega direto do URI local.
- [x] **Exibir biblioteca em lista (list)** - Mesma fonte de dados, layout alternativo. Alternância persistida via `HomeLayoutType` no DataStore.
- [x] **Buscar quadrinhos por título** - Filtro reativo em memória sobre o Flow do `ObserveLibraryUseCase`. Compara o título remoto (se existir) ou o nome da pasta.
- [x] **Ordenar biblioteca** - `HomeFilterSheet` permite ordenar por: título (A-Z / Z-A), quantidade de capítulos ou data de última atualização. Configuração salva em estado local (sem DataStore).
- [x] **Filtrar biblioteca** - Filtros rápidos: só bookmarks (por categoria), por fonte de metadados (`MetadataSource`: MangaDex / AniList / ComicInfo) ou quadrinhos sem metadados. Aplicados em memória sobre o Flow.
- [x] **Continuar lendo direto da home** - Botão de play no card aparece quando existe `ReadingHistoryDto` para o quadrinho. Abre o `ReaderActivity` passando `CHAPTER_ID` e `INITIAL_PAGE` como Extras.
- [x] **Menu de ações por quadrinho (long press / botão)** - `ComicActionsSheet` abre via `selectedMangaForActions`. Contém: favoritar com categoria, ocultar/mostrar e deletar (ambos com dialog de confirmação).
- [x] **Favoritar quadrinho com categoria** - `ManageCategoriesUseCase` associa um `categoryId` ao quadrinho no banco. A `ComicCategorySheet` lista as categorias disponíveis com RadioButton e cor.
- [x] **Ocultar / mostrar quadrinho** - `HideComicUseCase` chama `gateway.hideManga(comicId)`, que atualiza o campo `hidden` na entidade `ComicDirectory`. O quadrinho some da listagem principal mas o registro permanece.
- [x] **Deletar quadrinho** - `DeleteComicUseCase` chama `gateway.deleteManga(comicId)`. Remove o registro do Room. Arquivos físicos no dispositivo **não** são apagados.

---

## 📖 Tela do Quadrinho

- [x] **Exibir capa, título, autor e status** - Header (`Header.kt`) exibe: capa via URI local do `ComicDirectory.cover`, título do `ComicRemoteInfo.title` ou nome da pasta como fallback, autor de `ComicRemoteInfo.authors.name`, e status como `StatusBadge` lendo `ComicStatus.fromRawValue(remoteInfo.status)`.
- [x] **Exibir badge da fonte de metadados** - `SourceBadge` lê `remoteInfo.syncSource` (`MetadataSource`). Cor diferente para MangaDex (tertiaryContainer), AniList (primaryContainer) e ComicInfo (secondaryContainer).
- [x] **Exibir gêneros / tags como badges** - `GenreBadge` itera sobre `remoteInfo.genre` (lista de `Genre` vinda do banco, populada durante sync).
- [x] **Exibir sinopse expansível** - `Text` com `maxLines = 3` por padrão. Clique no `Surface` que envolve alterna `isExpanded`, expandindo para `Int.MAX_VALUE` linhas com `animateContentSize()`.
- [x] **Iniciar / Continuar / Reler** - Botão principal lê o `ReadingHistoryDto` do `ObserveComicHistoryUseCase`. Se `history == null` → "Começar", se `history.isCompleted == true` → "Reler", caso contrário → "Continuar". Abre o `ReaderActivity`.
- [x] **Listar capítulos com status de leitura** - `ObserveCombinedChaptersUseCase` junta `ChapterArchive` com `ChapterRead` do banco. Cada `ChapterItem` exibe o número do capítulo e um ícone de check se `isRead == true`.
- [x] **Marcar capítulo como lido / não lido** - `ComicChapterAction.ToggleReadStatus` dispara `TrackReadingProgressUseCase.toggleReadStatus()`. Se já lido → `historyRepository.unmarkChapterAsRead()`; se não lido → `markChapterAsRead()`.
- [x] **Ordenar capítulos** - `ChapterSortSheet` permite ordenar por número do capítulo ou por data de atualização, em asc/desc. Usa `SortNormalizer` para comparação decimal correta (ex: `0.01` antes de `0.10`).
- [x] **Agrupar capítulos por volume** - `ObserveVolumeChaptersUseCase` retorna capítulos agrupados por `VolumeArchive`. Header de volume (`VolumeHeader`) mostra nome e capa do volume.
- [x] **Trocar estilo de exibição de capítulos/volumes** - `VolumeStylePreference` / `ComicAction.UpdateVolumeView` alterna entre `VolumeViewType.VOLUME` (agrupado) e `VolumeViewType.LIST` (plano). Persiste via `UpdateComicSettingsUseCase`.
- [x] **Configurar paginação da lista de capítulos** - `PaginationPreference` / `ComicAction.UpdatePageSize` define `ChapterPageSizeType` (quantos capítulos por página). Salvo no banco via `UpdateComicSettingsUseCase`.
- [x] **Atribuir categoria ao quadrinho** - `ComicAction.UpdateCategory` chama `ManageCategoriesUseCase` para vincular/desvincular a `categoryId` do quadrinho.
- [x] **Ativar/desativar sync externo por quadrinho** - `ComicExternalSyncToggle` / `ComicAction.ToggleExternalSync` atualiza o campo `externalSyncEnabled` via `UpdateComicSettingsUseCase → gateway.updateMangaSettings()`.
- [x] **Sincronizar capítulos locais (rescaneamento de arquivos)** - `ComicSyncAction.SyncChaptersLocal` → `ChapterArchiveViewModel.syncChaptersByMangaDirectory()` → `RescanComicChaptersUseCase`. Rescana a pasta do quadrinho no SAF e atualiza os `ChapterArchive` no Room.
- [x] **Reescanear quadrinho completo** - `ComicSyncAction.RescanComic` → `RescanComicUseCase`. Refaz toda a leitura do diretório: detecta template de nome (`TemplateMatcher`), atualiza `ComicDirectory`, re-indexa capítulos.
- [x] **Sincronizar metadados pelo MangaDex** - `ComicSyncAction.SyncMangadexInfo` → `SyncComicMetadataUseCase` com `MetadataSource.MANGADEX`. Busca título, sinopse, autor, status e gêneros via `MangadexMangaInfoSource` (Retrofit). Salva cover via `CoverSaver` e banner via `BannerSaver`. Exporta `ComicInfo.xml` via `MetadataExporter` se habilitado.
- [x] **Sincronizar metadados pelo AniList** - `ComicSyncAction.SyncAnilistInfo` → `SyncComicMetadataUseCase` com `MetadataSource.ANILIST`. Busca via `AnilistMangaInfoSource` (Apollo GraphQL). Mesma pipeline de persistência.
- [x] **Sincronizar metadados pelo ComicInfo.xml** - `ComicSyncAction.SyncComicInfo` → `SyncComicMetadataUseCase` com `MetadataSource.COMIC_INFO`. `ComicInfoParser` lê o XML local e popula o `ComicRemoteInfo` no banco.
- [x] **Sincronizar capítulos pelo ComicInfo.xml** - `ComicSyncAction.SyncComicInfoChapters` → `ChapterMetadataViewModel.syncChaptersByComicInfo()` → `ComicInfoChapterEngine`. Lê entradas de capítulos do XML e cria/atualiza `ChapterMetadata` no banco.
- [x] **Extrair primeira página de capítulo como capa do quadrinho** - `ComicSyncAction.ExtractFirstPageAsCover` → `CoverFromChapterUseCase` → `CoverExtractor.extractFirstPageAsCover()`. Abre o primeiro `ChapterArchive` da obra (ordenado Vol ASC → Ch ASC), decodifica a página 0 via `ChapterSourceFactory` (CBZ ou CBR), comprime como JPEG 90% e salva como `cover.jpg` na pasta raiz do quadrinho via SAF. Atualiza o campo `ComicDirectory.cover`.
- [x] **Extrair capa do volume a partir do primeiro capítulo do volume** - `ComicSyncAction.ExtractVolumeCovers` → `ExtractAllVolumeCoversUseCase`. Itera todos os `VolumeArchive` do quadrinho. Para cada volume: `CoverExtractor.extractVolumeCover()` abre o primeiro capítulo daquele volume, extrai a página 0 e salva como `cover.jpg` dentro da pasta do volume. Atualiza `VolumeArchive.cover`.

---

## 📕 Leitor

- [x] **Ler arquivos .cbz** - `ChapterSourceFactory` detecta extensão e cria `CbzPageResolver`. O `ZipFile` é aberto na pasta do capítulo via SAF, páginas são listadas e abertas como `InputStream` sob demanda.
- [x] **Ler arquivos .cbr** - `ChapterSourceFactory` cria `CbrPageResolver`. Usa a lib `junrar` para descompressão de RAR. Páginas entregues como `InputStream` sob demanda.
- [x] **Converter .pdf para .cbz antes de ler** - `ArchiveValidator` detecta `.pdf` ao abrir um capítulo. `PdfToCbzConverter` usa `PdfRenderer` do Android para renderizar cada página como Bitmap (escala 2×, fundo branco), comprime como JPEG 90% e empacota num `ZipOutputStream`. O `.cbz` resultante é salvo na mesma pasta do PDF via SAF.
- [x] **Pré-carregar páginas adjacentes (prefetch)** - `ReaderProcessor.prefetchWindow()` pré-decodifica 2 páginas antes e 2 depois da página atual em `Dispatchers.IO`, controlado por `Semaphore(1)`. Resultados armazenados no `BitmapCacheHandler` (LRU).
- [x] **Modo de leitura horizontal paginado** - `HorizontalPagedReader` com `HorizontalPager`. Clique na metade esquerda/direita da tela avança/volta página via `ReaderAction.ChangePage`.
- [x] **Modo de leitura vertical paginado** - `VerticalPagedReader` com `VerticalPager`. Clique na metade superior/inferior avança/volta página.
- [x] **Modo Webtoon (scroll vertical contínuo)** - `WebtoonReader` com `LazyColumn`. Cada página é um item; `ReaderAction.PageVisible` atualiza a página atual conforme o scroll.
- [x] **Zoom nas páginas** - `ZoomablePageImage` implementa pinch-to-zoom e pan com `TransformableState`. Escala e offset são animados.
- [x] **Alternar modo de leitura dentro do leitor** - `SettingsSheet` no leitor permite trocar `ReadingMode` (HORIZONTAL / VERTICAL / WEBTOON) via `ReaderAction.UpdateReadingMode`. Persiste no DataStore via `AppPreferences`.
- [x] **Mostrar/ocultar controles do leitor** - Toque no centro da tela dispara `ReaderAction.ToggleUi`, que alterna visibilidade da `TopBar` e `BottomControls` com animação.
- [x] **Salvar progresso de leitura automaticamente** - `ReaderAction.CurrentPageChanged` dispara `TrackReadingProgressUseCase.saveProgress()` com `ReadingHistoryDto` (comicId, chapterId, lastPage). Persiste na tabela `reading_history`.
- [x] **Navegar para próximo/capítulo anterior** - `ReaderAction.LoadNextChapter` / `LoadPreviousChapter` carregam o capítulo adjacente via `ReaderUseCase.openChapter()`. O `ReaderProcessor` fecha o `PageSource` atual e abre o novo.

---

## 🧠 Metadados

- [x] **Sincronizar metadados de toda a biblioteca pelo MangaDex** - `ConfigAction.SyncMangadexMetadata` → `WorkManagerLibrarySyncScheduler` agenda o `MetadataSyncWorker` com `SOURCE_MANGADEX`. Worker roda como `ForegroundService` com notificação de progresso. Usa `MangadexSyncUseCase` que itera todos os `ComicDirectory` com `externalSyncEnabled = true`.
- [x] **Sincronizar metadados de toda a biblioteca pelo AniList** - `ConfigAction.SyncAnilistMetadata` → `MetadataSyncWorker` com `SOURCE_ANILIST`. Usa `AnilistSyncUseCase`. Progresso notificado via `NotificationHelper.updateProgress()`.
- [x] **Salvar capa na pasta do quadrinho** - `CoverSaver.processCover()` deleta capas existentes (`MediaFile.isCover()`), salva os bytes recebidos como `cover.jpg` via `FileStorageHandler`, atualiza `ComicDirectory.cover` e insere/atualiza a entidade `Cover` no banco (tabela `cover`).
- [x] **Salvar banner na pasta do quadrinho** - `BannerSaver.processBanner()` mesma lógica do `CoverSaver`, para o arquivo `banner.jpg`. Atualiza `ComicDirectory.banner` e tabela `banner`.
- [x] **Exportar metadados como ComicInfo.xml** - `MetadataExporter.exportFull()` só executa se `MetadataPreference.generateComicInfoFlow` retornar `true`. `ComicInfoParser.serialize()` gera o XML. Escrito via SAF na pasta do quadrinho. Atualiza `ComicRemoteInfo.hasComicInfo = true`.
- [x] **Ler metadados de ComicInfo.xml** - `ComicInfoParser` usa um parser XML para popular `ComicMetadataDto` com título, sinopse, autor, status, gêneros. Usado tanto no sync individual quanto no worker global.

---

## ⚙️ Configurações

- [x] **Selecionar pasta raiz da coleção** - `ConfigAction.SelectFolder` recebe a `Uri` do SAF picker. `WorkManagerLibrarySyncScheduler` agenda o `LibrarySyncWorker` com `SYNC_TYPE_INCREMENTAL` e a `baseUri`. Persiste a URI no DataStore.
- [x] **Scan incremental da biblioteca** - `ConfigAction.QuickSyncLibrary` → `LibrarySyncWorker` com `SYNC_TYPE_INCREMENTAL`. `SyncLibraryUseCase` compara o estado atual do SAF com os registros do Room, inserindo/atualizando/removendo apenas o que mudou.
- [x] **Scan profundo (rebuild) da biblioteca** - `ConfigAction.DeepScanLibrary` → `LibrarySyncWorker` com `SYNC_TYPE_REBUILD`. Apaga todos os registros e re-escaneia do zero via `DirectoryScanner.buildLibrary()`.
- [x] **Selecionar idioma global de metadados** - `LanguageSettings` / `ConfigAction.UpdateMetadataLanguage` salva o idioma no DataStore. Lido pelos adapters de MangaDex e AniList na hora do sync.
- [x] **Ativar/desativar geração de ComicInfo.xml** - `MetadataExportSettings` / `ConfigAction.UpdateGenerateComicInfo` salva o booleano em `MetadataPreference`. Verificado por `MetadataExporter.exportFull()` antes de qualquer escrita.
- [x] **Criar categoria** - `ConfigAction.CreateCategory(name, color)` → `ManageCategoriesUseCase` insere uma nova entidade `Category` com nome e cor ARGB no banco.
- [x] **Deletar categoria** - `ConfigAction.DeleteCategory(id)` → `ManageCategoriesUseCase` remove a categoria. Quadrinhos vinculados têm `categoryId` setado para `null` por cascade.
- [x] **Selecionar tema do app** - `ThemeSettings` / `ConfigAction.UpdateTheme` salva `AppTheme` no DataStore. Temas disponíveis: `CATPPUCCIN`, `DRACULA`, `ALUCARD`, `NORD`. Relido na composição raiz para trocar o `MaterialTheme`.
- [x] **Navegar para configuração de templates** - `ConfigAction.NavigateToTemplateConfig` dispara navegação para a `FilePatternScreen`.

---

## 🔣 Templates de Nomenclatura

- [x] **Criar template** - `FilePatternAction.AddTemplate(label, pattern, type)` → `AddTemplateUseCase`. Insere um `ArchiveTemplate` no banco com o padrão e `SortType` (CHAPTER ou VOLUME). O padrão usa macros: `{chapter}`, `{decimal}`, `*` (curinga).
- [x] **Editar template** - `FilePatternAction.EditTemplate(id, label, pattern, type)` → `UpdateTemplateUseCase`. Atualiza o registro existente.
- [x] **Deletar template** - `FilePatternAction.DeleteTemplate(id)` → `RemoveTemplateUseCase`. Remove do banco.
- [x] **Listar templates** - `ObserveTemplatesUseCase` retorna um `Flow<List<ArchiveTemplate>>`. `FilePatternScreen` exibe cada template com label, pattern e tipo.
- [x] **Detecção automática de template no scan** - `DirectoryScanner` usa `TemplateMatcher.detect()` para identificar qual template cadastrado bate com o nome do primeiro arquivo encontrado na pasta. O `archiveTemplateFk` é salvo no `ComicDirectory`.

---

## 🕐 Histórico

- [x] **Exibir leituras recentes** - `ObserveHistoryUseCase` retorna `Flow<List<ReadingHistoryDto>>` da tabela `reading_history`, ordenada por `lastRead` desc. `HistoryScreen` exibe `HistoryHeroCard` com capa, título e última página lida.
- [x] **Continuar pelo histórico** - Toque no `HistoryHeroCard` abre o `ReaderActivity` com `CHAPTER_ID` e `INITIAL_PAGE` do `ReadingHistoryDto`.

---

## 🎓 Onboarding

- [x] **Tutorial de primeira abertura** - `TutorialScreen` exibe páginas (`TutorialPage`) com instruções. Na última etapa abre o SAF picker para seleção de pasta, dispara o scan incremental e marca o tutorial como concluído no DataStore.

---

## 🚧 Pendente

- [x] **Marcar quadrinho / capítulo como concluído manualmente** - Não existe ação, use case ou campo `completed` no banco. Atualmente o estado "concluído" só é inferido quando `lastPage >= pageCount` na leitura.
- [x] **Seleção múltipla de quadrinhos e capítulos (multi-select)** - Nenhuma tela tem `selectedItems: Set<Long>` no UiState. Long press abre o 
  `ComicActionsSheet` para um único item.
- [x] **Ações em lote sobre seleção múltipla** - Depende do multi-select acima. Sem estado de seleção, não há como aplicar delete/hide/bookmark em 
  lote.
- [ ] **Permitir que o app Android possa ser fechado e rodar em segundo plano o sync** *(Alta)* - Fazer um worker que permite o app mobile fazer todos os syncs em segundo plano sem interferir no usuário.
- [x] **Sincronização individual sem ser global** *(Média)* - Novo protocolo Rust `acerola/sync-comic/1` (`native/rust/src/protocol/files/`) reaproveita a troca de manifesto/want-list/transferência do `sync-files`, filtrada a um único `comic_name` via uma fase 0 (`ComicSyncScope`) — cobre push e pull com o mesmo código, já que a troca é simétrica. `P2PNode.sync_comic()` grava o quadrinho alvo num registro pendente antes de conectar (`connect()` não carrega payload). Navegação da biblioteca remota sem sincronizar via novo protocolo `acerola/browse-library/1` (`native/rust/src/protocol/library_browse/`), que reaproveita `get_file_manifest()` agrupado por quadrinho. Entry points: `ComicActionsSheet` (Home) e nova seção em `ConfigSection` (Comic Detail) para push via `PeerPickerSheet`; novo item "Ver biblioteca remota" no `SyncScreen` para pull via `RemoteLibrarySheet`.
