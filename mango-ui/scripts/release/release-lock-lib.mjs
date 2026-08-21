import { randomUUID } from 'node:crypto';
import { closeSync, mkdirSync, openSync, readFileSync, unlinkSync, writeFileSync } from 'node:fs';
import { hostname } from 'node:os';
import { dirname } from 'node:path';

export function acquireReleaseLock(lockPath, identity) {
  mkdirSync(dirname(lockPath), { recursive: true });
  let descriptor;
  try {
    descriptor = openSync(lockPath, 'wx', 0o600);
  } catch (error) {
    if (error?.code === 'EEXIST') {
      throw new Error(`release lock already exists: ${lockPath}; inspect the process and journal before removing it`, {
        cause: error,
      });
    }
    throw error;
  }
  const lockId = randomUUID();
  writeFileSync(
    descriptor,
    `${JSON.stringify(
      {
        host: hostname(),
        pid: process.pid,
        startedAt: new Date().toISOString(),
        command: process.argv.join(' '),
        lockId,
        ...identity,
      },
      null,
      2,
    )}\n`,
  );
  closeSync(descriptor);
  let released = false;
  return () => {
    if (released) return;
    released = true;
    let current;
    try {
      current = JSON.parse(readFileSync(lockPath, 'utf8'));
    } catch (error) {
      if (error?.code === 'ENOENT') return;
      throw error;
    }
    if (current.lockId === lockId) unlinkSync(lockPath);
  };
}
