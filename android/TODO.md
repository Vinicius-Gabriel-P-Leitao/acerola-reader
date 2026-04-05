Tem razão, desculpa. Aqui está o README reorganizado em markdown:

---

# Acerola Leitor de Mangás

Acerola é um aplicativo Android desenvolvido para entusiastas de mangás que gerenciam sua própria biblioteca local de arquivos de mangás. Ele oferece
uma maneira bonita e eficiente de escanear, organizar e ler sua coleção, enriquecida com metadados de fontes online populares.

## Funcionalidades Principais

* **Gerenciamento da Biblioteca Local:** Escaneia automaticamente suas pastas e arquivos de mangás (`.cbz`, `.cbr`) no dispositivo e os organiza em
  uma biblioteca navegável.
* **Metadados Automáticos:** Busca e exibe metadados ricos para seus mangás, como capas, descrições, informações do autor e muito mais.
* **Interface Personalizável:** Adapte a aparência da sua biblioteca à sua preferência.
* **Leitor Integrado:** Leia seus mangás diretamente no aplicativo com um leitor projetado para uma experiência fluida.

---

## Status das Funcionalidades

### ✅ Implementado

#### Biblioteca

- [x] **Escaneamento da Biblioteca:** Escaneia as pastas especificadas para detectar mangás e capítulos, persistindo a estrutura em um banco de dados
  local.
- [x] **Sincronização da Biblioteca:** Mantém o banco de dados local sincronizado com o sistema de arquivos, detectando novas adições, renomeações e
  exclusões.
- [x] **Integração com MangaDex:** Busca automaticamente metadados de mangás do MangaDex com base nos nomes das pastas.
- [x] **Tela Inicial (Home Screen):** Exibe todos os mangás da sua biblioteca com dois layouts: Lista e Grade.
- [x] **Tela de Capítulos:** Mostra uma lista de todos os capítulos de um mangá selecionado com metadados detalhados.

#### Leitor e Configuração do Mangá

- [x] **Suporte para `.cbz` e `.cbr`** com leitura integrada.
- [x] **Sync Individual:** Sincronizar apenas um mangá (MangaDex + Filesystem).
- [x] **Paginação:** Configuração para alterar a quantidade de capítulos por página.
- [x] **Gerenciamento de Imagens:** Carregar, trocar, salvar e remover capa e banner.
- [x] **Informações de Armazenamento:** Exibir tamanho do mangá em GB ou MB.
- [x] **Ações de Limpeza (com confirmação):** Limpar metadados e capítulos.
- [x] **Interface do Leitor:** Ajuste do BottomSheet de configurações para visual opaco e integrado ao tema.

#### Configurações Globais

- [x] **Gerenciamento de Metadados (ComicInfo.xml vs DB):** Gerar/ler `ComicInfo.xml`, resolver conflitos e persistir escolha.
- [x] **Metadados de Capítulos (MangaDex):** Busca em background, desativada por padrão.
- [x] **Redesign Flat (Material 3):** Reformulação total das telas de configuração.
- [x] **Padronização de Fontes de Metadados:** Implementação do Enum `MetadataSource` de ponta a ponta (Banco → Engine → UI).
- [x] **Feedback de Fonte Ativa:** Identificação visual (Badge/Check) da fonte que provê os metadados atuais.
- [x] **Sincronização Contextual:** Botões de sincronizar capítulos aparecem apenas quando a fonte correspondente está ativa.
- [x] **Globalizar idioma de metadados do MangaDex:** Metadados e busca padrão respeitam o idioma selecionado nas configurações.

#### Fontes de Metadados

- [x] **AniList como fonte alternativa:** Opção para escolher AniList como provedor de metadados.
- [x] **Painel de configuração de provedor:** Seleção e configuração de MangaDex / AniList / ComicInfo com feedback de fonte ativa.
- [x] **Correção de bug na troca de source:** Troca de source agora ocorre livremente, incluindo via `ConfigScreen`.
- [x] **Refatoração RemoteInfo → Metadata:** Tabela de metadados agnóstica de origem, com tabelas auxiliares por source (MangaDex, AniList,
  ComicInfo).
- [x] **Correção de erro 500 do AniList:** Erro quando mangá não tem metadados no AniList tratado corretamente na UI.

#### Refatorações Estruturais

- [x] **Renomeação RemoteInfo → Metadata:** Sem conflito de domínio, tabelas e colunas atualizadas.
- [x] **Reorganização do módulo data:** Source e engine em harmonia.

#### UI/UX

