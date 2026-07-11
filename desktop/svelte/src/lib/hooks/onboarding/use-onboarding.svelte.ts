import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { LazyStore } from '@tauri-apps/plugin-store';

const store = new LazyStore(STORE_FILE);

let isCompleted = $state(false);
let currentStep = $state(0);
let isLoading = $state(true);

async function checkStatus() {
	const completed = await store.get<boolean>(STORE_KEYS.onboardingCompleted);
	isCompleted = completed ?? false;
	isLoading = false;
}

async function complete() {
	await store.set(STORE_KEYS.onboardingCompleted, true);
	await store.save();
	isCompleted = true;
}

checkStatus();

export function useOnboarding() {
	return {
		complete,
		get isLoading() {
			return isLoading;
		},
		get isCompleted() {
			return isCompleted;
		},
		get currentStep() {
			return currentStep;
		},
		setStep(step: number) {
			currentStep = step;
		},
		nextStep() {
			currentStep = Math.min(currentStep + 1, 4);
		},
		prevStep() {
			currentStep = Math.max(currentStep - 1, 0);
		}
	};
}
