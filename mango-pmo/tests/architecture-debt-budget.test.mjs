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

function runBaselineOnly(fixture, extra = []) {
  const result = spawnSync(process.execPath, [
    checker,
    '--baseline', fixture.baseline,
    '--base-budget', fixture.baseBudget,
    '--baseline-only',
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

function git(fixture, ...args) {
  return spawnSync('git', ['-C', fixture.root, ...args], {
    encoding: 'utf8',
    env: {
      ...process.env,
      GIT_AUTHOR_NAME: 'Mango Test',
      GIT_AUTHOR_EMAIL: 'mango-test@example.invalid',
      GIT_COMMITTER_NAME: 'Mango Test',
      GIT_COMMITTER_EMAIL: 'mango-test@example.invalid'
    }
  });
}

function writeOnboardingReport(file, extraIssues = [], omittedIssues = [], overrides = {}) {
  const onboardingModules = [
    {
      moduleKey: 'backend/modules/guarantee/guarantee-api',
      groupId: 'com.example.guarantee',
      artifactId: 'guarantee-api'
    },
    {
      moduleKey: 'backend/modules/guarantee/guarantee-core',
      groupId: 'com.example.guarantee',
      artifactId: 'guarantee-core'
    },
    {
      moduleKey: 'backend/modules/guarantee/guarantee-starter',
      groupId: 'com.example.guarantee',
      artifactId: 'guarantee-starter'
    },
    {
      moduleKey: 'backend/modules/billing/billing-core',
      groupId: 'com.example.billing',
      artifactId: 'billing-core'
    }
  ];
  const allIssues = [
    {
      ruleId: 'MANGO-ARCH-SERVICE-001',
      subject: 'LegacyGuaranteeServiceImpl',
      message: 'Legacy service naming',
      moduleKey: onboardingModules[1].moduleKey
    },
    {
      ruleId: 'MANGO-ARCH-CTRL-004',
      subject: 'LegacyGuaranteeController',
      message: 'Legacy controller contract',
      moduleKey: onboardingModules[2].moduleKey
    },
    ...extraIssues
  ].filter(issue => !omittedIssues.includes(issue.subject));
  fs.writeFileSync(file, JSON.stringify({
    schemaVersion: 2,
    modules: onboardingModules,
    dependencyIssues: [],
    archUnitIssues: allIssues,
    pmdIssues: [],
    blockingIssues: [],
    mode: 'changed',
    inventoryScope: 'full-reactor',
    issueInventory: 'all-detected-issues',
    inventoryOnly: true,
    reactorProjectCount: onboardingModules.length,
    expectedProjectCount: onboardingModules.length,
    ...overrides
  }));
}

function createOnboardingFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-module-onboarding-'));
  const fixture = {
    root,
    cwd: root,
    report: path.join(root, 'target/mango-architecture-report.json'),
    baseline: path.join(root, 'mango-pmo/baselines/architecture/debt-budget.json'),
    moduleProperties: path.join(
      root,
      'backend/modules/guarantee/guarantee-starter/src/main/resources/META-INF/mango/module.properties'
    )
  };
  fs.mkdirSync(path.dirname(fixture.report), { recursive: true });
  fs.mkdirSync(path.dirname(fixture.baseline), { recursive: true });
  writeOnboardingReport(fixture.report, [], [
    'LegacyGuaranteeServiceImpl',
    'LegacyGuaranteeController'
  ]);
  assert.equal(git(fixture, 'init', '-q').status, 0);
  assert.equal(run(fixture, ['--write']).status, 0);
  assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
  assert.equal(git(fixture, 'commit', '-qm', 'base architecture budget').status, 0);
  writeOnboardingReport(fixture.report);
  fs.mkdirSync(path.dirname(fixture.moduleProperties), { recursive: true });
  fs.writeFileSync(
    fixture.moduleProperties,
    'module-name=guarantee\nmodule-path=/guarantee\nmodule-number=900\n'
  );
  assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);
  return fixture;
}

function runOnboarding(fixture, extra = []) {
  return run(fixture, [
    '--onboard-module', 'backend/modules/guarantee',
    '--module-properties', fixture.moduleProperties,
    '--base-ref', 'HEAD',
    '--reason', 'reviewed first governance of the legacy guarantee module',
    '--write',
    ...extra
  ]);
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

test('baseline-only PR policy validates a governed increase without regenerating the full report', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    copyBaselineToBase(fixture);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-2'], pmd: [] });
    assert.equal(run(fixture, [
      '--write', '--accept-increase', '--reason', 'reviewed rule expansion'
    ]).status, 0);
    const checked = runBaselineOnly(fixture);
    assert.equal(checked.status, 0, checked.stderr);
    assert.equal(checked.report.action, 'governed-increase-recorded');

    const raised = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    raised.acceptedIncreaseFromSha256 = '0'.repeat(64);
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(raised, null, 2)}\n`);
    assert.equal(runBaselineOnly(fixture).status, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('keeps a prior accepted-increase record as inert audit evidence', () => {
  const fixture = createFixture({ dependency: [], archunit: ['ARCH-1'], pmd: [] });
  try {
    assert.equal(run(fixture, ['--write']).status, 0);
    writeReport(fixture.report, { dependency: [], archunit: ['ARCH-1', 'ARCH-2'], pmd: [] });
    assert.equal(
      run(fixture, [
        '--write', '--accept-increase', '--reason', 'prior reviewed rule adoption'
      ]).status,
      0
    );
    copyBaselineToBase(fixture);
    const accepted = run(fixture, ['--base-budget', fixture.baseBudget]);
    assert.equal(accepted.status, 0, accepted.stderr);
    assert.equal(accepted.report.action, 'historical-increase-recorded');

    writeReport(
      fixture.report,
      { dependency: [], archunit: ['ARCH-1', 'ARCH-2', 'ARCH-3'], pmd: [] }
    );
    const laterIncrease = run(fixture, ['--base-budget', fixture.baseBudget]);
    assert.equal(laterIncrease.status, 1);
    assert.equal(laterIncrease.report.action, 'check');
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

test('CI base-ref reads a budget larger than the child-process default buffer', () => {
  const issues = Array.from({ length: 14_000 }, (_, index) => ({
    ruleId: 'ARCH-LARGE-BASE',
    subject: `LargeBaseService${index}`,
    message: 'Existing governed architecture debt'
  }));
  const fixture = createFixture({ dependency: [], archunit: issues, pmd: [] });
  try {
    fixture.baseline = path.join(
      fixture.root,
      'mango-pmo/baselines/architecture/debt-budget.json'
    );
    const written = spawnSync(process.execPath, [
      checker,
      '--report', fixture.report,
      '--baseline', fixture.baseline,
      '--write'
    ], { cwd: fixture.root, encoding: 'utf8' });
    assert.equal(written.status, 0, written.stderr);
    assert.ok(fs.statSync(fixture.baseline).size > 1024 * 1024);

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
    assert.equal(git('commit', '-qm', 'large baseline').status, 0);

    const checked = spawnSync(process.execPath, [
      checker,
      '--report', fixture.report,
      '--baseline', fixture.baseline,
      '--base-ref', 'HEAD'
    ], { cwd: fixture.root, encoding: 'utf8' });
    assert.equal(checked.status, 0, checked.stderr);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('initial project budget requires a trusted full report and a budget-only Git diff', () => {
  for (const mutation of ['valid', 'baseline-only', 'business-source']) {
    const fixture = createFixture({ dependency: [], archunit: [], pmd: [] });
    fixture.cwd = fixture.root;
    try {
      assert.equal(git(fixture, 'init', '-q').status, 0);
      fs.writeFileSync(path.join(fixture.root, 'README.md'), 'project base\n');
      assert.equal(git(fixture, 'add', 'README.md').status, 0);
      assert.equal(git(fixture, 'commit', '-qm', 'project base').status, 0);
      assert.equal(run(fixture, ['--write']).status, 0);
      assert.equal(git(fixture, 'add', fixture.baseline).status, 0);

      if (mutation === 'business-source') {
        const source = path.join(fixture.root, 'backend/src/main/java/BusinessService.java');
        fs.mkdirSync(path.dirname(source), { recursive: true });
        fs.writeFileSync(source, 'class BusinessService {}\n');
        assert.equal(git(fixture, 'add', source).status, 0);
      }

      const checked = mutation === 'baseline-only'
        ? spawnSync(process.execPath, [
          checker,
          '--baseline', fixture.baseline,
          '--baseline-only',
          '--base-ref', 'HEAD',
          '--json'
        ], { cwd: fixture.root, encoding: 'utf8' })
        : run(fixture, ['--base-ref', 'HEAD']);
      if (mutation === 'valid') {
        assert.equal(checked.status, 0, checked.stderr);
        assert.equal(checked.report.action, 'initialized-against-base');
      } else if (mutation === 'baseline-only') {
        assert.equal(checked.status, 1, checked.stderr);
        assert.equal(JSON.parse(checked.stdout).action, 'initial-budget-report-required');
      } else {
        assert.equal(checked.status, 2);
        assert.match(checked.stderr, /budget file; forbidden changes/u);
      }
    } finally {
      fs.rmSync(fixture.root, { recursive: true, force: true });
    }
  }
});

test('missing project budget permits only a pure managed workflow governance upgrade', () => {
  const fixture = createFixture({ dependency: [], archunit: [], pmd: [] });
  fixture.cwd = fixture.root;
  try {
    writeReport(fixture.report, { dependency: [], archunit: [], pmd: [] }, { inventoryOnly: true });
    assert.equal(git(fixture, 'init', '-q').status, 0);
    fs.writeFileSync(path.join(fixture.root, 'README.md'), 'legacy project base\n');
    assert.equal(git(fixture, 'add', 'README.md').status, 0);
    assert.equal(git(fixture, 'commit', '-qm', 'legacy project base').status, 0);

    const workflow = path.join(fixture.root, '.github/workflows/pmo-doc-check.yml');
    fs.mkdirSync(path.dirname(workflow), { recursive: true });
    fs.writeFileSync(workflow, '# Managed by Mango CLI.\nname: PMO Documentation Checks\n');
    assert.equal(git(fixture, 'add', workflow).status, 0);
    const accepted = run(fixture, [
      '--base-ref', 'HEAD',
      '--allow-missing-for-governance-upgrade'
    ]);
    assert.equal(accepted.status, 0, accepted.stderr);
    assert.equal(accepted.report.action, 'missing-budget-governance-upgrade');

    writeReport(fixture.report, { dependency: [], archunit: [], pmd: [] }, { inventoryOnly: false });
    const nonInventory = run(fixture, [
      '--base-ref', 'HEAD',
      '--allow-missing-for-governance-upgrade'
    ]);
    assert.equal(nonInventory.status, 2);
    assert.match(nonInventory.stderr, /requires an inventoryOnly full-Reactor report/u);
    writeReport(fixture.report, { dependency: [], archunit: [], pmd: [] }, { inventoryOnly: true });

    const businessSource = path.join(fixture.root, 'backend/src/main/java/BusinessService.java');
    fs.mkdirSync(path.dirname(businessSource), { recursive: true });
    fs.writeFileSync(businessSource, 'class BusinessService {}\n');
    assert.equal(git(fixture, 'add', businessSource).status, 0);
    const rejected = run(fixture, [
      '--base-ref', 'HEAD',
      '--allow-missing-for-governance-upgrade'
    ]);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /pure PMO\/workflow upgrade.*forbidden changes/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboards one legacy module scope and verifies it with a trusted full report', () => {
  const fixture = createOnboardingFixture();
  try {
    const onboarded = runOnboarding(fixture);
    assert.equal(onboarded.status, 0, onboarded.stderr);
    assert.equal(onboarded.report.action, 'module-onboarded');
    assert.deepEqual(onboarded.report.selectedModules, [
      'backend/modules/guarantee/guarantee-api',
      'backend/modules/guarantee/guarantee-core',
      'backend/modules/guarantee/guarantee-starter'
    ]);

    const verified = run(fixture, ['--base-ref', 'HEAD']);
    assert.equal(verified.status, 0, verified.stderr);
    assert.equal(verified.report.action, 'module-onboarding-verified');
    const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    const [record] = Object.values(budget.moduleOnboardings);
    assert.equal(record.baseCommit, git(fixture, 'rev-parse', 'HEAD').stdout.trim());
    assert.equal(record.moduleName, 'guarantee');
    assert.equal(record.modulePath, '/guarantee');
    assert.equal(budget.totalIssueCount, 2);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboards from a committed empty project budget while the trusted report supplies the Reactor catalog', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(git(fixture, 'reset', '-q', 'HEAD', '--', fixture.moduleProperties).status, 0);
    const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    budget.modules = {};
    budget.totalIssueCount = 0;
    budget.engines = { archunit: 0, dependency: 0, pmd: 0 };
    budget.rules = {};
    budget.identities = {};
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(budget, null, 2)}\n`);
    assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
    assert.equal(git(fixture, 'commit', '--amend', '-qm', 'empty project architecture budget').status, 0);
    assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);

    const onboarded = runOnboarding(fixture);
    assert.equal(onboarded.status, 0, onboarded.stderr);
    assert.equal(onboarded.report.action, 'module-onboarded');
    assert.equal(run(fixture, ['--base-ref', 'HEAD']).status, 0);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboarding requires an explicitly inventory-only report', () => {
  for (const inventoryOnly of [undefined, false, 'true']) {
    const fixture = createOnboardingFixture();
    try {
      writeOnboardingReport(fixture.report, [], [], { inventoryOnly });
      const rejected = runOnboarding(fixture);
      assert.equal(rejected.status, 2);
      assert.match(
        rejected.stderr,
        inventoryOnly === 'true'
          ? /inventoryOnly must be boolean/u
          : /requires an inventoryOnly full-Reactor report/u
      );
    } finally {
      fs.rmSync(fixture.root, { recursive: true, force: true });
    }
  }
});

