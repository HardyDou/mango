import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateRepositoryCliEntry, validateRepositoryDevManifest } from './check-repository-dev-manifest.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const manifest = JSON.parse(fs.readFileSync(path.join(repositoryRoot, 'mango.dev.json'), 'utf8'));
const uiPackage = JSON.parse(fs.readFileSync(path.join(uiRoot, 'package.json'), 'utf8'));

function candidate() {
  return structuredClone(manifest);
}

test('accepts the repository backend manifest without a legacy install command', () => {
  assert.deepEqual(validateRepositoryDevManifest(candidate()), []);
});

test('rejects implicit or duplicate repository backend process modes', () => {
  const implicit = candidate();
  delete implicit.apps['mango-backend'].processMode;
  assert.match(validateRepositoryDevManifest(implicit).join('\n'), /explicitly declare processMode=runtime/u);

  const duplicate = candidate();
  duplicate.apps['mango-backend'].args.unshift('runtime');
  assert.match(validateRepositoryDevManifest(duplicate).join('\n'), /args must not declare a process mode/u);
});

test('rejects a repository backend legacy install command', () => {
  const legacy = candidate();
  legacy.apps['mango-backend'].install = {
    command: 'mvn',
    args: ['-DskipTests', 'install'],
  };
  assert.match(validateRepositoryDevManifest(legacy).join('\n'), /must not declare a legacy install command/u);
});

test('requires the Mango source script to execute the repository CLI entry directly', () => {
  assert.deepEqual(validateRepositoryCliEntry(uiPackage), []);

  const pathFallback = structuredClone(uiPackage);
  pathFallback.scripts.mango = 'mango';
  assert.match(validateRepositoryCliEntry(pathFallback).join('\n'), /cannot fall back to PATH/u);
});
