#!/usr/bin/env node
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';

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
    console.log(`${name}: ${packageName}@${result.stdout.trim()}`);
  }
}
