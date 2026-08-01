import type { ErrorPayload } from '$lib/contracts/shared/shared.payloads';

/**
 * Extracts a user-friendly error message from an unknown thrown error.
 * Safely parses string errors, Tauri ErrorPayload objects, or Error instances without using `any`.
 */
export function extractErrorMessage(error: unknown): string {
	if (error === null || error === undefined) {
		return 'Unknown error occurred';
	}
	if (typeof error === 'string') {
		return error;
	}
	if (typeof error === 'object') {
		const payload = error as Partial<ErrorPayload>;
		if (typeof payload.message === 'string' && payload.message.trim().length > 0) {
			return payload.message;
		}
		if ('message' in error && typeof (error as { message: unknown }).message === 'string') {
			return (error as { message: string }).message;
		}
	}
	return String(error);
}
