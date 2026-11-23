import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/check-migration-duplicates.mjs');

function createRoot(files) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'migration-duplicates-'));
  for (const [relativePath, content] of Object.entries(files)) {
    const target = path.join(root, relativePath);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, content);
  }
  return root;
}

function run(root, baselineContent) {
  const args = [checker, '--root', root];
  if (baselineContent !== null) {
    const baseline = path.join(root, 'baseline.txt');
    fs.writeFileSync(baseline, baselineContent);
    args.push('--baseline', baseline);
  } else {
    args.push('--baseline', path.join(root, 'missing-baseline.txt'));
  }
  return spawnSync(process.execPath, args, { encoding: 'utf8' });
}

const MODULE = 'module/src/main/resources/db/migration/demo';

test('fails when two migrations create the same table', () => {
  const root = createRoot({
    [`${MODULE}/V1__init.sql`]: 'CREATE TABLE IF NOT EXISTS `demo_order` (`id` bigint);\n',
    [`${MODULE}/V2__more.sql`]: 'create table demo_order (`id` bigint);\n',
  });

  const result = run(root, null);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /demo\/demo_order/);
  assert.match(result.stderr, /V1__init\.sql, V2__more\.sql/);
});

test('passes when the duplicate is registered in the baseline', () => {
  const root = createRoot({
    [`${MODULE}/V1__init.sql`]: 'CREATE TABLE demo_order (`id` bigint);\n',
    [`${MODULE}/V2__more.sql`]: 'CREATE TABLE demo_order (`id` bigint);\n',
  });

  const result = run(root, 'demo:demo_order -- 历史重复，前向处理\n');

  assert.equal(result.status, 0);
  assert.match(result.stdout, /duplicates=1, allowlisted=1/);
});

test('passes when tables are created in different modules', () => {
  const root = createRoot({
    [`${MODULE}/V1__init.sql`]: 'CREATE TABLE demo_order (`id` bigint);\n',
    ['other/src/main/resources/db/migration/finance/V1__init.sql']: 'CREATE TABLE demo_order (`id` bigint);\n',
  });

  const result = run(root, null);

  assert.equal(result.status, 0);
  assert.match(result.stdout, /duplicates=0/);
});

test('ignores duplicated names inside the same file', () => {
  const root = createRoot({
    [`${MODULE}/V1__init.sql`]: 'CREATE TABLE demo_order (`id` bigint);\n-- CREATE TABLE demo_order (`id` bigint);\n',
  });

  const result = run(root, null);

  assert.equal(result.status, 0);
});

test('warns about stale baseline entries', () => {
  const root = createRoot({
    [`${MODULE}/V1__init.sql`]: 'CREATE TABLE demo_order (`id` bigint);\n',
  });

  const result = run(root, 'demo:demo_gone -- 已删除的重复\n');

  assert.equal(result.status, 0);
  assert.match(result.stderr, /no longer applies: demo:demo_gone/);
});
