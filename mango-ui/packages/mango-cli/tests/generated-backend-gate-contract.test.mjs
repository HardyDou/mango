import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(fileURLToPath(new URL('..', import.meta.url)));
const gateSource = readFileSync(join(packageRoot, 'scripts/check-generated-backend-gate.mjs'), 'utf8');

test('generated backend gate stays within nine Maven invocations', () => {
  const budgetMatch = gateSource.match(/const MAX_MAVEN_INVOCATIONS = (\d+);/u);
  assert.ok(budgetMatch, 'gate must declare its Maven invocation budget');
  assert.ok(Number(budgetMatch[1]) <= 9, 'Maven invocation budget must remain at most nine');

  const runMavenReferences = gateSource.match(/\brunMaven\(/gu) || [];
  assert.equal(
    runMavenReferences.length - 1,
    Number(budgetMatch[1]),
    'each Maven scenario must be an explicit, budgeted call site',
  );
  assert.match(gateSource, /mavenInvocationCount \+= 1;/u);
  assert.match(gateSource, /mavenInvocationCount > MAX_MAVEN_INVOCATIONS/u);
  assert.match(gateSource, /assertMavenInvocationBudget\(\);/u);
  assert.doesNotMatch(gateSource, /runMaven\(\[\s*['"]clean['"]/u);
});

test('combined Maven scenarios retain the critical positive and negative coverage', () => {
  for (const expected of [
    'MANGO-ARCH-PATH-001',
    'MANGO-ARCH-PATH-002',
    'MANGO-ARCH-SVC-001',
    'MANGO-ARCH-ENGINE-017',
    'MANGO-ARCH-SVC-014',
    'MANGO-ARCH-ENTITY-003',
    'MANGO-ARCH-ENTITY-004',
    "assertMangoCheckRule('PERSISTENCE_SCHEMA')",
    "assertMangoCheckRule('MODULE_INFO')",
    "globalEntityManifestJson('order_global_reference')",
    'assertGeneratedPolicyContract();',
  ]) {
    assert.ok(gateSource.includes(expected), `coverage contract missing ${expected}`);
  }
});
