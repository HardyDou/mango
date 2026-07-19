import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { createFrontendInventory, writeFrontendInventory } from './frontend-inventory-lib.mjs';

const fixtureRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), '__fixtures__/component-inventory');

test('discovers named and async components, glob registrars, and generated widget metadata', () => {
  const report = createFrontendInventory(fixtureRoot);

  assert.equal(report.summary.workspaceCount, 1);
  assert.equal(report.summary.componentCandidateCount, 4);
  assert.equal(report.summary.publicVueExportCoverage, 1);
  assert.ok(
    report.publicVueExports.some((item) => item.exportName === 'NamedCard' && item.file.endsWith('NamedCard.vue')),
  );
  assert.ok(
    report.publicVueExports.some((item) => item.exportName === 'WidgetCard' && item.file.endsWith('WidgetCard.vue')),
  );
  assert.ok(report.dynamicImports.some((item) => item.specifier === './components/AsyncCard.vue'));
  assert.ok(report.globImports.some((item) => item.pattern === './pages/**/*.vue'));
  assert.ok(report.registrars.some((item) => item.name === 'registerFixturePages' && item.source === 'source'));
  assert.ok(report.registrars.some((item) => item.name === 'registerFixturePages' && item.source === 'manifest'));
  assert.ok(report.widgets.some((item) => item.type === 'fixture.generated'));
  assert.ok(report.widgets.some((item) => item.exportKey === './widgets/generated'));
});

test('produces deterministic content and writes only the requested report', () => {
  const first = createFrontendInventory(fixtureRoot);
  const second = createFrontendInventory(fixtureRoot);
  assert.deepEqual(second, first);

  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-frontend-inventory-'));
  const outputFile = path.join(directory, 'inventory.json');
  writeFrontendInventory(first, outputFile);
  assert.deepEqual(JSON.parse(fs.readFileSync(outputFile, 'utf8')), first);
});

test('fails closed when a workspace has no frontend source input', () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-empty-inventory-'));
  const workspace = path.join(directory, 'packages', 'empty');
  fs.mkdirSync(workspace, { recursive: true });
  fs.writeFileSync(path.join(workspace, 'package.json'), JSON.stringify({ name: '@fixture/empty' }));

  assert.throws(() => createFrontendInventory(directory), /source file count is zero/u);
});

test('fails closed when a concrete package code export has no source entry', () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-unresolved-export-'));
  const workspace = path.join(directory, 'packages', 'broken');
  fs.mkdirSync(path.join(workspace, 'src'), { recursive: true });
  fs.writeFileSync(
    path.join(workspace, 'package.json'),
    JSON.stringify({
      name: '@fixture/broken',
      exports: {
        '.': { import: './dist/missing.js' },
      },
    }),
  );
  fs.writeFileSync(path.join(workspace, 'src', 'Unused.vue'), '<template><div /></template>');

  assert.throws(() => createFrontendInventory(directory), /code export source entry coverage is incomplete/u);
});
