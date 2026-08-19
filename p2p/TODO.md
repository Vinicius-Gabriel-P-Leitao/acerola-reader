# AcerolaP2P — TODO: Interface de Configuração e Troca Dinâmica de Relay

Este documento detalha o planejamento para expor uma API intuitiva que permite aos aplicativos (Desktop, Android, CLI, etc.) configurar e alternar entre **4 modos de relay**, permitindo a construção de interfaces visuais e controle total da conectividade pelo usuário final.

---

## 🎯 Visão Geral dos 4 Modos de Relay

A camada de rede do Acerola deve expor um enum/interface de alto nível representando as 4 opções:

1. **Relay Oficial do Acerola (`RelayModeConfig::AcerolaDefault`)**:
   - Utiliza os servidores de relay oficiais mantidos pelo ecossistema Acerola.
   - Ideal para uso *out-of-the-box* pelos usuários finais dos apps.

2. **Relay Self-Hosted / Próprio (`RelayModeConfig::Custom(String)`)**:
   - Permite ao usuário informar uma URL HTTPS customizada de seu próprio servidor de relay Iroh (ex: `https://meu-relay.exemplo.com`).
   - Garante privacidade e autonomia total de infraestrutura para usuários avançados ou corporativos.

3. **Relay Público do Iroh (`RelayModeConfig::IrohDefault`)**:
   - Utiliza a infraestrutura pública global padrão do projeto Iroh (`iroh::RelayMode::Default`).
   - Opção de fallback ou uso com a rede pública aberta do ecossistema Iroh.

4. **Apenas mDNS / Rede Local (`RelayModeConfig::MdnsOnly`)**:
   - Desativa completamente o tráfego e discagem por relays remotos (`iroh::RelayMode::Disabled`).
   - Opera exclusivamente na rede local (LAN) via mDNS (zero saída para a internet, ideal para modo offline / alta privacidade).

---

## 📋 Tarefas de Implementação

### 1. Definição dos Tipos e Abstrações da API (`api/network/` e `core/transport/`)

- [ ] **Criar enum de alto nível `RelayModeConfig`** no módulo `api::network`:
  ```rust
  #[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
  pub enum RelayModeConfig {
      /// Relay oficial gerenciado pelo projeto Acerola
      AcerolaDefault,
      /// Servidor de relay próprio informado pelo usuário
      Custom(String),
      /// Servidores públicos padrão da rede Iroh
      IrohDefault,
      /// Sem relay remoto — apenas descoberta mDNS local (offline/LAN)
      MdnsOnly,
  }
  ```
- [ ] **Adicionar constante com a URL do Relay Oficial do Acerola** (ex: `const ACEROLA_DEFAULT_RELAY_URL: &str = "...";`).
- [ ] **Implementar método de conversão/resolução** que transforma `RelayModeConfig` na configuração concreta correspondente do Iroh (`iroh::RelayMode`).
- [ ] **Validar formato de URL** para o modo `Custom(String)` com erro descritivo (`ConnectionError::StartupFailed("invalid relay URL")`).

### 2. Configuração Inicial no Builder (`AcerolaP2pBuilder` & `IrohTransportBuilder`)

- [ ] **Adicionar método `.relay_mode(RelayModeConfig)` no `IrohTransportBuilder`** e na trait `TransportP2pBuilder`.
- [ ] **Adicionar método `.relay_mode(RelayModeConfig)` diretamente no `AcerolaP2pBuilder`** para acesso direto via fachada pública.
- [ ] **Persistência da escolha do usuário**:
  - Salvar a preferência do modo de relay no `P2PStorage` (se configurado).
  - Na inicialização, carregar o último modo selecionado pelo usuário como padrão.

### 3. Troca Dinâmica em Tempo de Execução (`AcerolaP2p` & `NetworkManager`)

- [ ] **Adicionar comando `NetworkCommand::SwitchRelay`** no `NetworkManager`:
  ```rust
  NetworkCommand::SwitchRelay {
      mode: RelayModeConfig,
      response: tokio::sync::oneshot::Sender<Result<(), ConnectionError>>,
  }
  ```
- [ ] **Implementar método público assíncrono `node.switch_relay(mode: RelayModeConfig).await`** no `AcerolaP2p`:
  - Envia o comando para o `NetworkManager`.
  - Atualiza o `Endpoint` do Iroh / reconecta ou reconfigura o `RelayMap` dinamicamente.
  - Mantém intactos o `PeerId`, chave mestra e tabela de peers conhecidos (`known_peers`).
- [ ] **Emitir evento via `EventEmitter`**:
  - Disparar `"network:relay_changed"` contendo o novo modo em JSON para que as interfaces gráficas (Tauri/Flutter/Kotlin) atualizem o status visual instantaneamente.

### 4. Testes Automatizados e Validação

- [ ] **Testes unitários dos 4 modos de relay** no `IrohTransportBuilder`:
  - `AcerolaDefault`: inicializa com a URL oficial.
  - `Custom`: aceita URL válida e rejeita URL malformada com erro.
  - `IrohDefault`: inicializa com o modo padrão do Iroh.
  - `MdnsOnly`: inicializa com `RelayMode::Disabled`.
- [ ] **Teste de integração da troca dinâmica (`switch_relay`)**:
  - Iniciar nó em `MdnsOnly` e chavear em runtime para `AcerolaDefault` / `Custom`.
  - Verificar que o evento `"network:relay_changed"` é emitido.
  - Garantir que a identidade do nó e peers conhecidos sobrevivem à troca.
- [ ] **Teste de persistência de preferência de relay no `P2PStorage`**.

### 5. Documentação e Exemplos de Integração com UI

- [ ] **Atualizar o `README.md`**:
  - Adicionar seção dedicada explicando os 4 modos e como construir um componente de seleção visual de Relay (Dropdown / Radio buttons).
  - Incluir exemplo de código consumindo `node.switch_relay(...)` e escutando `"network:relay_changed"`.
