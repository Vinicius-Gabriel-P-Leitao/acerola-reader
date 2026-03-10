# Acerola Leitor de Mangás

Acerola é um aplicativo Android desenvolvido para entusiastas de mangás que gerenciam sua própria biblioteca local de arquivos de mangás. Ele oferece uma maneira bonita e eficiente de escanear, organizar e ler sua coleção, enriquecida com metadados de fontes online populares.

## Funcionalidades Principais

*   **Gerenciamento da Biblioteca Local:** Escaneia automaticamente suas pastas e arquivos de mangás (`.cbz`, `.cbr`) no dispositivo e os organiza em uma biblioteca navegável.
*   **Metadados Automáticos:** Busca e exibe metadados ricos para seus mangás, como capas, descrições, informações do autor e muito mais.
*   **Interface Personalizável:** Adapte a aparência da sua biblioteca à sua preferência.
*   **Leitor Integrado:** Leia seus mangás diretamente no aplicativo com um leitor projetado para uma experiência fluida.

## Status das Funcionalidades

Este projeto está em desenvolvimento ativo. Abaixo está uma lista das funcionalidades atualmente implementadas e o que está planejado para o futuro.

### ✅ Implementado

*   [x] **Escaneamento da Biblioteca:** Escaneia as pastas especificadas para detectar mangás e capítulos, persistindo a estrutura em um banco de dados local.
*   [x] **Sincronização da Biblioteca:** Mantém o banco de dados local sincronizado com o sistema de arquivos, detectando novas adições, renomeações e exclusões.
*   [x] **Integração com MangaDex:** Busca automaticamente metadados de mangás do MangaDex com base nos nomes das pastas.
*   [x] **Tela Inicial (Home Screen):** Exibe todos os mangás da sua biblioteca com duas opções de layout: Lista e Grade.
*   [x] **Tela de Capítulos:** Mostra uma lista de todos os capítulos de um mangá selecionado, juntamente com seus metadados detalhados.

### 🛠️ FIXME (Correções Prioritárias)

*   [x] **Bug no Primeiro Scan:** Verificar por que no primeiro scan de mangás os capítulos não estão sendo mapeados.
*   [x] **Performance:** Reformular o `loadPage` da seção de capítulos na tela de detalhes do mangá.

### 🚧 Planejado / Em Andamento

*   **Leitor de Mangás Integrado:**
    *   [x] Suporte completo para formatos `.cbz` e `.cbr`.
*   **Configuração e Gerenciamento do Mangá (Refatoração):**
    *   [X] **Sync Individual:** Sincronizar apenas um mangá (MangaDex + Filesystem) buscando novos arquivos.
    *   [x] **Paginação:** Configuração para alterar a quantidade de capítulos por página.
    *   [ ] **Edição de Metadados:** Editar metadados básicos com opção de "Trancar" o mangá para impedir sobrescrita.
    *   [X] **Gerenciamento de Imagens:** Carregar, trocar, salvar e remover capa (`cover.jpg`/`.png`) e banner. Se 
        o banner for removido, a capa assume o lugar.
    *   [x] **Informações de Armazenamento:** Exibir tamanho do mangá em GB ou MB.
    *   [x] **Ações de Limpeza (com confirmação):**
        *   Limpar metadados (remove do DB e apaga `ComicInfo.xml`).
        *   Limpar capítulos (remove todos os arquivos e dados).
*   **Configurações Globais:**
    *   [] **Gerenciamento de Metadados (ComicInfo.xml vs DB):**
        *   Gerar `ComicInfo.xml` por padrão; ler se existir.
        *   Resolver conflitos: Adicionar opção "Metadata ComicInfo ? Database" e persistir essa escolha (SQLite/DataStore).
    *   [x] **Metadados de Capítulos (MangaDex):** Implementar busca (desativado por padrão, execução em background).
*   **Melhorias na UI/UX:**
    *   [x] **ChapterItem:** Reformular visual para ficar mais agradável.
    *   [x] **Busca de Capítulos:** Adicionar busca por número (`chapterSort`), nome do capítulo e nome do arquivo 
        (avaliar `SearchBar` do Material3).
    *   [ ] Redesenho da Barra Superior Principal para um visual mais clean (apple liquid glass) parecido porem com 
        thema mais dark.
*   **Background e Performance:**
    *   [x] **Notificações:** Transformar funções de sync demorado (ex: verificar arquivos existentes) em tarefas de background com notificação de progresso, permitindo sair do app.
*   **Fontes de Metadados Expandidas:**
    *   [ ] Opção para escolher **AniList** como uma fonte alternativa de metadados.
    *   [ ] Um painel de configurações para selecionar e configurar o provedor de metadados desejado (MangaDex/AniList).
*   **Rastreamento de Leitura:**
    *   [ ] Marcar capítulos como lidos/não lidos.
    *   [ ] Funcionalidade "Continuar Lendo" para pular rapidamente para o último capítulo lido.
*  **Adicionar funções e melhorar tela de Leitura**
    *   [ ] A interface já está bem agradável, será feito uma otimização e testes do código de interface.
    *   [ ] Será feito função de clique para trocar pagina nos modos paginados.

## Pilha de Tecnologia

*   **Linguagem:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Arquitetura:** MVVM (Model-View-ViewModel)
*   **Programação Assíncrona:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) e [Flow](https://kotlinlang.org/docs/flow.html)
*   **Banco de Dados:** [Room](https://developer.android.com/training/data-storage/room)
*   **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/)
*   **Injeção de Dependências:** Manual (via ViewModelFactories)
