import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {
  RELEASE_COMMAND_MAX_BUFFER_BYTES,
  RELEASE_STATES,
  findMangoRepository,
  redactReleaseText,
  resolveMavenRegistryProbe,
  runReleaseCli,
} from '../src/release-command.mjs';

test('release commands retain complete high-volume build output', async () => {
  const repository = path.resolve(import.meta.dirname, '../../../..');
  const largeOutput = 'x'.repeat(2 * 1024 * 1024);
  let spawnOptions;
  let written = '';

  await runReleaseCli(['prepare', '--consume-registry', 'https://registry.example/npm-group/'], {
    cwd: repository,
    spawnSync(_command, _args, options) {
      spawnOptions = options;
      return { status: 0, stdout: largeOutput, stderr: '' };
    },
    stdout: {
      write(chunk) {
        written += chunk;
      },
    },
  });

  assert.equal(spawnOptions.maxBuffer, RELEASE_COMMAND_MAX_BUFFER_BYTES);
  assert.equal(written.length, largeOutput.length);
});

test('release lifecycle uses the five local-first states', () => {
  assert.deepEqual(RELEASE_STATES, ['PREPARED', 'CANDIDATE_VERIFIED', 'PUBLISHED', 'CONSUMER_VERIFIED', 'COMPLETED']);
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
  const text = redactReleaseText('token=plain https://user:pass@example.test Bearer abc.def npm-secret', {
    MANGO_NPM_TOKEN: 'npm-secret',
  });
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
        stderr: {
          write(chunk) {
            stderr += chunk;
          },
        },
      }),
    /doctor failed/u,
  );
  assert.match(stderr, /publish registry/u);
  assert.match(stderr, /consume registry/u);
});

test('registry doctor requires the complete Maven role tuple when any Maven role is configured', async () => {
  let stderr = '';
  await assert.rejects(
    () =>
      runReleaseCli(['registry', 'doctor'], {
        cwd: process.cwd(),
        env: {
          MANGO_RELEASE_NPM_PUBLISH_REGISTRY: 'https://registry.example/npm-hosted/',
          MANGO_RELEASE_NPM_CONSUME_REGISTRY: 'https://registry.example/npm-group/',
          MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY: 'https://registry.example/maven-releases/',
        },
        stdout: { write() {} },
        stderr: {
          write(chunk) {
            stderr += chunk;
          },
        },
      }),
    /doctor failed/u,
  );
  assert.match(stderr, /Maven consume registry/u);
  assert.match(stderr, /Maven publish settings\.xml server id/u);
  assert.match(stderr, /Maven consume settings\.xml server id/u);
});

test('registry doctor probes an exact published Maven coordinate when repository roots return 404', async () => {
  const repository = path.resolve(import.meta.dirname, '../../../..');
  const probe = resolveMavenRegistryProbe(repository);
  const commands = [];
  const output = await runReleaseCli(['registry', 'doctor', '--json'], {
    cwd: repository,
    env: {
      MANGO_RELEASE_NPM_PUBLISH_REGISTRY: 'https://registry.example/npm-hosted/',
      MANGO_RELEASE_NPM_CONSUME_REGISTRY: 'https://registry.example/npm-group/',
      MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY: 'https://registry.example/maven-releases/',
      MANGO_RELEASE_MAVEN_CONSUME_REGISTRY: 'https://registry.example/maven-public/',
      MANGO_RELEASE_MAVEN_PUBLISH_SERVER_ID: 'mango-releases',
      MANGO_RELEASE_MAVEN_CONSUME_SERVER_ID: 'mango-public',
    },
    spawnSync(command, args) {
      commands.push([command, ...args]);
      const url = args.at(-1) || '';
      if (command === 'curl' && /\/repository\/(?:maven-releases|maven-public)\/$/u.test(url)) {
        return { status: 22, stdout: '', stderr: 'HTTP 404' };
      }
      return { status: 0, stdout: command === 'npm' ? 'ok\n' : '', stderr: '' };
    },
    stdout: { write() {} },
    stderr: { write() {} },
  });

  assert.equal(output.passed, true);
  assert.deepEqual(output.maven.probe, probe);
  const curlUrls = commands.filter(([command]) => command === 'curl').map((command) => command.at(-1));
  assert.deepEqual(curlUrls, [
    `https://registry.example/maven-releases/${probe.path}`,
    `https://registry.example/maven-public/${probe.path}`,
  ]);
});
