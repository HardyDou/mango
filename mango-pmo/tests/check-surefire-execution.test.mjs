import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/check-surefire-execution.mjs');

function createRoot() {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'mango-surefire-execution-'));
}

function createModule(root, relative, { tests = 1, reports = 1 } = {}) {
  const moduleDirectory = path.join(root, relative);
  fs.mkdirSync(moduleDirectory, { recursive: true });
  fs.writeFileSync(path.join(moduleDirectory, 'pom.xml'), '<project/>');
  fs.mkdirSync(path.join(moduleDirectory, 'src', 'test', 'java', 'demo'), { recursive: true });
  fs.writeFileSync(path.join(moduleDirectory, 'src', 'test', 'java', 'demo', 'DemoTest.java'), 'class DemoTest {}');
  if (tests === null) {
    return moduleDirectory;
  }
  const reportDirectory = path.join(moduleDirectory, 'target', 'surefire-reports');
  fs.mkdirSync(reportDirectory, { recursive: true });
  for (let index = 0; index < reports; index += 1) {
    fs.writeFileSync(
      path.join(reportDirectory, `TEST-demo.DemoTest${index}.xml`),
      `<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="demo.DemoTest" tests="${tests}" failures="0" errors="0" skipped="0" />\n`
    );
  }
  return moduleDirectory;
}

function run(root, extra = []) {
  const result = spawnSync(process.execPath, [checker, '--root', root, '--json', ...extra], {
    encoding: 'utf8'
  });
  return { status: result.status, report: JSON.parse(result.stdout) };
}

test('accepts modules that executed tests', () => {
  const root = createRoot();
  try {
    createModule(root, 'mango/platform/core');
    const result = run(root);
    assert.equal(result.status, 0);
    assert.deepEqual(result.report.modules, [
      { module: 'mango/platform/core', tests: 1, reports: 1 }
    ]);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects modules whose surefire reports hold zero tests', () => {
  const root = createRoot();
  try {
    createModule(root, 'mango/platform/core', { tests: 0, reports: 2 });
    const result = run(root);
    assert.equal(result.status, 1);
    assert.deepEqual(
      result.report.issues.map((issue) => issue.ruleId),
      ['SUREFIRE_ZERO_TESTS']
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects modules with test sources but no reports', () => {
  const root = createRoot();
  try {
    createModule(root, 'mango/platform/core', { tests: null });
    const result = run(root);
    assert.equal(result.status, 1);
    assert.deepEqual(
      result.report.issues.map((issue) => issue.ruleId),
      ['SUREFIRE_REPORT_MISSING']
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('ignores modules without test sources', () => {
  const root = createRoot();
  try {
    createModule(root, 'mango/platform/core');
    fs.mkdirSync(path.join(root, 'mango', 'platform', 'api'), { recursive: true });
    const result = run(root);
    assert.equal(result.status, 0);
    assert.equal(result.report.modules.length, 1);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('passes with a warning when no module has runnable tests', () => {
  const root = createRoot();
  try {
    fs.mkdirSync(path.join(root, 'mango'), { recursive: true });
    fs.writeFileSync(path.join(root, 'mango', 'pom.xml'), '<project/>');
    const result = run(root);
    assert.equal(result.status, 0);
    assert.deepEqual(result.report.warnings, ['没有模块包含可执行测试，本次未校验用例执行数']);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('limits the check to the requested modules', () => {
  const root = createRoot();
  try {
    createModule(root, 'mango/platform/core');
    createModule(root, 'mango/platform/broken', { tests: 0 });
    const result = run(root, ['--modules', 'mango/platform/core']);
    assert.equal(result.status, 0);
    assert.deepEqual(result.report.modules.map((item) => item.module), ['mango/platform/core']);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