test('onboarding module.properties must be owned by a selected starter module', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(git(fixture, 'reset', '-q', 'HEAD', '--', fixture.moduleProperties).status, 0);
    fs.rmSync(fixture.moduleProperties);
    fixture.moduleProperties = path.join(
      fixture.root,
      'backend/modules/guarantee/guarantee-core/src/main/resources/META-INF/mango/module.properties'
    );
    fs.mkdirSync(path.dirname(fixture.moduleProperties), { recursive: true });
    fs.writeFileSync(
      fixture.moduleProperties,
      'module-name=guarantee\nmodule-path=/guarantee\nmodule-number=900\n'
    );
    assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);
    const rejected = runOnboarding(fixture);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /must belong to a selected Maven starter module/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboarding rejects a base that is not an ancestor of HEAD', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(git(fixture, 'commit', '-qm', 'future onboarding commit').status, 0);
    const future = git(fixture, 'rev-parse', 'HEAD').stdout.trim();
    assert.equal(git(fixture, 'reset', '-q', 'HEAD^').status, 0);
    assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);
    const rejected = runOnboarding(fixture, ['--base-ref', future]);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /must be an ancestor of HEAD/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboarding rejects duplicate module-name and module-path identities', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(git(fixture, 'reset', '-q', 'HEAD', '--', fixture.moduleProperties).status, 0);
    const existing = path.join(
      fixture.root,
      'backend/modules/existing/existing-starter/src/main/resources/META-INF/mango/module.properties'
    );
    fs.mkdirSync(path.dirname(existing), { recursive: true });
    fs.writeFileSync(existing, 'module-name=guarantee\nmodule-path=/existing\n');
    assert.equal(git(fixture, 'add', existing).status, 0);
    assert.equal(git(fixture, 'commit', '--amend', '-qm', 'base with existing identity').status, 0);
    assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);
    const rejected = runOnboarding(fixture);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /module-name guarantee is already declared/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('a schemaVersion 4 base without onboarding metadata remains compatible', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(git(fixture, 'reset', '-q', 'HEAD', '--', fixture.moduleProperties).status, 0);
    const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    delete budget.moduleOnboardings;
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(budget, null, 2)}\n`);
    assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
    assert.equal(git(fixture, 'commit', '--amend', '-qm', 'legacy schema v4 budget').status, 0);
    assert.equal(git(fixture, 'add', fixture.moduleProperties).status, 0);
    const onboarded = runOnboarding(fixture);
    assert.equal(onboarded.status, 0, onboarded.stderr);
    const updated = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    assert.equal(Object.keys(updated.moduleOnboardings).length, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('fresh onboarding cannot pass the CI baseline-only shortcut', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(runOnboarding(fixture).status, 0);
    const checked = spawnSync(process.execPath, [
      checker,
      '--baseline', fixture.baseline,
      '--baseline-only',
      '--base-ref', 'HEAD',
      '--json'
    ], { cwd: fixture.root, encoding: 'utf8' });
    assert.equal(checked.status, 1, checked.stderr);
    assert.equal(JSON.parse(checked.stdout).action, 'onboarding-report-required');
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboarding refuses a PR that contains business source changes', () => {
  const fixture = createOnboardingFixture();
  try {
    const source = path.join(
      fixture.root,
      'backend/modules/guarantee/guarantee-core/src/main/java/GuaranteeService.java'
    );
    fs.mkdirSync(path.dirname(source), { recursive: true });
    fs.writeFileSync(source, 'class GuaranteeService {}\n');
    assert.equal(git(fixture, 'add', source).status, 0);
    const before = fs.readFileSync(fixture.baseline, 'utf8');
    const rejected = runOnboarding(fixture);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /forbidden changes/u);
    assert.equal(fs.readFileSync(fixture.baseline, 'utf8'), before);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('onboarding refuses debt increases outside the selected module scope', () => {
  const fixture = createOnboardingFixture();
  try {
    writeOnboardingReport(fixture.report, [{
      ruleId: 'MANGO-ARCH-ENTITY-001',
      subject: 'BillingEntity',
      message: 'Unrelated billing debt',
      moduleKey: 'backend/modules/billing/billing-core'
    }]);
    const rejected = runOnboarding(fixture);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /selected module scope/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('trusted CI rejects tampered onboarding audit metadata', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(runOnboarding(fixture).status, 0);
    const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    const [recordKey] = Object.keys(budget.moduleOnboardings);
    budget.moduleOnboardings[recordKey].modulePropertiesSha256 = '0'.repeat(64);
    fs.writeFileSync(fixture.baseline, `${JSON.stringify(budget, null, 2)}\n`);
    const rejected = run(fixture, ['--base-ref', 'HEAD']);
    assert.equal(rejected.status, 2);
    assert.match(rejected.stderr, /audit metadata/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('later PRs cannot remove or modify an existing onboarding record', () => {
  for (const mutation of ['remove', 'modify']) {
    const fixture = createOnboardingFixture();
    try {
      assert.equal(runOnboarding(fixture).status, 0);
      assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
      assert.equal(git(fixture, 'commit', '-qm', 'onboard guarantee module').status, 0);
      const budget = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
      const [recordKey] = Object.keys(budget.moduleOnboardings);
      if (mutation === 'remove') {
        delete budget.moduleOnboardings[recordKey];
      } else {
        budget.moduleOnboardings[recordKey].reason = 'silently rewritten reason';
      }
      fs.writeFileSync(fixture.baseline, `${JSON.stringify(budget, null, 2)}\n`);
      const rejected = run(fixture, ['--base-ref', 'HEAD']);
      assert.equal(rejected.status, 1, rejected.stderr);
      assert.equal(rejected.report.action, 'onboarding-record-tampered');
    } finally {
      fs.rmSync(fixture.root, { recursive: true, force: true });
    }
  }
});

test('a later business PR keeps history allowed but rejects a new identity', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(runOnboarding(fixture).status, 0);
    assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
    assert.equal(git(fixture, 'commit', '-qm', 'onboard guarantee module').status, 0);

    const unchanged = run(fixture, ['--base-ref', 'HEAD']);
    assert.equal(unchanged.status, 0, unchanged.stderr);

    writeOnboardingReport(fixture.report, [{
      ruleId: 'MANGO-ARCH-CTRL-002',
      subject: 'NewCreditPageController',
      message: 'New controller violation',
      moduleKey: 'backend/modules/guarantee/guarantee-starter'
    }]);
    const rejected = run(fixture, ['--base-ref', 'HEAD']);
    assert.equal(rejected.status, 1);
    assert.equal(rejected.report.action, 'check');
    assert.equal(rejected.report.comparison.identityIncreases.length, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('later debt reduction preserves the immutable onboarding record', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(runOnboarding(fixture).status, 0);
    assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
    assert.equal(git(fixture, 'commit', '-qm', 'onboard guarantee module').status, 0);
    const before = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8')).moduleOnboardings;

    writeOnboardingReport(fixture.report, [], ['LegacyGuaranteeServiceImpl']);
    const reduced = run(fixture, [
      '--module', 'backend/modules/guarantee',
      '--write'
    ]);
    assert.equal(reduced.status, 0, reduced.stderr);
    const after = JSON.parse(fs.readFileSync(fixture.baseline, 'utf8'));
    assert.deepEqual(after.moduleOnboardings, before);
    assert.equal(after.totalIssueCount, 1);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});

test('an onboarded module cannot repeat first governance after merge', () => {
  const fixture = createOnboardingFixture();
  try {
    assert.equal(runOnboarding(fixture).status, 0);
    assert.equal(git(fixture, 'add', fixture.baseline).status, 0);
    assert.equal(git(fixture, 'commit', '-qm', 'onboard guarantee module').status, 0);
    const repeated = spawnSync(process.execPath, [
      checker,
      '--report', fixture.report,
      '--baseline', fixture.baseline,
      '--onboard-module', 'backend/modules/guarantee',
      '--module-properties', fixture.moduleProperties,
      '--base-ref', 'HEAD',
      '--reason', 'repeat onboarding must fail',
      '--write'
    ], { cwd: fixture.root, encoding: 'utf8' });
    assert.equal(repeated.status, 2);
    assert.match(repeated.stderr, /overlaps an existing onboarding record|already exists in the onboarding base/u);
  } finally {
    fs.rmSync(fixture.root, { recursive: true, force: true });
  }
});
