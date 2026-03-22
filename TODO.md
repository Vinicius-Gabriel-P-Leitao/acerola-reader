# Acerola Leitor de Mangás

Acerola é um aplicativo Android desenvolvido para entusiastas de mangás que gerenciam sua própria biblioteca local de
arquivos de mangás. Ele oferece uma maneira bonita e eficiente de escanear, organizar e ler sua coleção, enriquecida com
metadados de fontes online populares.

## Funcionalidades Principais

* **Gerenciamento da Biblioteca Local:** Escaneia automaticamente suas pastas e arquivos de mangás (`.cbz`, `.cbr`) no
  dispositivo e os organiza em uma biblioteca navegável.
* **Metadados Automáticos:** Busca e exibe metadados ricos para seus mangás, como capas, descrições, informações do
  autor e muito mais.
* **Interface Personalizável:** Adapte a aparência da sua biblioteca à sua preferência.
* **Leitor Integrado:** Leia seus mangás diretamente no aplicativo com um leitor projetado para uma experiência fluida.

---

## Status das Funcionalidades

### ✅ Implementado

#### Biblioteca
- [x] **Escaneamento da Biblioteca:** Escaneia as pastas especificadas para detectar mangás e capítulos, persistindo a estrutura em um banco de dados local.
- [x] **Sincronização da Biblioteca:** Mantém o banco de dados local sincronizado com o sistema de arquivos, detectando novas adições, renomeações e exclusões.
- [x] **Integração com MangaDex:** Busca automaticamente metadados de mangás do MangaDex com base nos nomes das pastas.
- [x] **Tela Inicial (Home Screen):** Exibe todos os mangás da sua biblioteca com dois layouts: Lista e Grade.
- [x] **Tela de Capítulos:** Mostra uma lista de todos os capítulos de um mangá selecionado com metadados detalhados.

#### Correções (FIXME)
- [x] **Bug no Primeiro Scan:** Corrigido o mapeamento de capítulos no primeiro scan.
- [x] **Performance:** Reformulado o `loadPage` da seção de capítulos na tela de detalhes.

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
- [x] **Redesign Flat (Material 3):** Reformulação total das telas de configuração para um visual plano e moderno, seguindo os padrões do Material You.
- [x] **Padronização de Fontes de Metadados:** Implementação do Enum `MetadataSource` de ponta a ponta (Banco -> Engine -> UI).
- [x] **Feedback de Fonte Ativa:** Identificação visual (Badge/Check) da fonte que provê os metadados atuais.
- [x] **Sincronização Contextual:** Botões de sincronizar capítulos aparecem apenas quando a fonte correspondente está ativa.

#### UI/UX
- [x] **ChapterItem:** Visual reformulado.
- [x] **Busca de Capítulos:** Por número (`chapterSort`), nome e nome do arquivo.
- [x] **Redesenho da Barra Superior:** Visual mais clean (apple liquid glass) com tema dark.
- [x] **Home Screen:** Ajuste de padding inferior para evitar sobreposição do FAB (FloatingTool) com os itens da lista.

#### Background e Performance
- [x] **Notificações:** Syncs demorados rodam em background com progresso e notificação.
- [x] **Correção de notificações:** Sync geral do MangaDex e outras funções corrigidos para notificar corretamente.

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

#### Temas
- [x] **Tema adaptável** com persistência via DataStore.
- [x] **Catppuccin** como tema padrão com melhorias para o tema claro.
- [x] **Dracula** (dark) e **Alucard** (claro).
- [x] **Nord** em versões claro e escuro.

#### Logs e Limpeza
- [x] **Wrapper de logs:** Classe padronizada para facilitar busca e filtragem.
- [x] **Limpeza de mangás deletados:** Job que remove do banco entradas cujas pastas não existem mais.

---

### 🔲 Pendente

#### ❗ Principal Mudar todos arquivos que usam RemoteInfo para Metadata:
- [x]  necessário para não ter conflito de dominio, vai ser tudo virado para Metadata de RemoteInfo, já que agora os 
  sources tem tabelas para eles mesmos, será mudado também nome de tabela e colunas.

#### ❗ Principal refatoração e organização do modulo data:
- [x] Organizar o modulo data de forma que o source e o engine consigam ficar armoniosos.

#### Fontes de Metadados Expandidas
- [x] Opção para escolher **AniList** como fonte alternativa de metadados.
- [x] Painel de configurações para selecionar e configurar o provedor (MangaDex / AniList / ComicInfo) com feedback de fonte ativa.

#### Download via MangaDex
- [x] **Melhorar visualização:** Indicar visualmente que o mangá está sendo baixado e exibir fila de capítulos.
- [x] **Paginação no download:** Mangás com 300+ capítulos mostram apenas 0/100 — exibir de forma paginada e 
  permitir baixar tudo em sequência.
- [x] **Melhorar activity de download** A activity tem três problemas não funciona o botão baixar tudo, não dá pra 
  selecionar tudo, trocar pagina e clicar no selecionar tudo e ele considerar a as duas paginas e o bottom sheet a 
  seção está bem desorganizada.
- [x] **Patterns de regex configuráveis:** Tornar os patterns de nome de arquivo visíveis e permitir ao usuário 
  escolher qual usar no download.
- [x] **Reorganziar a tabela de métadados:** Essa refatoração vai ter como intuito tranformar a tabela de métadados 
  agnóstica de origem dos dados e teremos tabelas pequenas auxiliares para poder atender essa relação, quero uma de 
  mangadex, anilist e outra de comicInfo, por que dissso o mangadex trás dados validos para armazenar como ID do 
  anilist e link para o produto original. 

#### Filtros e Ordenação — Home Screen
- [ ] **Ordenação Asc/Desc** por: título, quantidade de capítulos e última atualização.
- [ ] **Persistência da ordenação** via DataStore.
- [ ] **UI do filtro:** Usar apenas ícone, sem label.

#### Filtros e Ordenação — Tela de Capítulos
- [ ] **Remover autoscroll** ao trocar de página.
- [ ] **Ordenação Asc/Desc** por: número do capítulo e última atualização.
- [ ] **Persistência da ordenação** por mangá (ou global) via DataStore.
- [ ] **UI do filtro:** Ícone na barra superior abrindo bottom sheet.

#### Verificar e Baixar Novos Capítulos
- [ ] **Botão "Verificar updates":** Na tela de detalhes do mangá, um botão que consulta o MangaDex e retorna
  capítulos mais recentes do que o último registrado no banco local.
- [ ] **Exibir capítulos disponíveis:** Mostrar uma lista/bottom sheet com os capítulos novos encontrados,
  exibindo número, nome e data de lançamento antes de qualquer ação.
- [ ] **Download sob demanda:** O usuário seleciona quais capítulos quer baixar e confirma — só então
  enfileira no download manager já existente.
- [ ] **Indicador de "atualizado" ou "tem novidade":** Badge ou ícone na tela de detalhes indicando que
  existem capítulos no MangaDex além do último local, sem baixar nada automaticamente.

---


## Pilha de Tecnologia

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Programação Assíncrona:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) e [Flow](https://kotlinlang.org/docs/flow.html)
* **Banco de Dados:** [Room](https://developer.android.com/training/data-storage/room)
* **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/)
* **Injeção de Dependências:** Manual (via ViewModelFactories)