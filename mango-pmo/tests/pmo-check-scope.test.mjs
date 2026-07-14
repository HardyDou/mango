import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

import {
  classifyChangedFiles,
  resolveMavenDependencyProjects,
  resolveMavenScope,
} from '../tools/classify-pmo-check-scope.mjs';

test('frontend-only button layout avoids Java, PMO, CLI and starter suites', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-ui/packages/rbac/src/views/RoleView.vue']),
    {
      pmo: false,
      backend: false,
      projection: false,
      distribution: false,
      readmes: true,
      generated_backend: false,
    },
  );
});

test('plain design document keeps only common lightweight gates', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-docs/designs/button-position.md']),
    {
      pmo: false,
      backend: false,
      projection: false,
      distribution: false,
      readmes: false,
      generated_backend: false,
    },
  );
});

test('backend Java triggers architecture and README audits without CLI tests', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango/mango-platform/mango-system/mango-system-core/src/main/java/example/UserService.java']),
    {
      pmo: false,
      backend: true,
      projection: false,
      distribution: false,
      readmes: true,
      generated_backend: false,
    },
  );
});

test('packaged PMO rules trigger PMO tests and projection checks', () => {
  assert.deepEqual(
    classifyChangedFiles(['mango-pmo/rules/09-test-case-automation-flow.md']),
    {
      pmo: true,
      backend: false,
      projection: true,
      distribution: false,
      readmes: false,
      generated_backend: false,
    },
  );
});

test('CLI or starter changes trigger distribution and projection checks', () => {
  const cli = classifyChangedFiles(['mango-ui/packages/mango-cli/src/index.mjs']);
  assert.equal(cli.distribution, true);
  assert.equal(cli.projection, true);
  assert.equal(cli.backend, false);
  assert.equal(cli.generated_backend, true);
});

test('generated backend validation only follows behavior-changing templates, generators and Java gates', () => {
  for (const file of [
    'mango/mango-tools/mango-architecture-rules/src/main/java/example/Rule.java',
    'mango/mango-tools/mango-maven-plugin/src/main/java/example/Gate.java',
    'mango-business-starter/backend/modules/{{moduleKebab}}/pom.xml',
    'mango-ui/packages/mango-cli/templates/full/backend/pom.xml',
    'mango-ui/packages/mango-cli/src/index.mjs',
    'mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs',
  ]) {
    assert.equal(classifyChangedFiles([file]).generated_backend, true, file);
  }
});

test('version, changelog, README, PMO contract projection and release locks skip generated backend validation', () => {
  for (const file of [
    'CHANGELOG.md',
    'mango-business-starter/README.md',
    'mango-ui/packages/mango-cli/CHANGELOG.md',
    'mango-ui/packages/mango-cli/package.json',
    'mango-ui/packages/mango-cli/release-versions.json',
    'mango-ui/packages/mango-pmo/package.json',
    'mango-pmo/contracts/technical-design.json',
    'mango-pmo/release-lock.json',
    'mango-business-starter/business-pmo/pmo-lock.json',
    'mango-business-starter/business-pmo/mango-baseline/baseline.json',
    'mango-business-starter/business-pmo/mango-baseline/contracts/technical-design.json',
  ]) {
    assert.equal(classifyChangedFiles([file]).generated_backend, false, file);
  }
});

test('unknown critical generated-backend governance inputs fail closed', () => {
  for (const file of [
    'mango/pom.xml',
    'mango/mango-parent/new-governance.xml',
    'mango-pmo/baselines/mango-check/new-policy.json',
    'mango-pmo/tools/classify-pmo-check-scope.mjs',
    'business-pmo/mango-baseline/tools/classify-pmo-check-scope.mjs',
  ]) {
    assert.equal(classifyChangedFiles([file]).generated_backend, true, file);
  }
});

test('governance workflow changes self-verify every conditional suite', () => {
  for (const file of [
    '.github/workflows/pmo-doc-check.yml',
    '.gitea/workflows/pmo-doc-check.yml',
    '.github/workflows/architecture-debt-inventory.yml',
  ]) {
    assert.deepEqual(
      classifyChangedFiles([file]),
      {
        pmo: true,
        backend: true,
        projection: true,
        distribution: true,
        readmes: true,
        generated_backend: true,
      },
      file,
    );
  }
});

test('fast PR contract workflow self-verifies PMO without starting Java or generated backend gates', () => {
  assert.deepEqual(
    classifyChangedFiles(['.github/workflows/pr-contract-check.yml']),
    {
      pmo: true,
      backend: false,
      projection: false,
      distribution: false,
      readmes: false,
      generated_backend: false,
    },
  );
});

test('business repositories resolve custom paths from mango.config.json', t => {
  const project = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-custom-business-scope-'));
  t.after(() => fs.rmSync(project, { recursive: true, force: true }));
  fs.mkdirSync(path.join(project, 'baohan-backend/order'), { recursive: true });
  fs.mkdirSync(path.join(project, 'baohan-backend/architecture-verification'), { recursive: true });
  fs.writeFileSync(path.join(project, 'baohan-backend/pom.xml'), '<project/>\n');
  fs.writeFileSync(path.join(project, 'baohan-backend/order/pom.xml'), '<project/>\n');
  fs.writeFileSync(path.join(project, 'baohan-backend/architecture-verification/pom.xml'), '<project/>\n');
  fs.writeFileSync(path.join(project, 'mango.config.json'), JSON.stringify({
    paths: {
      backend: 'baohan-backend',
      frontend: 'baohan-ui',
      businessDocs: 'business-docs',
    },
  }));

  assert.equal(
    classifyChangedFiles([
      'baohan-backend/order/src/main/java/com/example/OrderService.java',
    ], project).backend,
    true,
  );
  assert.deepEqual(
    resolveMavenScope([
      'baohan-backend/order/src/main/java/com/example/OrderService.java',
    ], project),
    { mode: 'partial', projects: ['architecture-verification', 'order'] },
  );
  assert.deepEqual(
    resolveMavenScope(['.gitea/workflows/pmo-doc-check.yml'], project),
    { mode: 'governance', projects: [] },
  );
});

