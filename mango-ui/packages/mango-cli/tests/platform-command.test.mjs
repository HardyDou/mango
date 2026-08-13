import assert from 'node:assert/strict';
import test from 'node:test';
import {
  commandForPlatform,
  commandPathCandidates,
  isCommandAvailable,
  resolveCommandForPlatform,
  spawnCommandSync,
  shouldUseShellForCommand,
} from '../src/platform-command.mjs';

test('Windows resolves command shims through cmd executables', () => {
  for (const command of ['mango', 'mvn', 'npm', 'npx', 'pnpm', 'yarn']) {
    assert.deepEqual(resolveCommandForPlatform(command, 'win32'), {
      command: `${command}.cmd`,
      shell: true,
    });
  }
});

test('Windows preserves explicit cmd commands and native executables', () => {
  assert.equal(commandForPlatform('mvn.cmd', 'win32'), 'mvn.cmd');
  assert.equal(shouldUseShellForCommand('mvn.cmd', 'win32'), true);
  assert.deepEqual(resolveCommandForPlatform('git', 'win32'), {
    command: 'git',
    shell: false,
  });
});

test('Windows resolves PATH and PATHEXT directly', () => {
  assert.deepEqual(
    commandPathCandidates('mvn', 'win32', {
      PATH: 'C:\\Maven\\bin;C:\\Windows\\System32',
      PATHEXT: '.EXE;.CMD',
    }),
    ['C:\\Maven\\bin\\mvn.cmd', 'C:\\Windows\\System32\\mvn.cmd'],
  );
  assert.equal(isCommandAvailable('mvn', 'win32', { PATH: '', PATHEXT: '.CMD' }), false);
});

test('Unix command availability requires an executable PATH entry', () => {
  assert.equal(isCommandAvailable('node', 'linux', { PATH: process.env.PATH }), true);
  assert.equal(isCommandAvailable('mvn', 'linux', { PATH: '' }), false);
});

test('explicit command paths are not rewritten', () => {
  assert.deepEqual(commandPathCandidates('.\\tools\\mvn', 'win32', { PATH: '' }), ['.\\tools\\mvn']);
});

test('non-Windows platforms preserve command names and do not enable a shell', () => {
  assert.deepEqual(resolveCommandForPlatform('mvn', 'linux'), {
    command: 'mvn',
    shell: false,
  });
});

test('synchronous execution preserves arguments without shell interpretation on the current platform', () => {
  const value = 'argument with spaces & metacharacters';
  const result = spawnCommandSync(process.execPath, ['-e', 'process.stdout.write(process.argv[1])', value], {
    encoding: 'utf8',
  });
  assert.equal(result.status, 0);
  assert.equal(result.stdout, value);
});
