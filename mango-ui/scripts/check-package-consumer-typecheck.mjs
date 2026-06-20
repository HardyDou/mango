#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const currentFile = fileURLToPath(import.meta.url);
const uiRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(uiRoot, '..');
const cli = join(uiRoot, 'packages/mango-cli/src/index.mjs');
const runId = `issue-148-${Date.now()}`;
const projectRoot = join(repoRoot, '.runtime/projects/package-consumer-typecheck', runId);
const packageStore = join(repoRoot, '.runtime/package-store/package-consumer-typecheck', runId);
const consumerName = 'mango-package-consumer-typecheck';
const registryArg = process.argv.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length) || 'https://registry.npmjs.org/';
const keepTemp = process.argv.includes('--keep-temp');

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || uiRoot,
    stdio: options.capture ? 'pipe' : 'inherit',
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0', npm_config_registry: registry, ...options.env },
  });
  if (options.capture) {
    return result;
  }
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with status ${result.status ?? 1}`);
  }
  return result;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

function readPackedPackageJson(tarballPath) {
  const result = run('tar', ['-xOf', tarballPath, 'package/package.json'], { capture: true });
  if (result.status !== 0) {
    throw new Error(`Failed to read package.json from ${tarballPath}:\n${result.stderr}`);
  }
  return JSON.parse(result.stdout);
}

function mapPackedMangoTarballs(frontendRoot) {
  const mappings = new Map();
  for (const file of readdirSync(packageStore)) {
    if (!file.endsWith('.tgz')) {
      continue;
    }
    const tarballPath = join(packageStore, file);
    const packageJson = readPackedPackageJson(tarballPath);
    if (packageJson.name?.startsWith('@mango/')) {
      mappings.set(packageJson.name, `file:${relative(frontendRoot, tarballPath)}`);
    }
  }
  return mappings;
}

function applyTarballMappings(frontendRoot, mappings) {
  const packageJsonPath = join(frontendRoot, 'package.json');
  const packageJson = readJson(packageJsonPath);
  for (const section of ['dependencies', 'devDependencies', 'peerDependencies']) {
    if (!packageJson[section]) {
      continue;
    }
    for (const dependency of Object.keys(packageJson[section])) {
      if (mappings.has(dependency)) {
        packageJson[section][dependency] = mappings.get(dependency);
      }
    }
  }
  packageJson.pnpm = packageJson.pnpm || {};
  packageJson.pnpm.overrides = { ...(packageJson.pnpm.overrides || {}) };
  for (const [dependency, tarball] of mappings) {
    packageJson.pnpm.overrides[dependency] = tarball;
  }
  writeJson(packageJsonPath, packageJson);
  writeFileSync(join(frontendRoot, '.npmrc'), `registry=${registry}\n`);
}

function cleanup() {
  if (!keepTemp) {
    rmSync(projectRoot, { recursive: true, force: true });
    rmSync(packageStore, { recursive: true, force: true });
  }
}

try {
  if (!existsSync(cli)) {
    throw new Error(`Mango CLI source not found: ${cli}`);
  }
  mkdirSync(projectRoot, { recursive: true });
  mkdirSync(packageStore, { recursive: true });

  console.log('Generating package styles before packing');
  run('pnpm', ['admin:styles']);

  console.log('Building Mango frontend packages before consumer typecheck');
  run('pnpm', ['-r', '--filter', './packages/*', '--filter', '!@mango/cli', '--if-present', 'run', 'build']);

  console.log('Checking Mango package exports before packing');
  run('pnpm', ['package-exports:check']);

  console.log('Packing Mango frontend packages for consumer typecheck');
  run('pnpm', ['-r', '--filter', './packages/*', '--filter', '!@mango/cli', 'pack', '--pack-destination', packageStore]);
  run('pnpm', ['--filter', '@mango/cli', 'pack', '--pack-destination', packageStore]);

  console.log('Generating temporary Mango business frontend consumer');
  run(process.execPath, [
    cli,
    'init',
    consumerName,
    '--preset',
    'custom',
    '--modules',
    'notice,workflow,workflow-example',
    '--npm-registry',
    registry,
    '--force',
  ], { cwd: projectRoot });

  const frontendRoot = join(projectRoot, consumerName, 'frontend');
  if (!existsSync(join(frontendRoot, 'package.json'))) {
    throw new Error(`Generated frontend package.json not found: ${frontendRoot}`);
  }
  const mappings = mapPackedMangoTarballs(frontendRoot);
  if (mappings.size === 0) {
    throw new Error(`No packed @mango/* tarballs found in ${packageStore}`);
  }
  applyTarballMappings(frontendRoot, mappings);

  console.log(`Installing generated consumer dependencies with ${mappings.size} local Mango tarballs`);
  run('pnpm', ['install', `--registry=${registry}`], {
    cwd: frontendRoot,
    env: {
      npm_config_registry: registry,
      NPM_CONFIG_REGISTRY: registry,
    },
  });

  console.log('Running generated consumer vue-tsc type gate');
  run('pnpm', ['run', 'typecheck'], { cwd: frontendRoot });

  console.log('Generated consumer vue-tsc type gate passed.');
  cleanup();
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  if (keepTemp) {
    console.error(`Temporary consumer project: ${join(projectRoot, consumerName)}`);
    console.error(`Temporary package store: ${packageStore}`);
  } else {
    cleanup();
    console.error('Temporary consumer project and package store were removed. Re-run with --keep-temp to inspect them.');
  }
  process.exit(1);
}
