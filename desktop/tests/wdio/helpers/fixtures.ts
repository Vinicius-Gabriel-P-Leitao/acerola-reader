import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const ONE_PIXEL_PNG = Buffer.from(
	'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
	'base64'
);

export type ReaderFixture = {
	rootDir: string;
	comicDir: string;
	comicTitle: string;
	cbzPath: string;
};

type ZipEntry = {
	name: string;
	bytes: Buffer;
};

const crcTable = new Uint32Array(256);

for (let index = 0; index < crcTable.length; index += 1) {
	let value = index;

	for (let bit = 0; bit < 8; bit += 1) {
		value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
	}

	crcTable[index] = value >>> 0;
}

function crc32(buffer: Buffer) {
	let crc = 0xffffffff;

	for (const byte of buffer) {
		crc = crcTable[(crc ^ byte) & 0xff] ^ (crc >>> 8);
	}

	return (crc ^ 0xffffffff) >>> 0;
}

function zipDateTime() {
	const year = 2026 - 1980;
	const month = 6;
	const day = 13;
	const hour = 0;
	const minute = 0;
	const second = 0;

	return {
		date: (year << 9) | (month << 5) | day,
		time: (hour << 11) | (minute << 5) | Math.floor(second / 2)
	};
}

function writeUInt16(value: number) {
	const buffer = Buffer.alloc(2);
	buffer.writeUInt16LE(value);
	return buffer;
}

function writeUInt32(value: number) {
	const buffer = Buffer.alloc(4);
	buffer.writeUInt32LE(value >>> 0);
	return buffer;
}

function localHeader(entry: ZipEntry, crc: number) {
	const fileName = Buffer.from(entry.name);
	const { date, time } = zipDateTime();

	return Buffer.concat([
		writeUInt32(0x04034b50),
		writeUInt16(20),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt16(time),
		writeUInt16(date),
		writeUInt32(crc),
		writeUInt32(entry.bytes.length),
		writeUInt32(entry.bytes.length),
		writeUInt16(fileName.length),
		writeUInt16(0),
		fileName
	]);
}

function centralDirectoryHeader(entry: ZipEntry, crc: number, localOffset: number) {
	const fileName = Buffer.from(entry.name);
	const { date, time } = zipDateTime();

	return Buffer.concat([
		writeUInt32(0x02014b50),
		writeUInt16(20),
		writeUInt16(20),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt16(time),
		writeUInt16(date),
		writeUInt32(crc),
		writeUInt32(entry.bytes.length),
		writeUInt32(entry.bytes.length),
		writeUInt16(fileName.length),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt32(0),
		writeUInt32(localOffset),
		fileName
	]);
}

function endOfCentralDirectory(entryCount: number, centralSize: number, centralOffset: number) {
	return Buffer.concat([
		writeUInt32(0x06054b50),
		writeUInt16(0),
		writeUInt16(0),
		writeUInt16(entryCount),
		writeUInt16(entryCount),
		writeUInt32(centralSize),
		writeUInt32(centralOffset),
		writeUInt16(0)
	]);
}

function createStoredZip(entries: ZipEntry[]) {
	const localParts: Buffer[] = [];
	const centralParts: Buffer[] = [];
	let offset = 0;

	for (const entry of entries) {
		const crc = crc32(entry.bytes);
		const header = localHeader(entry, crc);

		localParts.push(header, entry.bytes);
		centralParts.push(centralDirectoryHeader(entry, crc, offset));
		offset += header.length + entry.bytes.length;
	}

	const centralDirectory = Buffer.concat(centralParts);
	const localFiles = Buffer.concat(localParts);
	const end = endOfCentralDirectory(entries.length, centralDirectory.length, localFiles.length);

	return Buffer.concat([localFiles, centralDirectory, end]);
}

export function createReaderFixture(comicTitle = 'Acerola WDIO'): ReaderFixture {
	const rootDir = mkdtempSync(path.join(os.tmpdir(), 'acerola-wdio-library-'));
	const comicDir = path.join(rootDir, comicTitle);
	const cbzPath = path.join(comicDir, 'Ch. 1.cbz');

	mkdirSync(comicDir, { recursive: true });
	writeFileSync(
		cbzPath,
		createStoredZip([
			{ name: '001.png', bytes: ONE_PIXEL_PNG },
			{ name: '002.png', bytes: ONE_PIXEL_PNG },
			{ name: '003.png', bytes: ONE_PIXEL_PNG }
		])
	);

	return {
		rootDir,
		comicDir,
		comicTitle,
		cbzPath
	};
}

export function readerChapterFor(fixture: ReaderFixture) {
	return {
		id: `${fixture.comicTitle.toLowerCase().replace(/\s+/g, '-')}-chapter-1`,
		name: 'Ch. 1',
		path: fixture.cbzPath,
		chapterSort: '1',
		volumeId: null,
		volumeName: null,
		isSpecial: false,
		lastModified: 0
	};
}
