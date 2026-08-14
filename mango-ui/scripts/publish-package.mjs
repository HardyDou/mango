#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import {
  findPackage,
  GROUP_REGISTRY,
  HOSTED_REGISTRY,
  commandForPlatform,
  normalizePackageName,
  npmView,
  readReleaseContracts,
  run,
  shouldUseShellForCommand,
  verifyPublishedPackage,
} from './release-guard-utils.mjs';
import { classifyNpmBatchRecovery } from './npm-batch-recovery.mjs';

function usage() {
  console.log(`Usage: pnpm publish:pkg <package|short-name> --publish-registry=<url> --consume-registry=<url> [--dry-run] [--skip-shared-gates]
       pnpm publish:pkg <package|short-name> --verify-only --publish-registry=<url> --consume-registry=<url> --release-tag=<tag>
       node scripts/publish-package.mjs --verify-pmo-package-root=<extracted-package-root>

Examples:
  pnpm publish:pkg common
  pnpm publish:pkg @mango/file
  pnpm publish:pkg workflow --dry-run
  pnpm publish:pkg cli --release-tag=v2026.06.12-mango-platform-release
  MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm publish:pkg cli --release-tag=v2026.06.12-mango-platform-release --skip-shared-gates

Use --skip-shared-gates only after the release batch has already run shared gates such as
package-consumer:typecheck --release-candidate-matrix once for the full batch.
Registry values may also come from MANGO_NPM_PUBLISH_REGISTRY and MANGO_NPM_CONSUME_REGISTRY.
`);
}

function requireRegistry(value, name) {
  if (!value) {
    console.error(`${name} is required; pass the matching registry flag or environment variable.`);
    process.exit(1);
  }
  let parsed;
  try {
    parsed = new URL(value);
  } catch {
    console.error(`${name} must be an absolute URL.`);
    process.exit(1);
  }
  if (!['http:', 'https:'].includes(parsed.protocol) || parsed.username || parsed.password) {
    console.error(`${name} must be an HTTP(S) URL without embedded credentials.`);
    process.exit(1);
  }
}

function checkReleaseNotes(packageName, version, options = {}) {
  const args = ['./scripts/check-release-notes.mjs', `--package=${packageName}`, `--version=${version}`];
  if (options.releaseTag) {
    args.push(`--tag=${options.releaseTag}`);
  }
  if (options.checkGithubRelease) {
    args.push('--check-github-release');
  }
  run('node', args);
}

function checkReleaseCandidateMatrix(consumeRegistry, phase) {
  console.log(`${phase}: checking local candidate tarballs against unchanged consume-registry packages`);
  run(pnpmCommand, [
    'run',
    'package-consumer:typecheck',
    '--',
    '--release-candidate-matrix',
    `--registry=${consumeRegistry}`,
  ]);
}

function verifyPublishedCliLocks(packageRoot, foundPackage) {
  const publishedLocksPath = join(packageRoot, 'release-versions.json');
  const sourceLocksPath = join(process.cwd(), 'packages', foundPackage.dir, 'release-versions.json');
  if (!existsSync(publishedLocksPath)) {
    console.error('Published @mango/cli tarball is missing release-versions.json.');
    process.exit(1);
  }
  const publishedLocks = JSON.parse(readFileSync(publishedLocksPath, 'utf8'));
  const sourceLocks = JSON.parse(readFileSync(sourceLocksPath, 'utf8'));
  if (JSON.stringify(publishedLocks) !== JSON.stringify(sourceLocks)) {
    console.error('Published @mango/cli release-versions.json does not match source release lock.');
    process.exit(1);
  }
  if (!existsSync(join(packageRoot, 'CHANGELOG.md'))) {
    console.error('Published @mango/cli tarball is missing CHANGELOG.md.');
    process.exit(1);
  }
}

