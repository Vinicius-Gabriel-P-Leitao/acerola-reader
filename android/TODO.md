# Acerola — Leitor de Quadrinhos

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

## ✅ Histórico de Implementações (Changelog)

### 📚 Biblioteca e Gerenciamento Local
- **Motor de Escaneamento e Sync:** Leitura e persistência automática de pastas e arquivos (`.cbz`, `.cbr`), com sincronização contínua de adições, renomeações e exclusões.
- **Sync Contextual e Individual:** Opção de sincronizar apenas um quadrinho específico e botões de atualização que respeitam a fonte de metadados ativa.
- **Ações por Quadrinhos:** Adicionar aos favoritos (Bookmark), ocultar ou deletar direto do menu de contexto.
- **Limpeza de Dados:** Ações (com confirmação) para limpar metadados ou capítulos. Um Job em background também limpa dados de quadrinhos cujas pastas não existem mais.
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
- **Paginação de Grandes Quadrinhos:** Otimização para listar e baixar obras com mais de 300 capítulos em sequência.

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

### ⚙️ Arquitetura, Performance e Refatorações de Código
- **Coleções Imutáveis:** Migração de `MangaUiState` e `ReaderUiState` para `kotlinx-collections-immutable`, evitando recomposições desnecessárias.
- **Otimização de Memória:** Remoção de `Map<Int, Bitmap>` da UI State do Reader, delegando a gestão integral de cache para o Coil.
- **Desacoplamento:** Refatoração de `RemoteInfo` para `Metadata` agnóstico. Criação do `ChapterTemplateDto` para evitar vazamento de dependência do Room na camada de UI.
- **Tratamento de Exceções:** Envelopamento robusto de erros no `ChapterDownloadWorker`, `MetadataSyncWorker` e `FileSystemAccessManager`.
- **Clean Code (Tech Debt Eliminado):**
    - Strings e logs hardcoded movidos para `strings.xml` e padronizados em um Wrapper de log.
    - Correção de falhas nos contratos de UI (ex: `GlobalProgressViewModel`).
- **Testes:** Cobertura aplicada nos motores principais (ex: `MetadataSyncWorker`, `ChapterDownloadWorker`).

---

## 🚧 Tarefas Pendentes (TODO)

### Aplicar mudanças da lib rust acerola p2p

- [ ] **A lib está a ser feita:** Será feito um grande refactor no campo de rust para poder montar a FFI atualizada e otimizada para poder salvar chaves de PeerId, DeviceInfo entre outros, poder usar o keystore para salvar dados que devem ser criptografados.

### Adicionar um worker para o conversor de pdf

- [ ] **Montar um worker:** Criar um worker para quando um pdf for virar cbz, pode demorar muito, ou se melhor como tenho uma lista de pastas e arquivos na hora de converter, se possível tenta converter 2 ao mesmo tempo, se for viável.

### Novas Funcionalidades (Features)

- [ ] **Ação de Conclusão Manual:** Implementar botão/opção para o usuário marcar um quadrinho ou capítulo como concluído manualmente.
- [ ] **Seleção Múltipla (Multi-select):** Permitir a seleção de múltiplos capítulos e quadrinhos segurando (*long press*) o card ou botão correspondente.

### Modificar tabelas do banco de dados para ter nomes consistentes

- [ ] **Alguns campos que deveriam ser _fk estão com _id:** Será feito refactor e migration, deixa isso para versão do banco 3.