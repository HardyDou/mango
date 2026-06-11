#!/usr/bin/env node
import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const HOSTED_REGISTRY = 'http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/';
const GROUP_REGISTRY = 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';

function usage() {
  console.log(`Usage: pnpm publish:pkg <package|short-name> [--dry-run]

Examples:
  pnpm publish:pkg common
  pnpm publish:pkg @mango/file
  pnpm publish:pkg workflow --dry-run
`);
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    stdio: options.capture ? 'pipe' : 'inherit',
    encoding: 'utf8',
    ...options,
  });
  if (options.capture) {
    return result;
  }
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
  return result;
}

function normalizePackageName(input) {
  if (!input) {
    return '';
  }
  if (input.startsWith('@')) {
    return input;
  }
  if (input === 'cli' || input === 'mango-cli') {
    return '@mango/cli';
  }
  return `@mango/${input.replace(/^mango-/, '')}`;
}

function findPackage(packageName) {
  const packagesDir = join(process.cwd(), 'packages');
  for (const dir of readdirSync(packagesDir)) {
    const packageJsonPath = join(packagesDir, dir, 'package.json');
    if (!existsSync(packageJsonPath)) {
      continue;
    }
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    if (packageJson.name === packageName) {
      return { dir, packageJson, packageJsonPath };
    }
  }
  return null;
}

function npmView(packageName, registry) {
  return spawnSync('npm', ['view', packageName, 'version', `--registry=${registry}`], {
    stdio: 'pipe',
    encoding: 'utf8',
  });
}

function verifyPublishedPackage(packageName, version, foundPackage) {
  const tempDir = mkdtempSync(join(tmpdir(), 'mango-npm-publish-verify-'));
  try {
    console.log(`Verifying published tarball ${packageName}@${version}`);
    run('npm', [
      'pack',
      `${packageName}@${version}`,
      `--registry=${HOSTED_REGISTRY}`,
      '--pack-destination',
      tempDir,
    ]);
    const tarball = readdirSync(tempDir).find((file) => file.endsWith('.tgz'));
    if (!tarball) {
      console.error(`Published tarball not found for ${packageName}@${version}.`);
      process.exit(1);
    }
    run('tar', ['-xzf', join(tempDir, tarball), '-C', tempDir]);
    const packageRoot = join(tempDir, 'package');
    const publishedPackageJsonPath = join(packageRoot, 'package.json');
    if (!existsSync(publishedPackageJsonPath)) {
      console.error(`Published tarball for ${packageName}@${version} does not contain package.json.`);
      process.exit(1);
    }
    const publishedPackageJson = JSON.parse(readFileSync(publishedPackageJsonPath, 'utf8'));
    if (publishedPackageJson.name !== packageName || publishedPackageJson.version !== version) {
      console.error(
        `Published tarball metadata mismatch: expected ${packageName}@${version}, got ${publishedPackageJson.name}@${publishedPackageJson.version}.`,
      );
      process.exit(1);
    }
    verifyPublishedFiles(packageName, packageRoot, foundPackage.packageJson);
    if (packageName === '@mango/cli') {
      verifyPublishedCliLocks(packageRoot, foundPackage);
    }
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

function verifyPublishedFiles(packageName, packageRoot, sourcePackageJson) {
  for (const entry of sourcePackageJson.files ?? []) {
    if (!existsSync(join(packageRoot, entry))) {
      console.error(`Published tarball for ${packageName} is missing files entry: ${entry}`);
      process.exit(1);
    }
  }
  if (sourcePackageJson.exports?.['./style.css'] && !existsSync(join(packageRoot, 'style.css'))) {
    console.error(`Published tarball for ${packageName} is missing exported style.css.`);
    process.exit(1);
  }
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

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const packageArg = args.find((arg) => !arg.startsWith('--'));

if (args.includes('--help') || args.includes('-h')) {
  usage();
  process.exit(0);
}

if (!packageArg) {
  usage();
  process.exit(1);
}

const packageName = normalizePackageName(packageArg);
const found = findPackage(packageName);

if (!found) {
  console.error(`Package not found in packages/*: ${packageName}`);
  process.exit(1);
}

const version = found.packageJson.version;
if (packageName === '@mango/cli') {
  console.log('Checking CLI release version lock before publish');
  run('pnpm', ['--filter', packageName, 'run', 'check:release-versions']);
  if (!dryRun) {
    run('pnpm', [
      '--filter',
      packageName,
      'run',
      'check:release-versions',
      '--',
      '--check-registry',
      `--registry=${GROUP_REGISTRY}`,
    ]);
  }
}
if (found.packageJson.scripts?.build) {
  console.log(`Building ${packageName} before publish`);
  run('pnpm', ['--filter', packageName, 'build']);
}
if (found.packageJson.scripts?.['check:styles']) {
  console.log(`Checking generated package styles for ${packageName}`);
  run('pnpm', ['--filter', packageName, 'check:styles']);
}

const existing = npmView(packageName, HOSTED_REGISTRY);
if (!dryRun && existing.status === 0 && existing.stdout.trim() === version) {
  console.error(`${packageName}@${version} already exists in npm-hosted. Bump package.json version before publishing.`);
  process.exit(1);
}

const whoami = spawnSync('npm', ['whoami', `--registry=${HOSTED_REGISTRY}`], {
  stdio: 'pipe',
  encoding: 'utf8',
});
if (!dryRun && whoami.status !== 0) {
  console.error(`Not logged in to npm-hosted. Run:
  npm login --registry=${HOSTED_REGISTRY}`);
  process.exit(1);
}

const publishArgs = ['--filter', packageName, 'publish', '--no-git-checks', '--access', 'public'];
if (dryRun) {
  publishArgs.push('--dry-run');
}

console.log(`${dryRun ? 'Dry-run publishing' : 'Publishing'} ${packageName}@${version}`);
run('pnpm', publishArgs);

if (!dryRun) {
  for (const [name, registry] of [['npm-hosted', HOSTED_REGISTRY], ['npm-group', GROUP_REGISTRY]]) {
    const result = npmView(packageName, registry);
    if (result.status !== 0) {
      console.error(`Published, but ${name} verification failed for ${packageName}.`);
      process.exit(1);
    }
    const publishedVersion = result.stdout.trim();
    if (publishedVersion !== version) {
      console.error(
        `Published, but ${name} resolved ${packageName}@${publishedVersion}; expected ${packageName}@${version}.`,
      );
      process.exit(1);
    }
    console.log(`${name}: ${packageName}@${publishedVersion}`);
  }
  verifyPublishedPackage(packageName, version, found);
}
