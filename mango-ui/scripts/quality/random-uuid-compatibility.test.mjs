import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';
import { removeMockServiceWorkerFromBuild } from '../../build-config/mockServiceWorkerBuild.mjs';
import { findDisallowedRandomUUIDCalls } from './check-random-uuid-usage.mjs';

const uiRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const uuidV4Pattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u;

test('direct-call gate rejects calls outside controlled compatibility files', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-random-uuid-gate-'));
  mkdirSync(join(root, 'packages/common/utils'), { recursive: true });
  mkdirSync(join(root, 'packages/demo'), { recursive: true });
  const directCall = ['crypto', 'randomUUID()'].join('.');
  writeFileSync(join(root, 'packages/common/utils/webCrypto.ts'), directCall);
  writeFileSync(join(root, 'packages/demo/index.ts'), directCall);

  assert.deepEqual(findDisallowedRandomUUIDCalls({ root, scanRoots: ['packages'] }), ['packages/demo/index.ts:1']);
});

test('controlled worker generates a v4 request ID without native randomUUID', () => {
  const workerSource = readFileSync(join(uiRoot, 'apps/mango-admin/public/mockServiceWorker.js'), 'utf8');
  const context = vm.createContext({
    addEventListener() {},
    crypto: {
      getRandomValues(bytes) {
        bytes.forEach((_value, index) => {
          bytes[index] = index;
        });
        return bytes;
      },
    },
    self: {},
  });
  vm.runInContext(workerSource, context);

  const requestId = vm.runInContext('createMockRequestId()', context);
  assert.match(requestId, uuidV4Pattern);
  assert.equal(requestId, '00010203-0405-4607-8809-0a0b0c0d0e0f');
});

test('controlled worker preserves native randomUUID', () => {
  const workerSource = readFileSync(join(uiRoot, 'apps/mango-admin/public/mockServiceWorker.js'), 'utf8');
  const context = vm.createContext({
    addEventListener() {},
    crypto: { randomUUID: () => 'native-request-id' },
    self: {},
  });
  vm.runInContext(workerSource, context);

  assert.equal(vm.runInContext('createMockRequestId()', context), 'native-request-id');
});

test('production boundary removes the worker asset and keeps other public files', () => {
  const outDir = mkdtempSync(join(tmpdir(), 'mango-admin-dist-'));
  const worker = join(outDir, 'mockServiceWorker.js');
  const favicon = join(outDir, 'favicon.ico');
  writeFileSync(worker, 'worker');
  writeFileSync(favicon, 'favicon');

  assert.equal(removeMockServiceWorkerFromBuild(outDir), true);
  assert.equal(removeMockServiceWorkerFromBuild(outDir), false);
  assert.equal(readFileSync(favicon, 'utf8'), 'favicon');
});

test('MSW registration remains development-only while stale production registrations are cleaned', () => {
  const mainSource = readFileSync(join(uiRoot, 'apps/mango-admin/src/main.ts'), 'utf8');
  const browserSource = readFileSync(join(uiRoot, 'apps/mango-admin/src/mocks/browser.ts'), 'utf8');

  assert.match(mainSource, /import\.meta\.env\.DEV && import\.meta\.env\.VITE_USE_MOCK/u);
  assert.match(mainSource, /registration\.unregister\(\)/u);
  assert.doesNotMatch(browserSource, /__ENABLE_MOCK__/u);
  assert.match(browserSource, /if \(import\.meta\.env\.DEV\)/u);
});