test('configured backend paths fail closed when their root POM is absent', t => {
  const project = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-invalid-business-scope-'));
  t.after(() => fs.rmSync(project, { recursive: true, force: true }));
  fs.writeFileSync(path.join(project, 'mango.config.json'), JSON.stringify({
    paths: { backend: 'missing-backend' },
  }));

  assert.throws(
    () => resolveMavenScope(['README.md'], project),
    /Configured backend POM does not exist: missing-backend\/pom\.xml/,
  );
});

test('clean CI builds explicit architecture prerequisites without expanding the quality reactor', () => {
  const workflow = fs.readFileSync(
    new URL('../../.github/workflows/pmo-doc-check.yml', import.meta.url),
    'utf8',
  );
  assert.match(workflow, /pull_request:\n\s+types: \[opened, synchronize, reopened\]/);
  assert.doesNotMatch(workflow, /types: \[[^\]]*edited[^\]]*\]/);
  assert.match(
    workflow,
    /concurrency:\n\s+group: pmo-doc-check-\$\{\{ github\.event\.pull_request\.number \}\}\n\s+cancel-in-progress: true/,
  );
  for (const job of ['pmo', 'cli_js', 'java', 'docs']) {
    assert.match(
      workflow,
      new RegExp(`\\n  ${job}:\\n[\\s\\S]*?\\n    needs: preflight_scope`),
      job,
    );
  }
  assert.match(
    workflow,
    /Build and verify the reproducible PMO package[\s\S]*?build-package\.mjs[\s\S]*?check-package\.mjs/,
  );
  assert.match(workflow, /generated_backend: \$\{\{ steps\.scope\.outputs\.generated_backend \}\}/);
  assert.match(
    workflow,
    /Build the architecture gate prerequisites[\s\S]*?-pl :mango-parent,:mango-common,:mango-tools[\s\S]*?-DskipTests[\s\S]*?install/,
  );
  assert.match(
    workflow,
    /Build generated four-layer backend prerequisites[\s\S]*?:mango-infra-persistence-api[\s\S]*?:mango-infra-feign-starter[\s\S]*?install/,
  );
  const dependencyBuild = workflow.match(
    /      - name: Build affected-module upstream dependencies[\s\S]*?(?=\n      - name:)/,
  )?.[0] ?? '';
  assert.match(
    dependencyBuild,
    /needs\.preflight_scope\.outputs\.backend_mode == 'partial'[\s\S]*?needs\.preflight_scope\.outputs\.maven_dependency_projects != ''/,
  );
  assert.match(dependencyBuild, /MAVEN_DEPENDENCY_PROJECTS: \$\{\{ needs\.preflight_scope\.outputs\.maven_dependency_projects \}\}/);
  assert.match(dependencyBuild, /-pl "\$MAVEN_DEPENDENCY_PROJECTS"[\s\S]*?\n\s+-am \\/);
  assert.match(dependencyBuild, /-DskipTests[\s\S]*?-Dcheckstyle\.skip=true[\s\S]*?-Dpmd\.skip=true[\s\S]*?-Dspotbugs\.skip=true[\s\S]*?install/);

  const qualityGate = workflow.match(
    /      - name: Enforce affected-module architecture and Java quality contracts[\s\S]*?(?=\n      - name:)/,
  )?.[0] ?? '';
  assert.match(qualityGate, /-pl "\$MAVEN_PROJECTS"/);
  assert.doesNotMatch(qualityGate, /^\s+-am(?:d)?(?:\s|\\)/m);
  assert.doesNotMatch(workflow, /Enforce full-Reactor architecture/u);
  assert.match(
    workflow,
    /Verify generated business backend gates[\s\S]*?needs\.preflight_scope\.outputs\.generated_backend == 'true'/,
  );
  assert.match(
    workflow,
    /Build the local PMO bundle for generated project acceptance[\s\S]*?mango-pmo\/scripts\/build-package\.mjs[\s\S]*?Build the architecture gate prerequisites/,
  );

  const generatedBackendGate = fs.readFileSync(
    new URL('../../mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs', import.meta.url),
    'utf8',
  );
  assert.match(generatedBackendGate, /pathToFileURL\(localMangoRepository\)/u);
  assert.doesNotMatch(generatedBackendGate, /nexus\.inner\.yunxinbaokeji\.com/u);

  const stableSummary = workflow.match(/\n  pmo-doc-check:\n[\s\S]*$/)?.[0] ?? '';
  assert.match(stableSummary, /name: pmo-doc-check/);
  assert.match(stableSummary, /needs: \[preflight_scope, pmo, cli_js, java, docs\]/);
  assert.match(stableSummary, /if: \$\{\{ always\(\) \}\}/);
  assert.match(stableSummary, /success\|skipped\) ;;/);
  assert.match(stableSummary, /\*\) exit 1 ;;/);
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
  assert.deepEqual(resolveMavenDependencyProjects(scope), [
    'mango-platform/mango-system/mango-system-core',
  ]);
  assert.deepEqual(resolveMavenDependencyProjects({
    mode: 'partial',
    projects: [':mango-architecture-verification', 'mango-architecture-verification'],
  }), []);
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
