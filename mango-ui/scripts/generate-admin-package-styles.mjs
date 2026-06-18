#!/usr/bin/env node
import { fileURLToPath } from 'node:url';
import { main } from './generate-package-styles.mjs';

const checkOnly = process.argv.includes('--check');
const rootDir = fileURLToPath(new URL('..', import.meta.url));

main([
  '--root',
  rootDir,
  '--manifest',
  'packages/admin/admin-modules.json',
  '--package',
  'packages/admin/package.json',
  '--out',
  'packages/admin/generated-package-styles.css',
  '--admin-manifest-out',
  'packages/admin/admin-packages.json',
  '--full-style-out',
  'packages/admin/style-full.css',
  '--full-entry-out',
  'packages/admin/src/full.ts',
  '--full-types-out',
  'packages/admin/src/full.d.ts',
  '--build-deps-script-out',
  'packages/admin/build-style-deps.mjs',
  ...(checkOnly ? ['--check'] : []),
]);
