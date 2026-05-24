import { describe, it, expect, beforeEach } from "vitest";
import { LRUService } from "./lru.service";

describe("LRUService", () => {
  let cacheInstance: LRUService<number, { id: number }>;

  beforeEach(() => {
    cacheInstance = new LRUService({ max: 3 });
  });

  it("should store and retrieve items correctly", () => {
    const itemData = { id: 1 };
    cacheInstance.set(1, itemData);
    expect(cacheInstance.get(1)).toEqual(itemData);
  });

  it("should evict the least recently used item when max capacity is reached", () => {
    const items = [1, 2, 3, 4];
    
    items.forEach((itemIndex) => {
      cacheInstance.set(itemIndex, { id: itemIndex });
    });

    // Capacity is 3, so item 1 should be evicted after adding item 4
    expect(cacheInstance.size).toBe(3);
    expect(cacheInstance.has(1)).toBe(false);
    expect(cacheInstance.has(2)).toBe(true);
    expect(cacheInstance.has(3)).toBe(true);
    expect(cacheInstance.has(4)).toBe(true);
  });

  it("should update eviction priority when accessing an item with get()", () => {
    cacheInstance.set(1, { id: 1 });
    cacheInstance.set(2, { id: 2 });
    cacheInstance.set(3, { id: 3 });

    // Access item 1 to move it to the most recently used position
    cacheInstance.get(1);

    // Adding item 4 should now evict item 2 (least recently used) instead of item 1
    cacheInstance.set(4, { id: 4 });
    
    expect(cacheInstance.has(2)).toBe(false);
    expect(cacheInstance.has(1)).toBe(true);
  });

  it("should NOT update eviction priority when accessing an item with peek()", () => {
    cacheInstance.set(1, { id: 1 });
    cacheInstance.set(2, { id: 2 });
    cacheInstance.set(3, { id: 3 });

    // Peek at item 1 (read-only, no priority update)
    cacheInstance.peek(1);

    // Adding item 4 should still evict item 1 as it is the oldest
    cacheInstance.set(4, { id: 4 });
    
    expect(cacheInstance.has(1)).toBe(false);
  });

  it("should clear all items when clear() is called", () => {
    cacheInstance.set(1, { id: 1 });
    cacheInstance.clear();
    
    expect(cacheInstance.size).toBe(0);
    expect(cacheInstance.has(1)).toBe(false);
  });

  it("should return keys in most-recently-used order", () => {
    const insertionSequence = [1, 2, 3];
    insertionSequence.forEach((key) => cacheInstance.set(key, { id: key }));
    
    // Most recent is first
    expect(cacheInstance.keys).toEqual([3, 2, 1]);
  });
});
