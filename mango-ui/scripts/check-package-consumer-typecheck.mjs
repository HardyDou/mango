#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const currentFile = fileURLToPath(import.meta.url);
const uiRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(uiRoot, '..');
const cli = join(uiRoot, 'packages/mango-cli/src/index.mjs');
const runId = Date.now().toString(36);
const runtimeRoot = join(repoRoot, '.runtime/pct', runId);
const projectRoot = join(runtimeRoot, 'p');
const packageStore = join(runtimeRoot, 's');
const consumerName = 'mango-package-consumer-typecheck';
const registryArg = process.argv.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length) || 'https://registry.npmjs.org/';
const keepTemp = process.argv.includes('--keep-temp');
const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

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

function listTarballFiles(tarballPath) {
  const result = run('tar', ['-tzf', tarballPath], { capture: true });
  if (result.status !== 0) {
    throw new Error(`Failed to list ${tarballPath}:\n${result.stderr}`);
  }
  return result.stdout.split(/\r?\n/).filter(Boolean);
}

function exportedTypePaths(packageJson) {
  const paths = [];
  if (packageJson.types) {
    paths.push(packageJson.types);
  }
  for (const exportConfig of Object.values(packageJson.exports || {})) {
    if (typeof exportConfig !== 'string' && exportConfig?.types) {
      paths.push(exportConfig.types);
    }
  }
  return paths.map((entry) => `package/${entry.replace(/^\.\//, '')}`);
}

function wildcardToRegExp(pattern) {
  const escaped = pattern.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`^${escaped.replaceAll('*', '[^/]+')}$`);
}

function assertPackedPackageBoundary(tarballPath) {
  const packageJson = readPackedPackageJson(tarballPath);
  if (!packageJson.name?.startsWith('@mango/') || packageJson.name === '@mango/cli') {
    return;
  }
  const files = listTarballFiles(tarballPath);
  const sourceFile = files.find((file) =>
    /^package\/src\//.test(file)
    || /^package\/(?:api|components|hooks|types|utils|views)\//.test(file)
    || /^package\/index\.ts$/.test(file),
  );
  if (sourceFile) {
    throw new Error(`${packageJson.name} tarball must not publish source file: ${sourceFile}`);
  }
  for (const typePath of exportedTypePaths(packageJson)) {
    if (typePath.includes('*')) {
      const pattern = wildcardToRegExp(typePath);
      if (!files.some((file) => pattern.test(file))) {
        throw new Error(`${packageJson.name} tarball is missing exported declaration pattern: ${typePath}`);
      }
      continue;
    }
    if (!files.includes(typePath)) {
      throw new Error(`${packageJson.name} tarball is missing exported declaration: ${typePath}`);
    }
  }
}

function listPackableMangoPackages() {
  const packagesRoot = join(uiRoot, 'packages');
  return readdirSync(packagesRoot)
    .map((packageDir) => join(packagesRoot, packageDir))
    .filter((packageRoot) => {
      const packageJsonPath = join(packageRoot, 'package.json');
      if (!existsSync(packageJsonPath)) {
        return false;
      }
      const packageJson = readJson(packageJsonPath);
      return packageJson.name?.startsWith('@mango/') && !packageJson.private;
    });
}

function mapPackedMangoTarballs(frontendRoot) {
  const mappings = new Map();
  for (const file of readdirSync(packageStore)) {
    if (!file.endsWith('.tgz')) {
      continue;
    }
    const tarballPath = join(packageStore, file);
    assertPackedPackageBoundary(tarballPath);
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
    rmSync(runtimeRoot, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
  }
}

try {
  if (!existsSync(cli)) {
    throw new Error(`Mango CLI source not found: ${cli}`);
  }
  mkdirSync(projectRoot, { recursive: true });
  mkdirSync(packageStore, { recursive: true });

  console.log('Generating package styles before packing');
  run(pnpmCommand, ['admin:styles']);

  console.log('Building Mango frontend packages before consumer typecheck');
  run(pnpmCommand, ['-r', '--filter', './packages/*', '--filter', '!@mango/cli', '--if-present', 'run', 'build']);

  console.log('Checking Mango package exports before packing');
  run(pnpmCommand, ['package-exports:check']);

  console.log('Packing Mango frontend packages for consumer typecheck');
  for (const packageRoot of listPackableMangoPackages()) {
    run(pnpmCommand, ['pack', '--pack-destination', packageStore], { cwd: packageRoot });
  }

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
  run(pnpmCommand, ['install', `--registry=${registry}`], {
    cwd: frontendRoot,
    env: {
      npm_config_registry: registry,
      NPM_CONFIG_REGISTRY: registry,
    },
  });

  console.log('Running generated consumer vue-tsc type gate');
  run(pnpmCommand, ['run', 'typecheck'], { cwd: frontendRoot });

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
