import '@poppanator/sveltekit-svg/dist/svg.d.ts';
import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';

declare global {
	namespace App {
		// interface Error {}
		// interface Locals {}
		// interface PageData {}
		interface PageState {
			chapter?: ReaderChapterPayload;
			startPage?: number;
			chapterIndex?: number;
			totalChapters?: number;
			chapterScope?: string;
		}
		// interface Platform {}
	}
}

export {};
