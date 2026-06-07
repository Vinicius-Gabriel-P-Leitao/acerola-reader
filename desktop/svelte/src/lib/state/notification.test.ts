import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createNotifications } from './notification.svelte';

describe('createNotifications', () => {
	beforeEach(() => {
		vi.useFakeTimers();
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('adiciona notificação pela variante e retorna id', () => {
		const store = createNotifications(['success', 'error'] as const);

		const id = store.notify.success('Scan concluído', { duration: 0 });

		expect(id).toBe(0);
		expect(store.notifications).toHaveLength(1);
		expect(store.notifications[0]).toMatchObject({
			id,
			message: 'Scan concluído',
			variant: 'success',
			duration: 0
		});
	});

	it('remove notificação pelo id', () => {
		const store = createNotifications(['info'] as const);
		const id = store.notify.info('Em andamento', { duration: 0 });

		store.pop(id);

		expect(store.notifications).toEqual([]);
	});

	it('mantém lista quando remove id inexistente', () => {
		const store = createNotifications(['info'] as const);
		store.notify.info('Em andamento', { duration: 0 });

		store.pop(999);

		expect(store.notifications).toHaveLength(1);
	});

	it('limpa todas as notificações', () => {
		const store = createNotifications(['info', 'error'] as const);
		store.notify.info('Primeira', { duration: 0 });
		store.notify.error('Segunda', { duration: 0 });

		store.clearAll();

		expect(store.notifications).toEqual([]);
	});

	it('remove automaticamente após duração configurada', () => {
		const store = createNotifications(['info'] as const);
		store.notify.info('Temporária', { duration: 1000 });

		expect(store.notifications).toHaveLength(1);

		vi.advanceTimersByTime(1000);

		expect(store.notifications).toEqual([]);
	});
});
