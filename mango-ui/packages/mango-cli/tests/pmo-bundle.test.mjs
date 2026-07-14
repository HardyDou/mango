import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { chmodSync, copyFileSync, cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testRoot = dirname(fileURLToPath(import.meta.url));
const cliRoot = resolve(testRoot, '..');
const uiRoot = resolve(cliRoot, '../..');
const cli = join(cliRoot, 'src/index.mjs');
const pmoRoot = join(uiRoot, 'packages/mango-pmo');
const packageExportsCheck = join(uiRoot, 'scripts/check-package-exports.mjs');
const publishPackage = join(uiRoot, 'scripts/publish-package.mjs');
const PMO_BASELINE_TEST_PATH = 'business-pmo/mango-baseline';

test('PMO bundle install, locked repair, stale cleanup, and rollback', () => {
  execFileSync(process.execPath, ['scripts/build-package.mjs'], { cwd: pmoRoot, stdio: 'pipe' });
  execFileSync(process.execPath, ['scripts/check-package.mjs'], { cwd: pmoRoot, stdio: 'pipe' });

  const tempRoot = mkdtempSync(join(tmpdir(), 'mango-pmo-bundle-'));
  const projectRoot = join(tempRoot, 'project');
  try {
    run([cli, 'init', 'project', '--preset', 'custom', '--modules', 'none'], tempRoot);
    run([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);

    const manifestPath = join(projectRoot, 'business-pmo/mango-baseline/baseline.json');
    const lockPath = join(projectRoot, 'business-pmo/pmo-lock.json');
    const skillStatePath = join(projectRoot, '.agents/skills/.mango-pmo.json');
    const manifest = readJson(manifestPath);
    const lock = readJson(lockPath);
    const skillState = readJson(skillStatePath);
    assert.equal(lock.packageVersion, manifest.packageVersion);
    assert.equal(lock.bundleSha256, manifest.bundleSha256);
    assert.equal(lock.contracts.length, manifest.contracts.length);
    assert.ok(skillState.roots.includes('mango-pmo-lifecycle'));
    assert.ok(existsSync(join(projectRoot, '.agents/skills/mango-pmo-lifecycle/SKILL.md')));

    const currentLifecycleSkill = join(projectRoot, '.agents/skills/mango-pmo-lifecycle');
    const legacyLifecycleSkill = join(projectRoot, '.agents/skills/mango-pm-lifecycle');
    cpSync(currentLifecycleSkill, legacyLifecycleSkill, { recursive: true });
    rmSync(currentLifecycleSkill, { recursive: true, force: true });
    const legacyManifest = readJson(manifestPath);
    legacyManifest.files = legacyManifest.files.map(file => ({
      ...file,
      path: file.path.replace(
        'skills/mango-pmo-lifecycle/',
        'skills/mango-pm-lifecycle/',
      ),
    }));
    legacyManifest.bundleSha256 = createHash('sha256').update(JSON.stringify({
      files: legacyManifest.files,
      contracts: legacyManifest.contracts,
      plugin: legacyManifest.plugin ?? null,
    })).digest('hex');
    writeFileSync(manifestPath, `${JSON.stringify(legacyManifest, null, 2)}\n`);
    const legacyLock = readJson(lockPath);
    legacyLock.bundleSha256 = legacyManifest.bundleSha256;
    writeFileSync(lockPath, `${JSON.stringify(legacyLock, null, 2)}\n`);
    run([
      cli,
      'pmo',
      'upgrade',
      '--project-dir',
      projectRoot,
      '--to',
      manifest.packageVersion,
    ], projectRoot);
    assert.equal(existsSync(legacyLifecycleSkill), false);
    assert.ok(existsSync(join(currentLifecycleSkill, 'SKILL.md')));

    const mismatchedLock = { ...lock, bundleSha256: '0'.repeat(64) };
    writeFileSync(lockPath, `${JSON.stringify(mismatchedLock, null, 2)}\n`);
    const lockedMismatch = runFailure([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);
    assert.match(lockedMismatch.stdout, /does not match installed/);
    const refusedSync = runFailure([cli, 'pmo', 'sync', '--project-dir', projectRoot], projectRoot);
    assert.match(refusedSync.stderr, /sync repairs the locked/);
    assert.equal(readJson(lockPath).bundleSha256, mismatchedLock.bundleSha256);
    run([
      cli,
      'pmo',
      'upgrade',
      '--project-dir',
      projectRoot,
      '--to',
      manifest.packageVersion,
    ], projectRoot);
    assert.equal(readJson(lockPath).bundleSha256, manifest.bundleSha256);

    const businessSkillPath = join(projectRoot, '.agents/skills/business-owned/SKILL.md');
    mkdirSync(dirname(businessSkillPath), { recursive: true });
    writeFileSync(businessSkillPath, '# Business owned\n');
    const forgedState = readJson(skillStatePath);
    forgedState.roots.push('business-owned');
    writeFileSync(skillStatePath, `${JSON.stringify(forgedState, null, 2)}\n`);
    run([cli, 'pmo', 'sync', '--project-dir', projectRoot], projectRoot);
    assert.equal(readFileSync(businessSkillPath, 'utf8'), '# Business owned\n');

    const previousVersion = '0.9.0-test';
    setInstalledPmoVersion(projectRoot, previousVersion);
    run([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);
    run([
      cli,
      'pmo',
      'upgrade',
      '--project-dir',
      projectRoot,
      '--to',
      manifest.packageVersion,
    ], projectRoot);
    run([
      cli,
      'pmo',
      'rollback',
      '--project-dir',
      projectRoot,
      '--to',
      previousVersion,
    ], projectRoot);
    assert.equal(readJson(lockPath).packageVersion, previousVersion);
    run([
      cli,
      'pmo',
      'upgrade',
      '--project-dir',
      projectRoot,
      '--to',
      manifest.packageVersion,
    ], projectRoot);

    const stalePath = join(projectRoot, 'business-pmo/mango-baseline/rules/stale-rule.md');
    writeFileSync(stalePath, '# stale\n');
    const staleCheck = runFailure([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);
    assert.match(staleCheck.stdout, /stale baseline files/);
    run([cli, 'pmo', 'sync', '--project-dir', projectRoot], projectRoot);
    assert.equal(existsSync(stalePath), false);

    const skillPath = join(projectRoot, '.agents/skills/mango-pmo-lifecycle/SKILL.md');
    const originalSkill = readFileSync(skillPath, 'utf8');
    writeFileSync(skillPath, `${originalSkill}\ncorrupt\n`);
    const skillCheck = runFailure([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);
    assert.match(skillCheck.stdout, /project PMO skill files differ/);
    const repair = run([cli, 'pmo', 'sync', '--project-dir', projectRoot], projectRoot);
    assert.match(repair.stdout, /no user-level Codex plugin installation was performed/);
    assert.equal(readFileSync(skillPath, 'utf8'), originalSkill);

    const beforeUnavailableUpgrade = readJson(lockPath);
    const unavailable = runFailure([
      cli,
      'pmo',
      'upgrade',
      '--project-dir',
      projectRoot,
      '--to',
      '0.0.0-test',
    ], projectRoot);
    assert.match(unavailable.stderr, /is not available to this CLI/);
    assert.deepEqual(readJson(lockPath), beforeUnavailableUpgrade);

    const rollback = run([
      cli,
      'pmo',
      'rollback',
      '--project-dir',
      projectRoot,
      '--to',
      manifest.packageVersion,
    ], projectRoot);
    assert.match(rollback.stdout, /PMO rollback complete/);
    run([cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], projectRoot);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('PMO bundle install refuses an unowned project skill collision without partial writes', () => {
  execFileSync(process.execPath, ['scripts/build-package.mjs'], { cwd: pmoRoot, stdio: 'pipe' });
  const tempRoot = mkdtempSync(join(tmpdir(), 'mango-pmo-collision-'));
  const projectRoot = join(tempRoot, 'project');
  const userSkill = join(projectRoot, '.agents/skills/mango-pmo-lifecycle/SKILL.md');
  try {
    mkdirSync(dirname(userSkill), { recursive: true });
    writeFileSync(userSkill, '# User-owned collision\n');
    const result = runFailure([cli, 'pmo', 'upgrade', '--project-dir', projectRoot], projectRoot);
    assert.match(result.stderr, /exists but is not owned by the current PMO bundle/);
    assert.equal(readFileSync(userSkill, 'utf8'), '# User-owned collision\n');
    assert.equal(existsSync(join(projectRoot, PMO_BASELINE_TEST_PATH)), false);
    assert.equal(existsSync(join(projectRoot, 'business-pmo/pmo-lock.json')), false);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('published PMO package exports plugin Skills and rejects a tampered bundle', () => {
  execFileSync(process.execPath, ['scripts/build-package.mjs'], { cwd: pmoRoot, stdio: 'pipe' });
  run([packageExportsCheck, '--package=@mango/pmo'], uiRoot);
  run([publishPackage, `--verify-pmo-package-root=${pmoRoot}`], uiRoot);

  const tempRoot = mkdtempSync(join(tmpdir(), 'mango-pmo-published-'));
  const packageRoot = join(tempRoot, 'package');
  try {
    mkdirSync(packageRoot, { recursive: true });
    copyFileSync(join(pmoRoot, 'package.json'), join(packageRoot, 'package.json'));
    for (const directory of ['dist', '.codex-plugin', 'skills']) {
      cpSync(join(pmoRoot, directory), join(packageRoot, directory), { recursive: true });
    }
    const skillPath = join(packageRoot, 'skills/mango-pmo-lifecycle/SKILL.md');
    const originalSkill = readFileSync(skillPath, 'utf8');
    writeFileSync(skillPath, `${originalSkill}\ntampered\n`);
    const tampered = runFailure([
      publishPackage,
      `--verify-pmo-package-root=${packageRoot}`,
    ], uiRoot);
    assert.match(tampered.stderr, /plugin file differs from its manifest/);

    if (process.platform !== 'win32') {
      writeFileSync(skillPath, originalSkill);
      chmodSync(join(packageRoot, 'dist/baseline/tools/pmo-preflight.mjs'), 0o644);
      const wrongMode = runFailure([
        publishPackage,
        `--verify-pmo-package-root=${packageRoot}`,
      ], uiRoot);
      assert.match(wrongMode.stderr, /baseline file differs from its manifest/);
    }
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});

function run(args, cwd) {
  const result = spawnSync(process.execPath, args, { cwd, encoding: 'utf8' });
  assert.equal(result.status, 0, `${args.join(' ')} failed:\n${result.stdout}\n${result.stderr}`);
  return result;
}

function runFailure(args, cwd) {
  const result = spawnSync(process.execPath, args, { cwd, encoding: 'utf8' });
  assert.notEqual(result.status, 0, `${args.join(' ')} unexpectedly succeeded`);
  return result;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function setInstalledPmoVersion(projectRoot, version) {
  for (const relativePath of [
    'business-pmo/mango-baseline/baseline.json',
    'business-pmo/pmo-lock.json',
    '.agents/skills/.mango-pmo.json',
  ]) {
    const path = join(projectRoot, relativePath);
    const value = readJson(path);
    value.packageVersion = version;
    writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
  }
}
