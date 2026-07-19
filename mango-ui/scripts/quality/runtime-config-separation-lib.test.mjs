import assert from 'node:assert/strict';
import test from 'node:test';
import { validateRuntimeConfigSeparation } from './runtime-config-separation-lib.mjs';

const deploy = {
  profile: 'monolith',
  modules: { orders: { mode: 'local', runtimeCode: 'orders-local' } },
};
const development = {
  profile: 'hybrid',
  modules: { orders: { mode: 'micro', runtimeCode: 'orders-app', entry: 'http://localhost:5181/' } },
};

test('accepts a safe deploy config and a separately served development config', () => {
  assert.deepEqual(validateRuntimeConfigSeparation(deploy, development), []);
});

test('rejects development entries in public config or production dist', () => {
  const publicDevelopment = {
    profile: 'hybrid',
    modules: { orders: { mode: 'micro', entry: 'http://b.mango.io:5181/' } },
  };
  const failures = validateRuntimeConfigSeparation(publicDevelopment, development, [
    { path: 'assets/app.js', content: 'http://127.0.0.1:5181/' },
  ]);
  assert.match(failures.join('\n'), /monolith profile/);
  assert.match(failures.join('\n'), /only local modules/);
  assert.match(failures.join('\n'), /development micro-app entry/);
  assert.match(failures.join('\n'), /production dist/);
});
