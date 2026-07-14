import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');

test('architecture verification POM is published without a partial-Reactor verify phase', () => {
  const result = spawnSync(
    path.join(root, 'scripts/publish-maven-batch.sh'),
    [
      'mango-tools/mango-architecture-verification',
      '--release-version',
      '1.0.17',
      '--skip-verify',
      '--dry-run'
    ],
    { cwd: root, encoding: 'utf8' }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /Reactor deploy modules: none/);
  assert.match(result.stdout, /process-resources/);
  assert.match(result.stdout, /maven-deploy-plugin:3\.1\.4:deploy-file/);
  assert.doesNotMatch(result.stdout, /-pl .*mango-architecture-verification.* deploy/);
});

test('an explicit Maven batch can include the version-matched docs bundle', () => {
  const result = spawnSync(
    path.join(root, 'scripts/publish-maven-batch.sh'),
    [
      'mango-tools/mango-architecture-verification',
      '--include-docs-bundle',
      '--release-version',
      '1.0.18',
      '--skip-verify',
      '--dry-run'
    ],
    { cwd: root, encoding: 'utf8' }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /Include docs bundle: true/);
  assert.match(result.stdout, /Publishing version-matched Mango documentation bundle/);
  assert.match(result.stdout, /HEAD:mango-docs/);
  assert.match(result.stdout, /-DartifactId=mango-docs-bundle/);
  assert.match(result.stdout, /-Dversion=1\.0\.18/);
});

test('all-non-app discovery excludes nested Maven test fixtures', () => {
  const result = spawnSync(
    path.join(root, 'scripts/publish-maven-batch.sh'),
    [
      '--all-non-app',
      '--release-version',
      '1.0.20',
      '--skip-verify',
      '--dry-run'
    ],
    { cwd: root, encoding: 'utf8' }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /Publish scope: all non-app Maven modules/);
  assert.doesNotMatch(result.stdout, /src\/test\/resources\/architecture-path-binding/);
});
