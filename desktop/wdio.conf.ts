import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { Options } from '@wdio/types';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const appPath =
	process.platform === 'win32'
		? 'src-tauri/target/release/acerola.exe'
		: 'src-tauri/target/release/acerola';
const localNativeDriverPath =
	process.platform === 'win32' ? path.resolve(__dirname, '.bin/windows/msedgedriver.exe') : null;

let tauriDriver: ChildProcessWithoutNullStreams | undefined;

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const config: Options.Testrunner = {
	runner: 'local',
	hostname: '127.0.0.1',
	port: 4444,
	specs: ['./tests/wdio/specs/**/*.spec.ts'],
	maxInstances: 1,
	capabilities: [
		{
			browserName: process.platform === 'win32' ? 'chrome' : 'wry',
			'tauri:options': {
				application: appPath
			}
		}
	],
	logLevel: 'info',
	framework: 'mocha',
	reporters: ['spec'],
	waitforTimeout: 30_000,
	connectionRetryTimeout: 30_000,
	connectionRetryCount: 1,
	mochaOpts: {
		ui: 'bdd',
		timeout: 30_000
	},
	onPrepare: async () => {
		const binaryPath = path.resolve(__dirname, appPath);

		if (!existsSync(binaryPath)) {
			throw new Error(`Binário não encontrado em: ${binaryPath}\nRode: cargo tauri build`);
		}

		const tauriDriverArgs =
			localNativeDriverPath && existsSync(localNativeDriverPath)
				? ['--native-driver', localNativeDriverPath]
				: [];

		tauriDriver = spawn('tauri-driver', tauriDriverArgs, {
			cwd: __dirname,
			stdio: ['ignore', 'pipe', 'pipe'],
			shell: process.platform === 'win32'
		});

		tauriDriver.stderr.on('data', (chunk) => {
			process.stderr.write(`[tauri-driver] ${chunk}`);
		});

		tauriDriver.on('error', (error) => {
			console.error('[tauri-driver] erro ao iniciar:', error);
		});

		await wait(2_000);
	},
	onComplete: () => {
		if (tauriDriver && !tauriDriver.killed) {
			tauriDriver.kill();
		}
	}
};