- [x] **ChapterItem:** Visual reformulado.
- [x] **Busca de Capítulos:** Por número (`chapterSort`), nome e nome do arquivo.
- [x] **Redesenho da Barra Superior:** Visual mais clean (apple liquid glass) com tema dark.
- [x] **Home Screen:** Ajuste de padding inferior para evitar sobreposição do FAB com os itens da lista.
- [x] **Snackbar personalizada:** Variantes com tons vermelhos para erros e verdes para sucesso, de acordo com o tema.
- [x] **Texto da SearchScreen centralizado.**
- [x] **Cor dos dialogs em modo claro corrigida.**
- [x] **Animação bugada da SearchBar corrigida** (ao sair de foco).

#### Background e Performance

- [x] **Notificações:** Syncs demorados rodam em background com progresso e notificação.
- [x] **Correção de notificações:** Sync geral do MangaDex e outras funções corrigidos.
- [x] **ProgressBar global na MainActivity:** Aparece em todas as telas.

#### Rastreamento de Leitura

- [x] Marcar capítulos como lidos/não lidos.
- [x] Funcionalidade "Continuar Lendo".
- [x] Otimização da interface do leitor.
- [x] Clique para trocar página nos modos paginados.

#### Download via MangaDex

- [x] **Busca de tradução:** Por nome, ID do MangaDex ou URL.
- [x] **Seleção de idioma** antes do download.
- [x] **Download como `.cbz`** com empacotamento automático na pasta do mangá.
- [x] **Fila de download:** Múltiplos downloads simultâneos com progresso e notificação.
- [x] **Integração com biblioteca:** Sync automático após download.
- [x] **Correção:** Geração correta de `.cbz`/`.cbr` (sem extensão dupla `.cbz.zip`).
- [x] **Metadados antes dos capítulos:** Baixa cover, gênero etc. antes de iniciar os capítulos.
- [x] **Visualização melhorada:** Indicação visual de download ativo e fila de capítulos.
- [x] **Paginação no download:** Mangás com 300+ capítulos exibidos de forma paginada com opção de baixar tudo em sequência.
- [x] **Activity de download melhorada:** Botão "baixar tudo", seleção multi-página e bottom sheet reorganizados.
- [x] **Patterns de regex configuráveis:** Patterns de nome de arquivo visíveis e selecionáveis pelo usuário.
- [x] **Correção no download de cover e banner:** Usa corretamente o caminho do mangá em vez do path de salvamento.
- [x] **SearchBar da tela de download corrigida** visualmente após busca.
- [x] **Progresso de download** com visualização melhorada.
- [x] **Verificação e download sob demanda de novos capítulos.**

#### Filtros e Ordenação — Home Screen

- [x] **Ordenação Asc/Desc** por: título, quantidade de capítulos e última atualização.
- [x] **Persistência da ordenação** via DataStore.
- [x] **UI do filtro:** Apenas ícone, sem label.
- [x] **Mostrar ocultos** apenas quando filtrados.
- [x] **Filtros sem DataStore:** Por bookmark, source de metadados ou sem metadados.

#### Filtros e Ordenação — Tela de Capítulos

- [x] **Removido autoscroll** ao trocar de página.
- [x] **Ordenação Asc/Desc** por número do capítulo e última atualização.
- [x] **Persistência da ordenação** por mangá via DataStore.
- [x] **UI do filtro:** Ícone na barra superior abrindo bottom sheet.

#### Ações por Mangá

- [x] **Bookmark** no menu de ações do card do mangá.
- [x] **Deletar ou ocultar mangá** no menu de ações.

#### Temas

- [x] **Tema adaptável** com persistência via DataStore.
- [x] **Catppuccin** como tema padrão com melhorias para o tema claro.
- [x] **Dracula** (dark) e **Alucard** (claro).
- [x] **Nord** em versões claro e escuro.
- [x] **Bolinhas dos temas** adaptam cor e nome conforme dark/light em todos os temas.

#### Patterns

- [x] **Dialog de registrar pattern** mais descritivo: explica `{chapter}`, `{decimal}` e `*` corretamente.
- [x] **Visualização da lista de patterns** corrigida.
- [x] **Padronização do botão de pattern** ("Ver menos"/"Ver mais" consistente).

#### Logs e Limpeza

- [x] **Wrapper de logs:** Classe padronizada para facilitar busca e filtragem.
- [x] **Limpeza de mangás deletados:** Job que remove do banco entradas cujas pastas não existem mais.

---

### 🔲 Pendente - Refatorações Planejadas (Auditoria)

