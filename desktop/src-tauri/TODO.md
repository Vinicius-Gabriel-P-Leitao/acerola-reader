# Acerola P2P & Device Identity

Este documento descreve as tarefas para aprimorar a integração com a biblioteca `acerola-p2p` e implementar a persistência de identidade baseada no hardware.

## 🚀 Próximos Passos

### 1. Derivação de Identidade P2P (Seed do Dispositivo)
Atualmente, a identidade do nó (Peer ID) não está vinculada de forma persistente ao dispositivo de maneira determinística via hardware. O objetivo é remover o uso de bytes hardcoded ou geração aleatória e usar dados do sistema.

- [ ] **Identificação Única do Hardware**: Pesquisar e integrar uma crate ou método para obter um ID único (ex: `machine-id`, UUID da placa-mãe ou serial do disco).
- [ ] **Derivação de Seed**: Implementar uma função que transforme o ID do dispositivo em um seed de 32 bytes (utilizando SHA-256 ou similar).
- [ ] **Integração no Builder**: Atualizar o `IrohTransportBuilder` (ou o fluxo de construção do nó) para aceitar esse seed, garantindo que o `SecretKey` do Iroh seja derivado dele.
- [ ] **Persistência**: Garantir que, ao reinstalar o app no mesmo computador, o Peer ID permaneça o mesmo.

### 2. Refinamento da Integração `acerola-p2p`
Melhorar a comunicação entre o backend Tauri e a biblioteca de rede.

- [ ] **Tratamento de Erros**: Mapear erros internos do `acerola-p2p` para o `AppError` do Tauri para fornecer feedback claro na UI.
- [ ] **Métricas em Tempo Real**: Expor eventos de conexão/desconexão e status de latência do relay através do `EventEmitter`.
- [ ] **Gestão de Estado**: Sincronizar melhor o estado de "Online/Offline/Connecting" entre o `NetworkService` e o frontend Svelte.

### 3. Configuração de Rede
- [ ] **Relay Customizado**: Permitir que o usuário configure URLs de relay personalizadas em vez de usar apenas a padrão da Iroh.
- [ ] **Auto-Discovery**: Melhorar a detecção de peers na rede local (mDNS) em conjunto com o relay.
