import assert from 'node:assert/strict';
import test from 'node:test';
import { shouldRunDevInstall } from '../src/dev-install-policy.mjs';

const backendApp = {
  type: 'spring-boot-maven',
  install: { command: 'mvn', args: ['install'] },
};

test('spring boot development never executes the legacy install command', () => {
  assert.equal(shouldRunDevInstall(backendApp), false);
  assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: 'true' }), false);
  assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: 'unexpected' }), false);
});

test('legacy backend install is ignored regardless of the old environment switch', () => {
  for (const value of ['false', 'FALSE', '0', 'no', 'off', 'true']) {
    assert.equal(shouldRunDevInstall(backendApp, { MANGO_BACKEND_AUTO_INSTALL: value }), false);
  }
});

test('non-backend install commands are not affected by the backend switch', () => {
  const commandApp = { type: 'command', install: { command: 'pnpm', args: ['install'] } };
  assert.equal(shouldRunDevInstall(commandApp, { MANGO_BACKEND_AUTO_INSTALL: 'false' }), true);
  assert.equal(shouldRunDevInstall({ type: 'spring-boot-maven' }, {}), false);
});
