import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildWindowsShellCommand,
  commandForPlatform,
  commandPathCandidates,
  isCommandAvailable,
  resolveCommandForPlatform,
  spawnCommandSync,
  shouldUseShellForCommand,
} from '../src/platform-command.mjs';

test('Windows shell commands quote each argument before cmd parses metacharacters', () => {
  assert.equal(
    buildWindowsShellCommand('mvn.cmd', [
      '-Dspring-boot.run.arguments=runtime --spring.datasource.url=jdbc:mysql://127.0.0.1/db?a=1&b=2',
      'spring-boot:run',
    ]),
    '"mvn.cmd" "-Dspring-boot.run.arguments=runtime --spring.datasource.url=jdbc:mysql://127.0.0.1/db?a=1&b=2" "spring-boot:run"',
  );
});

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
  assert.deepEqual(
    commandPathCandidates('mvn', 'win32', {
      Path: 'C:\\Maven\\bin',
      PathExt: '.CMD',
    }),
    ['C:\\Maven\\bin\\mvn.cmd'],
  );
});

test('Unix command availability requires an executable PATH entry', () => {
  assert.equal(isCommandAvailable(process.execPath, 'linux', { PATH: '' }), true);
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

test('Windows command shims preserve spaces and shell metacharacters', { skip: process.platform !== 'win32' }, () => {
  const result = spawnCommandSync('mvn', ['--version', '-Dprobe=value with spaces & metacharacters'], {
    encoding: 'utf8',
    env: { ...process.env },
  });
  assert.equal(result.status, 0);
  assert.match(result.stdout, /Apache Maven/u);
  assert.equal(result.stderr, '');
});
