# Acerola P2P Architecture - TODO & Roadmap

Este documento centraliza o planejamento arquitetural da rede P2P do Acerola (PC <-> Mobile) utilizando Iroh, delineando as features que serão construídas no core em Rust, os motivos das escolhas tecnológicas e o fluxo de integração futura com o Android.

---

## 🗂️ Estrutura de Pastas

```
src-tauri/src/
│
├── cmd/                          ← Camada Tauri. Só fala com o frontend via IPC.
│   ├── events/                      payloads de resposta
│   └── features/
│       ├── home/
│       ├── library/
│       └── network/              ← comandos Tauri de rede (get_node_id, connect_to...)
│
├── core/                         ← Lógica pura. Não sabe que Tauri existe.
│   ├── services/                    engines de negócio existentes
│   └── connection/peer/           ← contratos da conexão P2P (traits/ports)
│       ├── transport.rs             trait P2PTransport
│       ├── guard.rs                 trait ConnectionGuard
│       └── handlers/                traits dos handlers de protocolo
│           ├── blobs.rs
│           ├── graphql.rs
│           └── rpc.rs
│
├── data/                         ← Camada de dados. Local + Remote.
│   ├── models/
│   ├── repositories/                SQLite
│   └── remote/                   ← adapters que implementam os contratos do core
│       ├── iroh_transport.rs        IrohTransport impl de P2PTransport
│       ├── mangadex.rs              (futuro)
│       └── anilist.rs               (futuro)
│
├── infra/                        ← Utilitários transversais sem lógica de negócio.
│   ├── db/
│   ├── error/
│   ├── filesystem/                  guards de filesystem existentes
│   └── remote/                   ← guards transversais para conexões
│       ├── peer_guard.rs            OpenGuard, TokenGuard
│       └── rate_guard.rs            throttle/timeout p/ APIs externas (futuro)
│
└── lib.rs                        ← Composition root. Único lugar que conhece
                                     core/ e data/remote/ ao mesmo tempo.
                                     Instancia IrohTransport, injeta via trait
                                     no NetworkManager e registra no tauri::State.
```

### Fluxo de dependência

```
cmd → core/connection  (depende do trait, nunca da impl)
           ↑
      lib.rs injeta
           ↓
    data/remote/iroh_transport  (implementa o trait)

infra/remote/peer_guard  →  usado por core/connection no momento do accept
```

**Regra:** `core/` nunca importa de `data/` diretamente. A inversão é feita pelo `lib.rs`.

---

## 🛠️ Fase 1: Implementação no Core Rust (Tauri / PC)

O objetivo desta fase é criar o núcleo de rede no PC, mantendo o código isolado em `src-tauri/src/core/network` para que a dependência do Tauri seja apenas uma casca de injeção.

- [ ] **Isolar o `NetworkManager`:** Criar uma struct pura em Rust que gerencie o `Endpoint` do Iroh, independente do `tauri::State`. O `lib.rs` só injeta dependências.

- [ ] **Abstrair o transporte via trait `P2PTransport`:**
  - O `NetworkManager` e os handlers dependem do trait, não do Iroh diretamente.
  - Permite trocar Iroh por `quinn` puro ou `libp2p` sem tocar nos handlers.
  ```rust
  trait P2PTransport: Send + Sync {
      async fn send_blob(&self, peer: NodeId, hash: Hash) -> Result<()>;
      async fn open_stream(&self, peer: NodeId, alpn: &[u8]) -> Result<BiStream>;
  }
  ```

- [ ] **Configurar o `iroh::protocol::Router`:** Substituir o loop simples de `accept` pelo Router oficial do Iroh para multiplexar conexões QUIC por ALPN.