function verifyPublishedPmoBaseline(packageRoot) {
  const manifestPath = join(packageRoot, 'dist/baseline.json');
  const baselineRoot = join(packageRoot, 'dist/baseline');
  if (!existsSync(manifestPath)) {
    console.error('Published @mango/pmo tarball is missing dist/baseline.json.');
    process.exit(1);
  }
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  const packageJson = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'));
  if (manifest.packageName !== '@mango/pmo' || manifest.packageVersion !== packageJson.version) {
    console.error('Published @mango/pmo manifest identity does not match package.json.');
    process.exit(1);
  }
  if (
    manifest.schemaVersion !== 2 ||
    !Array.isArray(manifest.files) ||
    !Array.isArray(manifest.contracts) ||
    typeof manifest.bundleSha256 !== 'string'
  ) {
    console.error('Published @mango/pmo baseline manifest has an unsupported structure.');
    process.exit(1);
  }
  const baselineFiles = new Map((manifest.files ?? []).map((entry) => [entry.path, entry]));
  if (baselineFiles.size !== manifest.files.length) {
    console.error('Published @mango/pmo baseline manifest contains duplicate file paths.');
    process.exit(1);
  }
  for (const file of ['rules/index.json', 'tools/pmo-preflight.mjs', 'agents/03-dev-agent.md']) {
    if (!existsSync(join(baselineRoot, file))) {
      console.error(`Published @mango/pmo tarball is missing baseline file: ${file}`);
      process.exit(1);
    }
    if (!baselineFiles.has(file)) {
      console.error(`Published @mango/pmo baseline manifest is missing: ${file}`);
      process.exit(1);
    }
  }

  for (const entry of baselineFiles.values()) {
    verifyPublishedPmoFile(packageRoot, `dist/baseline/${entry.path}`, entry, 'baseline');
  }

  const plugin = manifest.plugin;
  if (plugin?.path !== 'package-root' || !Array.isArray(plugin.files) || plugin.files.length === 0) {
    console.error('Published @mango/pmo manifest is missing its package-root Codex plugin projection.');
    process.exit(1);
  }
  const pluginFiles = new Map(plugin.files.map((entry) => [entry.path, entry]));
  if (pluginFiles.size !== plugin.files.length) {
    console.error('Published @mango/pmo plugin projection contains duplicate file paths.');
    process.exit(1);
  }
  for (const entry of pluginFiles.values()) {
    if (!entry.path?.startsWith('.codex-plugin/') && !entry.path?.startsWith('skills/')) {
      console.error(`Published @mango/pmo plugin projection contains an out-of-scope path: ${entry.path}`);
      process.exit(1);
    }
  }
  for (const requiredPath of [
    '.codex-plugin/plugin.json',
    'skills/mango-pmo-lifecycle/SKILL.md',
    'skills/mango-requirements-business/SKILL.md',
    'skills/mango-requirements-system/SKILL.md',
    'skills/mango-design-technical/SKILL.md',
    'skills/mango-plan-implementation/SKILL.md',
  ]) {
    if (!pluginFiles.has(requiredPath) || !existsSync(join(packageRoot, requiredPath))) {
      console.error(`Published @mango/pmo tarball is missing package-root plugin file: ${requiredPath}`);
      process.exit(1);
    }
  }
  for (const entry of pluginFiles.values()) {
    verifyPublishedPmoFile(packageRoot, entry.path, entry, 'plugin');
  }
  for (const [baselinePath, baselineEntry] of baselineFiles) {
    if (!baselinePath.startsWith('skills/')) {
      continue;
    }
    const projectedEntry = pluginFiles.get(baselinePath);
    if (
      !projectedEntry ||
      projectedEntry.sha256 !== baselineEntry.sha256 ||
      projectedEntry.size !== baselineEntry.size
    ) {
      console.error(`Published @mango/pmo project Skill differs from its baseline source: ${baselinePath}`);
      process.exit(1);
    }
  }

  const pluginSha256 = sha256(Buffer.from(JSON.stringify(plugin.files), 'utf8'));
  if (plugin.sha256 !== pluginSha256) {
    console.error(
      `Published @mango/pmo plugin aggregate hash mismatch: expected ${pluginSha256}, got ${plugin.sha256}.`,
    );
    process.exit(1);
  }
  const bundleSha256 = sha256(
    Buffer.from(
      JSON.stringify({
        files: manifest.files,
        contracts: manifest.contracts,
        plugin: manifest.plugin,
      }),
      'utf8',
    ),
  );
  if (manifest.bundleSha256 !== bundleSha256) {
    console.error(`Published @mango/pmo bundle hash mismatch: expected ${bundleSha256}, got ${manifest.bundleSha256}.`);
    process.exit(1);
  }

  const pluginManifest = JSON.parse(readFileSync(join(packageRoot, '.codex-plugin/plugin.json'), 'utf8'));
  if (pluginManifest.version !== packageJson.version || pluginManifest.skills !== './skills/') {
    console.error('Published @mango/pmo plugin metadata does not match the package version or skills projection.');
    process.exit(1);
  }
}

