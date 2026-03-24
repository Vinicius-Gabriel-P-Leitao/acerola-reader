# PLAN: Ações do botão ... de cada mangá

**Objetivo:** Implementar as actions nos cards de mangá da Home:
- **Bookmark** — atribuir/remover categoria ao mangá
- **Ocultar** — marcar `hidden = true` em `MangaDirectory`, filtrado da Home por padrão (futuro: botão de filtro mostra ocultos)
- **Deletar** — remove do DB (cascade) + apaga pasta no FS

---

## Contexto de arquitetura

- `MangaDirectory` entity: sem campo `hidden` ainda
- DB: dev → só bumpar versão, usuário deleta o banco manualmente
- `BaseDao` já tem `delete(entity)` → cascade limpa filhos
- `ManageCategoriesUseCase.updateMangaCategory(directoryId, categoryId?)` → bookmark já funciona
- `hideManga` e `deleteManga` **não são operações de sync** → ficam fora de `MangaSyncGateway`

---

## Tarefa 1 — Entity: adicionar campo `hidden`

**Arquivo:** `data/src/main/java/br/acerola/manga/local/entity/archive/MangaDirectory.kt`

Adicionar ao final da data class:
```kotlin
@ColumnInfo(name = "hidden", defaultValue = "0")
val hidden: Boolean = false,
```

Bumpar `version` em `AcerolaDatabase.kt`: `version = 2` → `version = 3`.

---

## Tarefa 2 — DAO: filtrar `hidden` + query de update

**Arquivo:** `data/src/main/java/br/acerola/manga/local/dao/archive/MangaDirectoryDao.kt`

Atualizar query de listagem para omitir ocultos por padrão:
```kotlin
@Query("SELECT * FROM manga_directory WHERE hidden = 0 ORDER BY id ASC")
fun getAllMangaDirectory(): Flow<List<MangaDirectory>>
```

Adicionar query de update (evita carregar entidade só para setar um campo):
```kotlin
@Query("UPDATE manga_directory SET hidden = :hidden WHERE id = :mangaId")
suspend fun setHidden(mangaId: Long, hidden: Boolean)
```

---

## Tarefa 3 — Gateway: nova interface `MangaLibraryWriteGateway`

**Arquivo:** `data/src/main/java/br/acerola/manga/adapter/contract/gateway/MangaGateway.kt`

Adicionar interface separada (não é sync):
```kotlin
interface MangaLibraryWriteGateway {
    suspend fun hideManga(mangaId: Long): Either<LibrarySyncError, Unit>
    suspend fun deleteManga(mangaId: Long): Either<LibrarySyncError, Unit>
}
```

`MangaGateway` não precisa estender essa interface — as implementações fazem isso diretamente.

---

## Tarefa 4 — Engine: implementar `MangaLibraryWriteGateway`

**Arquivo:** `data/src/main/java/br/acerola/manga/adapter/library/MangaDirectoryEngine.kt`

Adicionar à declaração da classe:
```kotlin
class MangaDirectoryEngine ... : MangaGateway<MangaDirectoryDto>, MangaLibraryWriteGateway {
```

Implementações:
```kotlin
override suspend fun hideManga(mangaId: Long): Either<LibrarySyncError, Unit> =
    withContext(Dispatchers.IO) {
        Either.catch {
            directoryDao.setHidden(mangaId, hidden = true)
        }.mapLeft { LibrarySyncError.UnexpectedError(it) }
    }

override suspend fun deleteManga(mangaId: Long): Either<LibrarySyncError, Unit> =
    withContext(Dispatchers.IO) {
        Either.catch {
            val directory = directoryDao.getMangaDirectoryById(mangaId) ?: return@catch
            // Apagar pasta no FS antes do DB (se falhar no FS, não remove do DB)
            val folderUri = directory.path.toUri()
            DocumentsContract.deleteDocument(context.contentResolver, folderUri)
            // Remove do DB (CASCADE limpa chapters, metadata, history, etc.)
            directoryDao.delete(directory)
        }.mapLeft { LibrarySyncError.UnexpectedError(it) }
    }
```

> **Nota:** `DocumentsContract.deleteDocument` requer que o URI seja um document URI (não tree URI). Se `path` for tree URI, usar `DocumentFile.fromTreeUri(context, uri)?.delete()`. Verificar o formato salvo durante implementação.

---

## Tarefa 5 — Use cases

**Novo:** `core/.../core/usecase/manga/HideMangaUseCase.kt`
```kotlin
class HideMangaUseCase @Inject constructor(
    @DirectoryEngine private val gateway: MangaLibraryWriteGateway,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> =
        gateway.hideManga(mangaId)
}
```

**Novo:** `core/.../core/usecase/manga/DeleteMangaUseCase.kt`
```kotlin
class DeleteMangaUseCase @Inject constructor(
    @DirectoryEngine private val gateway: MangaLibraryWriteGateway,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> =
        gateway.deleteManga(mangaId)
}
```

**Arquivo existente:** `core/.../core/usecase/DirectoryCaseModule.kt`

Adicionar `@Provides` para ambos, seguindo padrão dos use cases existentes no módulo.

> Verificar se `@DirectoryEngine` qualifier já está importado no módulo ou se precisa ser adicionado.

---

## Tarefa 6 — ViewModel

**Arquivo:** `ui/.../common/viewmodel/library/archive/MangaDirectoryViewModel.kt`

Injetar:
```kotlin
private val hideMangaUseCase: HideMangaUseCase,
private val deleteMangaUseCase: DeleteMangaUseCase,
private val manageCategoriesUseCase: ManageCategoriesUseCase,
```