- [ ] **Implementar o `ConnectionGuard` (Auth Modular):**
  - Trait que senta entre o Router e os handlers. O Router chama `guard.authorize()` antes de despachar qualquer conexão.
  - Dois modos trocáveis na inicialização, sem alterar os handlers:
    - `OpenGuard` — sempre autoriza (desenvolvimento / rede local confiável).
    - `TokenGuard` — valida `NodeId` remoto + token de sessão mantido em banco.
  ```rust
  trait ConnectionGuard: Send + Sync {
      async fn authorize(&self, node_id: NodeId, alpn: &[u8]) -> Result<(), AuthError>;
  }
  ```

- [ ] **Implementar `BlobsHandler` — dois fluxos distintos:**
  - **Push/Pull de arquivos completos (CBZ/CBR):** Usar o protocolo nativo do `iroh-blobs` (ALPN próprio, verificação por chunks BLAKE3). Não requer handler customizado.
  - **Streaming sob demanda (leitor):** Dado um CBZ e um range de páginas, extrair em memória, salvar no `MemStore` e retornar os Hashes BLAKE3. O Iroh serve os bytes via ALPN de blobs.
  - **Eviction no `MemStore` (PC):** Política LRU espelhando o lado Android (janela de ~7 páginas). Entradas antigas são removidas do `MemStore` quando a janela avança, evitando crescimento ilimitado de memória em sessões longas.

- [ ] **Implementar `GraphQLHandler` (Dados Estruturados & Sync):**
  - ALPN customizado: `b"acerola/gql"`.
  - Receber Strings JSON do P2P, repassar para a engine do `async-graphql` e devolver a String JSON de resposta via stream bidirecional.
  - Atenção: `async-graphql` espera contexto HTTP em alguns middlewares — o contexto de autenticação/sessão precisa ser injetado manualmente no executor, sem camada HTTP.

- [ ] **Implementar `RpcWebhookHandler` (Triggers & Comandos Rápidos):**
  - ALPN customizado: `b"acerola/rpc"`.
  - Usado para comandos leves: "Sincronize o histórico", "Pausar leitura", "Avançar página".
  - Comunicação via `serde_json` ou `bincode` sobre streams QUIC fragmentadas (`LinesCodec` / `FramedRead`).

- [ ] **Configurar Relay próprio:**
  - Usar o binário `iroh-relay` auto-hospedado em vez dos servidores públicos da n0.
  - Configurar via `Endpoint::builder().relay_url(sua_url)` na inicialização do `NetworkManager`.
  - O relay só enxerga metadados de conexão (NodeIds, timestamps). O conteúdo é E2E via QUIC/NOISE.
  - Conexões LAN diretas continuam funcionando mesmo se o relay cair.

---

## 🧠 Por que essas decisões?

1. **Dois fluxos de blob (Push/Pull vs Streaming):** A transferência de arquivo completo (sincronização de biblioteca) e a entrega de páginas durante a leitura são casos de uso com requisitos opostos. O protocolo nativo do `iroh-blobs` é otimizado para arquivos grandes e verificação de integridade. O `MemStore` com LRU é otimizado para latência baixa em leitura sequencial.

2. **LRU espelhado (PC + Android):** O Android mantém uma janela de 7 páginas em `LruCache`. O PC espelha essa janela no `MemStore` para não acumular páginas que o cliente já descartou. O GC da JVM cuida do lado Kotlin; a eviction manual cuida do lado Rust.

3. **`ConnectionGuard` como módulo, não como feature futura:** Retrofitar auth em handlers já construídos quebra assinaturas. Construir o trait desde o início com `OpenGuard` como implementação padrão tem custo zero e deixa o caminho aberto para `TokenGuard` sem refatoração.

4. **Separação de Protocolos (GraphQL vs RPC):** GraphQL é excelente para manter tipos e sincronizar o histórico (Apollo Client com Normalized Cache). RPC é cru e rápido para coordenação em tempo real sem o overhead do parser GraphQL.

5. **Core Agnóstico (Write Once, Run Anywhere):** A lógica de rede em Rust puro compila para Windows/Linux/Mac (Tauri) e para Android (JNI/ARM64). O `crate-type = ["staticlib", "cdylib", "rlib"]` já está configurado. O `lib.rs` vira apenas injeção de dependências e rotas.

