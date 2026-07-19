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
- [ ] **Marcar capítulo como lido / não lido** - Ação explícita na UI chamando um evento no backend (Tauri) para inserir ou deletar row no histórico.
- [x] **Ordenar capítulos** - Menu com 4 opções: número (crescente/decrescente) e última modificação (crescente/decrescente). Aplica-se a chapters e volumes.
- [x] **Agrupar capítulos por volume** - Agrupamento e renderização dos dados que possuem `volumeId` formatados no Rust.
- [x] **Trocar estilo de exibição de capítulos/volumes** - O frontend muda o layout de list para volume baseado nas preferências da store do Svelte.
- [x] **Configurar paginação da lista de capítulos** - Parametrização customizável gravada nas preferências que quebra requisições grandes pro Rust.
- [x] **Atribuir categoria ao quadrinho** - Modal/Dropdown no Svelte que dispara o update do quadrinho no banco.
- [ ] **Ativar/desativar sync externo por quadrinho** - Toggle na UI repassado para o backend que anula ou permite metadados online específicos.
- [ ] **Sincronizar capítulos locais (rescaneamento manual/folder watch)** - Chamada de comando Tauri que instrui o Rust a recarregar apenas essa pasta pontual do File System.
- [ ] **Reescanear quadrinho completo** - Invalida metadados atuais do banco e extrai tudo de novo daquele subdiretório.
- [ ] **Sincronizar metadados pelo MangaDex** - Endpoint no Rust com um HTTP Client para buscar cover/banner/tags da API externa.
- [ ] **Sincronizar metadados pelo AniList** - Endpoint no Rust utilizando queries GraphQL pro serviço.
- [ ] **Sincronizar metadados pelo ComicInfo.xml** - O Rust decodifica e carrega o arquivo XML para popular o banco interno.
- [ ] **Sincronizar capítulos pelo ComicInfo.xml** - O Rust associa as `Pages` e informações estruturais de capítulo via parse do XML.
- [ ] **(validar se faz sentido nesse caso) Extrair primeira página de capítulo como capa do quadrinho** - Rust abre o `cbz/rar`, processa a page 0 e salva em disco como miniatura persistente.
- [ ] **(validar se faz sentido nesse caso) Extrair capa do volume a partir do primeiro capítulo do volume** - O Rust resolve o primeiro item do volume local e exporta uma thumb isolada na pasta correspondente.

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

- [ ] **Sincronizar metadados de toda a biblioteca pelo MangaDex** - Task de background no Rust processa recursivamente toda biblioteca em lotes (pool) buscando atualizações.
- [ ] **Sincronizar metadados de toda a biblioteca pelo AniList** - Rotina similar ao anterior no backend usando GraphQL batch queries.
- [ ] **(validar se faz sentido nesse caso) Salvar capa na pasta do quadrinho** - Rust realiza a operação de FS:I/O na mesma pasta do arquivo original com uma thumb otimizada (ex: resize jpeg).
- [ ] **(validar se faz sentido nesse caso) Salvar banner na pasta do quadrinho** - Similar à escrita local, focado na imagem `banner.jpg` extraída ou da web.
- [ ] **Exportar metadados como ComicInfo.xml** - Rotina do Rust que agrupa as tabelas do SQLite num formatador XML e injeta na pasta raíz caso configurado.
- [ ] **Ler metadados de ComicInfo.xml** - Parser Rust que intercepta o ComicInfo durante o rescan incremental e sobrescreve as propriedades.

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
- [ ] **Navegar para configuração de templates** - Rota isolada listando as macros e regras.

---

## Templates de Nomenclatura

- [ ] **Criar template** - Frontend expõe os macros (ex: `{chapter}`, `{decimal}`) e grava numa entidade Rust de Parsing.
- [ ] **Editar template** - Update das tabelas locais relacionadas via Tauri Invoke.
- [ ] **Deletar template** - Delete row.
- [ ] **Listar templates** - O backend lista os templates ordenados que o usuário fez pra parser.
- [ ] **Detecção automática de template no scan** - O parser de texto nativo no Rust intercepta arquivos de nome que não possuem um padrão pré-descrito, associando as strings corretas.

---

## Histórico

- [x] **Exibir leituras recentes** - Rota renderizada agrupando items a partir de uma call SQL (Tauri) listando as leituras e capas ativas.
- [x] **Continuar pelo histórico** - Payload de clique empacota state via router Svelte abrindo direto a page do `reader`.

---

## Onboarding

- [x] **Tutorial de primeira abertura** - Rota vazia caso DB esteja cru guiando o user pela seleção da primeira pasta no Tauri FS open API.

---

## Pendente

- [ ] **Marcar quadrinho / capítulo como concluído manualmente** - Ação direta pro Tauri alterar a prop bool no banco.
- [ ] **Seleção múltipla de quadrinhos e capítulos (multi-select)** - Manter Set Array/Map ativo na memória do Svelte UI pra realizar highlights com shift/ctrl cliques (mouse interaction).
- [ ] **Ações em lote sobre seleção múltipla** - Loopar actions de API e passar Listas para queries batch do SQLite (Tauri) otimizando deletes.
