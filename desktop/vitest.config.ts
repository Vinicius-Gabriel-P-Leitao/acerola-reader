import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { svelteTesting } from '@testing-library/svelte/vite';
import { playwright } from '@vitest/browser-playwright';
import { fileURLToPath } from 'node:url';
import path from 'path';
import { defineConfig } from 'vitest/config';
import svg from '@poppanator/sveltekit-svg';

const dirname =
	typeof __dirname !== 'undefined' ? __dirname : path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
	plugins: [
		svelte({
			hot: false,
			compilerOptions: {
				dev: true
			}
		}),
		svg({
			includePaths: ['./svelte/src/lib/assets/'],
			svgoOptions: {
				multipass: true,
				plugins: ['preset-default']
			}
		})
	],
	optimizeDeps: {
		include: [
			'@tauri-apps/api/window',
			'aria-query',
			'axobject-query',
			'lz-string',
			'dom-accessibility-api',
			'pretty-format',
			'picocolors'
		]
	},
	resolve: {
		alias: {
			$lib: path.resolve('./svelte/src/lib'),
			$theme: path.resolve('./svelte/src/theme'),
			$services: path.resolve('./svelte/src/services'),
			'$app/state': path.resolve('./svelte/tests/mocks/app-state.ts'),
			'$app/environment': path.resolve('./svelte/tests/mocks/app-environment.ts'),
			'$app/navigation': path.resolve('./svelte/tests/mocks/app-navigation.ts')
		},
		// Crucial for Svelte 5 Browser Mode
		conditions: ['browser', 'svelte', 'development']
	},
	// INFO: Não usar paraglide nos testes.
	test: {
		projects: [
			{
				extends: true,
				plugins: [svelteTesting()],
				test: {
					name: 'unit',
					globals: true,
					environment: 'jsdom',
					include: ['svelte/src/**/*.test.ts'],
					exclude: ['svelte/src/**/*.browser.test.ts'],
					setupFiles: ['svelte/tests/setup.ts']
				}
			},
			{
				extends: true,
				test: {
					name: 'integration',
					setupFiles: ['svelte/tests/setup.browser.ts'],
					include: ['svelte/src/**/*.browser.test.ts'],
					browser: {
						enabled: true,
						headless: true,
						provider: playwright({}),
						instances: [
							{
								browser: 'chromium'
							}
						]
					}
				}
			},
			{
				extends: true,
				plugins: [
					storybookTest({
						configDir: path.join(dirname, '.storybook')
					})
				],
				test: {
					name: 'storybook',
					setupFiles: ['svelte/tests/setup.browser.ts'],
					browser: {
						enabled: true,
						headless: true,
						provider: playwright({}),
						instances: [
							{
								browser: 'chromium'
							}
						]
					}
				}
			}
		]
	}
});
