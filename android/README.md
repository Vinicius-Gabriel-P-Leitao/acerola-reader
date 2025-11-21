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

### 🚧 Planejado / Em Andamento

*   **Leitor de Mangás Integrado:**
    *   [ ] Suporte completo para formatos `.cbz` e `.cbr`.
*   **Fontes de Metadados Expandidas:**
    *   [ ] Opção para escolher **AniList** como uma fonte alternativa de metadados.
    *   [ ] Um painel de configurações para selecionar e configurar o provedor de metadados desejado (MangaDex/AniList).
*   **Configuração Aprimorada:**
    *   [ ] Configurações avançadas relacionadas a arquivos.
    *   [ ] Opções de personalização da interface do usuário.
*   **Melhorias na UI/UX:**
    *   [ ] Opção para uma "UI Limpa" (minimalista) vs. "UI Rica em Funcionalidades".
    *   [ ] Redesenho da Barra Superior Principal para um visual mais clean.
*   **Rastreamento de Leitura:**
    *   [ ] Marcar capítulos como lidos/não lidos.
    *   [ ] Funcionalidade "Continuar Lendo" para pular rapidamente para o último capítulo lido.

## Pilha de Tecnologia

*   **Linguagem:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Arquitetura:** MVVM (Model-View-ViewModel)
*   **Programação Assíncrona:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) e [Flow](https://kotlinlang.org/docs/flow.html)
*   **Banco de Dados:** [Room](https://developer.android.com/training/data-storage/room)
*   **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/)
*   **Injeção de Dependências:** Manual (via ViewModelFactories)
