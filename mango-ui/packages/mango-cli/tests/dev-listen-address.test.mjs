import assert from 'node:assert/strict';
import test from 'node:test';
import { describeDevListenAddress, isLoopbackHost, isWildcardHost } from '../src/dev-listen-address.mjs';

test('loopback and wildcard hosts are classified', () => {
  for (const host of ['127.0.0.1', 'localhost', '::1', '[::1]', '127.0.0.1 ']) {
    assert.equal(isLoopbackHost(host), true, host);
    assert.equal(isWildcardHost(host), false, host);
  }
  for (const host of ['0.0.0.0', '::', '[::]']) {
    assert.equal(isWildcardHost(host), true, host);
    assert.equal(isLoopbackHost(host), false, host);
  }
  assert.equal(isLoopbackHost('192.168.1.10'), false);
  assert.equal(isWildcardHost('192.168.1.10'), false);
});

test('frontend pinned to loopback explains how to expose it', () => {
  const notice = describeDevListenAddress({
    name: 'frontend',
    type: 'vite',
    port: 30012,
    host: '127.0.0.1',
    hostEnv: 'MANGO_FRONTEND_HOST',
  });
  assert.match(notice, /^http:\/\/127\.0\.0\.1:30012 \(loopback only;/u);
  assert.match(notice, /MANGO_FRONTEND_HOST=0\.0\.0\.0/u);
  assert.match(notice, /\.mango\/dev-workspace\.env/u);
});

test('frontend exposed on all interfaces is reported without the hint', () => {
  const notice = describeDevListenAddress({
    name: 'frontend',
    type: 'vite',
    port: 30012,
    host: '0.0.0.0',
    hostEnv: 'MANGO_FRONTEND_HOST',
  });
  assert.equal(notice, 'http://0.0.0.0:30012 (all interfaces)');
});

test('lan host is reported as reachable', () => {
  const notice = describeDevListenAddress({
    name: 'frontend',
    type: 'vite',
    port: 30012,
    host: '192.168.1.10',
    hostEnv: 'MANGO_FRONTEND_HOST',
  });
  assert.match(notice, /^http:\/\/192\.168\.1\.10:30012 \(host 192\.168\.1\.10;/u);
});

test('apps without a host env keep the plain url', () => {
  assert.equal(describeDevListenAddress({ name: 'backend', port: 18006, host: '127.0.0.1' }), 'http://127.0.0.1:18006');
});

test('apps without a port print nothing', () => {
  assert.equal(describeDevListenAddress({ name: 'worker', port: 0, host: '127.0.0.1' }), '');
  assert.equal(describeDevListenAddress({ name: 'worker', port: undefined }), '');
  assert.equal(describeDevListenAddress(undefined), '');
});

test('env file path is configurable for tests and future layouts', () => {
  const notice = describeDevListenAddress(
    { name: 'frontend', port: 30012, host: '127.0.0.1', hostEnv: 'MANGO_FRONTEND_HOST' },
    { envFile: '.mango/dev-workspace.local.json' },
  );
  assert.match(notice, /\.mango\/dev-workspace\.local\.json/u);
});
