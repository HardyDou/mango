import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/check-architecture-debt-budget.mjs');
const modules = [
  {
    moduleKey: 'mango-platform/order/order-api',
    groupId: 'io.mango.order',
    artifactId: 'order-api'
  },
  {
    moduleKey: 'mango-platform/order/order-core',
    groupId: 'io.mango.order',
    artifactId: 'order-core'
  },
  {
    moduleKey: 'mango-platform/billing/billing-core',
    groupId: 'io.mango.billing',
    artifactId: 'billing-core'
  }
];

function createFixture(ruleIds) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-architecture-debt-'));
  const report = path.join(root, 'report.json');
  const baseline = path.join(root, 'budget.json');
  const baseBudget = path.join(root, 'base-budget.json');
  writeReport(report, ruleIds);
  return { root, report, baseline, baseBudget };
}

function writeReport(file, ruleIds, overrides = {}) {
  const issue = (value, moduleKey) => typeof value === 'string'
    ? { ruleId: value, subject: value, message: value, moduleKey }
    : { moduleKey, ...value };
  fs.writeFileSync(file, JSON.stringify({
    schemaVersion: 2,
    modules,
    dependencyIssues: ruleIds.dependency.map(value => issue(value, modules[0].moduleKey)),
    archUnitIssues: ruleIds.archunit.map(value => issue(value, modules[1].moduleKey)),
    pmdIssues: ruleIds.pmd.map(value => issue(value, modules[2].moduleKey)),
    blockingIssues: [],
    mode: 'changed',
    inventoryScope: 'full-reactor',
    issueInventory: 'all-detected-issues',
    reactorProjectCount: 3,
    expectedProjectCount: 3,
    ...overrides
  }));
}

function run(fixture, extra = []) {
  const result = spawnSync(process.execPath, [
    checker,
    '--report', fixture.report,
    '--baseline', fixture.baseline,
    '--json',
    ...extra
  ], { cwd: fixture.cwd, encoding: 'utf8' });
  return {
    status: result.status,
    stdout: result.stdout,
    stderr: result.stderr,
    report: result.stdout ? JSON.parse(result.stdout) : null
  };
}

function createSourceFixture(root) {
  fs.mkdirSync(root, { recursive: true });
  const source = path.join(root, 'mango/example/src/main/java/ExampleService.java');
  fs.mkdirSync(path.dirname(source), { recursive: true });
  fs.writeFileSync(source, 'class ExampleService {\n  private static final int LIMIT = 42;\n}\n');
  const fixture = {
    cwd: root,
    root,
    report: path.join(root, 'report.json'),
    baseline: path.join(root, 'budget.json'),
    baseBudget: path.join(root, 'base-budget.json')
  };
  writeReport(fixture.report, {
    dependency: [],
    archunit: [],
    pmd: [{
      ruleId: 'PMD-MAGIC',
      subject: `${source}:2`,
      message: 'Avoid magic literals'
    }]
  });
  const initialized = spawnSync('git', ['init', '-q'], { cwd: root, encoding: 'utf8' });
  assert.equal(initialized.status, 0, initialized.stderr);
  return fixture;
}

function copyBaselineToBase(fixture) {
  fs.copyFileSync(fixture.baseline, fixture.baseBudget);
}

