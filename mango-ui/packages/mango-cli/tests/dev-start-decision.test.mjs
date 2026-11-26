import assert from 'node:assert/strict';
import test from 'node:test';
import { DEV_START_ACTIONS, DEV_START_REASONS, decideDevAppStart } from '../src/dev-start-decision.mjs';

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
  assert.equal(decision.message, 'backend is already running: pid=4321');
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