6. **Relay próprio vs n0 público:** Metadados de conexão (NodeIds) não trafegam por infraestrutura de terceiros. Relay só é necessário para NAT traversal — conexões LAN não passam por ele.

---

## ⚠️ Riscos e Estratégia de Migração

**Risco principal: instabilidade de API pré-1.0 do Iroh.**
O ecossistema Iroh está em versões 0.9x com breaking changes frequentes entre releases. O risco não é descontinuação, é ficar travado esperando compatibilidade entre crates (`iroh`, `iroh-blobs`, etc.).

**Estratégia:** O trait `P2PTransport` isola os handlers do Iroh. Se a API travar ou quebrar demais, as opções de migração em ordem de esforço são:

| Alternativa | O que muda | O que fica igual |
|---|---|---|
| `quinn` direto | `NetworkManager` + relay custom (~200 linhas) | Todos os handlers, o trait, a lógica de blobs |
| `libp2p` | `NetworkManager` + reaprender API | Todos os handlers, o trait |

**WebRTC foi descartado:** Ecossistema Rust mal mantido (`webrtc-rs`), overhead de SDP/ICE desnecessário para nativo-a-nativo, e modelo de programação incompatível com o que foi planejado.

**Versioning:** Pinar as versões do Iroh quando a Fase 1 estiver estável. Não atualizar sem ler changelog completo e testar a compilação para Android (ARM64).

---

## 📱 Fase 2: Integração Mobile (Android / Kotlin)

Quando o Core Rust estiver maduro e testado no Desktop, ele será envelopado para Android. A regra de ouro: **O Rust é "burro e rápido" (transporta bytes e criptografia), o Kotlin é inteligente (UI, Cache e Ciclo de Vida).**

### Escopo e limites definidos:

- **PC = servidor, Mobile = cliente.** A inversão de papéis (mobile servindo para outro mobile) foi descartada: exigiria um relay com conhecimento de domínio para rotear mobile → relay → mobile, adicionando uma terceira camada de complexidade sem benefício prático dado as limitações de background do Android.
- **Offline no mobile:** Sem PC na rede, o mobile fica apenas conectado ao relay. Nenhuma operação de leitura ou sync é iniciada. O progresso de leitura em andamento é salvo como estado "pausado".

### Como vai funcionar do lado Android:

1. **JNI / FFI Bridge Limpa:**
   - Inspirado na arquitetura do *Spacedrive*, usar UniFFI ou JNI manual focado no padrão **JSON-RPC**.
   - O Rust no Mobile só expõe métodos de enviar/receber strings e inicializar o Iroh Node em background. Structs de negócios não são espelhadas para o C-ABI.

2. **Workers e Coroutines:**
   - A chamada JNI rodará dentro de um `CoroutineScope(Dispatchers.IO)` atrelado a um Foreground Service/Worker.
   - A Thread Principal da UI nunca é bloqueada pelas rotinas P2P. Se o processo cair, a coroutine morre de forma controlada.

3. **Injeção no Apollo GraphQL:**
   - Sem proxy `localhost`.
   - `IrohNetworkTransport` customizado implementando a interface nativa do **Apollo Kotlin**.
   - Fluxo: Apollo gera a query → string enviada via JNI → Rust envia via QUIC ao PC → PC responde via QUIC → JNI devolve JSON ao Kotlin → Apollo popula o Normalized Cache.
   - Requer implementação de cancelamento de request (coroutine cancellation → JNI cancel → Rust aborta o stream QUIC).

4. **Cache LRU Mobile:**
   - Gestão de memória (janela de 7 páginas) e expurgo de Hashes feita inteiramente no Kotlin via `LruCache` nativas.
   - O GC da JVM cuida da limpeza; o Rust não gerencia memória da interface do usuário.
