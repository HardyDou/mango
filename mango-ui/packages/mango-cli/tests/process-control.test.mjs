import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import test from 'node:test';
import { isProcessAlive, isProcessGroupAlive, stopProcessGroup } from '../src/process-control.mjs';

const unixOnly = process.platform === 'win32' ? test.skip : test;

test('stopProcessGroup rejects unsafe process ids before signal handling', async () => {
  const invalidProcessIds = [0, -1, Number.NaN, 1.5, 'not-a-pid'];
  const originalKill = process.kill;
  let signalAttempts = 0;
  process.kill = (...args) => {
    signalAttempts += 1;
    return originalKill(...args);
  };

  try {
    for (const pid of invalidProcessIds) {
      assert.equal(isProcessAlive(pid), false);
      assert.equal(isProcessGroupAlive(pid), false);
      await assert.rejects(stopProcessGroup(pid), {
        name: 'TypeError',
        message: 'pid must be a positive safe integer',
      });
    }
    assert.equal(signalAttempts, 0);
  } finally {
    process.kill = originalKill;
  }
});

unixOnly('stopProcessGroup waits for the whole group after its leader exits', async () => {
  const leader = spawn(
    process.execPath,
    [
      '-e',
      [
        "const { spawn } = require('node:child_process');",
        "const child = spawn(process.execPath, ['-e', \"process.on('SIGTERM', () => {}); setInterval(() => {}, 1000)\"], { stdio: 'ignore' });",
        'console.log(child.pid);',
        'process.exit(0);',
      ].join(' '),
    ],
    { detached: true, stdio: ['ignore', 'pipe', 'inherit'] },
  );
  const childPid = Number((await readFirstLine(leader.stdout)).trim());
  await once(leader, 'exit');
  await new Promise((resolvePromise) => setTimeout(resolvePromise, 150));

  try {
    assert.equal(isProcessAlive(leader.pid), false);
    assert.equal(isProcessGroupAlive(leader.pid), true);

    const outcome = await stopProcessGroup(leader.pid, {
      graceMs: 100,
      killWaitMs: 3000,
      pollIntervalMs: 25,
    });

    assert.deepEqual(outcome, { stopped: true, forced: true });
    assert.equal(isProcessGroupAlive(leader.pid), false);
  } finally {
    if (isProcessAlive(childPid)) {
      process.kill(childPid, 'SIGKILL');
    }
  }
});

unixOnly('stopProcessGroup reports graceful termination without SIGKILL', async () => {
  const child = spawn(
    process.execPath,
    ['-e', "process.on('SIGTERM', () => process.exit(0)); setInterval(() => {}, 1000)"],
    { detached: true, stdio: 'ignore' },
  );
  assert.equal(isProcessGroupAlive(child.pid), true);

  const outcome = await stopProcessGroup(child.pid, {
    graceMs: 3000,
    killWaitMs: 1000,
    pollIntervalMs: 25,
  });

  assert.deepEqual(outcome, { stopped: true, forced: false });
  assert.equal(isProcessGroupAlive(child.pid), false);
});

async function readFirstLine(stream) {
  let text = '';
  for await (const chunk of stream) {
    text += chunk.toString();
    const newline = text.indexOf('\n');
    if (newline >= 0) {
      return text.slice(0, newline);
    }
  }
  return text;
}
