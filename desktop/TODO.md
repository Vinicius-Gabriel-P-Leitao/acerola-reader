# Acerola Desktop TODO de Features

---

## Home

- [x] **Exibir biblioteca em grade (grid)** - Cards com capa retornada pelo backend (Rust). O Svelte carrega os assets do disco local.
- [x] **Buscar quadrinhos por título** - Filtro reativo no frontend (Svelte store) comparando com os itens em memória.
- [x] **Ordenar biblioteca** - Menu na UI que permite ordenar os itens do frontend por: título (A-Z / Z-A), contagem ou data de atualização.
- [x] **Filtrar biblioteca** - Filtros aplicados no state manager (MangaDex, AniList, Local, lidos/não lidos).
- [x] **(validar se faz sentido nesse caso) Menu de ações por quadrinho (multi-select / hover menu / botão direito)** - Menu para: favoritar com categoria, ocultar/mostrar e deletar. (Para Desktop o long press não se aplica).
- [x] **Favoritar quadrinho com categoria** - Comando Tauri vincula um `category_id` ao quadrinho no banco de dados SQLite local.
- [x] **Ocultar / mostrar quadrinho** - Comando Tauri atualiza o campo de status do quadrinho. O frontend apenas remove/adiciona visualmente.
- [x] **Deletar quadrinho** - Comando Tauri deleta o registro do banco local, sem afetar o arquivo físico (com dialog de confirmação no Svelte).

---

## Tela do Quadrinho

- [x] **Exibir capa, título, autor e status** - O componente do header exibe as strings baseadas no retorno de metadados do backend em Rust.
- [x] **Exibir badge da fonte de metadados** - Badge indicando a origem dos metadados processados no Rust (MangaDex, AniList, Local).
- [x] **Exibir gêneros / tags como badges** - Chips das tags extraídas e formatadas.
- [x] **Exibir sinopse expansível** - Truncate via CSS/Svelte que permite expansão para leitura do texto completo.
- [x] **Iniciar / Continuar / Reler** - O Svelte consulta `history_get_comic` via Tauri e decide a string e para qual página a rota `/reader` vai navegar.
- [x] **Listar capítulos com status de leitura** - Listagem renderizada com os `readChapters` marcados visualmente.
- [x] **Marcar capítulo como lido / não lido** - Ação explícita na UI (menu do capítulo + seleção múltipla em lote) chamando um evento no backend (Tauri) para inserir ou deletar row no histórico.
- [x] **Ordenar capítulos** - Menu com 4 opções: número (crescente/decrescente) e última modificação (crescente/decrescente). Aplica-se a chapters e volumes.
- [x] **Agrupar capítulos por volume** - Agrupamento e renderização dos dados que possuem `volumeId` formatados no Rust.
- [x] **Trocar estilo de exibição de capítulos/volumes** - O frontend muda o layout de list para volume baseado nas preferências da store do Svelte.
- [x] **Configurar paginação da lista de capítulos** - Parametrização customizável gravada nas preferências que quebra requisições grandes pro Rust.
- [x] **Atribuir categoria ao quadrinho** - Modal/Dropdown no Svelte que dispara o update do quadrinho no banco.
- [x] **Ativar/desativar sync externo por quadrinho** - Toggle na UI repassado para o backend que anula ou permite metadados online específicos.
- [x] **Sincronizar capítulos locais (rescaneamento manual/folder watch)** - Chamada de comando Tauri que instrui o Rust a recarregar apenas essa pasta pontual do File System.
- [x] **Reescanear quadrinho completo** - Invalida metadados atuais do banco e extrai tudo de novo daquele subdiretório.
- [x] **Sincronizar metadados pelo MangaDex** - Endpoint no Rust com um HTTP Client para buscar cover/banner/tags da API externa.
- [x] **Sincronizar metadados pelo AniList** - Endpoint no Rust utilizando queries GraphQL pro serviço.
- [x] **Sincronizar metadados pelo ComicInfo.xml** - O Rust decodifica e carrega o arquivo XML para popular o banco interno.
- [x] **Sincronizar capítulos pelo ComicInfo.xml** - O Rust associa as `Pages` e informações estruturais de capítulo via parse do XML.
- [x] **(validar se faz sentido nesse caso) Extrair primeira página de capítulo como capa do quadrinho** - Rust abre o `cbz/rar`, processa a page 0 e salva em disco como miniatura persistente.
- [x] **(validar se faz sentido nesse caso) Extrair capa do volume a partir do primeiro capítulo do volume** - O Rust resolve o primeiro item do volume local e exporta uma thumb isolada na pasta correspondente.

