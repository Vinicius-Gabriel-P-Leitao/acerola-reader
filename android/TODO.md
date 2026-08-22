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
- [x] **`browse-library` v2: sinal de versão de capa na resposta** *(Média)* - `ChapterArchiveDao.getLibrarySummary()` ganhou `cd.last_modified AS cover_version` no SELECT; `LibrarySummaryRow.coverVersion`/`FfiComicSummaryEntry.cover_version` (novo campo no `Record` UniFFI, bindings Kotlin regenerados) carregam até o wire (`protocol/library_browse/model.rs::ComicSummaryEntry`) e o evento `browse:library:result` (`coverVersion` no payload). `getLibrarySummary()` continua só SQL/Room, sem tocar SAF/`DocumentFile`.
- [x] **Novo protocolo `acerola/browse-cover/1`** *(Baixa)* - Novo módulo `protocol/cover_browse/` (`model.rs`, `exchange.rs`, `mod.rs`): outbound manda `{ comic_name, known_version }`, inbound responde `not_modified`/`unavailable`/`changed{cover_hash}` — bytes de verdade vão via blobs (`ChapterTransfer::publish`/`fetch_reader`, reaproveitado da Fase 1), não mais chunking manual. Novo trait FFI `CoverBrowseProvider` (`callbacks.rs`) + `CoverBrowseProviderImpl.kt` (lê `cover.jpg` via SAF, grava capas remotas em `context.cacheDir/remote_covers/`). `P2PNode::browse_cover()` (fire-and-forget, mesmo padrão de `sync_comic`) → `browse:cover:result`/`error`. UI: `SyncViewModel` dispara a busca em paralelo pra cada item de `browse:library:result`, cacheia em memória por `(peerId, comicName)`, `RemoteLibrarySheet` mostra a thumbnail via Coil quando disponível.
- [x] **Migrar `sync-files`/`sync-comic` pra usar blobs do `acerola-p2p`** *(Baixa)* - `acerola-p2p` subiu pra `0.0.6` com a feature `iroh-blobs-adapter` habilitada. Fases 1-2 do protocolo (troca de manifesto/want-list) continuam iguais — só a fase 3 (transferência) foi trocada: em vez de `stream_chapter_out`/`receive_chapter_bytes` (chunking manual de 64KB), cada capítulo é publicado no blob store local (`ChapterTransfer::publish`, novo `protocol/files/transfer.rs`) e o `FileHeader` passa a carregar um `blob_hash` (BLAKE3, hex) além do `checksum` (SHA-256, inalterado — continua indo pro `begin_chapter_write`/verificação Kotlin como antes). Quem recebe busca via `ChapterTransfer::fetch_reader` (`P2pBlobStore::fetch` + `get`), que já verifica integridade automaticamente. `BlobContext` (`protocol/blob_context.rs`) resolve o problema de acesso ao `node.blobs()`/`node.known_peers()` de dentro dos handlers, que são registrados no builder antes do node existir (guarda um `Weak<AcerolaP2p>` preenchido só depois do `.build()`).
- [x] **Sincronização individual sem ser global** *(Média)* - Novo protocolo Rust `acerola/sync-comic/1` (`native/rust/src/protocol/files/`) reaproveita a troca de manifesto/want-list/transferência do `sync-files`, filtrada a um único `comic_name` via uma fase 0 (`ComicSyncScope`) — cobre push e pull com o mesmo código, já que a troca é simétrica. `P2PNode.sync_comic()` grava o quadrinho alvo num registro pendente antes de conectar (`connect()` não carrega payload). Navegação da biblioteca remota sem sincronizar via novo protocolo `acerola/browse-library/1` (`native/rust/src/protocol/library_browse/`), que reaproveita `get_file_manifest()` agrupado por quadrinho. Entry points: `ComicActionsSheet` (Home) e nova seção em `ConfigSection` (Comic Detail) para push via `PeerPickerSheet`; novo item "Ver biblioteca remota" no `SyncScreen` para pull via `RemoteLibrarySheet`.
- [ ] **[Crítica] `FsStore` do `iroh-blobs` trava a thread principal e vira ANR (mitigado, não corrigido)** - Descoberto em 22/08/2026, depois de habilitar `.blobs(IrohBlobsConfig::fs(blobs_dir))` (item acima): `P2PNode::new()` faz `runtime.block_on(...)` internamente, e esse `.build()` trava indefinidamente ao tentar abrir o blob store em disco — como `P2PNode::new()` é chamado de forma síncrona no `init{}` do `P2pService` (resolvido pelo Hilt na primeira tela, na THREAD PRINCIPAL), isso trava a UI inteira e vira ANR depois de 5s ("Input dispatching timed out... Waited 5000ms"). Confirmado isolado, fora do app: um teste mínimo em `acerola-p2p` (`core/blobs/iroh/mod.rs::tests::fs_store_load_does_not_hang`, não commitado na lib) chamando só `IrohBlobStore::new` com config `Fs` num diretório limpo trava e estoura 15s sozinho — bug real na integração `acerola-p2p`/`iroh-blobs` com store em disco (a lib nunca tinha testado esse caminho, só `IrohBlobsConfig::mem()`). **Mitigação aplicada nesta rodada:** trocado `.blobs(IrohBlobsConfig::fs(blobs_dir))` por `.blobs(IrohBlobsConfig::mem())` em `native/rust/src/api.rs` (parâmetro `blobs_dir` mantido na assinatura FFI, só não é mais usado — `let _ = &blobs_dir;`) — destrava o app, mas blobs deixam de persistir entre reinícios do app. Testado só via `cargo test`/build no host, **não testado ao vivo num device/emulador Android** (sem um disponível nesta sessão) — precisa confirmar que o ANR realmente sumiu antes de considerar resolvido de verdade. **Pendente:** achar a causa raiz do hang no `FsStore::load_with_opts` (`iroh-blobs`) e voltar pra `.fs(...)` depois.
- [ ] **[Alta] Busca de biblioteca remota (`browse-library`) falha com timeout mesmo depois da correção SQL** - Reportado em 22/08/2026, testado ao vivo entre um Android real e um Desktop real: `stream error: timeout waiting for frame` nos dois lados ao tentar navegar a biblioteca remota. Log do Desktop (lado outbound) mostra a conexão abrindo normalmente e depois travando 30s até estourar o timeout de frame — nenhuma resposta chega do lado inbound. Não investigado a fundo ainda; suspeita principal é que o teste foi feito enquanto um dos lados ainda não tinha o node totalmente inicializado por causa do bug do `FsStore` acima (ver esse item) — precisa reproduzir de novo com os dois lados já rodando a mitigação `.mem()` antes de investigar mais.
- [ ] **[Alta] Sync de "Omoide Emanon" do Android pro Desktop não trouxe todos os capítulos** - Reportado em 22/08/2026: usuário disparou `sync_comic` do Android pro Desktop, precisou mandar de novo manualmente (a primeira tentativa não completou), e da segunda vez só 1 capítulo foi de fato transferido. No lado que recebeu (Desktop), o log mostra 10 headers de capítulo chegando pelo fio mas só 8 persistindo na biblioteca, com 5x `UNIQUE constraint violated` no meio — ver o item correspondente no `TODO.md` do Desktop pra mais detalhes (o bug em si parece estar do lado de `persist_received_chapter`, receptor). Do lado Android (remetente), vale investigar por que a primeira tentativa de `sync_comic` não completou e por que o reenvio só mandou 1 capítulo em vez do conjunto inteiro que faltava — não investigado ainda.
