import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateRepositoryDevManifest } from './check-repository-dev-manifest.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const manifest = JSON.parse(fs.readFileSync(path.join(repositoryRoot, 'mango.dev.json'), 'utf8'));

function candidate() {
  return structuredClone(manifest);
}

test('accepts the repository backend manifest with one process mode and a complete revision reactor', () => {
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

test('rejects an install reactor that omits the local BOM or backend module', () => {
  const withoutBom = candidate();
  withoutBom.apps['mango-backend'].install.args[withoutBom.apps['mango-backend'].install.args.indexOf('-pl') + 1] =
    'mango-app/monolith/mango-monolith-app';
  assert.match(validateRepositoryDevManifest(withoutBom).join('\n'), /must include :mango-bom/u);

  const withoutBackend = candidate();
  withoutBackend.apps['mango-backend'].install.args[
    withoutBackend.apps['mango-backend'].install.args.indexOf('-pl') + 1
  ] = ':mango-bom';
  assert.match(validateRepositoryDevManifest(withoutBackend).join('\n'), /must include mango-app/u);
});
