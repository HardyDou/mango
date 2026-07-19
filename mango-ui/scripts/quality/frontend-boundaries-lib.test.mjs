import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {
  analyzeFrontendBoundaries,
  compareFrontendBoundaryBaselines,
  compareFrontendBoundaryReport,
  createFrontendBoundaryBaseline,
} from './frontend-boundaries-lib.mjs';

function fixture(files = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-frontend-boundaries-'));
  const defaults = {
    'packages/app-runtime/package.json': JSON.stringify({
      name: '@mango/app-runtime',
      mangoArchitecture: { role: 'foundation' },
    }),
    'packages/app-runtime/src/index.ts': "export async function mount() { return import('wujie') }",
    'packages/domain/package.json': JSON.stringify({
      name: '@mango/domain',
      exports: { '.': './dist/index.js', './style.css': './dist/style.css' },
      mangoArchitecture: { role: 'domain' },
    }),
    'packages/domain/src/api/items.ts': "import { get } from '@mango/common'\nexport const list = () => get('/items')",
    'apps/example/package.json': JSON.stringify({
      name: 'example-app',
      dependencies: { '@mango/app-runtime': 'workspace:*', '@mango/domain': 'workspace:*' },
      mangoArchitecture: { role: 'app' },
    }),
    'apps/example/src/main.ts': "import '@mango/domain/style.css'",
    ...files,
  };
  for (const [relative, content] of Object.entries(defaults)) {
    const file = path.join(root, relative);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, content);
  }
  return root;
}

test('accepts typed API, explicit package style and adapter-owned vendor access', () => {
  const root = fixture();
  const report = analyzeFrontendBoundaries(root);
  assert.equal(report.summary.violationCount, 0);
  assert.ok(report.scannedFileCount > 0);
});

test('finds API, presentation, CSS and vendor boundary violations', () => {
  const root = fixture({
    'packages/domain/src/api/bad.ts': [
      "import { ref } from 'vue'",
      "import axios from 'axios'",
      'export type ApiId = number',
      "export const load = () => fetch(import.meta.env.VITE_API + '/items')",
      "export const remote = () => axios.get('https://example.test/items')",
    ].join('\n'),
    'packages/domain/src/views/Bad.vue': [
      '<script setup lang="ts">',
      "import { request } from '@mango/common'",
      "window.$wujie?.bus.$emit('bad')",
      '</script>',
      '<style>.el-button { color: red; }</style>',
    ].join('\n'),
    'apps/example/src/main.ts': "import { domain } from '@mango/domain'\nexport const app = domain",
  });
  const rules = new Set(analyzeFrontendBoundaries(root).violations.map((item) => item.rule));
  for (const expected of [
    'api/no-ui-framework',
    'api/no-direct-transport',
    'api/no-environment-access',
    'api/string-api-id',
    'layer/presentation-no-transport',
    'css/page-style-must-be-scoped-or-module',
    'css/no-global-element-plus-page-override',
    'css/micro-app-explicit-package-style',
    'microfrontend/vendor-symbol-outside-adapter',
  ])
    assert.ok(rules.has(expected), `missing ${expected}`);
});

test('uses exact identities and only permits baseline debt to shrink', () => {
  const root = fixture({
    'packages/domain/src/views/Bad.vue': '<script setup>window.$wujie?.bus</script>',
  });
  const report = analyzeFrontendBoundaries(root);
  const baseline = createFrontendBoundaryBaseline(report);
  assert.deepEqual(compareFrontendBoundaryReport(report, baseline), []);
  const clean = createFrontendBoundaryBaseline(analyzeFrontendBoundaries(fixture()));
  assert.equal(compareFrontendBoundaryBaselines(baseline, clean).length, 1);
  assert.deepEqual(compareFrontendBoundaryBaselines(clean, baseline), []);
});

test('fails closed for empty source input and missing vendor adapter implementation', () => {
  const empty = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-frontend-boundaries-empty-'));
  assert.throws(() => analyzeFrontendBoundaries(empty), /input is empty/u);
  const root = fixture({ 'packages/app-runtime/src/index.ts': 'export const adapter = {}' });
  assert.throws(() => analyzeFrontendBoundaries(root), /vendor adapter owner is missing/u);
});
