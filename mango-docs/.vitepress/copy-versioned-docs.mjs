import { cp, mkdir, readdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const docsRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const versionsRoot = resolve(docsRoot, 'versions');
const distRoot = resolve(docsRoot, '.vitepress/public-src/.vitepress/dist');
const targetVersionsRoot = resolve(distRoot, 'versions');

let entries;
try {
  entries = await readdir(versionsRoot, { withFileTypes: true });
} catch (error) {
  if (error?.code === 'ENOENT') {
    console.log('No Mango Docs version snapshots to copy.');
    process.exit(0);
  }
  throw error;
}

const versionDirs = entries.filter((entry) => entry.isDirectory() && !entry.name.startsWith('.'));

if (versionDirs.length === 0) {
  console.log('No Mango Docs version snapshots to copy.');
  process.exit(0);
}

await mkdir(targetVersionsRoot, { recursive: true });

for (const entry of versionDirs) {
  await cp(resolve(versionsRoot, entry.name), resolve(targetVersionsRoot, entry.name), {
    recursive: true
  });
}

await cp(resolve(versionsRoot, 'manifest.json'), resolve(targetVersionsRoot, 'manifest.json'));

console.log(`Copied ${versionDirs.length} Mango Docs version snapshot(s) into Pages dist.`);
