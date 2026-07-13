import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

import { classifyChangedFiles, resolveMavenScope } from '../tools/classify-pmo-check-scope.mjs';

test('frontend-only button layout avoids Java, PMO, CLI and starter suites', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-ui/packages/rbac/src/views/RoleView.vue']),
    { pmo: false, backend: false, projection: false, distribution: false, readmes: true },
  );
});

test('plain design document keeps only common lightweight gates', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-docs/designs/button-position.md']),
    { pmo: false, backend: false, projection: false, distribution: false, readmes: false },
  );
});

test('backend Java triggers architecture and README audits without CLI tests', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango/mango-platform/mango-system/mango-system-core/src/main/java/example/UserService.java']),
    { pmo: false, backend: true, projection: false, distribution: false, readmes: true },
  );
});

test('packaged PMO rules trigger PMO tests and projection checks', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-pmo/rules/09-test-case-automation-flow.md']),
    { pmo: true, backend: false, projection: true, distribution: false, readmes: false },
  );
});

test('CLI or starter changes trigger distribution and projection checks', () => {
  const cli = classifyChangedFiles(['mango-ui/packages/mango-cli/src/index.mjs']);
  assert.equal(cli.distribution, true);
  assert.equal(cli.projection, true);
  assert.equal(cli.backend, false);
});

test('governance workflow changes self-verify every conditional suite', () => {
  for (const file of [
    '.github/workflows/pmo-doc-check.yml',
    '.github/workflows/architecture-debt-inventory.yml',
  ]) {
    assert.deepEqual(
      classifyChangedFiles([file]),
      { pmo: true, backend: true, projection: true, distribution: true, readmes: true },
      file,
    );
  }
});

test('clean CI builds explicit architecture prerequisites without expanding the quality reactor', () => {
  const workflow = fs.readFileSync(
    new URL('../../.github/workflows/pmo-doc-check.yml', import.meta.url),
    'utf8',
  );
  assert.match(
    workflow,
    /Build the architecture gate prerequisites[\s\S]*?-pl :mango-parent,:mango-common,:mango-tools[\s\S]*?-DskipTests[\s\S]*?install/,
  );
  assert.doesNotMatch(workflow, /^\s+-am(?:d)?(?:\s|\\)/m);
  assert.doesNotMatch(workflow, /Enforce full-Reactor architecture/u);
});

test('Java source maps to one Maven module plus the governed architecture aggregator', () => {
  const scope = resolveMavenScope([
    'mango/mango-platform/mango-system/mango-system-core/src/main/java/example/UserService.java',
  ]);
  assert.equal(scope.mode, 'partial');
  assert.deepEqual(scope.projects, [
    ':mango-architecture-verification',
    'mango-platform/mango-system/mango-system-core',
  ]);
});

test('module POM selects its descendant Maven projects without selecting the whole reactor', () => {
  const scope = resolveMavenScope(['mango/mango-infra/mango-infra-kv/pom.xml']);
  assert.equal(scope.mode, 'partial');
  assert.ok(scope.projects.includes('mango-infra/mango-infra-kv/mango-infra-kv-core'));
  assert.ok(scope.projects.length < 20, `unexpectedly broad Maven scope: ${scope.projects.length}`);
});

test('global backend inputs use governance acceptance instead of a full PR reactor', () => {
  for (const file of [
    '.github/workflows/pmo-doc-check.yml',
    '.github/workflows/architecture-debt-inventory.yml',
    'mango/pom.xml',
    'mango/mango-parent/pom.xml',
    'mango/mango-tools/mango-maven-plugin/src/main/java/example/Gate.java',
    'mango-pmo/baselines/architecture/debt-budget.json',
  ]) {
    assert.deepEqual(resolveMavenScope([file]), { mode: 'governance', projects: [] }, file);
  }
});

test('installed business baseline resolves backend modules from the project Git root', t => {
  const project = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-business-scope-'));
  t.after(() => fs.rmSync(project, { recursive: true, force: true }));
  fs.mkdirSync(path.join(project, 'backend/order'), { recursive: true });
  fs.mkdirSync(path.join(project, 'backend/architecture-verification'), { recursive: true });
  fs.writeFileSync(path.join(project, 'backend/pom.xml'), '<project/>\n');
  fs.writeFileSync(path.join(project, 'backend/order/pom.xml'), '<project/>\n');
  fs.writeFileSync(path.join(project, 'backend/architecture-verification/pom.xml'), '<project/>\n');

  assert.deepEqual(
    resolveMavenScope(['backend/order/src/main/java/com/example/OrderService.java'], project),
    { mode: 'partial', projects: ['architecture-verification', 'order'] },
  );
  assert.deepEqual(
    resolveMavenScope(['backend/pom.xml'], project),
    { mode: 'governance', projects: [] },
  );
});
