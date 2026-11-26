/**
 * Decides what `mango dev start` does with one app before spawning it.
 *
 * <p>The decision exists because a workspace only tracks the processes it started itself: a backend
 * that is already serving its port from a bare `java`/`mvn` invocation, from another terminal, or
 * after a `dev start` was interrupted looks identical to a free port, and a `dependsOn` expansion
 * turns that into a hard failure that also blocks unrelated apps (running `dev start frontend` dies
 * on a backend port it never intended to start).
 *
 * <p>Rules:
 * <ul>
 *   <li>a live pid file wins: the app is managed by this workspace and is left alone;</li>
 *   <li>a busy port without a live pid file is treated as "already running elsewhere" for apps that
 *       were pulled in through {@code dependsOn}, so the requested apps still start;</li>
 *   <li>a busy port for an app the caller explicitly asked for still fails, because the caller asked
 *       for that app and the CLI cannot know what is serving the port.</li>
 * </ul>
 */

export const DEV_START_ACTIONS = Object.freeze({
  START: 'start',
  SKIP: 'skip',
  FAIL: 'fail',
});

export const DEV_START_REASONS = Object.freeze({
  MANAGED: 'already-running',
  EXTERNAL: 'port-served-externally',
  CONFLICT: 'explicit-port-conflict',
});

export function decideDevAppStart(input) {
  const name = input?.name || 'app';
  const port = Number(input?.port) || 0;
  if (input?.managedPidAlive) {
    const pid = input?.managedPid?.pid ? `pid=${input.managedPid.pid}` : 'pid=unknown';
    return {
      action: DEV_START_ACTIONS.SKIP,
      reason: DEV_START_REASONS.MANAGED,
      message: `${name} is already running: ${pid}`,
    };
  }
  if (!input?.portInUse) {
    return { action: DEV_START_ACTIONS.START, reason: '', message: '' };
  }
  const occupants = input.occupantText || '';
  if (input.explicit) {
    return {
      action: DEV_START_ACTIONS.FAIL,
      reason: DEV_START_REASONS.CONFLICT,
      message:
        `${name} port ${port} is already in use${occupants}${input.ownerHint || ''}. ` +
        `Stop that process, or start a group that does not include ${name}.`,
    };
  }
  return {
    action: DEV_START_ACTIONS.SKIP,
    reason: DEV_START_REASONS.EXTERNAL,
    message:
      `${name}: skipped, port ${port} is already served${occupants}${input.ownerHint || ''} ` +
      'and no live pid file belongs to this workspace, so it is treated as already running',
  };
}

/**
 * Classifies a pid file that exists on disk but may no longer describe a live process.
 *
 * <p>A pid file outlives its process in two common ways: the process exited (possibly as a zombie
 * that the parent never reaped, which still answers `kill(pid, 0)` on some platforms) or the pid was
 * recycled by an unrelated command. Neither should keep `dev start` from starting the app, but both
 * must be visible: silently ignoring the file leaves the operator wondering why nothing started, and
 * trusting a recycled pid would make `dev stop` kill somebody else's process.
 */

export const MANAGED_PID_STATES = Object.freeze({
  ABSENT: 'absent',
  RUNNING: 'running',
  STALE_GONE: 'stale-process-gone',
  STALE_UNRELATED: 'stale-process-reused',
});

const MANAGED_COMMAND_HINTS = Object.freeze(['node', 'java', 'mvn', 'pnpm', 'npm', 'yarn', 'bun']);

export function assessManagedPid(input) {
  const pid = input?.pidFile?.pid;
  if (!pid) {
    return { state: MANAGED_PID_STATES.ABSENT, message: '' };
  }
  const name = input?.name || 'app';
  if (!input?.alive) {
    return {
      state: MANAGED_PID_STATES.STALE_GONE,
      message: `pid file is stale (pid ${pid} is no longer running), cleaned up and starting again`,
    };
  }
  const commandLine = typeof input?.commandLine === 'string' ? input.commandLine.trim() : '';
  if (commandLine && !MANAGED_COMMAND_HINTS.some((hint) => commandLine.includes(hint))) {
    const command = commandLine.split(/\s+/).slice(0, 2).join(' ');
    return {
      state: MANAGED_PID_STATES.STALE_UNRELATED,
      message: `pid file is stale (pid ${pid} now belongs to "${command}"), cleaned up and starting again`,
    };
  }
  return { state: MANAGED_PID_STATES.RUNNING, message: `${name} is already running: pid=${pid}` };
}
