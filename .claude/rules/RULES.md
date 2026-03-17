Aqui está o arquivo `RULES.md` atualizado, incluindo a seção de organização por Namespaces que você definiu para os módulos de UI.

---

# 📜 Regras de Desenvolvimento e Boas Práticas (Android Jetpack Compose)

Este documento define as diretrizes obrigatórias para a escrita de código, arquitetura e interface neste projeto. O objetivo é garantir consistência, performance e manutenibilidade.

---

## 1. Importações e Sintaxe
### 🚫 Sem Importações Ambíguas
*   **Nunca** utilizar importações com wildcard (ex: `import kotlinx.coroutines.*`).
*   Cada classe, função ou componente deve ser importado individualmente para facilitar a rastreabilidade e evitar conflitos.

### 🛠️ Uso de Extension Functions
*   **Prioridade:** Sempre que uma lógica puder ser encapsulada fora da classe principal, use `Extension Functions`.
*   **Camada de Apresentação (Presenter):** Utilize extensões para transformar modelos ou formatar dados específicos da View (ex: `User.toUiState()`).
*   **Clean Code:** Evite classes "Utils" genéricas; prefira extensões em tipos específicos.

---

## 2. Interface e Design (Material 3)
### 🎨 Material Design 3 (M3)
*   **Obrigatório:** Todo o sistema de cores, tipografia e componentes deve derivar do `androidx.compose.material3`.
*   **Tematização:** Use sempre `MaterialTheme.colorScheme`, `MaterialTheme.typography` e `MaterialTheme.shapes`.
*   **Componentes:** Utilize apenas versões oficiais do M3 (ex: `Scaffold`, `CenterAlignedTopAppBar`, `Button`).

### 📱 Jetpack Compose Best Practices
*   **Zero Depreciação:** É estritamente proibido o uso de funções ou libs marcadas como `@Deprecated`.
*   **State Hoisting:** Mantenha o estado no ViewModel e passe apenas dados imutáveis e lambdas de eventos para os Composables.

---

## 3. Organização de UI com Namespaces
Para organizar componentes e layouts, utilizamos uma estrutura de `objects` aninhados. Isso evita poluição do escopo global e facilita a autodescoberta de componentes via IDE.

### 🏗️ Estrutura de Módulos (Namespace)
Todo componente ou layout de UI deve ser colocado dentro de seu respectivo escopo seguindo o padrão abaixo:

```kotlin
package br.acerola.manga.module.main

/**
 * Namespace para componentes e layouts específicos do módulo de Início (Home).
 */
object Main {
    object Common {
        object Component
    }
    object Config {
        object Component
        object Layout
    }
    object History {
        object Component
        object Layout
    }
    object Home {
        object Component
        object Layout
    }
}
```

### 💡 Como aplicar:
Ao criar um Composable para a Home, ele deve ser uma extensão do namespace correspondente:

```kotlin
// Exemplo de uso
@Composable
fun Main.Home.Layout.HomeScreen() { 
    /* implementação */ 
}

@Composable
fun Main.Home.Component.UserAvatar() { 
    /* implementação */ 
}
```

---

## 4. Exemplo de Padrão de Código Consolidado

```kotlin
// ✅ Correto: Imports específicos
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.acerola.manga.module.main.Main

// ✅ Correto: Extension Function para Presenter
fun String.toTitleCase(): String = this.lowercase().replaceFirstChar { it.uppercase() }

// ✅ Correto: Uso de Namespace e Material 3
@Composable
fun Main.Home.Component.Header(title: String) {
    Text(
        text = title.toTitleCase(),
        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
    )
}
```

---

## ✅ Checklist para a IA
1.  [ ] **Imports:** Verificou se não existem imports `.*`?
2.  [ ] **Extensions:** A lógica de formatação/apresentação está em uma Extension Function?
3.  [ ] **M3:** Todos os componentes e temas pertencem ao Material 3?
4.  [ ] **Depreciação:** Verificou se as libs e funções são as mais recentes e não depreciadas?
5.  [ ] **Namespace:** O componente foi declarado como extensão de um objeto dentro de `Main` (ex: `Main.Home.Layout`)?