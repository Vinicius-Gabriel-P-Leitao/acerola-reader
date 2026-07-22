import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { Options } from '@wdio/types';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(
	__dirname,
	process.platform === 'win32'
		? 'src-tauri/target/release/acerola.exe'
		: 'src-tauri/target/release/acerola'
);

export const config: Options.Testrunner = {
	runner: 'local',
	specs: ['./tests/wdio/specs/**/*.spec.ts'],
	maxInstances: 1,
	capabilities: [
		{
			browserName: 'tauri',
			'tauri:options': {
				application: appPath
			}
		}
	],
	services: [
		[
			'tauri',
			{
				appBinaryPath: appPath,
				driverProvider: 'external'
			}
		]
	],
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
		specs.forEach((spec, index) => {
			if (spec.startsWith('file://')) {
				specs[index] = fileURLToPath(spec);
			}
		});
	}
};
