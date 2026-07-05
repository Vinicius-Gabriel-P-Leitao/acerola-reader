import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { debug } from '@tauri-apps/plugin-log';
import { toast } from 'svelte-sonner';
import { HOME_COMMANDS } from '$lib/contracts/home/home.commands';
import { HOME_EVENTS } from '$lib/contracts/home/home.events';
import type { ComicSummaryPayload } from '$lib/contracts/home/home.payloads';
import type { ErrorPayload } from '$lib/contracts/shared/shared.payloads';
import { resolveErrorMessage } from '$lib/contracts/errors/errors.i18n';
import { notificationStore } from '$lib/components/acerola-notification/acerola-notification.svelte';
import { m } from '$lib/paraglide/messages';

const { notify } = notificationStore;

export function useComicSummary() {
	let comics = $state<ComicSummaryPayload | undefined>(undefined);
	let loading = $state(false);

	let fetchQueued = false;

	async function fetch(): Promise<void> {
		if (loading) {
			fetchQueued = true;
			return;
		}
		loading = true;
		fetchQueued = false;

		return new Promise<void>(async (resolve) => {
			const unlisten = await listen<ComicSummaryPayload>(HOME_EVENTS.homeData, (event) => {
				comics = event.payload;

				debug(
					`[useComicSummary] total=${event.payload.total} fetchedAt=${event.payload.fetchedAt} payload=${JSON.stringify(event.payload.comics.slice(0, 3))}`
				);

				unlisten();
				unlistenErr();
				resolve();
			});

			const unlistenErr = await listen<ErrorPayload>(HOME_EVENTS.homeError, (event) => {
				const description = resolveErrorMessage(event.payload);

				notify.error(m['hooks.comic_summary.error_title'](), {
					description,
					duration: 0
				});

				toast.error(description);
				unlisten();
				unlistenErr();
				resolve();
			});

			await invoke(HOME_COMMANDS.getComicSummary);
		}).finally(() => {
			loading = false;
			if (fetchQueued) {
				fetch();
			}
		});
	}

	return {
		fetch,
		get comics() {
			return comics;
		},
		get loading() {
			return loading;
		}
	};
}