#### Fase 1 - Arquitetura (Remoção de Layer Leak)

- [x] Refatorar `FilePatternUiState` e `TemplateItem` para usar `ChapterTemplateDto`
- [x] Extrair `ChapterTemplateDto` e remover acoplamento do Room na UI

#### Fase 2 - Performance (Recomposições)

- [x] Migrar `MangaUiState` e `ReaderUiState` para usar `kotlinx-collections-immutable`

#### Fase 3 - Performance (Reader)

- [x] Refatorar `ReaderUiState` para remover `Map<Int, Bitmap>` (delegar para Coil)

#### Fase 4 - Regra de Negócio (Ordenação)

- [x] Implementar `ChapterSortNormalizer` e atualizar persistência (migration)

#### Fase 5 - Robustez

- [x] Envolver `ChapterDownloadWorker` e `MetadataSyncWorker` com tratamento robusto de exceções

#### Fase 6 - Verificação

- [x] Executar testes e build após cada fase

#### Erro ao listar mangás com decimal

- [x] **Busca de mangás com chapter decimal:** Novamente problema com chapters 0.01 e logo em seguida vim o 0.10 ao invez de 0.02

#### ‼️ UX para modo deitado

- [x] **Fazer UX para deitado** Quando deitado a BottomBar tem que virar uma SideBar e ao lado o conteudo das paginas.
- [x] **Melhorar UX deitado** Quando deitado a BottomBar tem que virar uma SideBar e ao lado o conteudo das paginas trabalhar os BottomSheet como 
  SideSheet, deixar cor da sidebar de cor diferente igual a BottomSheet, centralizar itens verticalmente.

#### Corrigir SearchLayout

- [x] **SearchBar ux ruim** A SearchBar da tela de SearchLayout está com ux descentralizado, mais especificamente a SearchBar o input, ele está a 
  esquerda da tela, principalmente no modo deitado, deveria estar no centro.

#### Snackbar — Uso das variantes

- [x] **Mapear casos de sucesso e aviso** No código com `TODO:` e aplicar variantes da Snackbar (success/warn).

#### PatternScreen

- [x] **Refatorar design da PatternScreen:** Atualmente funcional, mas visualmente não agradável. Repensar a forma como funciona.

#### Tutorial

- [x] **Tutorial introdutório:** Ensinar como selecionar a pasta de mangás e usar o botão de sync profundo.

### Testes

- [x] **Escrever testes**
    - [MetadataSyncWorker.kt](core/src/main/java/br/acerola/manga/core/worker/MetadataSyncWorker.kt)
    - [ChapterDownloadWorker.kt](core/src/main/java/br/acerola/manga/core/worker/ChapterDownloadWorker.kt)

---

### 🛠️ TODOs e FIXMEs no Código

Encontrados 10 itens em 9 arquivos.

#### FIXME

- [ ] Problema de sync bugado após alguma operação (provavelmente ao definir a primeira imagem como cover ou adicionar novos mangás). Corrigir botão de sincronizar cover/banner, que atualmente não funciona e pode estar relacionado.
- [x] `ChapterArchiveEngine.kt:102` — Usar `ChapterTemplatePattern` para pegar o primeiro pattern como default.
- [x] `SearchBar.kt:184,202` — Transformar strings hardcoded em `strings.xml`
- [x] `GlobalProgressViewModel.kt:12` — Contrato frágil para valores do progresso; definir um contrato explícito.
- [x] `TemplateValidatorPattern.kt:51` — Mover texto de `InvalidPattern` para `strings.xml`.
- [x] `MainActivity.kt:28` — Implementar `fakeLoading` para pré-carregar itens antes de exibi-los, foi feito diferente e tratado na homescreen.

#### TODO

- [x] `LocalSnackbarHostState.kt:6` — Tratar melhor o erro de estado do Snackbar.
- [x] `FileSystemAccessManager.kt:20` — Tratar erros de forma mais personalizada.
- [x] `DownloadViewModel.kt:77` — Refatorar componente de progresso para não depender de strings internas do worker.
- [x] `TemplateValidatorPattern.kt:51` — Texto de erro `InvalidPattern` deve estar em `strings.xml`.
- [x] `ChapterSourceFactory.kt:14` — Usar erro traduzido respeitando o tratamento de erro do app.

---

## Pilha de Tecnologia

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Banco de Dados:** [Room](https://developer.android.com/training/data-storage/room)
* **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Injeção de Dependências:** Manual (via ViewModelFactories)
* **Programação Assíncrona:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
  e [Flow](https://kotlinlang.org/docs/flow.html)
