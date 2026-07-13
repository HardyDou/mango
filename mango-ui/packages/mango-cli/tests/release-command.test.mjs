import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {
  RELEASE_STATES,
  doctorReleaseConfig,
  redactReleaseText,
  releaseApplicability,
  resolveReleaseConfig,
  runReleaseCli
} from '../src/release-command.mjs';

function tempProject(config) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-release-command-'));
  fs.writeFileSync(path.join(root, '.mango-release.json'), `${JSON.stringify(config, null, 2)}\n`);
  return root;
}

function disabledArtifacts() {
  return {
    maven: { mode: 'disabled', disabledReason: 'test fixture has no Maven artifact' },
    npm: { mode: 'disabled', disabledReason: 'test fixture has no npm artifact' }
  };
}

function passAdapter() {
  return { command: process.execPath, args: ['-e', 'process.exit(0)'] };
}

function fixtureConfig(overrides = {}) {
  const notApplicable = {
    pr: 'test fixture has no pull request',
    tag: 'test fixture has no immutable tag',
    'github-release': 'test fixture has no GitHub Release',
    'cli-lock': 'test fixture has no CLI lock',
    'docs-latest': 'test fixture has no documentation site',
    'post-verify': 'test fixture has no consumer runtime'
  };
  const stateAdapters = Object.fromEntries(
    ['source', 'versions', 'changelog', 'readmes', 'tests'].map((state) => [
      state,
      { verify: passAdapter() }
    ])
  );
  return {
    schemaVersion: 1,
    releaseKind: 'npm-only',
    manifestDirectory: '.mango/releases',
    artifacts: disabledArtifacts(),
    notApplicable,
    stateAdapters,
    ...overrides
  };
}

function outputSink() {
  let value = '';
  return {
    stream: { write(chunk) { value += chunk; } },
    read() { return value; }
  };
}

async function invoke(root, args, env = {}) {
  const stdout = outputSink();
  const stderr = outputSink();
  try {
    const result = await runReleaseCli(args, {
      cwd: root,
      env: { ...process.env, ...env },
      stdout: stdout.stream,
      stderr: stderr.stream
    });
    return { result, error: null, stdout: stdout.read(), stderr: stderr.read() };
  } catch (error) {
    return { result: null, error, stdout: stdout.read(), stderr: stderr.read() };
  }
}

