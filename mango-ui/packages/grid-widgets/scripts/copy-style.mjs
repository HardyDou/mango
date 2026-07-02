#!/usr/bin/env node
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sourcePath = resolve(packageRoot, 'src/style.css');
const targetPath = resolve(packageRoot, 'dist/style.css');
const css = readFileSync(sourcePath, 'utf8');

if (!css.trim()) {
  console.error('Refusing to publish missing @mango/grid-widgets style.css artifact.');
  process.exit(1);
}

mkdirSync(dirname(targetPath), { recursive: true });
writeFileSync(targetPath, css);
