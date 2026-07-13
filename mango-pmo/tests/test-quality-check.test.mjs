import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/test-quality-check.mjs');

function createRepository() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-test-quality-'));
  execFileSync('git', ['init', '-b', 'main'], { cwd: root });
  return root;
}

function write(root, relativePath, source) {
  const target = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, source);
}

function run(root, paths) {
  const result = spawnSync(process.execPath, [checker, '--paths', paths, '--json'], {
    cwd: root,
    encoding: 'utf8'
  });
  return { status: result.status, report: JSON.parse(result.stdout) };
}

test('accepts assertions over executed behavior', () => {
  const root = createRepository();
  try {
    write(root, 'src/test/java/example/CalculatorTest.java', [
      'class CalculatorTest {',
      '  void adds() { assertEquals(4, new Calculator().add(2, 2)); }',
      '}'
    ].join('\n'));
    const result = run(root, 'src/test');
    assert.equal(result.status, 0);
    assert.equal(result.report.filesChecked, 1);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects tautologies and same-literal assertions', () => {
  const root = createRepository();
  try {
    const tautology = 'assertTrue(' + 'true);';
    const sameValue = 'expect(10)' + '.toBe(10);';
    write(root, 'src/test/java/example/MeaninglessTest.java', `class MeaninglessTest { void test() { ${tautology} } }`);
    write(root, 'ui/src/__tests__/meaningless.spec.ts', `test('x', () => { ${sameValue} });`);
    const result = run(root, 'src/test,ui/src/__tests__');
    assert.equal(result.status, 1);
    assert.deepEqual(result.report.issues.map((issue) => issue.ruleId), ['PMO-TEST-001', 'PMO-TEST-001']);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects mocking the class named as the subject under test', () => {
  const root = createRepository();
  try {
    const annotation = '@' + 'Mock';
    write(root, 'src/test/java/example/OrderServiceTest.java', [
      'class OrderServiceTest {',
      `  ${annotation} private OrderService orderService;`,
      '}'
    ].join('\n'));
    const result = run(root, 'src/test');
    assert.equal(result.status, 1);
    assert.equal(result.report.issues[0].ruleId, 'PMO-TEST-002');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
