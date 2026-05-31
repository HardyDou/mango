#!/usr/bin/env node
import { fileURLToPath } from 'node:url';
import { main } from './generate-package-styles.mjs';

const checkOnly = process.argv.includes('--check');
const rootDir = fileURLToPath(new URL('..', import.meta.url));

main([
  '--root',
  rootDir,
  '--manifest',
  'packages/admin/admin-packages.json',
  '--package',
  'packages/admin/package.json',
  '--out',
  'packages/admin/generated-package-styles.css',
  ...(checkOnly ? ['--check'] : []),
]);
