import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { RELEASE_STATES, findMangoRepository, redactReleaseText, runReleaseCli } from '../src/release-command.mjs';

test('release lifecycle uses the five local-first states', () => {
  assert.deepEqual(RELEASE_STATES, [
    'PREPARED',
    'CANDIDATE_VERIFIED',
    'PUBLISHED',
    'CONSUMER_VERIFIED',
    'COMPLETED',
  ]);
});

test('release repository lookup walks upward and fails outside Mango source', () => {
  const repository = path.resolve(import.meta.dirname, '../../../..');
  assert.equal(findMangoRepository(path.join(repository, 'mango-ui/packages/mango-cli')), repository);
  const outside = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-release-outside-'));
  try {
    assert.throws(() => findMangoRepository(outside), /inside a Mango source repository/u);
  } finally {
    fs.rmSync(outside, { recursive: true, force: true });
  }
});

test('removed verify command cannot re-enter the legacy state machine', async () => {
  await assert.rejects(() => runReleaseCli(['verify']), /was removed/u);
});

test('release output redacts credentials', () => {
  const text = redactReleaseText(
    'token=plain https://user:pass@example.test Bearer abc.def npm-secret',
    { MANGO_NPM_TOKEN: 'npm-secret' },
  );
  assert.equal(text.includes('plain'), false);
  assert.equal(text.includes('user:pass'), false);
  assert.equal(text.includes('abc.def'), false);
  assert.equal(text.includes('npm-secret'), false);
});

test('registry doctor fails closed before running npm when registry roles are missing', async () => {
  let stderr = '';
  await assert.rejects(
    () =>
      runReleaseCli(['registry', 'doctor'], {
        cwd: process.cwd(),
        env: {},
        stdout: { write() {} },
        stderr: { write(chunk) { stderr += chunk; } },
      }),
    /doctor failed/u,
  );
  assert.match(stderr, /publish registry/u);
  assert.match(stderr, /consume registry/u);
});

test('registry doctor requires the complete Maven role tuple when any Maven role is configured', async () => {
  let stderr = '';
  await assert.rejects(
    () => runReleaseCli(['registry', 'doctor'], {
      cwd: process.cwd(),
      env: {
        MANGO_RELEASE_NPM_PUBLISH_REGISTRY: 'https://registry.example/npm-hosted/',
        MANGO_RELEASE_NPM_CONSUME_REGISTRY: 'https://registry.example/npm-group/',
        MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY: 'https://registry.example/maven-releases/',
      },
      stdout: { write() {} },
      stderr: { write(chunk) { stderr += chunk; } },
    }),
    /doctor failed/u,
  );
  assert.match(stderr, /Maven consume registry/u);
  assert.match(stderr, /Maven publish settings\.xml server id/u);
  assert.match(stderr, /Maven consume settings\.xml server id/u);
});
