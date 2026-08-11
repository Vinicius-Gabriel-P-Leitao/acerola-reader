import { readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

// Guarda estrutural: garante que todo `invoke()` do Svelte aponta para um
// #[tauri::command] realmente registrado em bios::build(). Sem isso, renomear
// ou remover um command no Rust só quebra em runtime.

const contractsDir = path.dirname(fileURLToPath(import.meta.url));
const svelteSrcDir = path.resolve(contractsDir, '../..');
const acerolaRoot = path.resolve(contractsDir, '../../../..');
const rustHandlerFile = path.join(acerolaRoot, 'src-tauri/src/bios/mod.rs');

function listFiles(dir: string, suffix: string): string[] {
	return readdirSync(dir, { recursive: true })
		.map((entry) => String(entry))
		.filter((entry) => entry.endsWith(suffix))
		.map((entry) => path.join(dir, entry));
}

function extractRustCommands(): Set<string> {
	const source = readFileSync(rustHandlerFile, 'utf-8');
	const handlerBlock = source.match(/invoke_handler\(tauri::generate_handler!\[([\s\S]*?)\]\)/);
	if (!handlerBlock) {
		throw new Error(`Não foi possível localizar o bloco invoke_handler em ${rustHandlerFile}`);
	}

	const commands = new Set<string>();
	for (const match of handlerBlock[1].matchAll(/(\w+)::(\w+)/g)) {
		commands.add(match[2]);
	}
	return commands;
}

// Assume pares "Const.prop" globalmente únicos entre os arquivos *.commands.ts,
// o que é verdade hoje mesmo havendo nomes de const duplicados (ex.: LIBRARY_COMMANDS
// em módulos distintos), pois suas propriedades não se sobrepõem.
function extractContractCommandMap(): Map<string, string> {
	const map = new Map<string, string>();

	for (const file of listFiles(contractsDir, '.commands.ts')) {
		const source = readFileSync(file, 'utf-8');
		for (const constMatch of source.matchAll(/export const (\w+) = \{([\s\S]*?)\} as const;/g)) {
			const [, constName, body] = constMatch;
			for (const propMatch of body.matchAll(/(\w+)\s*:\s*'([^']+)'/g)) {
				const [, propName, value] = propMatch;
				map.set(`${constName}.${propName}`, value);
			}
		}
	}

	return map;
}

interface InvokeCall {
	file: string;
	commandRef: string;
}

function findInvokeCalls(): InvokeCall[] {
	const files = [...listFiles(svelteSrcDir, '.ts'), ...listFiles(svelteSrcDir, '.svelte')].filter(
		(file) => !file.endsWith('.test.ts') && !file.includes(`${path.sep}paraglide${path.sep}`)
	);

	const calls: InvokeCall[] = [];
	for (const file of files) {
		const source = readFileSync(file, 'utf-8');
		for (const match of source.matchAll(
			/\binvoke(?:<[^()]*>)?\(\s*([A-Z][A-Z0-9_]*\.\w+|'[^']+')/g
		)) {
			calls.push({ file: path.relative(acerolaRoot, file), commandRef: match[1] });
		}
	}
	return calls;
}

describe('IPC contract sync (Svelte contracts vs. Rust commands)', () => {
	it('todo invoke() estático resolve para um command registrado em bios::build()', () => {
		const rustCommands = extractRustCommands();
		const contractMap = extractContractCommandMap();
		const invokeCalls = findInvokeCalls();

		expect(rustCommands.size).toBeGreaterThan(0);
		expect(invokeCalls.length).toBeGreaterThan(0);

		const mismatches = invokeCalls.flatMap(({ file, commandRef }) => {
			const commandName = commandRef.startsWith("'")
				? commandRef.slice(1, -1)
				: contractMap.get(commandRef);

			if (commandName === undefined) {
				return [`${file}: referência não resolvida "${commandRef}"`];
			}

			if (!rustCommands.has(commandName)) {
				return [
					`${file}: "${commandRef}" -> "${commandName}" sem #[tauri::command] correspondente`
				];
			}

			return [];
		});

		expect(mismatches).toEqual([]);
	});
});
