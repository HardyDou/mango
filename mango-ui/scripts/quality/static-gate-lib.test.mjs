import assert from 'node:assert/strict';
import test from 'node:test';
import {
  assertToolExecution,
  collectDiagnosticIdentities,
  compareIdentityMultisets,
  compareMetrics,
} from './static-gate-lib.mjs';

test('ratchet accepts equal or reduced debt and rejects regression', () => {
  assert.deepEqual(
    compareMetrics('eslint', { fatal: 0, errors: 3, warnings: 8 }, { fatal: 0, errors: 4, warnings: 8 }),
    [],
  );
  assert.deepEqual(
    compareMetrics('eslint', { fatal: 0, errors: 5, warnings: 8 }, { fatal: 0, errors: 4, warnings: 8 }),
    [{ metric: 'errors', actual: 5, allowed: 4 }],
  );
});

test('strict mode requires every metric to be zero', () => {
  assert.deepEqual(compareMetrics('prettier', { files: 1 }, { files: 10 }, true), [
    { metric: 'files', actual: 1, allowed: 0 },
  ]);
});

test('identity ratchet rejects same-count diagnostic replacement', () => {
  assert.deepEqual(compareIdentityMultisets(['old', 'new'], ['old', 'other']), ['new']);
  assert.deepEqual(compareIdentityMultisets(['old'], ['old', 'old']), []);
  assert.deepEqual(compareIdentityMultisets(['old', 'old'], ['old']), ['old']);
});

test('strict identity mode rejects every diagnostic', () => {
  assert.deepEqual(compareIdentityMultisets(['legacy'], ['legacy'], true), ['legacy']);
});

test('diagnostic identities ignore line movement but preserve file, rule, severity, and message', () => {
  const first = collectDiagnosticIdentities(
    'eslint',
    [
      {
        filePath: '/repo/apps/demo.ts',
        messages: [{ severity: 2, ruleId: 'demo/rule', messageId: 'bad', message: 'Bad value', line: 2 }],
      },
    ],
    '/repo',
  );
  const moved = collectDiagnosticIdentities(
    'eslint',
    [
      {
        filePath: '/repo/apps/demo.ts',
        messages: [{ severity: 2, ruleId: 'demo/rule', messageId: 'bad', message: 'Bad value', line: 200 }],
      },
    ],
    '/repo',
  );
  assert.deepEqual(first, moved);
  assert.match(first[0], /^apps\/demo\.ts\|error\|demo\/rule\|bad\|Bad value$/u);
});

test('diagnostic identities normalize the UI root embedded in messages', () => {
  const diagnostic = (root) => ({
    results: [
      {
        diagnostics: [
          {
            workspace: 'demo',
            file: `${root}/apps/demo.ts`,
            severity: 'error',
            code: 'TS2322',
            message: `Type from ${root}/packages/source.ts is not assignable`,
          },
        ],
      },
    ],
  });
  const host = collectDiagnosticIdentities('typecheck', diagnostic('/Users/demo/mango-ui'), '/Users/demo/mango-ui');
  const container = collectDiagnosticIdentities('typecheck', diagnostic('/workspace/mango-ui'), '/workspace/mango-ui');

  assert.deepEqual(host, container);
  assert.match(host[0], /<ui-root>\/packages\/source\.ts/u);
});

test('prettier identities bind an unformatted file to its content', () => {
  const first = collectDiagnosticIdentities('prettier', ['demo.ts'], '/repo', () => 'const x=1');
  const changed = collectDiagnosticIdentities('prettier', ['demo.ts'], '/repo', () => 'const x = 2');
  assert.notDeepEqual(first, changed);
});

test('tool execution fails closed for spawn errors, signals, and unexpected statuses', () => {
  assert.throws(() => assertToolExecution({ error: new Error('missing') }, [0], 'eslint'), /failed to start/u);
  assert.throws(() => assertToolExecution({ signal: 'SIGKILL' }, [0], 'eslint'), /SIGKILL/u);
  assert.throws(() => assertToolExecution({ status: 2, stderr: 'bad config' }, [0, 1], 'eslint'), /bad config/u);
  assert.doesNotThrow(() => assertToolExecution({ status: 1 }, [0, 1], 'eslint'));
});
