import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { compareLegacyComponentBaselines, validateComponentContracts } from './component-contracts-lib.mjs';

function fixture() {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-component-contract-'));
  fs.writeFileSync(path.join(directory, 'README.md'), '# fixture\n');
  fs.mkdirSync(path.join(directory, 'tests'));
  fs.writeFileSync(path.join(directory, 'tests', 'Card.spec.ts'), 'export {};\n');
  const manifest = {
    name: '@fixture/components',
    private: false,
    exports: { '.': './dist/index.js', './style.css': './dist/style.css' },
    mangoArchitecture: {
      sourceExports: { '.': { source: './src/index.ts', kind: 'code' } },
      nonCodeExports: {
        './style.css': { source: './style.css', dist: './dist/style.css', generation: 'static' },
      },
    },
  };
  const inventory = {
    publicVueExports: [
      { workspace: manifest.name, exportKey: '.', exportName: 'Card' },
      { workspace: manifest.name, exportKey: '.', exportName: 'LegacyCard' },
    ],
  };
  const registry = {
    schemaVersion: 1,
    packageName: manifest.name,
    components: [
      {
        name: 'Card',
        exportKey: '.',
        exportName: 'Card',
        styleExportKeys: ['./style.css'],
        level: 'C4',
        hostProfile: 'host-agnostic',
        environmentProfile: 'universal',
        distribution: ['workspace', 'npm'],
        deploymentModes: ['monolith'],
        stability: 'stable',
        ownerRole: 'Owner',
        docs: { path: 'README.md', requiredSections: ['api'] },
        testEvidence: ['tests/Card.spec.ts'],
      },
    ],
    legacyComponentExports: [
      {
        exportKey: '.',
        exportName: 'LegacyCard',
        ownerRole: 'Owner',
        targetPhase: 'PR-2E',
        exitCriteria: 'Classify and graduate the component.',
      },
    ],
  };
  return { inventory, record: { registry, manifest, directory, file: 'component-contracts.json' } };
}

test('accepts complete C4 and exact legacy classifications', () => {
  const { inventory, record } = fixture();
  assert.deepEqual(validateComponentContracts(inventory, [record]), []);
});

test('fails for unclassified, duplicate, invalid style, and invalid C4 contracts', () => {
  const { inventory, record } = fixture();
  const duplicate = structuredClone(record.registry.components[0]);
  duplicate.styleExportKeys = ['./missing.css'];
  duplicate.hostProfile = 'admin-shell';
  duplicate.distribution = ['workspace'];
  record.registry.components.push(duplicate);
  record.registry.legacyComponentExports = [];
  const failures = validateComponentContracts(inventory, [record]);
  assert.ok(failures.some((failure) => failure.includes('missing style export')));
  assert.ok(failures.some((failure) => failure.includes('C4 requires npm distribution')));
  assert.ok(failures.some((failure) => failure.includes('C4 cannot use admin-shell')));
  assert.ok(failures.some((failure) => failure.includes('classified more than once')));
  assert.ok(failures.some((failure) => failure.includes('LegacyCard')));
});

test('requires component and style keys to use the architecture machine source', () => {
  const { inventory, record } = fixture();
  record.manifest.mangoArchitecture.sourceExports = {};
  record.manifest.mangoArchitecture.nonCodeExports = {};
  const failures = validateComponentContracts(inventory, [record]);
  assert.ok(failures.some((failure) => failure.includes('mangoArchitecture.sourceExports key')));
  assert.ok(failures.some((failure) => failure.includes('mangoArchitecture.nonCodeExports key')));
});

test('fails closed for empty inventory and registry input', () => {
  assert.deepEqual(validateComponentContracts({ publicVueExports: [] }, []), [
    'public Vue export inventory is empty',
    'component registry input is empty',
  ]);
});

test('legacy baseline may only stay equal or shrink', () => {
  const { record } = fixture();
  assert.deepEqual(compareLegacyComponentBaselines([record], [record]), []);
  const promoted = structuredClone(record);
  promoted.registry.legacyComponentExports = [];
  assert.deepEqual(compareLegacyComponentBaselines([promoted], [record]), []);
});

test('rejects new, renamed, retargeted, or reassigned legacy entries', () => {
  const { record } = fixture();
  const changed = structuredClone(record);
  changed.registry.legacyComponentExports[0].ownerRole = 'Other';
  changed.registry.legacyComponentExports[0].targetPhase = 'PR-3Z';
  changed.registry.legacyComponentExports[0].exitCriteria = 'Keep forever.';
  changed.registry.legacyComponentExports.push({
    exportKey: '.',
    exportName: 'NewLegacyCard',
    ownerRole: 'Owner',
    targetPhase: 'PR-3Z',
    exitCriteria: 'Later.',
  });
  assert.deepEqual(compareLegacyComponentBaselines([changed], [record]), [
    'legacy component ownerRole changed: @fixture/components:.:LegacyCard',
    'legacy component targetPhase changed: @fixture/components:.:LegacyCard',
    'legacy component exitCriteria changed: @fixture/components:.:LegacyCard',
    'new legacy component export is not allowed: @fixture/components:.:NewLegacyCard',
  ]);
});
