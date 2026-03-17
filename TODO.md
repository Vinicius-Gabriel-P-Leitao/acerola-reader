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

## Status das Funcionalidades

Este projeto está em desenvolvimento ativo. Abaixo está uma lista das funcionalidades atualmente implementadas e o que
está planejado para o futuro.

### ✅ Implementado

*   [x] **Escaneamento da Biblioteca:** Escaneia as pastas especificadas para detectar mangás e capítulos, persistindo a
    estrutura em um banco de dados local.
*   [x] **Sincronização da Biblioteca:** Mantém o banco de dados local sincronizado com o sistema de arquivos,
    detectando novas adições, renomeações e exclusões.
*   [x] **Integração com MangaDex:** Busca automaticamente metadados de mangás do MangaDex com base nos nomes das
    pastas.
*   [x] **Tela Inicial (Home Screen):** Exibe todos os mangás da sua biblioteca com duas opções de layout: Lista e
    Grade.
*   [x] **Tela de Capítulos:** Mostra uma lista de todos os capítulos de um mangá selecionado, juntamente com seus
    metadados detalhados.

### 🛠️ FIXME (Correções Prioritárias)

*   [x] **Bug no Primeiro Scan:** Verificar por que no primeiro scan de mangás os capítulos não estão sendo mapeados.
*   [x] **Performance:** Reformular o `loadPage` da seção de capítulos na tela de detalhes do mangá.

### 🚧 Planejado / Em Andamento

* **Leitor de Mangás Integrado:**
    *   [x] Suporte completo para formatos `.cbz` e `.cbr`. 
* **Configuração e Gerenciamento do Mangá (Refatoração):**
    *   [X] **Sync Individual:** Sincronizar apenas um mangá (MangaDex + Filesystem) buscando novos arquivos.
    *   [x] **Paginação:** Configuração para alterar a quantidade de capítulos por página.
    *   [X] **Gerenciamento de Imagens:** Carregar, trocar, salvar e remover capa (`cover.jpg`/`.png`) e banner. Se o
        banner for removido, a capa assume o lugar.
    *   [x] **Informações de Armazenamento:** Exibir tamanho do mangá em GB ou MB.
    *   [x] **Ações de Limpeza (com confirmação):**
        * Limpar metadados (remove do DB e apaga `ComicInfo.xml`).
        * Limpar capítulos (remove todos os arquivos e dados).
* **Configurações Globais:**
    *   [x] **Gerenciamento de Metadados (ComicInfo.xml vs DB):**
        * Gerar `ComicInfo.xml` por padrão; ler se existir.
        * Resolver conflitos: Adicionar opção "Metadata ComicInfo ? Database" e persistir essa escolha (
          SQLite/DataStore).
    *   [x] **Metadados de Capítulos (MangaDex):** Implementar busca (desativado por padrão, execução em background).
* **Melhorias na UI/UX:**
    *   [x] **ChapterItem:** Reformular visual para ficar mais agradável.
    *   [x] **Busca de Capítulos:** Adicionar busca por número (`chapterSort`), nome do capítulo e nome do arquivo
        (avaliar `SearchBar` do Material3).
    *   [x] Redesenho da Barra Superior Principal para um visual mais clean (apple liquid glass) parecido porém com
        tema mais dark.
* **Background e Performance:**
    *   [x] **Notificações:** Transformar funções de sync demorado (ex: verificar arquivos existentes) em tarefas de
        background com notificação de progresso, permitindo sair do app.
* **Fontes de Metadados Expandidas:**
    *   [ ] Opção para escolher **AniList** como uma fonte alternativa de metadados.
    *   [ ] Um painel de configurações para selecionar e configurar o provedor de metadados desejado (MangaDex/AniList).
* **Rastreamento de Leitura:**
    *   [x] Marcar capítulos como lidos/não lidos.
    *   [x] Funcionalidade "Continuar Lendo" para pular rapidamente para o último capítulo lido.
* **Adicionar funções e melhorar tela de Leitura:**
    *   [x] A interface já está bem agradável, será feito uma otimização e testes do código de interface.
    *   [x] Será feito função de clique para trocar página nos modos paginados.

---

* **Download de Traduções via MangaDex:**
    *   [x] **Busca de tradução:** Localizar capítulos traduzidos por nome do mangá, ID do MangaDex ou URL do mangá.
    *   [x] **Seleção de idioma:** Permitir ao usuário escolher o idioma da tradução antes de baixar.
    *   [x] **Download como `.cbz`:** Baixar as páginas do capítulo e empacotar automaticamente em arquivo `.cbz` na
        pasta do mangá correspondente.
    *   [x] **Fila de download:** Gerenciar múltiplos downloads simultâneos com progresso individual por capítulo
        (tarefa de background com notificação).
    *   [x] **Integração com biblioteca:** Após o download, acionar sync automático para que o capítulo apareça na
        biblioteca sem intervenção manual.
    *   [x] **Melhorar visualização:** Melhorar visualização de que o mangá está sendo baixado e mostrar a fila de 
        chapters já baixados.
* **Filtros e Ordenação da Biblioteca (Home Screen):**
    *   [ ] **Ordenação:** Suporte a Asc/Desc para os critérios abaixo:
        * Título (A–Z / Z–A).
        * Quantidade de capítulos (menor → maior / maior → menor).
        * Última atualização / `lastUpdate` (mais recente → mais antigo / mais antigo → mais recente).
    *   [ ] **Persistência de preferência:** Salvar a ordenação escolhida via DataStore para manter entre sessões.
    *   [ ] **UI do filtro:** Usar o icone já na tela de HOME, não colocar label só icone mesmo.
* **Filtros e Ordenação da Tela de Capítulos:**
    *   [ ] **Tirar autoscroll** Remover autoscroll da tela de chapter quando troco a paginação, deixar o usuário
        clicar em trocar página e não quebrar
    *   [ ] **Ordenação:** Suporte a Asc/Desc para os critérios abaixo:
        * Número do capítulo (`chapterSort`) — crescente / decrescente.
        * Última atualização / `lastUpdate` (mais recente → mais antigo / mais antigo → mais recente).
    *   [ ] **Persistência de preferência:** Salvar a ordenação por mangá (ou global) via DataStore.
    *   [ ] **UI do filtro:** Ícone de ordenação na barra superior da tela de capítulos, abrindo bottom sheet
        consistente com o da Home.
* **Implementar mais logs:**
    *   [x] **Implementar uma classe de logs ou função de faz um wrapper para o Log.** Essa classe ou função tem
        de padronizar os logs do projeto para que fique fácil de buscar e filtrar
* **Adiciona função de trocar thema:**
    *   [x] **Thema adaptável** Isso já é default do android então só dataStore para salvar preferencia.
    *   [x] **Thema default vai ser o cattpuccin** Deixar ele default e melhorar cores para thema branco.
* **Corrigir bug de notificação para todos os sync**
   *    [x] **Corrigir o sync para métadados e outras funções na config geral** Sync do mangadex geral não gera 
        notifição e deixa com o app rodando em background, verificar se existe outro que tem o mesmo problema. 
* **Adicionar novos themas, Nord e Dracula**
    *    [x] **Adicionar Dracula e Alucard** Dracula vai ser o dark e o Alucard o claro
    *    [x] **Adicionar o Nord** Verificar como o nord pode ser aplicado em claro e escuro

## Pilha de Tecnologia

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Programação Assíncrona:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) e [Flow](https://kotlinlang.org/docs/flow.html)
* **Banco de Dados:** [Room](https://developer.android.com/training/data-storage/room)
* **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/)
* **Injeção de Dependências:** Manual (via ViewModelFactories)