test('publish persists every required state with a non-empty auditable reason', async () => {
  const root = tempProject(fixtureConfig());
  try {
    const published = await invoke(root, [
      'publish', '--version', '1.2.3', '--authorize', '--json'
    ]);
    assert.equal(published.error, null, published.error?.message);
    assert.equal(published.result.manifest.completed, true);
    assert.deepEqual(Object.keys(published.result.manifest.states), RELEASE_STATES);
    for (const state of RELEASE_STATES) {
      const item = published.result.manifest.states[state];
      assert.match(item.status, /^(passed|failed|pending|not_applicable)$/);
      assert.equal(item.reason.trim().length > 0, true, state);
      if (item.status === 'passed') {
        assert.equal(item.evidence.length > 0, true, state);
        for (const evidence of item.evidence) {
          assert.equal(evidence.command.trim().length > 0, true, state);
          assert.equal(Number.isInteger(evidence.exitCode), true, state);
          assert.equal(evidence.output.trim().length > 0, true, state);
          assert.equal(Number.isFinite(Date.parse(evidence.startedAt)), true, state);
          assert.equal(Number.isFinite(Date.parse(evidence.completedAt)), true, state);
        }
      } else if (item.status === 'not_applicable') {
        assert.equal(item.evidence.length, 0, state);
      }
    }
    assert.equal(fs.existsSync(published.result.manifestFile), true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('repair resumes a failed state and skips states that already passed', async () => {
  const marker = 'tests-pass.marker';
  const config = fixtureConfig();
  config.stateAdapters.tests.verify = {
    command: process.execPath,
    args: [
      '-e',
      'process.exit(require("node:fs").existsSync(process.argv[1]) ? 0 : 9)',
      `{projectRoot}/${marker}`
    ]
  };
  const root = tempProject(config);
  try {
    const first = await invoke(root, ['publish', '--version', '2.0.0', '--authorize']);
    assert.match(first.error.message, /stopped before completion/);
    const manifestPath = path.join(root, '.mango/releases/2.0.0/manifest.json');
    const failed = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    assert.equal(failed.states.tests.status, 'failed');
    assert.equal(failed.states.source.attempts, 1);

    fs.writeFileSync(path.join(root, marker), 'pass\n');
    const repaired = await invoke(root, ['repair', '--version', '2.0.0', '--authorize']);
    assert.equal(repaired.error, null, repaired.error?.message);
    assert.equal(repaired.result.manifest.completed, true);
    assert.equal(repaired.result.manifest.states.source.attempts, 1);
    assert.equal(repaired.result.manifest.states.tests.attempts, 2);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('repair refuses to repeat an immutable publish without a repair adapter', async () => {
  const config = fixtureConfig({
    releaseKind: 'maven',
    artifacts: {
      maven: { mode: 'artifact-only' },
      npm: { mode: 'disabled', disabledReason: 'Maven-only test fixture' }
    },
    notApplicable: {
      ...fixtureConfig().notApplicable,
      tag: 'tag behavior is isolated in another state',
      'github-release': 'GitHub release behavior is isolated in another state',
      'docs-snapshot': 'snapshot behavior is isolated in another state'
    }
  });
  config.stateAdapters.maven = {
    publish: { command: process.execPath, args: ['-e', 'process.exit(7)'] },
    verify: passAdapter()
  };
  const root = tempProject(config);
  try {
    const first = await invoke(root, ['publish', '--version', '3.0.0', '--authorize']);
    assert.match(first.error.message, /stopped before completion/);
    const repaired = await invoke(root, ['repair', '--version', '3.0.0', '--authorize']);
    assert.match(repaired.error.message, /stopped before completion/);
    const manifest = JSON.parse(fs.readFileSync(
      path.join(root, '.mango/releases/3.0.0/manifest.json'),
      'utf8'
    ));
    assert.match(manifest.states.maven.reason, /repair adapter required/);
    assert.equal(manifest.states.maven.attempts, 1);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('repair re-verifies a previously published immutable artifact without republishing', async () => {
  const config = fixtureConfig({
    releaseKind: 'maven',
    artifacts: {
      maven: { mode: 'artifact-only' },
      npm: { mode: 'disabled', disabledReason: 'Maven-only test fixture' }
    },
    notApplicable: {
      ...fixtureConfig().notApplicable,
      tag: 'tag behavior is isolated in another state',
      'github-release': 'GitHub release behavior is isolated in another state',
      'docs-snapshot': 'snapshot behavior is isolated in another state'
    }
  });
  config.stateAdapters.maven = {
    publish: {
      command: process.execPath,
      args: [
        '-e',
        'require("node:fs").appendFileSync(process.argv[1], "publish\\n")',
        '{projectRoot}/publish-count.log'
      ]
    },
    verify: {
      command: process.execPath,
      args: [
        '-e',
        'process.exit(require("node:fs").existsSync(process.argv[1]) ? 0 : 8)',
        '{projectRoot}/registry-ready.marker'
      ]
    }
  };
  const root = tempProject(config);
  try {
    const published = await invoke(root, ['publish', '--version', '3.1.0', '--authorize']);
    assert.equal(published.error, null, published.error?.message);
    const verified = await invoke(root, ['verify', '--version', '3.1.0']);
    assert.match(verified.error.message, /stopped before completion/);
    fs.writeFileSync(path.join(root, 'registry-ready.marker'), 'ready\n');
    const repaired = await invoke(root, ['repair', '--version', '3.1.0', '--authorize']);
    assert.equal(repaired.error, null, repaired.error?.message);
    assert.equal(
      fs.readFileSync(path.join(root, 'publish-count.log'), 'utf8').trim().split(/\r?\n/).length,
      1
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('configuration precedence is CLI, environment, user, then project', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-release-precedence-'));
  const user = path.join(root, 'user.json');
  fs.writeFileSync(path.join(root, '.mango-release.json'), JSON.stringify({
    artifacts: { maven: { mode: 'artifact-only' }, npm: { mode: 'artifact-only' } }
  }));
  fs.writeFileSync(user, JSON.stringify({ artifacts: { maven: { mode: 'disabled', disabledReason: 'user' } } }));
  try {
    const config = resolveReleaseConfig({
      projectDir: root,
      userConfigPath: user,
      mavenMode: 'public-registry'
    }, {
      MANGO_RELEASE_MAVEN_MODE: 'private-registry',
      MANGO_RELEASE_NPM_MODE: 'disabled',
      MANGO_RELEASE_NPM_DISABLED_REASON: 'environment'
    });
    assert.equal(config.artifacts.maven.mode, 'public-registry');
    assert.equal(config.artifacts.npm.mode, 'disabled');
    assert.equal(config.artifacts.npm.disabledReason, 'environment');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('registry doctor validates explicit modes and authentication references', () => {
  const config = {
    schemaVersion: 1,
    releaseKind: 'mixed',
    artifacts: {
      maven: { mode: 'private-registry', serverId: 'mango-releases' },
      npm: { mode: 'private-registry', tokenEnv: 'MANGO_NPM_TOKEN' }
    },
    registries: {
      maven: { publish: 'https://maven.example/releases', consume: 'https://maven.example/public' },
      npm: { publish: 'https://npm.example/hosted', consume: 'https://npm.example/group' }
    },
    stateAdapters: {},
    notApplicable: {}
  };
  assert.equal(doctorReleaseConfig(config, { MANGO_NPM_TOKEN: 'secret-value' }).passed, true);
  assert.equal(doctorReleaseConfig(config, {}).passed, false);
  assert.equal(
    doctorReleaseConfig({ ...config, releaseKind: 'unsupported' }, { MANGO_NPM_TOKEN: 'secret-value' }).passed,
    false
  );
  assert.equal(doctorReleaseConfig({
    ...config,
    stateAdapters: {
      maven: { repair: { command: 'mvn', args: ['deploy'] } }
    }
  }, { MANGO_NPM_TOKEN: 'secret-value' }).passed, false);
  assert.equal(doctorReleaseConfig({
    ...config,
    stateAdapters: {
      maven: { repair: [] }
    }
  }, { MANGO_NPM_TOKEN: 'secret-value' }).passed, false);
  assert.equal(doctorReleaseConfig({
    ...config,
    stateAdapters: {
      maven: {
        verify: passAdapter(),
        repair: { kind: 'verify-existing' }
      }
    }
  }, { MANGO_NPM_TOKEN: 'secret-value' }).passed, true);
});

test('status rejects a tampered completed manifest with incomplete evidence', async () => {
  const root = tempProject(fixtureConfig());
  try {
    const published = await invoke(root, [
      'publish', '--version', '4.0.0', '--authorize', '--json'
    ]);
    assert.equal(published.error, null, published.error?.message);
    const manifest = JSON.parse(fs.readFileSync(published.result.manifestFile, 'utf8'));
    manifest.states.tests.evidence[0].command = '';
    fs.writeFileSync(published.result.manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);

    const status = await invoke(root, ['status', '--version', '4.0.0']);
    assert.match(status.error.message, /state tests is invalid/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('status rejects changing an applicable required state to not_applicable', async () => {
  const root = tempProject(fixtureConfig());
  try {
    const published = await invoke(root, [
      'publish', '--version', '4.1.0', '--authorize', '--json'
    ]);
    assert.equal(published.error, null, published.error?.message);
    const manifest = JSON.parse(fs.readFileSync(published.result.manifestFile, 'utf8'));
    manifest.states.tests = {
      ...manifest.states.tests,
      status: 'not_applicable',
      reason: 'tampered required state',
      attempts: 0,
      evidence: []
    };
    fs.writeFileSync(published.result.manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);

    const status = await invoke(root, ['status', '--version', '4.1.0']);
    assert.match(status.error.message, /state tests is invalid/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('redaction removes credentials from command evidence and URLs', () => {
  const text = redactReleaseText(
    'token=plain https://user:pass@example.test Bearer abc.def npm-secret',
    { MANGO_NPM_TOKEN: 'npm-secret' }
  );
  assert.equal(text.includes('plain'), false);
  assert.equal(text.includes('user:pass'), false);
  assert.equal(text.includes('abc.def'), false);
  assert.equal(text.includes('npm-secret'), false);
});

test('Maven releases require snapshots while npm-only releases do not', () => {
  const maven = releaseApplicability({
    releaseKind: 'maven',
    artifacts: { maven: { mode: 'artifact-only' }, npm: { mode: 'disabled' } },
    notApplicable: {}
  });
  const npmOnly = releaseApplicability({
    releaseKind: 'npm-only',
    artifacts: { maven: { mode: 'disabled' }, npm: { mode: 'artifact-only' } },
    notApplicable: {}
  });
  assert.equal(maven['docs-snapshot'].applicable, true);
  assert.equal(npmOnly['docs-snapshot'].applicable, false);
});

test('the published CLI route exposes release status', () => {
  const root = tempProject(fixtureConfig());
  try {
    const result = spawnSync(process.execPath, [
      path.resolve(import.meta.dirname, '../src/index.mjs'),
      'release',
      'status',
      '--version',
      '4.0.0',
      '--project-dir',
      root,
      '--json'
    ], { encoding: 'utf8' });
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
    const output = JSON.parse(result.stdout);
    assert.equal(output.command, 'status');
    assert.equal(output.manifest.version, '4.0.0');
    assert.equal(output.plan.length > 0, true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
