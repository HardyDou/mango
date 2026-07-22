import assert from 'node:assert/strict';
import test from 'node:test';
import { shouldRunDevInstall } from '../src/dev-install-policy.mjs';

const backendApp = {
  type: 'spring-boot-maven',
  install: { command: 'mvn', args: ['install'] },
};

test('backend install remains enabled by default', () => {
  assert.equal(shouldRunDevInstall(backendApp), true);
  assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: 'true' }), true);
  assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: 'unexpected' }), true);
});

test('backend install is skipped only for an explicit false value', () => {
  for (const value of ['false', 'FALSE', '0', 'no', 'off']) {
    assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: value }), false);
  }
});

test('non-backend install commands are not affected by the backend switch', () => {
  const commandApp = { type: 'command', install: { command: 'pnpm', args: ['install'] } };
  assert.equal(shouldRunDevInstall(commandApp, { MANGO_BACKEND_AUTO_INSTALL: 'false' }), true);
  assert.equal(shouldRunDevInstall({ type: 'spring-boot-maven' }, {}), false);
});
