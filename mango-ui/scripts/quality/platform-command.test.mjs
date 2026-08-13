import assert from 'node:assert/strict';
import test from 'node:test';
import { commandForPlatform, resolveCommandForPlatform, shouldUseShellForCommand } from '../platform-command.mjs';

test('Windows resolves package manager shims as cmd commands executed through a shell', () => {
  for (const command of ['npm', 'pnpm', 'npx']) {
    assert.deepEqual(resolveCommandForPlatform(command, 'win32'), {
      command: `${command}.cmd`,
      shell: true,
    });
  }
});

test('Windows leaves native executables unchanged and does not enable a shell', () => {
  assert.equal(commandForPlatform('git', 'win32'), 'git');
  assert.equal(shouldUseShellForCommand('git', 'win32'), false);
});

test('non-Windows platforms preserve package manager command names', () => {
  assert.deepEqual(resolveCommandForPlatform('pnpm', 'linux'), {
    command: 'pnpm',
    shell: false,
  });
});
