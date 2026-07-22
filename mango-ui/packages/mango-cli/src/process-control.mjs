import { spawnSync } from 'node:child_process';

const DEFAULT_STOP_GRACE_MS = 60000;
const DEFAULT_KILL_WAIT_MS = 5000;
const DEFAULT_POLL_INTERVAL_MS = 100;

export function isProcessAlive(pid) {
  const numericPid = positiveProcessId(pid);
  if (numericPid === null) {
    return false;
  }
  try {
    process.kill(numericPid, 0);
  } catch {
    return false;
  }
  const stat = spawnSync('ps', ['-o', 'stat=', '-p', String(numericPid)], { encoding: 'utf8' });
  if (stat.error?.code === 'ENOENT') {
    return true;
  }
  return stat.status === 0 && !stat.stdout.trim().startsWith('Z');
}

export function isProcessGroupAlive(processGroupId) {
  const numericProcessGroupId = positiveProcessId(processGroupId);
  if (numericProcessGroupId === null) {
    return false;
  }
  if (process.platform === 'win32') {
    return isProcessAlive(numericProcessGroupId);
  }
  try {
    process.kill(-numericProcessGroupId, 0);
    return true;
  } catch (error) {
    return error?.code === 'EPERM';
  }
}

export async function stopProcessGroup(pid, options = {}) {
  const numericPid = positiveProcessId(pid);
  if (numericPid === null) {
    throw new TypeError('pid must be a positive safe integer');
  }
  const graceMs = positiveDuration(options.graceMs, DEFAULT_STOP_GRACE_MS);
  const killWaitMs = positiveDuration(options.killWaitMs, DEFAULT_KILL_WAIT_MS);
  const pollIntervalMs = positiveDuration(options.pollIntervalMs, DEFAULT_POLL_INTERVAL_MS);
  const signalScope = sendSignal(numericPid, 'SIGTERM');
  if (signalScope === null) {
    return { stopped: true, forced: false };
  }
  const isAlive = signalScope === 'group' ? isProcessGroupAlive : isProcessAlive;
  if (await waitUntilStopped(() => isAlive(numericPid), graceMs, pollIntervalMs)) {
    return { stopped: true, forced: false };
  }
  sendSignal(numericPid, 'SIGKILL', signalScope);
  const stopped = await waitUntilStopped(() => isAlive(numericPid), killWaitMs, pollIntervalMs);
  return { stopped, forced: true };
}

function sendSignal(pid, signal, preferredScope) {
  if (preferredScope !== 'process' && process.platform !== 'win32') {
    try {
      process.kill(-pid, signal);
      return 'group';
    } catch (error) {
      if (error?.code !== 'ESRCH') {
        throw error;
      }
      if (preferredScope === 'group') {
        return null;
      }
    }
  }
  try {
    process.kill(pid, signal);
    return 'process';
  } catch (error) {
    if (error?.code === 'ESRCH') {
      return null;
    }
    throw error;
  }
}

async function waitUntilStopped(isAlive, timeoutMs, pollIntervalMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (!isAlive()) {
      return true;
    }
    await new Promise((resolvePromise) => setTimeout(resolvePromise, pollIntervalMs));
  }
  return !isAlive();
}

function positiveDuration(value, defaultValue) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : defaultValue;
}

function positiveProcessId(value) {
  const numeric =
    typeof value === 'number' ? value : typeof value === 'string' && value.trim() !== '' ? Number(value) : Number.NaN;
  return Number.isSafeInteger(numeric) && numeric > 0 ? numeric : null;
}
