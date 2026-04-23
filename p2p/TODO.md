### 1. Identidade Determinística (Além do PeerID Aleatório)

* **O que fazer:** Criar uma entidade de rede que derive chaves a partir de uma semente (seed) estável, permitindo que o `PeerId` seja recuperado em qualquer instalação.
* **Referência:** [`identity.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/utils/identity.rs#L61) (Uso de HKDF para derivar chaves) e [`device.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/domain/device.rs#L132) (Mapeamento de Hardware/OS para o ID).
* **Aplicação na Acerola:** Garante que o usuário do Android não perca sua identidade ao limpar o cache, mantendo a confiança entre os nós estável.

### 2. Persistência Segura (P2PStorage via Callback)

* **O que fazer:** Implementar uma interface de callback (Trait UniFFI) para delegar ao Android/Desktop o salvamento da `SecretKey`, do cache de endereços (`EndpointAddr`) e da lista de confiança (`Trusted/Blocked`).
* **Referência:** [`persistence.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/device/persistence.rs#L65).
* **Aplicação na Acerola:** Permite que o Android use o *EncryptedSharedPreferences* e o Desktop o *Keyring*, mantendo a lib Rust pura e independente de plataforma.

### 3. Protocolo de Bootstrap (Handshake de Aplicação)

* **O que fazer:** Transformar a primeira troca de mensagens (RPC) que atualmente só faz ping e pong em um processo de "apresentação", onde os nós trocam metadados (Nome, OS, Versão) logo após o bind do Iroh.
* **Referência:** [`messaging.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/protocol/messaging.rs#L6).
* **Aplicação na Acerola:** Assim que conectar, a Acerola já sabe quem é o peer e como mostrá-lo na UI, sem precisar de uma segunda fase de busca.

### 4. Camada de Segurança Robusta (Challenge-Response)

* **O que fazer:** Implementar um desafio criptográfico (Pilar A, B e C) onde um nó prova a posse da sua chave privada através de uma assinatura digital (estilo SSH).
* **Referência:** [`initiator.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/protocol/pairing/initiator.rs#L13).
* **Aplicação na Acerola:** Impede ataques de personificação (man-in-the-middle). O Iroh abre o cano, mas a Acerola só deixa passar quem provar o segredo compartilhado via HKDF.

### 5. Multiplexador de Protocolos/Bibliotecas

* **O que fazer:** Utilizar um único ALPN principal e um multiplexador interno para rotear mensagens para diferentes recursos (Discovery, Blobs, Sync).
* **Referência:** [`multiplexer.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/protocol/sync/multiplexer.rs#L97).
* **Aplicação na Acerola:** Reduz o overhead de rede no Android (menos handshakes QUIC) e permite que a lib gerencie múltiplos fluxos de dados (Chat, Arquivos, Sync) de forma isolada e organizada.

### 6. Loop de Eventos e Monitoramento (O Ouvinte)

* **O que fazer:** Centralizar a gestão de conexões, comandos e monitoramento de latência em um loop assíncrono robusto (Event Loop). Atualmente quem faz isso é o network.rs de forma bem simples.
* **Referência:** [`event_loop.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/core/event_loop.rs#L2).
* **Aplicação na Acerola:** O motor da lib que percebe quando uma conexão caiu e dispara automaticamente o "Auto-reconnect" baseado nos dados salvos no `P2PStorage`.

### 7. Interface baseada em Actions e Abstração de Conexão

* **O que fazer:** Expor a funcionalidade da rede através de `Actions` simples para a UI e usar canais `mpsc/oneshot` para troca de mensagens, eliminando a necessidade de gerenciar `PeerId` manualmente em todo o código.
* **Referência (Actions):** [`action.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/ops/network/pair/join/action.rs#L43).
* **Referência (Connection):** [`connection.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/device/connection.rs#L6).
* **Referência (Jobs/MPSC):** [`job_activity.rs`](https://github.com/spacedriveapp/spacedrive/blob/60369e9f00b5abe07f4518626bf36f6a7453476f/core/src/service/network/protocol/job_activity.rs#L17).
* **Aplicação na Acerola:** Transforma a biblioteca em um produto fácil de consumir. O desenvolvedor chama `join()` e a lib cuida do resto, reportando progresso via canais de job.

---

### 1. Identidade e Criptografia (O "Core")

**Requisitos Obrigatórios:**

* **Semente Estável:** A `SecretKey` do Iroh **deve** ser derivada via HKDF (SHA-256) a partir de uma semente de 32 bytes (Master Key).
* **UUID Determinístico:** O `DeviceId` da aplicação deve ser um UUID v5 ou v4 gerado a partir do hash (Blake3) da chave pública do nó.
* **Troca de Chaves (PFS):** O emparelhamento deve gerar chaves de sessão efêmeras (X25519) para cada par de dispositivos.
* **Assinatura de Desafio:** Todo novo nó tentando conectar deve assinar um `challenge` de 32 bytes gerado aleatoriamente pelo nó remoto.

**Cuidados:**

* **Nunca** armazene a semente ou a chave privada em texto claro (plain text).
* Use bibliotecas padrão (ex: `ed25519-dalek`, `x25519-dalek`) e evite implementações manuais de algoritmos.

---

### 2. Persistência e Integração de Plataforma (O "Storage")

**Requisitos Obrigatórios:**

* **Callback Interface:** A lib deve exportar uma `P2PStorage` (via UniFFI) que o Android/Desktop implementa.
* **Separação de Dados:**
  * *Vault:* Chaves de identidade e segredos de sessão (Devem ir para Android Keystore/macOS Keychain).
  * *Cache:* Endereços de rede e metadados de peers (Podem ir para SQLite/JSON).
* **Atomicidade:** O salvamento de um novo peer deve ser atômico; se a persistência falhar, a conexão deve ser encerrada por segurança.

**Cuidados:**

* No Android, prever que o `SharedPreferences` pode falhar se o armazenamento estiver cheio.
* As chaves de sessão devem ter um campo `expires_at` para limpeza automática de dados obsoletos.

---

### 3. Protocolos e Comunicação (O "Cérebro")

**Requisitos Obrigatórios:**

* **Multiplexador Único:** A lib deve registrar apenas um ALPN no Iroh (ex: `acerola/v1`).
* **Enquadramento (Framing):** Todas as mensagens devem ser prefixadas com 4 bytes (BigEndian u32) indicando o tamanho do payload.
* **Identificação Inicial:** A primeira stream aberta após o handshake **deve** ser o protocolo de identificação (Device Info).
* **Identificador de Recurso:** O envelope de mensagens deve conter um `resource_id` (UUID) para roteamento interno.

**Cuidados:**

* Limite o tamanho máximo de uma mensagem no multiplexador (ex: 10MB) para evitar ataques de estouro de memória (DoS).
* Use formatos binários (MessagePack) para mensagens frequentes (progresso/status) e JSON apenas para mensagens de configuração.

---

### 4. Loop de Eventos e Ciclo de Vida (A "Vida")

**Requisitos Obrigatórios:**

* **Single Source of Truth:** O estado de "quem está online" deve morar apenas no `DeviceRegistry` dentro do Event Loop.
* **Monitoramento Ativo:** O loop deve checar a latência (Iroh `latency()`) a cada 10-30 segundos.
* **Reconexão Exponencial:** Falhas de conexão devem disparar tentativas de reconexão com *Exponential Backoff* (1s, 2s, 4s, 8s...).
* **Shutdown Gracioso:** Ao fechar a lib, enviar uma mensagem "Goodbye" aos peers para que eles atualizem o status instantaneamente.

**Cuidados:**

* No Android, o loop deve lidar com a suspensão do processo. Ao "acordar", a lib deve revalidar todas as conexões imediatamente.
* Evite travar o loop de eventos com I/O de disco; delegue o salvamento para threads de background.

---

### 5. API de Ações (O "Produto")

**Requisitos Obrigatórios:**

* **Async/Await:** Todas as operações de rede expostas pela lib devem ser assíncronas.
* **Progress Reporting:** Ações de longa duração (como pareamento ou transferência) devem retornar um canal (mpsc) para reportar progresso à UI.
* **Tratamento de Erros:** Erros de rede devem ser tipados (`NetworkError`), distinguindo "Peer Offline" de "Falha de Autenticação".

**Cuidados:**

* Não exponha tipos internos do Iroh na API pública (UniFFI). Converta `PublicKey` para `String` e `EndpointAddr` para structs simples.

---

### 6. Requisitos de Rede (A "Infra")

**Requisitos Obrigatórios:**

* **Relay Customizado:** O `RelayMode` deve ser `Custom` com sua própria lista de URLs.
* **mDNS Ativo:** A descoberta local deve estar habilitada com um `service_name` único para a Acerola.
* **Fallback Automático:** Se a conexão direta falhar, o sistema deve alternar para o Relay sem derrubar a stream de aplicação.

**Cuidados:**

* O mDNS pode não funcionar em algumas redes corporativas ou hotéis; o Relay é o seu "seguro de vida".

---

### Tabela de Verificação de "Production Ready"

| Recurso | Critério de Aceite |
| :--- | :--- |
| **Identidade** | O PeerID é o mesmo após deletar e reinstalar o app (usando a mesma seed). |
| **Segurança** | Um dispositivo não pareado não consegue enviar mensagens de sistema. |
| **Persistência** | Peers descobertos via mDNS são lembrados após o restart do app. |
| **Performance** | O uso de CPU em standby é próximo de 0%. |
| **Resiliência** | O app alterna entre Wi-Fi e 4G sem precisar de intervenção manual do usuário. |
| **Android** | As chaves privadas estão protegidas pelo Android Keystore (Tee/Hardware). |

Esta spec garante que a **AcerolaP2p** não seja apenas uma camada sobre o Iroh, mas uma solução de rede completa, segura e pronta para escalar.
