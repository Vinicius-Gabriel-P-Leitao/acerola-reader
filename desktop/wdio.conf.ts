import { spawn, type ChildProcessByStdio } from 'node:child_process';
import { existsSync, mkdtempSync, rmSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import type { Readable } from 'node:stream';
import { fileURLToPath } from 'node:url';
import type { Capabilities, Options } from '@wdio/types';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const appPath =
	process.platform === 'win32'
		? path.resolve(__dirname, 'src-tauri/target/release/acerola.exe')
		: path.resolve(__dirname, 'src-tauri/target/release/acerola');
const localNativeDriverPath =
	process.platform === 'win32' ? path.resolve(__dirname, '.bin/windows/msedgedriver.exe') : null;

type TauriCapability = WebdriverIO.Capabilities & {
	'tauri:options': {
		application: string;
	};
};

let tauriDriver: ChildProcessByStdio<null, Readable, Readable> | undefined;
let appDataDir: string | undefined;

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const capabilities: TauriCapability[] = [
	{
		browserName: process.platform === 'win32' ? 'chrome' : 'wry',
		'tauri:options': {
			application: appPath
		}
	}
];

export const config: Options.Testrunner & Capabilities.WithRequestedTestrunnerCapabilities = {
	runner: 'local',
	hostname: '127.0.0.1',
	port: 4444,
	specs: ['./tests/wdio/specs/**/*.spec.ts'],
	maxInstances: 1,
	capabilities,
	logLevel: 'info',
	framework: 'mocha',
	reporters: ['spec'],
	waitforTimeout: 30_000,
	connectionRetryTimeout: 120_000,
	connectionRetryCount: 3,
	mochaOpts: {
		ui: 'bdd',
		timeout: 30_000
	},
	beforeSession: (_config, _capabilities, specs) => {
		for (let index = 0; index < specs.length; index += 1) {
			if (specs[index].startsWith('file://')) {
				specs[index] = fileURLToPath(specs[index]);
			}
		}
	},
	onPrepare: async () => {
		const binaryPath = path.resolve(__dirname, appPath);

		if (!existsSync(binaryPath)) {
			throw new Error(`Binário não encontrado em: ${binaryPath}\nRode: cargo tauri build`);
		}

		appDataDir = mkdtempSync(path.join(os.tmpdir(), 'acerola-wdio-appdata-'));

		const tauriDriverArgs =
			localNativeDriverPath && existsSync(localNativeDriverPath)
				? ['--native-driver', localNativeDriverPath]
				: [];

		const isCI = !!process.env.CI;
		const webview2Args = isCI ? '--disable-gpu' : undefined;

		const driver = spawn('tauri-driver', tauriDriverArgs, {
			cwd: __dirname,
			env: {
				...process.env,
				APPDATA: appDataDir,
				LOCALAPPDATA: appDataDir,
				XDG_DATA_HOME: appDataDir,
				...(webview2Args ? { WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS: webview2Args } : {})
			},
			stdio: ['ignore', 'pipe', 'pipe'],
			shell: false
		});
		tauriDriver = driver;

		driver.stderr.on('data', (chunk) => {
			process.stderr.write(`[tauri-driver STDERR] ${chunk}`);
		});
		driver.stdout.on('data', (chunk) => {
			process.stdout.write(`[tauri-driver STDOUT] ${chunk}`);
		});

		driver.on('error', (error) => {
			console.error('[tauri-driver] erro ao iniciar:', error);
		});

		await wait(isCI ? 8_000 : 2_000);
	},
	onComplete: () => {
		if (tauriDriver && !tauriDriver.killed) {
			tauriDriver.kill();
		}

		if (appDataDir) {
			rmSync(appDataDir, { recursive: true, force: true });
		}
	}
};