function verifyPublishedPmoFile(packageRoot, packageRelativePath, entry, kind) {
  if (
    !isSafePmoPackagePath(packageRelativePath) ||
    typeof entry?.sha256 !== 'string' ||
    !/^[0-9a-f]{64}$/i.test(entry.sha256) ||
    !Number.isInteger(entry.size) ||
    entry.size < 0 ||
    !['0644', '0755'].includes(entry.mode)
  ) {
    console.error(`Published @mango/pmo ${kind} manifest contains an invalid file descriptor: ${packageRelativePath}`);
    process.exit(1);
  }
  const filePath = join(packageRoot, packageRelativePath);
  if (!existsSync(filePath)) {
    console.error(`Published @mango/pmo tarball is missing ${kind} file: ${packageRelativePath}`);
    process.exit(1);
  }
  const content = readFileSync(filePath);
  const actualHash = sha256(content);
  const actualMode = process.platform === 'win32' ? entry.mode : statSync(filePath).mode & 0o111 ? '0755' : '0644';
  if (content.length !== entry.size || actualHash !== entry.sha256 || actualMode !== entry.mode) {
    console.error(`Published @mango/pmo ${kind} file differs from its manifest: ${packageRelativePath}`);
    process.exit(1);
  }
}

function isSafePmoPackagePath(value) {
  return (
    typeof value === 'string' &&
    value.length > 0 &&
    !value.startsWith('/') &&
    !value.includes('\\') &&
    value.split('/').every((segment) => segment && segment !== '.' && segment !== '..')
  );
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function verifyPublishedRelease(packageName, version, found, releaseContracts, registries) {
  for (const [name, registry] of [
    ['publish registry', registries.publish],
    ['consume registry', registries.consume],
  ]) {
    const result = npmView(`${packageName}@${version}`, registry);
    if (result.status !== 0) {
      throw new Error(`${name} verification failed for ${packageName}@${version}.`);
    }
    const publishedVersion = result.stdout.trim();
    if (publishedVersion !== version) {
      throw new Error(`${name} resolved ${packageName}@${publishedVersion}; expected ${packageName}@${version}.`);
    }
    console.log(`${name}: ${packageName}@${publishedVersion}`);
  }
  verifyPublishedPackage(packageName, version, found, {
    registry: registries.publish,
    contract: releaseContracts[packageName],
  });
  verifyPublishedPackage(packageName, version, found, {
    registry: registries.consume,
    contract: releaseContracts[packageName],
    afterExtract: (packageRoot) => {
      if (packageName === '@mango/cli') {
        verifyPublishedCliLocks(packageRoot, found);
      }
      if (packageName === '@mango/pmo') {
        verifyPublishedPmoBaseline(packageRoot);
      }
    },
  });
}

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const verifyOnly = args.includes('--verify-only');
const skipSharedGates = args.includes('--skip-shared-gates');
const batchResume = verifyOnly && process.env.MANGO_RELEASE_BATCH_RESUME === '1';
const sharedGatePackage = process.env.MANGO_RELEASE_SHARED_GATE_PACKAGE || '';
const releaseTagArg = args.find((arg) => arg.startsWith('--release-tag='));
const releaseTag = releaseTagArg?.slice('--release-tag='.length) || '';
const verifyPmoPackageRootArg = args.find((arg) => arg.startsWith('--verify-pmo-package-root='));
const publishRegistryArg = args.find((arg) => arg.startsWith('--publish-registry='));
const consumeRegistryArg = args.find((arg) => arg.startsWith('--consume-registry='));
const publishRegistry = publishRegistryArg?.slice('--publish-registry='.length) || HOSTED_REGISTRY;
const consumeRegistry = consumeRegistryArg?.slice('--consume-registry='.length) || GROUP_REGISTRY;
const packageArg = args.find((arg) => !arg.startsWith('--'));
const pnpmCommand = commandForPlatform('pnpm');
const npmCommand = commandForPlatform('npm');

if (args.includes('--help') || args.includes('-h')) {
  usage();
  process.exit(0);
}

if (verifyPmoPackageRootArg) {
  const packageRoot = resolve(verifyPmoPackageRootArg.slice('--verify-pmo-package-root='.length));
  verifyPublishedPmoBaseline(packageRoot);
  console.log(`Verified extracted @mango/pmo package root: ${packageRoot}`);
  process.exit(0);
}

if (!packageArg) {
  usage();
  process.exit(1);
}

const packageName = normalizePackageName(packageArg);
const found = findPackage(packageName);
const releaseContracts = readReleaseContracts();

if (!found) {
  console.error(`Package not found in packages/*: ${packageName}`);
  process.exit(1);
}

const version = found.packageJson.version;
requireRegistry(publishRegistry, 'npm publish registry');
requireRegistry(consumeRegistry, 'npm consume registry');
if ((!dryRun || verifyOnly) && !releaseTag) {
  console.error('Publish or verify-only mode requires --release-tag=<tag> so GitHub Release notes can be verified.');
  process.exit(1);
}
if (skipSharedGates && !dryRun && process.env.MANGO_SHARED_PUBLISH_GATES_PASSED !== '1') {
  console.error(
    'Real publish with --skip-shared-gates requires MANGO_SHARED_PUBLISH_GATES_PASSED=1 after the release batch shared gates have passed.',
  );
  process.exit(1);
}
console.log('Checking platform release notes before publish');
checkReleaseNotes(packageName, version, {
  releaseTag,
  checkGithubRelease: !dryRun || verifyOnly,
});
if (packageName === '@mango/cli') {
  console.log('Checking CLI release version lock before publish');
  run(pnpmCommand, ['--filter', packageName, 'run', 'check:release-versions']);
  if (!dryRun || verifyOnly) {
    console.log('Checking published release locks before CLI publish, excluding the CLI package being published');
    run(pnpmCommand, [
      '--filter',
      packageName,
      'run',
      'check:release-versions',
      '--',
      '--check-registry',
      `--registry=${consumeRegistry}`,
      `--ignore-registry-package=${packageName}`,
    ]);
  }
}
if (batchResume && packageName === sharedGatePackage) {
  checkReleaseCandidateMatrix(consumeRegistry, 'npm batch recovery');
  console.log('Shared npm batch recovery gates passed');
}
if (verifyOnly && !batchResume) {
  try {
    verifyPublishedRelease(packageName, version, found, releaseContracts, {
      publish: publishRegistry,
      consume: consumeRegistry,
    });
    if (packageName === '@mango/cli') {
      checkReleaseCandidateMatrix(consumeRegistry, 'post-publish pure registry verification');
    }
    console.log(`Verified published package without republishing: ${packageName}@${version}`);
    process.exit(0);
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}
if (batchResume) {
  const publishResult = npmView(`${packageName}@${version}`, publishRegistry);
  const consumeResult = npmView(`${packageName}@${version}`, consumeRegistry);
  const recovery = classifyNpmBatchRecovery(publishResult, consumeResult, version);
  if (recovery === 'verify-existing') {
    try {
      verifyPublishedRelease(packageName, version, found, releaseContracts, {
        publish: publishRegistry,
        consume: consumeRegistry,
      });
      if (packageName === '@mango/cli') {
        checkReleaseCandidateMatrix(consumeRegistry, 'recovery pure registry verification');
      }
      console.log(`Verified existing package during npm batch recovery: ${packageName}@${version}`);
      process.exit(0);
    } catch (error) {
      console.error(error.message);
      process.exit(1);
    }
  }
  if (recovery !== 'publish-absent') {
    console.error(
      `Registry state for ${packageName}@${version} is inconsistent or unknown; refusing immutable publish.`,
    );
    process.exit(1);
  }
  console.log(`Both registries confirm ${packageName}@${version} is absent; continuing exact first publish`);
}
if (skipSharedGates || batchResume) {
  console.log('Skipping shared publish gates because the release batch gates already passed');
} else {
  checkReleaseCandidateMatrix(consumeRegistry, 'pre-publish mixed matrix verification');
}
if (found.packageJson.scripts?.build) {
  console.log(`Building ${packageName} before publish`);
  run(pnpmCommand, ['--filter', packageName, 'build']);
}
if (found.packageJson.scripts?.['check:styles']) {
  console.log(`Checking generated package styles for ${packageName}`);
  run(pnpmCommand, ['--filter', packageName, 'check:styles']);
}

const existing = npmView(`${packageName}@${version}`, publishRegistry);
if (!dryRun && existing.status === 0 && existing.stdout.trim() === version) {
  console.error(`${packageName}@${version} already exists in npm-hosted. Bump package.json version before publishing.`);
  process.exit(1);
}

const whoami = spawnSync(npmCommand, ['whoami', `--registry=${publishRegistry}`], {
  stdio: 'pipe',
  encoding: 'utf8',
  shell: shouldUseShellForCommand(npmCommand),
});
if (!dryRun && whoami.status !== 0) {
  console.error(`Not logged in to npm-hosted. Run:
  npm login --registry=${publishRegistry}`);
  process.exit(1);
}

const publishArgs = ['--filter', packageName, 'publish', '--no-git-checks', '--access', 'public'];
publishArgs.push(`--registry=${publishRegistry}`);
if (dryRun) {
  publishArgs.push('--dry-run');
}

console.log(`${dryRun ? 'Dry-run publishing' : 'Publishing'} ${packageName}@${version}`);
run(pnpmCommand, publishArgs);

if (!dryRun) {
  try {
    verifyPublishedRelease(packageName, version, found, releaseContracts, {
      publish: publishRegistry,
      consume: consumeRegistry,
    });
    if (packageName === '@mango/cli') {
      checkReleaseCandidateMatrix(consumeRegistry, 'post-publish pure registry verification');
    }
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}