---

## Leitor

- [x] **Ler arquivos .cbz** - Tauri/Rust lidam com zip/cbz em disco, retornando uma lista de bytes serializada ou lendo via protocolo de assets (asset://).
- [x] **Ler arquivos .cbr** - Backend Rust trata a descompressão do rar no SO e entrega as páginas no front.
- [x] **(validar se faz sentido nesse caso) Converter .pdf para .cbz antes de ler** - O backend converte o PDF internamente num zip de bitmaps para servir ao leitor.
- [x] **(validar se faz sentido nesse caso) Pré-carregar páginas adjacentes (prefetch)** - O store do leitor (Svelte) pode fazer um fetch em background das N próximas imagens, ou o Tauri as deixa em buffer de RAM cache (LRU).
- [x] **Modo de leitura horizontal paginado** - Componente web lidando com interações de next/prev horizontalmente (mouse, teclado, etc).
- [x] **Modo de leitura vertical paginado** - Scroll restrito p/ cada página por vez.
- [x] **Modo Webtoon (scroll vertical contínuo)** - Lista CSS (`flex-col`) contínua para mangás compridos com intersection observers reportando a página.
- [x] **Zoom nas páginas** - Pan & Zoom controlado via Svelte/CSS (wheel do mouse, pinch).
- [x] **Alternar modo de leitura dentro do leitor** - Menu rápido (Command Palette ou Header) atualiza instantaneamente as stores de user preference (Svelte).
- [x] **Mostrar/ocultar controles do leitor** - Clique simples alterna classes de visibilidade dos toolbars.
- [x] **Salvar progresso de leitura automaticamente** - O Svelte observa a página ativa com Intersection Observer e invoca no Tauri o save do timestamp + page info.
- [x] **Navegar para próximo/capítulo anterior** - Shortcut de teclado ou botão na UI manda requisição pro Rust carregar as rotas do capítulo adjacente.

---

## Metadados

- [x] **Sincronizar metadados de toda a biblioteca pelo MangaDex** - Task de background no Rust processa recursivamente toda biblioteca em lotes (pool) buscando atualizações.
- [x] **Sincronizar metadados de toda a biblioteca pelo AniList** - Rotina similar ao anterior no backend usando GraphQL batch queries.
- [x] **(validar se faz sentido nesse caso) Salvar capa na pasta do quadrinho** - Rust realiza a operação de FS:I/O na mesma pasta do arquivo original com uma thumb otimizada (ex: resize jpeg).
- [x] **(validar se faz sentido nesse caso) Salvar banner na pasta do quadrinho** - Similar à escrita local, focado na imagem `banner.jpg` extraída ou da web.
- [x] **Exportar metadados como ComicInfo.xml** - Rotina do Rust que agrupa as tabelas do SQLite num formatador XML e injeta na pasta raíz caso configurado.
- [x] **Ler metadados de ComicInfo.xml** - Parser Rust que intercepta o ComicInfo durante o rescan incremental e sobrescreve as propriedades.

---

## Configurações

- [x] **Selecionar pasta raiz da coleção** - Abre o window picker padrão do Tauri e persiste a configuração pro Svelte state e backend db.
- [x] **Scan incremental da biblioteca** - Botão dispara evento Tauri (e.g. `refresh_library`) que usa filesystem watchers ou diff entre db e arquivos.
- [x] **Scan profundo (rebuild) da biblioteca** - O comando `rebuild_library` dropa informações antigas no Rust e itera pesadamente em todos arquivos de novo.
- [x] **Selecionar idioma global de metadados** - Svelte envia o código `pt-br`, `en`, para parametrizar APIs do backend.
- [x] **Ativar/desativar geração de ComicInfo.xml** - Estado guardado que liga rotinas passivas do Rust.
- [x] **Criar categoria** - Envio dos params (Label, HexColor) do modal do frontend para insert no SQLite via Tauri.
- [x] **Deletar categoria** - Deleção propagada em cascata pelo Rust com as devidas confirmações no Svelte.
- [x] **Selecionar tema do app** - Comportamento nativo SvelteKit pra injetar class CSS da cor (Ex: Catppuccin/Dracula).
- [x] **Navegar para configuração de templates** - Rota isolada (`/config/templates`) listando as macros e regras.

---

## Templates de Nomenclatura

- [x] **Criar template** - Frontend expõe os macros (ex: `{chapter}`, `{decimal}`) e grava numa entidade Rust de Parsing.
- [ ] **Editar template** - Update das tabelas locais relacionadas via Tauri Invoke.
- [x] **Deletar template** - Delete row, bloqueado para templates padrão (`is_default`).
- [x] **Listar templates** - O backend lista os templates ordenados que o usuário fez pra parser.
- [x] **Detecção automática de template no scan** - O parser de texto nativo no Rust intercepta arquivos de nome que não possuem um padrão pré-descrito, associando as strings corretas.

---

## Histórico

- [x] **Exibir leituras recentes** - Rota renderizada agrupando items a partir de uma call SQL (Tauri) listando as leituras e capas ativas.
- [x] **Continuar pelo histórico** - Payload de clique empacota state via router Svelte abrindo direto a page do `reader`.
- [ ] **[Alta] Corrigir bug no histórico** - Quando um capítulo é marcado como concluído o app não atualiza o histórico para otimizar o histórico.

---

## Onboarding

- [x] **Tutorial de primeira abertura** - Rota vazia caso DB esteja cru guiando o user pela seleção da primeira pasta no Tauri FS open API.

---

## Pendente

- [x] **Marcar quadrinho / capítulo como concluído manualmente** - Ação direta pro Tauri alterar a prop bool no banco.
- [x] **Seleção múltipla de quadrinhos e capítulos (multi-select)** - Manter Set Array/Map ativo na memória do Svelte UI pra realizar highlights com shift/ctrl cliques (mouse interaction).
- [x] **Ações em lote sobre seleção múltipla** - Loopar actions de API e passar Listas para queries batch do SQLite (Tauri) otimizando deletes.

---

## P2P / Sincronização

- [x] **[Média] Sincronização individual sem ser global** - Ter uma forma de poder sincronizar um quadrinho só com outro dispositivo: o usuário deve conseguir clicar em um quadrinho e enviar para outro dispositivo, ou pesquisar quadrinhos em outro dispositivo e pedir para sincronizar.
- [x] **[Alta] Botão "Escolher dispositivo" não abre a seleção de dispositivo** - Na tela de preferências do quadrinho, o botão que deveria abrir o popover com a lista de dispositivos pareados (pra sync individual) não abre nada ao clicar.
- [ ] **[Alta] Busca de biblioteca remota falha nos dois lados** - **REABERTO** (22/08/2026): a correção abaixo (`browse-library` v2, SQL puro) não resolveu — testado ao vivo entre Desktop e Android reais, ainda falha nos dois lados. Log do Desktop (outbound):
  ```
  [21:40:52] outbound; alpn="acerola/browse-library/1"
  [21:40:53] outbound connection established
  [21:41:23] outbound handler failed error=StreamFailed("stream error: timeout waiting for frame")
  ```
  Exatamente 30s entre a conexão estabelecer e o timeout — bate com `FRAME_TIMEOUT` (`framing.rs`). A conexão abre, mas nenhuma resposta chega do lado inbound (Android) dentro do timeout. Causa raiz ainda não investigada nesta rodada — meio óbvio suspeitar da mitigação de blobs em memória abaixo (ver esse item: se o Android ainda não tinha aplicado a mitigação/reiniciado no momento do teste, o node dele podia estar com o `.build()` travado, sem handler nenhum registrado pra responder). Precisa reproduzir de novo com os dois lados já rodando a mitigação `.mem()` antes de investigar mais fundo.

  **Investigado em 22/08/2026 (ainda sem causa raiz confirmada):** revisado `LibraryBrowseInbound`/`Outbound`, o registro de ALPN em `bios/network.rs`, o `NetworkManager::handle_incoming` (`acerola-p2p`) e o cache de conexões por `(peer, ALPN)` em `IrohTransport` — cada ALPN vive numa conexão QUIC física própria, então uma sessão travada de `sync-comic`/`sync-files` pro mesmo peer não deveria bloquear uma conexão nova de `browse-library` (chave de cache diferente). Também descartadas: (a) o guard de sessão (`FileSyncSessionGuard`) não é usado por `browse-library`, então não é ele; (b) o `TofuGuard`/`SecureTrustedStore` só faz leitura de um `RwLock` em memória, não deveria travar; (c) nenhum código do backend usa transação SQL explícita (`.begin()`/`.transaction()`), então não há uma transação longa segurando lock do SQLite durante um rescan. Não foi possível reproduzir o travamento localmente (precisa de um peer Android real). Duas mudanças defensivas aplicadas em `library_browse_handler.rs` enquanto a causa raiz não é confirmada: (1) logging em cada etapa (`querying`/`writing`/`written`, e falha do `run()`) — antes o handler não logava nada, então não dava pra saber em qual etapa travava; (2) `get_library_summary()` agora roda sob um timeout próprio de 10s (bem menor que os 30s do `FRAME_TIMEOUT` do outro lado) — se a query nunca retornar (ex: contenção no pool do SQLite por outra sessão de sync ativa), o handler falha rápido e loga a causa em vez de deixar o outbound estourar sem nenhuma pista. **Pendente:** reproduzir de novo com essa instrumentação nos dois lados pra finalmente ver em qual etapa trava.
- [x] **[Média] `browse-library` v2: resposta 100% SQL + sinal de versão de capa** - Nova query dedicada `ChapterRepository::get_library_summary()` (`COUNT(*) GROUP BY cd.name`, sem `tokio::fs::metadata`), exposta via `FileSyncService::get_library_summary()`. `LibraryBrowseInbound::run` parou de chamar `build_manifest()` pra essa ALPN — elimina o estouro do `FRAME_TIMEOUT` (30s) em bibliotecas grandes/discos lentos. `ComicSummaryEntry` (wire) e o payload do evento `library:query:result` ganharam `cover_version`/`coverVersion` (reaproveita `comic_directory.last_modified`), igual ao Android. **Nota:** essa correção sozinha não resolveu o item acima, que continua falhando por outro motivo ainda não identificado.
- [x] **[Baixa] Novo protocolo `acerola/browse-cover/1`** - Novo `infra/sync/protocol/cover_browse_handler.rs`: outbound manda `{ comic_name, known_version }` (registrado via `PendingCoverRequestRegistry`, mesmo padrão de `sync_comic`), inbound responde `not_modified`/`unavailable`/`changed{cover_hash}` — bytes de verdade vão via blobs (`ChapterTransfer`, reaproveitado da Fase 1), não a máquina de chunking do `transfer.rs`. `FileSyncService::get_local_cover()` lê `cover.jpg` direto do disco (path já em `comic_directory.cover`). Cliente grava a capa recebida em `<app_data_dir>/remote_covers/` e emite o caminho (não bytes) — Svelte resolve via `convertFileSrc`/`resolveArtworkPath`, mesmo padrão das capas locais. Novo comando Tauri `query_remote_cover`; `use-remote-library.svelte.ts` dispara a busca em paralelo pra cada item de `library:query:result`, cacheia em memória por `(peerId, comicName)`; thumbnail exibida no diálogo de biblioteca remota (`network/+page.svelte`).
- [x] **[Baixa] Migrar `sync-files`/`sync-comic` pra usar blobs do `acerola-p2p`** - `acerola-p2p` subiu pra `0.0.6` com a feature `iroh-blobs-adapter` habilitada. Fases de manifesto/want-list continuam iguais — só a fase de transferência (`infra/sync/protocol/transfer.rs::send_files`/`receive_files`) mudou: em vez de `send_file_bytes`/`receive_file_to_disk` (chunking manual de 256KB), cada capítulo é publicado no blob store local (`ChapterTransfer::publish`) e o `FileHeader` passa a carregar um `blob_hash` (BLAKE3, hex) além do `checksum` (SHA-256, inalterado — mantido só pra continuidade da verificação/telemetria local). Quem recebe busca via `ChapterTransfer::fetch_reader` (`P2pBlobStore::fetch` + `get`, que já verifica integridade automaticamente). `BlobContext` (`infra/sync/blob_context.rs`) resolve o acesso a `node.blobs()`/`node.known_peers()` de dentro dos handlers, que são registrados no builder antes do node existir (guarda um `Weak<AcerolaP2p>` preenchido só depois do `.build()`).
- [ ] **[Crítica] `FsStore` do `iroh-blobs` trava indefinidamente ao abrir store em disco (mitigado, não corrigido)** - Descoberto em 22/08/2026, depois de habilitar `.blobs(IrohBlobsConfig::fs(...))` (item acima): `AcerolaP2p::builder(...).build()` passou a travar 100% das vezes, estourando o timeout de 10s em `setup_network` (`[Bios::Network] Timeout waiting for AcerolaP2p::build(): Elapsed(())`) — como resultado, `network_service` nunca era `.manage()`do, e TODO comando Tauri que depende dele (`get_local_id`, QR code de pareamento, tudo) quebrava com `state not managed`. Confirmado isolado, fora do app: um teste mínimo em `acerola-p2p` (`core/blobs/iroh/mod.rs::tests::fs_store_load_does_not_hang`, não commitado na lib) chamando só `IrohBlobStore::new` com config `Fs` num diretório limpo trava e estoura 15s sozinho — bug real na integração `acerola-p2p`/`iroh-blobs` com store em disco (a lib nunca tinha testado esse caminho, só `IrohBlobsConfig::mem()`). **Mitigação aplicada nesta rodada:** trocado `.blobs(IrohBlobsConfig::fs(app_data_directory.join("blobs")))` por `.blobs(IrohBlobsConfig::mem())` em `bios/network.rs` — destrava o app (confirmado: `P2P network service initialized successfully` aparece na hora), mas blobs deixam de persistir entre reinícios do app. **Pendente:** achar a causa raiz do hang no `FsStore::load_with_opts` (`iroh-blobs`) e voltar pra `.fs(...)` depois.
- [x] **[Alta] Capítulos recebidos via `sync-comic` não persistem todos, com `UNIQUE constraint violated`** - Corrigido em 22/08/2026: `persist_received_chapter` (`core/services/sync/file_sync.rs`) tratava `Err(DbError::UniqueViolation)` como um no-op (`=> Ok(())`) sempre que o `(comic_directory_fk, chapter)` recebido já existia (reenvio de uma sessão interrompida/repetida) — o arquivo novo já tinha sido gravado em disco (`tokio::fs::rename` acima), mas a linha do banco ficava com o `path`/`checksum` antigos, fazendo o capítulo "sumir" ou nunca refletir o conteúdo realmente recebido. Agora busca a linha existente via `find_by_comic_and_chapter` antes de decidir: se já existe, faz `UPDATE` nela (preservando `id`/`volume_id_fk`) em vez de tentar um `INSERT` que colide; se o `path` antigo mudou, o arquivo órfão é removido do disco. `UniqueViolation` só continua sendo engolido como no-op na corrida rara de duas inserções concorrentes pro mesmo par exatamente no mesmo instante (janela entre o `SELECT` e o `INSERT`, sem transação). Teste de regressão: `persist_received_chapter_updates_existing_row_on_resend_instead_of_dropping_it` (reenvia o mesmo capítulo duas vezes com checksums diferentes, confere que fica só 1 linha com o checksum novo). Suite completa roda limpa via `cargo make test` (274 passed, 0 failed).

---

## Arquitetura & Infraestrutura (Rust)

- [x] **Gerar seed dinâmico para nó P2P** - (validar se é a melhor forma) Substituir o seed hardcoded por geração de 32 bytes aleatórios persistidos em arquivo local (.key) ou SQLite para cada instalação ter sua identidade P2P isolada.
- [x] **Tratamento gracioso de erro na inicialização assíncrona do Rust** - Substituir o uso de `panic!` na inicialização de serviços assíncronos (banco de dados SQLite, nó de rede P2P) por retornos de `Result` e exibição de alerta gráfico ao usuário.
- [x] **Otimizar e dinamizar o gerenciamento de escopos do File System (fs_scope)** - Substituir a leitura crua do settings.json via std::fs pelo plugin tauri-plugin-store e atualizar dinamicamente as permissões do fs_scope quando o usuário alterar a pasta da biblioteca em runtime.
- [ ] **[Média] Fazer o app conseguir ficar em segundo plano com ícone escondido** - Fazer o app poder ficar colapsado em segundo plano e na barra de tarefas do sistema para quando o usuário executar algo demorado ele poder deixar fechado enquanto roda.
