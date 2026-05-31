# Acerola Desktop - Próximos Passos

Este documento descreve as tarefas pendentes e os objetivos para o desenvolvimento do Acerola.

## 🛠️ Manutenção e Performance (Imediato)

- [ ] **Code Review Cosmético**: Revisar o código para identificar problemas comuns de estilo, consistência de nomes e organização de módulos (principalmente após as refatorações de IPC).
- [ ] **Suporte ao LRU (Cache)**: Implementar um mecanismo de Cache LRU no frontend (Svelte) para o scroll infinito. O objetivo é manter apenas as páginas de capítulos próximas à visualização atual na memória, otimizando o consumo de RAM.
- [ ] **Refinamento de UI/UX**: Melhorar os estados de transição entre visualizações e polir as animações de expansão de volumes.

## 🚀 Acerola P2P & Device Identity

### 1. Derivação de Identidade P2P (Seed do Dispositivo)
Vincular de forma persistente a identidade do nó (Peer ID) ao hardware do dispositivo.

- [ ] **Identificação Única do Hardware**: Integrar método para obter um ID único (ex: `machine-id`, UUID da placa-mãe ou serial do disco).
- [ ] **Derivação de Seed**: Implementar função para transformar o ID do hardware em um seed de 32 bytes (SHA-256).
- [ ] **Integração no Builder**: Atualizar o `IrohTransportBuilder` para utilizar esse seed na geração do `SecretKey`.
- [ ] **Persistência**: Garantir estabilidade do Peer ID após reinstalações.

### 2. Refinamento da Integração `acerola-p2p`
Melhorar a robustez da camada de rede.

- [ ] **Tratamento de Erros**: Mapear erros internos do `acerola-p2p` para o `AppError` do Tauri.
- [ ] **Métricas em Tempo Real**: Expor eventos de status de rede e latência via `EventEmitter`.
- [ ] **Gestão de Estado**: Sincronizar estados de conectividade entre o `NetworkService` e o Svelte.

### 3. Configuração de Rede
- [ ] **Relay Customizado**: Permitir configuração de URLs de relay personalizadas.
- [ ] **Auto-Discovery**: Melhorar a detecção de peers na rede local (mDNS).
