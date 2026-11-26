import assert from 'node:assert/strict';
import test from 'node:test';
import {
  DEV_START_ACTIONS,
  DEV_START_REASONS,
  MANAGED_PID_STATES,
  assessManagedPid,
  decideDevAppStart,
} from '../src/dev-start-decision.mjs';

test('a live pid file keeps the managed app untouched', () => {
  const decision = decideDevAppStart({
    name: 'backend',
    port: 18006,
    explicit: true,
    managedPid: { pid: 4321 },
    managedPidAlive: true,
    portInUse: true,
  });
  assert.equal(decision.action, DEV_START_ACTIONS.SKIP);
  assert.equal(decision.reason, DEV_START_REASONS.MANAGED);
  assert.equal(
    decision.message,
    'backend is already running: pid=4321. If it is not responding, run `mango dev restart backend`',
  );
});

test('a free port starts the app', () => {
  const decision = decideDevAppStart({ name: 'frontend', port: 30012, explicit: true, portInUse: false });
  assert.equal(decision.action, DEV_START_ACTIONS.START);
  assert.equal(decision.message, '');
});

test('a dependency whose port is served without a pid file is skipped, not fatal', () => {
  const decision = decideDevAppStart({
    name: 'baohan-system-service',
    port: 18006,
    explicit: false,
    managedPid: null,
    managedPidAlive: false,
    portInUse: true,
    occupantText: ' (pid=2778767 command=java)',
    ownerHint: ' owner=/data/Yunxin/baohan/baohan-system',
  });
  assert.equal(decision.action, DEV_START_ACTIONS.SKIP);
  assert.equal(decision.reason, DEV_START_REASONS.EXTERNAL);
  assert.match(decision.message, /^baohan-system-service: skipped, port 18006 is already served/u);
  assert.match(decision.message, /pid=2778767/u);
  assert.match(decision.message, /owner=\/data\/Yunxin/u);
  assert.match(decision.message, /treated as already running$/u);
});

test('a stale pid file pointing at a dead process is treated like no pid file', () => {
  const decision = decideDevAppStart({
    name: 'backend',
    port: 18006,
    explicit: false,
    managedPid: { pid: 2778767 },
    managedPidAlive: false,
    portInUse: true,
  });
  assert.equal(decision.action, DEV_START_ACTIONS.SKIP);
  assert.equal(decision.reason, DEV_START_REASONS.EXTERNAL);
});

test('an explicitly requested app still fails on a busy port', () => {
  const decision = decideDevAppStart({
    name: 'backend',
    port: 18006,
    explicit: true,
    managedPid: { pid: 2778767 },
    managedPidAlive: false,
    portInUse: true,
    occupantText: ' (pid=2778767 command=java)',
  });
  assert.equal(decision.action, DEV_START_ACTIONS.FAIL);
  assert.equal(decision.reason, DEV_START_REASONS.CONFLICT);
  assert.match(decision.message, /^backend port 18006 is already in use/u);
  assert.match(decision.message, /start a group that does not include backend/u);
});

test('apps without a port only depend on the pid file', () => {
  assert.equal(
    decideDevAppStart({ name: 'worker', port: 0, explicit: true, portInUse: false }).action,
    DEV_START_ACTIONS.START,
  );
  assert.equal(
    decideDevAppStart({ name: 'worker', port: 0, explicit: false, managedPidAlive: true, portInUse: false }).action,
    DEV_START_ACTIONS.SKIP,
  );
});

test('a pid file whose process is gone is stale and gets cleaned up', () => {
  const managed = assessManagedPid({ name: 'backend', pidFile: { pid: 2778767 }, alive: false });
  assert.equal(managed.state, MANAGED_PID_STATES.STALE_GONE);
  assert.equal(managed.message, 'pid file is stale (pid 2778767 is no longer running), cleaned up and starting again');
});

test('a recycled pid that now runs an unrelated command is stale, not a managed app', () => {
  const managed = assessManagedPid({
    name: 'frontend',
    pidFile: { pid: 4321 },
    alive: true,
    commandLine: 'python3 -m http.server 30012',
  });
  assert.equal(managed.state, MANAGED_PID_STATES.STALE_UNRELATED);
  assert.ok(managed.message.includes('now belongs to "python3 -m"'));
});

test('a live managed command keeps the app untouched', () => {
  const nodeManaged = assessManagedPid({
    name: 'frontend',
    pidFile: { pid: 11 },
    alive: true,
    commandLine: 'node /repo/node_modules/@mango/cli/src/index.mjs dev start',
  });
  const javaManaged = assessManagedPid({
    name: 'backend',
    pidFile: { pid: 12 },
    alive: true,
    commandLine: '/usr/bin/java -jar app.jar',
  });
  assert.equal(nodeManaged.state, MANAGED_PID_STATES.RUNNING);
  assert.equal(javaManaged.state, MANAGED_PID_STATES.RUNNING);
  assert.equal(javaManaged.message, 'backend is already running: pid=12');
});

test('an unknown command line trusts the pid instead of guessing', () => {
  const managed = assessManagedPid({
    name: 'backend',
    pidFile: { pid: 13 },
    alive: true,
    commandLine: null,
  });
  assert.equal(managed.state, MANAGED_PID_STATES.RUNNING);
});

test('no pid file means there is nothing to assess', () => {
  const managed = assessManagedPid({ name: 'backend', pidFile: null, alive: false });
  assert.equal(managed.state, MANAGED_PID_STATES.ABSENT);
  assert.equal(managed.message, '');
});

test('explicit target that this workspace already runs points at its health check', () => {
  const decision = decideDevAppStart({
    name: 'backend',
    port: 20001,
    explicit: true,
    managedPid: { pid: 4242 },
    managedPidAlive: true,
    healthUrl: 'http://127.0.0.1:20001/actuator/health',
  });
  assert.equal(decision.action, 'skip');
  assert.equal(decision.reason, 'already-running');
  assert.match(decision.message, /backend is already running: pid=4242/);
  assert.match(decision.message, /health check is http:\/\/127\.0\.0\.1:20001\/actuator\/health/);
  assert.match(decision.message, /mango dev restart backend/);
});

test('dependency skip stays quiet about restarts', () => {
  const decision = decideDevAppStart({
    name: 'backend',
    port: 20001,
    explicit: false,
    managedPid: { pid: 4242 },
    managedPidAlive: true,
    healthUrl: 'http://127.0.0.1:20001/actuator/health',
  });
  assert.equal(decision.message, 'backend is already running: pid=4242');
});