test('initializes and verifies a full-Reactor architecture debt budget', () => {
  const fixture = createFixture({ dependency: ['DEP-1'], archunit: ['ARCH-1'], pmd: ['PMD-1'] });
  try {
    assert.equal(run(fixture).status, 1);
    assert.equal(run(fixture, ['--write']).status, 0);
    const checked = run(fixture);
    assert.equal(checked.status, 0, checked.stderr);
    assert.equal(checked.report.current.totalIssueCount, 3);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects reports that do not prove a complete Reactor inventory', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    writeReport(
      fixture.report,
      { dependency: [], archunit: ['ARCH-1'], pmd: [] },
      { inventoryScope: 'partial-reactor', reactorProjectCount: 2, expectedProjectCount: 3 }
    );
    const rejected = run(fixture, ['--write']);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /inventoryScope must be full-reactor/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('requires the committed budget to ratchet down after debt removal', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1', 'ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1'], pmd: [] });
    const reduced = run(fixture);
    assert.equal(reduced.status, 1);
    assert.equal(reduced.report.action, 'ratchet-required');
    assert.equal(run(fixture, ['--write']).status, 0);
    assert.equal(run(fixture).status, 0);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects current inventory increases unless governance records an explicit reason', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-2'], pmd: [] });
    assert.equal(run(fixture).status, 1);
    assert.equal(run(fixture, ['--write']).report.action, 'write-refused');
    assert.equal(run(fixture, ['--write', '--accept-increase']).status, 2);
    const accepted = run(fixture, [
      '--write',
      '--accept-increase',
      '--reason',
      'new governed rule with reviewed legacy inventory'
    ]);
    assert.equal(accepted.status, 0, accepted.stderr);
    assert.equal(accepted.report.action, 'increase-accepted');
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects a PR that manually raises its budget relative to the base budget', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    copyBaselineToBase(fixture);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-2'], pmd: [] });
    assert.equal(
      run(fixture, [
        '--write', '--accept-increase', '--reason', 'temporary reviewed increase'
      ]).status,
      0
    );
    const raised = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    raised.acceptedIncreaseReason = null;
    raised.acceptedIncreaseFromSha256 = null;
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(raised, null, 2)}\n`);
    const rejected = run(fixture, ['--base-budget', fixture.baseBudget]);
    assert.equal(rejected.status, 1);
    assert.equal(rejected.report.action, 'base-budget-increase');
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('accepts a governed increase only when its reason is bound to the exact base budget', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    copyBaselineToBase(fixture);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-2'], pmd: [] });
    assert.equal(
      run(fixture, [
        '--write', '--accept-increase', '--reason', 'reviewed upstream rule expansion'
      ]).status,
      0
    );
    const accepted = run(fixture, ['--base-budget', fixture.baseBudget]);
    assert.equal(accepted.status, 0, accepted.stderr);
    assert.equal(accepted.report.action, 'governed-increase-recorded');

    const tamperedBase = JSON.parse(fs.readFileSync(fixture.baseBudget, 'utf8'));
    tamperedBase.generatedAt = '2026-07-13T00:00:00.000Z';
    fs.writeFileSync(fixture.baseBudget, `${JSON.stringify(tamperedBase, null, 2)}\n`);
    assert.equal(run(fixture, ['--base-budget', fixture.baseBudget]).status, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects stale accepted-increase metadata when the PR budget did not increase', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    copyBaselineToBase(fixture);
    const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    budget.acceptedIncreaseReason = 'stale prior approval';
    budget.acceptedIncreaseFromSha256 = 'a'.repeat(64);
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(budget, null, 2)}\n`);
    const rejected = run(fixture, ['--base-budget', fixture.baseBudget]);
    assert.equal(rejected.status, 1);
    assert.equal(rejected.report.action, 'stale-increase-reason');
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('a reduced budget cannot rebound in a later PR without fresh governance metadata', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1', 'ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1'], pmd: [] });
    assert.equal(run(fixture, ['--write']).status, 0);
    copyBaselineToBase(fixture);

    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-1'], pmd: [] });
    assert.equal(
      run(fixture, [
        '--write', '--accept-increase', '--reason', 'candidate rebound for adversarial test'
      ]).status,
      0
    );
    const rebound = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    rebound.acceptedIncreaseReason = null;
    rebound.acceptedIncreaseFromSha256 = null;
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(rebound, null, 2)}\n`);
    assert.equal(run(fixture, ['--base-budget', fixture.baseBudget]).status, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects replacing one historical issue with a new identity under the same rule', () => {
  const oldIssue = { ruleId: 'ARCH-1', subject: 'OldService', message: 'same rule' };
  const newIssue = { ruleId: 'ARCH-1', subject: 'NewService', message: 'same rule' };
  const fixture = createFixture({ dependency: [], archunit: [oldIssue], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, { dependency: [], archunit: [newIssue], pmd: [] });
    const rejected = run(fixture);
    assert.equal(rejected.status, 1);
    assert.equal(rejected.report.action, 'check');
    assert.equal(rejected.report.comparison.totalDelta, 0);
    assert.equal(rejected.report.comparison.identityIncreases.length, 1);
    assert.equal(rejected.report.comparison.identityReductions.length, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('selects a module by artifactId or a module directory prefix', () => {
  const fixture = createFixture({
    dependency: ['DEP-1'],
    archunit: ['ARCH-1', 'ARCH-2'],
    pmd: ['PMD-1']
  });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);

    const leaf = run(fixture, ['--module', 'order-core']);
    assert.equal(leaf.status, 0, leaf.stderr);
    assert.deepEqual(leaf.report.selectedModules, ['mango-platform/order/order-core']);
    assert.equal(leaf.report.current.totalIssueCount, 2);

    const directory = run(fixture, ['--module', 'mango-platform/order']);
    assert.equal(directory.status, 0, directory.stderr);
    assert.deepEqual(directory.report.selectedModules, [
      'mango-platform/order/order-api',
      'mango-platform/order/order-core'
    ]);
    assert.equal(directory.report.current.totalIssueCount, 3);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('ratchets only the selected module and preserves every other module budget', () => {
  const fixture = createFixture({
    dependency: [],
    archunit: ['ARCH-1', 'ARCH-2'],
    pmd: ['PMD-1']
  });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    const before = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    writeReport(fixture.report, {
      dependency: [],
      archunit: ['ARCH-1'],
      pmd: ['PMD-1']
    });

    const reduced = run(fixture, ['--module', 'order-core']);
    assert.equal(reduced.status, 1);
    assert.equal(reduced.report.action, 'ratchet-required');

    const written = run(fixture, ['--module', 'order-core', '--write']);
    assert.equal(written.status, 0, written.stderr);
    assert.equal(written.report.action, 'module-ratcheted');
    const after = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    assert.equal(after.modules['mango-platform/order/order-core'].totalIssueCount, 1);
    assert.deepEqual(
      after.modules['mango-platform/billing/billing-core'],
      before.modules['mango-platform/billing/billing-core']
    );
    assert.equal(run(fixture).status, 0);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects moving an unchanged identity between modules', () => {
  const issue = { ruleId: 'ARCH-1', subject: 'OrderService', message: 'same issue' };
  const fixture = createFixture({ dependency: [], archunit: [issue], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, {
      dependency: [],
      archunit: [{ ...issue, moduleKey: 'mango-platform/billing/billing-core' }],
      pmd: []
    });
    const moved = run(fixture);
    assert.equal(moved.status, 1);
    assert.equal(moved.report.action, 'check');
    assert.equal(moved.report.comparison.totalDelta, 0);
    assert.equal(
      moved.report.comparison.moduleComparisons.some(item => item.moduleKey === 'mango-platform/billing/billing-core'
        && item.identityIncreases.length === 1),
      true
    );
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects unknown and overlapping module selectors', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    assert.equal(run(fixture, ['--module', 'unknown-module']).status, 2);
    assert.equal(run(fixture, [
      '--module', 'mango-platform/order',
      '--module', 'order-core'
    ]).status, 2);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('rejects missing issue ownership and tampered module aggregates', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    writeReport(fixture.report, {
      dependency: [],
      archunit: [{
        ruleId: 'ARCH-1',
        subject: 'OrderService',
        message: 'missing owner',
        moduleKey: 'unknown/module'
      }],
      pmd: []
    });
    assert.equal(run(fixture, ['--write']).status, 2);

    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1'], pmd: [] });
    assert.equal(run(fixture, ['--write']).status, 0);
    const tampered = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    tampered.modules['mango-platform/order/order-core'].totalIssueCount = 0;
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(tampered, null, 2)}\n`);
    assert.equal(run(fixture).status, 2);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('migrates a schemaVersion 3 current budget but only reads it as a Git base', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    const current = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    const legacy = {
      ...current,
      schemaVersion: 3
    };
    delete legacy.modules;
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(legacy, null, 2)}\n`);

    const required = run(fixture);
    assert.equal(required.status, 1);
    assert.equal(required.report.action, 'migration-required');
    assert.equal(run(fixture, ['--module', 'order-core']).status, 2);

    const migrated = run(fixture, ['--write']);
    assert.equal(migrated.status, 0, migrated.stderr);
    assert.equal(migrated.report.action, 'migrated');
    fs.writeFileSync(fixture.baseBudget, `${JSON.stringify(legacy, null, 2)}\n`);
    assert.equal(run(fixture, ['--base-budget', fixture.baseBudget]).status, 0);
    assert.equal(run(fixture, [
      '--module', 'order-core',
      '--base-budget', fixture.baseBudget
    ]).status, 2);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('source identities are stable when GitHub repeats the repository name in the checkout path', () => {
  const parent = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-architecture-checkouts-'));
  const local = createSourceFixture(path.join(parent, 'local-checkout'));
  const runner = createSourceFixture(path.join(parent, 'mango/mango'));
  try {
    assert.equal(run(local, ['--write']).status, 0);
    fs.copyFileSync(local.baseline, runner.baseline);
    const checked = run(runner);
    assert.equal(checked.status, 0, checked.stderr);
    assert.deepEqual(
      Object.keys(checked.report.current.identities),
      Object.keys(JSON.parse(fs.readFileSync(local.baseline, 'utf8')).identities)
    );
  } finally {
    fs.rmSync(parent, { recursive: true, force: true });
  }
});

test('CI base-ref reads a large committed base budget from Git', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    const nestedBaseline = path.join(
      fixture.root,
      'mango-pmo/baselines/architecture/debt-budget.json'
    );
    fixture.baseline = nestedBaseline;
    assert.equal(run(fixture, ['--write']).status, 0);
    const largeBudget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    largeBudget.testPadding = 'x'.repeat(2 * 1024 * 1024);
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(largeBudget, null, 2)}\n`);
    const git = (...args) => spawnSync('git', ['-C', fixture.root, ...args], {
      encoding: 'utf8',
      env: {
        ...process.env,
        GIT_AUTHOR_NAME: 'Mango Test',
        GIT_AUTHOR_EMAIL: 'mango-test@example.invalid',
        GIT_COMMITTER_NAME: 'Mango Test',
        GIT_COMMITTER_EMAIL: 'mango-test@example.invalid'
      }
    });
    assert.equal(git('init', '-q').status, 0);
    assert.equal(git('add', '.').status, 0);
    assert.equal(git('commit', '-qm', 'baseline').status, 0);
    const checked = spawnSync(process.execPath, [
      checker,
      '--report', fixture.report,
      '--baseline', fixture.baseline,
      '--base-ref', 'HEAD',
      '--json'
    ], {
      cwd: fixture.root,
      encoding: 'utf8',
      maxBuffer: 16 * 1024 * 1024
    });
    assert.equal(checked.status, 0, checked.stderr);
    assert.equal(JSON.parse(checked.stdout).action, 'check');
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});
