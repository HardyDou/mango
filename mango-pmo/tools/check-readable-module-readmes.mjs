#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const defaultTargets = [
  'mango/mango-platform/mango-resource/README.md',
  'mango/mango-platform/mango-file/README.md',
  'mango/mango-platform/mango-workflow/README.md',
  'mango-ui/packages/file/README.md',
  'mango-ui/packages/workflow/README.md',
  'mango-ui/packages/common/components/MangoDataTable/README.md',
];
const targets = valueAfter('--files')?.split(',').map(value => value.trim()).filter(Boolean) ?? defaultTargets;
const failures = [];
for (const relative of targets) {
  const file = path.resolve(repoRoot, relative);
  if (!existsSync(file) || !lstatSync(file).isFile() || lstatSync(file).isSymbolicLink()) {
    failures.push(`${relative}: file is missing or not a regular file`);
    continue;
  }
  const source = readFileSync(file, 'utf8');
  if (!/^#\s+\S/mu.test(source)) failures.push(`${relative}: missing H1`);
  for (const marker of ['场景 / 路径', '边界 / 源码']) {
    if (!source.includes(marker)) failures.push(`${relative}: missing ${marker} entry`);
  }
  if (!/\*\*(?:边界 \/ 源码|源码入口)：\*\*[\s\S]*?\]\([^)]*(?:src|index\.vue)/u.test(source)) {
    failures.push(`${relative}: source entry does not link to a source path`);
  }
  if (!/```[\s\S]*?```/u.test(source)) failures.push(`${relative}: missing a runnable or inspectable example`);
}
if (failures.length) {
  console.error('[FAIL] readable module README pilot');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log(`[PASS] readable module README pilot: ${targets.length} files`);

function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
