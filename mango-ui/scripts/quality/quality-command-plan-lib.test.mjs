import assert from 'node:assert/strict';
import test from 'node:test';
import { createQualityCommandPlan } from './quality-command-plan-lib.mjs';

const records = [
  { name: '@mango/contracts', scripts: { build: 'build', test: 'test' } },
  { name: '@mango/domain', scripts: { build: 'build' } },
  { name: 'admin-app', scripts: { test: 'test' } },
];

function rendered(plan) {
  return plan.map(([command, arguments_]) => [command, ...arguments_].join(' '));
}

test('deep full profile preserves the existing complete frontend gate', () => {
  assert.deepEqual(rendered(createQualityCommandPlan(records, { mode: 'full' })), ['pnpm run check:full']);
});

test('PR full profile checks all build and test targets without running the serial deep gate', () => {
  const plan = rendered(
    createQualityCommandPlan(
      records,
      {
        mode: 'full',
        selected: records.map((item) => item.name),
        publishableChanged: [],
      },
      'pr',
    ),
  );
  assert.ok(plan.includes('pnpm --filter @mango/contracts --filter @mango/domain -r build'));
  assert.ok(plan.includes('pnpm --filter @mango/contracts --filter admin-app -r test'));
  assert.ok(plan.includes('pnpm package-exports:check'));
  assert.ok(plan.includes('pnpm typecheck'));
  assert.ok(!plan.includes('pnpm run check:full'));
  assert.ok(!plan.includes('pnpm package-consumer:typecheck'));
  assert.ok(plan.every((command) => !command.includes('test:e2e')));
});

test('PR affected profile runs selected builds, tests, and public-package consumer compatibility', () => {
  const plan = rendered(
    createQualityCommandPlan(
      records,
      {
        mode: 'affected',
        selected: ['@mango/contracts', 'admin-app'],
        publishableChanged: ['@mango/contracts'],
      },
      'pr',
    ),
  );
  assert.ok(plan.includes('pnpm --filter @mango/contracts -r build'));
  assert.ok(plan.includes('pnpm --filter @mango/contracts --filter admin-app -r test'));
  assert.ok(plan.includes('pnpm package-consumer:typecheck'));
});

test('PR non-frontend profile still produces a deterministic toolchain result', () => {
  assert.deepEqual(rendered(createQualityCommandPlan(records, { mode: 'none' }, 'pr')), ['pnpm quality:versions']);
});

test('unknown profile fails closed', () => {
  assert.throws(
    () => createQualityCommandPlan(records, { mode: 'none' }, 'quick'),
    /unsupported frontend quality profile: quick/,
  );
});
