import { LRUCache } from "lru-cache";

export interface LRUConfig {
  /**
   * Número máximo de itens para manter no cache.
   */
  max: number;
  /**
   * Tempo de vida em milissegundos.
   */
  ttl?: number;
}

/**
 * Uma abstração genérica de serviço LRU (Least Recently Used).
 * Usada para gerenciar estruturas de dados eficientes em memória, especialmente para scrolls infinitos.
 */
export class LRUService<KeyType extends string | number, ValueType extends {}> {
  private cache: LRUCache<KeyType, ValueType>;

  constructor(config: LRUConfig) {
    this.cache = new LRUCache({
      max: config.max,
      ttl: config.ttl || 1000 * 60 * 60, // Padrão: 1 hora
    });
  }

  /**
   * Recupera um item do cache sem atualizar o status de "recentemente usado".
   */
  peek(key: KeyType): ValueType | undefined {
    return this.cache.peek(key);
  }

  /**
   * Recupera um item do cache e atualiza o status de "recentemente usado".
   */
  get(key: KeyType): ValueType | undefined {
    return this.cache.get(key);
  }

  /**
   * Define um item no cache.
   * Se o cache exceder o 'max', o item menos recentemente usado é removido.
   */
  set(key: KeyType, value: ValueType): void {
    const isNew = !this.cache.has(key);
    this.cache.set(key, value);
    console.log(`[LRUService] SET key=${key} | size=${this.cache.size}/${this.cache.max} | new=${isNew}`);
  }

  /**
   * Retorna se uma chave existe no cache sem atualizar seu status.
   */
  has(key: KeyType): boolean {
    return this.cache.has(key);
  }

  /**
   * Retorna todas as chaves atuais no cache, ordenadas da mais recentemente usada para a menos.
   */
  get keys(): KeyType[] {
    return Array.from(this.cache.keys());
  }

  /**
   * Retorna todos os valores atuais no cache.
   */
  get values(): ValueType[] {
    return Array.from(this.cache.values());
  }

  /**
   * Remove um item específico do cache.
   */
  delete(key: KeyType): void {
    console.log(`[LRUService] DELETE key=${key}`);
    this.cache.delete(key);
  }

  /**
   * Limpa todo o cache.
   */
  clear(): void {
    console.log(`[LRUService] CLEAR cache (prev size: ${this.cache.size})`);
    this.cache.clear();
  }

  /**
   * Tamanho atual do cache.
   */
  get size(): number {
    return this.cache.size;
  }
}