Adicionar StateFlow de categorias:
```kotlin
val allCategories: StateFlow<List<CategoryDto>> = manageCategoriesUseCase.getAllCategories()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

Adicionar funções:
```kotlin
fun hideManga(mangaId: Long) {
    viewModelScope.launch {
        hideMangaUseCase(mangaId).onLeft { error ->
            _uiEvents.send(UserMessage.fromError(error))
        }
    }
}

fun deleteManga(mangaId: Long) {
    viewModelScope.launch {
        deleteMangaUseCase(mangaId).onLeft { error ->
            _uiEvents.send(UserMessage.fromError(error))
        }
    }
}

fun setMangaCategory(mangaId: Long, categoryId: Long?) {
    viewModelScope.launch {
        manageCategoriesUseCase.updateMangaCategory(mangaId, categoryId)
    }
}
```

> Verificar como `UserMessage` é construído a partir de `LibrarySyncError` no projeto — seguir padrão existente em outros métodos do ViewModel.

---

## Tarefa 7 — UI: `ModalBottomSheet` de ações

Em vez de `DropdownMenu` (pequeno, pouco tátil), usar **`ModalBottomSheet`** — padrão moderno do Material 3 para ações contextuais (Google Files, YouTube, etc.).

### 7a — Assinatura dos componentes

**`MangaGridItem.kt`** e **`MangaListItem.kt`**: adicionar parâmetros de callback:
```kotlin
onShowActions: () -> Unit,  // abre o sheet
```

O `IconButton(MoreHoriz)` passa a chamar `onShowActions()`.

### 7b — Sheet de ações (`MangaActionsSheet.kt`)

**Novo arquivo:** `ui/.../module/main/common/component/MangaActionsSheet.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main.Common.Component.MangaActionsSheet(
    manga: MangaDto,
    categories: List<CategoryDto>,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: (categoryId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Header com capa + título
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            AsyncImage(/* capa miniatura */)
            Column { Text(título, fontWeight = Bold); Text(subtítulo, bodySmall) }
        }
        HorizontalDivider()

        // Ação: Bookmark
        ListItem(
            leadingContent = { Icon(Icons.Rounded.Bookmark, ...) },
            headlineContent = { Text("Bookmark") },
            supportingContent = { Text(/* categoria atual ou "Sem bookmark" */) },
            modifier = Modifier.clickable { /* abre sub-sheet de categorias */ }
        )

        // Ação: Ocultar
        ListItem(
            leadingContent = { Icon(Icons.Rounded.VisibilityOff, ...) },
            headlineContent = { Text("Ocultar") },
            supportingContent = { Text("Esconde da biblioteca") },
            modifier = Modifier.clickable { onHide() }
        )

        // Ação: Deletar (destrutivo — cor error)
        ListItem(
            leadingContent = { Icon(Icons.Rounded.Delete, tint = error) },
            headlineContent = { Text("Deletar", color = error) },
            supportingContent = { Text("Remove do banco e do dispositivo", color = error) },
            modifier = Modifier.clickable { onDelete() }
        )

        Spacer(Modifier.navigationBarsPadding())
    }
}
```

### 7c — Sub-sheet de categorias (bookmark)

Ao clicar em Bookmark, mostrar um segundo `ModalBottomSheet` (ou substituir o conteúdo do sheet atual) com:
- Lista de categorias com `RadioButton` ou chip selecionável + cor
- Opção "Remover bookmark" se já tem categoria atribuída
- Botão confirmar

### 7d — Diálogos de confirmação

Ocultar e Deletar abrem `AlertDialog` de confirmação antes de executar.

**Deletar** — texto explícito sobre irreversibilidade + botão em `error` color.

**Ocultar** — texto simples sobre filtro.

---

## Tarefa 8 — Strings

**Arquivo:** `ui/src/main/res/values/strings.xml`

```xml
<string name="action_bookmark">Bookmark</string>
<string name="action_hide">Ocultar</string>
<string name="action_delete">Deletar</string>
<string name="action_cancel">Cancelar</string>
<string name="action_remove_bookmark">Remover bookmark</string>
<string name="label_no_bookmark">Sem bookmark</string>
<string name="description_hide">Esconde da biblioteca</string>
<string name="description_delete">Remove do banco e do dispositivo</string>
<string name="dialog_hide_title">Ocultar mangá?</string>
<string name="dialog_hide_message">Este mangá será ocultado da biblioteca. Você pode usar o filtro para ver mangás ocultos.</string>
<string name="dialog_delete_title">Deletar mangá?</string>
<string name="dialog_delete_message">Esta ação remove o mangá do banco de dados e apaga a pasta do dispositivo. Não pode ser desfeita.</string>
```

---

## Tarefa 9 — Conectar na HomeScreen

**Arquivo:** HomeScreen (localizar onde `MangaGridItem`/`MangaListItem` são chamados)

Adicionar estado local:
```kotlin
var selectedMangaForActions by remember { mutableStateOf<MangaDto?>(null) }
var confirmHide by remember { mutableStateOf<Long?>(null) }
var confirmDelete by remember { mutableStateOf<Long?>(null) }
```

Nos callsites dos componentes:
```kotlin
onShowActions = { selectedMangaForActions = manga }
```

Renderizar o sheet e diálogos condicionalmente no corpo da Screen.

---

## Ordem de execução

1. Tarefa 1 — entity + DB version
2. Tarefa 2 — DAO
3. Tarefa 3 — nova interface
4. Tarefa 4 — engine
5. Tarefa 5 — use cases + DI
6. Tarefa 6 — ViewModel
7. Tarefa 8 — strings
8. Tarefa 7 — Sheet de ações
9. Tarefa 9 — conectar na HomeScreen

Compilar após Tarefa 6 (dados prontos) e novamente após Tarefa 9 (UI completa).
