#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { assertPackedPackageBoundary as assertPackedPackageFiles } from './quality/packed-package-boundary.mjs';
import { classifyRegistryVersionResult } from './package-consumer-matrix.mjs';
import { toCanonicalRelativePath } from './release/consumer-tarball-paths.mjs';

const currentFile = fileURLToPath(import.meta.url);
const uiRoot = resolve(dirname(currentFile), '..');
const sourceCli = join(uiRoot, 'packages/mango-cli/src/index.mjs');
const runId = Date.now().toString(36);
const runtimeBase = process.env.MANGO_PACKAGE_CONSUMER_RUNTIME_BASE
  ? resolve(process.env.MANGO_PACKAGE_CONSUMER_RUNTIME_BASE)
  : join(tmpdir(), 'mango-package-consumer');
const runtimeRoot = join(runtimeBase, runId);
const projectRoot = join(runtimeRoot, 'p');
const consumerName = 'mango-package-consumer-typecheck';
const registryArg = process.argv.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length) || 'https://registry.npmjs.org/';
const candidateDirectoryArg = process.argv.find((arg) => arg.startsWith('--candidate-dir='));
const candidateDirectory = candidateDirectoryArg ? resolve(candidateDirectoryArg.slice('--candidate-dir='.length)) : '';
const packageStore = candidateDirectory || join(runtimeRoot, 's');
const keepTemp = process.argv.includes('--keep-temp');
const offline = process.argv.includes('--offline') || process.env.MANGO_PACKAGE_CONSUMER_OFFLINE === '1';
const reuseBuild = process.argv.includes('--reuse-build');
const releaseCandidateMatrix = process.argv.includes('--release-candidate-matrix');
const pureRegistry = process.argv.includes('--pure-registry');
const consumerStoreDir = process.env.MANGO_PACKAGE_CONSUMER_STORE_DIR;
const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const governedPackageManager = readJson(join(uiRoot, 'package.json')).packageManager;

function shouldUseShellForCommand(command) {
  return process.platform === 'win32' && /\.cmd$/iu.test(command);
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || uiRoot,
    stdio: options.capture ? 'pipe' : 'inherit',
    encoding: 'utf8',
    shell: shouldUseShellForCommand(command),
    env: { ...process.env, FORCE_COLOR: '0', npm_config_registry: registry, ...options.env },
  });
  if (options.capture) {
    return result;
  }
  if (result.error) {
    console.error(result.error.message);
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

function assertPackedPackageBoundary(tarballPath) {
  const packageJson = readPackedPackageJson(tarballPath);
  const files = listTarballFiles(tarballPath);
  assertPackedPackageFiles(packageJson, files);
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

function mapPackedMangoTarballs(frontendRoot, packageNames) {
  const mappings = new Map();
  for (const file of readdirSync(packageStore)) {
    if (!file.endsWith('.tgz')) {
      continue;
    }
    const tarballPath = join(packageStore, file);
    assertPackedPackageBoundary(tarballPath);
    const packageJson = readPackedPackageJson(tarballPath);
    if (packageJson.name?.startsWith('@mango/') && packageNames.has(packageJson.name)) {
      mappings.set(packageJson.name, `file:${toPackageRelativePath(frontendRoot, tarballPath)}`);
    }
  }
  return mappings;
}

function packedPackageIndex() {
  const mappings = new Map();
  for (const file of readdirSync(packageStore)) {
    if (!file.endsWith('.tgz')) continue;
    const tarballPath = join(packageStore, file);
    assertPackedPackageBoundary(tarballPath);
    const packageJson = readPackedPackageJson(tarballPath);
    if (!packageJson.name?.startsWith('@mango/')) continue;
    if (mappings.has(packageJson.name)) throw new Error(`Duplicate candidate tarball for ${packageJson.name}`);
    mappings.set(packageJson.name, tarballPath);
  }
  return mappings;
}

function selectCandidatePackages(packageRoots) {
  if (pureRegistry) return [];
  if (!releaseCandidateMatrix) {
    return packageRoots;
  }
  const candidates = [];
  for (const packageRoot of packageRoots) {
    const packageJson = readJson(join(packageRoot, 'package.json'));
    const coordinate = `${packageJson.name}@${packageJson.version}`;
    const result = run(npmCommand, ['view', coordinate, 'version', `--registry=${registry}`], { capture: true });
    let state;
    try {
      state = classifyRegistryVersionResult(result, packageJson.version);
    } catch (error) {
      throw new Error(`${coordinate}: ${error instanceof Error ? error.message : error}`, { cause: error });
    }
    if (state === 'candidate') {
      candidates.push(packageRoot);
      console.log(`Candidate tarball: ${coordinate} is absent from the consume registry`);
    } else {
      console.log(`Registry dependency: ${coordinate}`);
    }
  }
  return candidates;
}

function installCandidateCliRunner(candidateTarballs) {
  const cliTarball = candidateTarballs.get('@mango/cli');
  if (!cliTarball) throw new Error('Candidate CLI tarball is missing');
  const runnerRoot = join(runtimeRoot, 'candidate-cli');
  mkdirSync(runnerRoot, { recursive: true });
  writeJson(join(runnerRoot, 'package.json'), {
    name: 'mango-candidate-cli-consumer-runner',
    private: true,
    packageManager: governedPackageManager,
    dependencies: { '@mango/cli': `file:${cliTarball}` },
  });
  const pmoTarball = candidateTarballs.get('@mango/pmo');
  writeFileSync(
    join(runnerRoot, 'pnpm-workspace.yaml'),
    ['packages:', '  - .', ...(pmoTarball ? ['overrides:', `  "@mango/pmo": "file:${pmoTarball}"`] : []), ''].join(
      '\n',
    ),
  );
  writeFileSync(join(runnerRoot, '.npmrc'), `registry=${registry}\n`);
  run(pnpmCommand, ['install', `--registry=${registry}`], { cwd: runnerRoot });
  const candidateCli = join(runnerRoot, 'node_modules/@mango/cli/src/index.mjs');
  if (!existsSync(candidateCli)) throw new Error(`Candidate Mango CLI entry not found: ${candidateCli}`);
  return candidateCli;
}

function installPublishedCliRunner() {
  const cliPackageJson = readJson(join(uiRoot, 'packages/mango-cli/package.json'));
  const runnerRoot = join(runtimeRoot, 'published-cli');
  mkdirSync(runnerRoot, { recursive: true });
  writeJson(join(runnerRoot, 'package.json'), {
    name: 'mango-published-cli-consumer-runner',
    private: true,
    packageManager: governedPackageManager,
    dependencies: {
      '@mango/cli': cliPackageJson.version,
    },
  });
  writeFileSync(join(runnerRoot, '.npmrc'), `registry=${registry}\n`);
  run(
    pnpmCommand,
    [
      'install',
      ...(offline ? ['--offline'] : []),
      ...(consumerStoreDir ? [`--store-dir=${consumerStoreDir}`] : []),
      `--registry=${registry}`,
    ],
    {
      cwd: runnerRoot,
      env: {
        npm_config_registry: registry,
        NPM_CONFIG_REGISTRY: registry,
      },
    },
  );
  const publishedCli = join(runnerRoot, 'node_modules/@mango/cli/src/index.mjs');
  if (!existsSync(publishedCli)) {
    throw new Error(`Published Mango CLI entry not found: ${publishedCli}`);
  }
  return publishedCli;
}

function hasPublishedTypes(value) {
  if (typeof value === 'string' || value == null) {
    return false;
  }
  if (Array.isArray(value)) {
    return value.some(hasPublishedTypes);
  }
  if (typeof value === 'object') {
    return typeof value.types === 'string' || Object.values(value).some(hasPublishedTypes);
  }
  return false;
}

function listTypedPackedPackages() {
  const packageNames = [];
  for (const file of readdirSync(packageStore)) {
    if (!file.endsWith('.tgz')) {
      continue;
    }
    const packageJson = readPackedPackageJson(join(packageStore, file));
    if (packageJson.name?.startsWith('@mango/') && (packageJson.types || hasPublishedTypes(packageJson.exports))) {
      packageNames.push(packageJson.name);
    }
  }
  return packageNames.sort();
}

function toPackageRelativePath(fromRoot, targetPath) {
  return toCanonicalRelativePath(fromRoot, targetPath);
}

function applyTarballMappings(frontendRoot, mappings) {
  const packageJsonPath = join(frontendRoot, 'package.json');
  const packageJson = readJson(packageJsonPath);
  packageJson.packageManager = governedPackageManager;
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
  packageJson.devDependencies ||= {};
  const declaredDependencies = new Set(
    ['dependencies', 'devDependencies', 'peerDependencies'].flatMap((section) =>
      Object.keys(packageJson[section] || {}),
    ),
  );
  for (const [dependency, tarball] of mappings) {
    if (!declaredDependencies.has(dependency)) {
      packageJson.devDependencies[dependency] = tarball;
    }
  }
  delete packageJson.pnpm;
  writeJson(packageJsonPath, packageJson);
  const workspacePath = join(frontendRoot, 'pnpm-workspace.yaml');
  const workspace = readFileSync(workspacePath, 'utf8').trimEnd();
  assertGeneratedInstallPolicy(workspace);
  if (/^overrides:/mu.test(workspace)) {
    throw new Error('Generated pnpm workspace unexpectedly contains overrides before consumer mapping');
  }
  if (mappings.size > 0) {
    writeFileSync(
      workspacePath,
      [
        workspace,
        'overrides:',
        ...[...mappings].map(([dependency, tarball]) => `  "${dependency}": "${tarball}"`),
        '',
      ].join('\n'),
    );
  } else {
    writeFileSync(workspacePath, `${workspace}\n`);
  }
  writeFileSync(join(frontendRoot, '.npmrc'), `registry=${registry}\n`);
}

function writePackageTypeSmoke(frontendRoot, packageNames) {
  const declarations = packageNames.map(
    (packageName, index) => `type MangoPackage${index + 1} = typeof import('${packageName}');`,
  );
  const tuple = packageNames.map((_, index) => `MangoPackage${index + 1}`).join(', ');
  const mangoDialogExposeSmoke = packageNames.includes('@mango/common')
    ? [
        "import { ref } from 'vue';",
        "import type { MangoDialogExpose } from '@mango/common';",
        '',
        'export const mangoDialogRef = ref<MangoDialogExpose | null>(null);',
        '',
        'export function focusMangoDialog() {',
        '  mangoDialogRef.value?.bringToFront();',
        '}',
        '',
      ]
    : [];
  const httpClientSmoke =
    packageNames.includes('@mango/api-schema') && packageNames.includes('@mango/http-client')
      ? [
          "import type { HttpClient } from '@mango/api-schema';",
          "import { createMangoHttpClient } from '@mango/http-client';",
          '',
          'export function createHttpClientContractSmoke(): HttpClient {',
          "  return createMangoHttpClient({ baseUrl: '/api' });",
          '}',
          '',
          'export function requestHttpContractSmoke(client: HttpClient, signal?: AbortSignal) {',
          "  return client.request<{ id: string }>({ method: 'GET', url: '/orders/100', signal });",
          '}',
          '',
        ]
      : [];
  writeFileSync(
    join(frontendRoot, 'src/mango-package-contract-smoke.ts'),
    [
      ...mangoDialogExposeSmoke,
      ...httpClientSmoke,
      ...declarations,
      '',
      `export type MangoPublishedPackageContracts = [${tuple}];`,
      '',
    ].join('\n'),
  );
}

function assertGeneratedInstallPolicy(workspace) {
  const requiredLines = [
    "  - 'packages/*'",
    'allowBuilds:',
    "  '@swc/core': true",
    '  core-js-pure: true',
    '  es5-ext: true',
    '  esbuild: true',
    '  msw: true',
    '  vue-demi: true',
  ];
  const lines = workspace.split(/\r?\n/u);
  for (const line of requiredLines) {
    if (!lines.includes(line)) {
      throw new Error(`Generated pnpm workspace missing required install policy: ${line}`);
    }
  }
}

function cleanup() {
  if (!keepTemp) {
    rmSync(runtimeRoot, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
  }
}

try {
  if (!existsSync(sourceCli)) {
    throw new Error(`Mango CLI source not found: ${sourceCli}`);
  }
  mkdirSync(projectRoot, { recursive: true });
  if (candidateDirectory && !existsSync(candidateDirectory)) {
    throw new Error(`Candidate tarball directory does not exist: ${candidateDirectory}`);
  }
  mkdirSync(packageStore, { recursive: true });

  const packablePackages = listPackableMangoPackages();
  const candidateTarballs = candidateDirectory ? packedPackageIndex() : new Map();
  const candidatePackages = candidateDirectory ? [] : selectCandidatePackages(packablePackages);
  const candidatePackageNames = candidateDirectory
    ? new Set(candidateTarballs.keys())
    : new Set(candidatePackages.map((packageRoot) => readJson(join(packageRoot, 'package.json')).name));
  const cli = candidatePackageNames.has('@mango/cli')
    ? installCandidateCliRunner(candidateTarballs)
    : releaseCandidateMatrix
      ? installPublishedCliRunner()
      : sourceCli;

  if (!reuseBuild && !candidateDirectory) {
    console.log('Generating package styles before packing');
    run(pnpmCommand, ['admin:styles']);

    console.log('Prebuilding Mango admin dependency chain before recursive package build');
    run(pnpmCommand, ['--filter', '@mango/admin', 'run', 'build']);

    console.log('Building Mango frontend packages before consumer typecheck');
    run(pnpmCommand, [
      '-r',
      '--workspace-concurrency=1',
      '--filter',
      './packages/*',
      '--filter',
      '!@mango/cli',
      '--if-present',
      'run',
      'build',
    ]);
  } else {
    console.log('Reusing existing package build outputs for consumer typecheck');
  }

  if (!candidateDirectory && (!releaseCandidateMatrix || candidatePackages.length > 0)) {
    console.log('Checking Mango package exports before packing');
    run(pnpmCommand, ['package-exports:check']);

    console.log('Packing Mango frontend packages for consumer typecheck');
    for (const packageRoot of candidatePackages) {
      // Every candidate package was built and its exports were checked immediately above.
      // Avoid re-running lifecycle builds once per tarball during this publication-boundary check.
      run(pnpmCommand, ['--config.ignore-scripts=true', 'pack', '--pack-destination', packageStore], {
        cwd: packageRoot,
      });
    }
  } else if (!candidateDirectory) {
    console.log('All locked Mango versions already exist; testing the pure consume-registry matrix');
  } else {
    console.log(`Using ${candidateTarballs.size} prebuilt candidate tarball(s) from ${candidateDirectory}`);
  }

  console.log('Generating temporary Mango business frontend consumer');
  run(
    process.execPath,
    [
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
    ],
    { cwd: projectRoot },
  );

  const frontendRoot = join(projectRoot, consumerName, 'frontend');
  if (!existsSync(join(frontendRoot, 'package.json'))) {
    throw new Error(`Generated frontend package.json not found: ${frontendRoot}`);
  }
  const mappings = mapPackedMangoTarballs(frontendRoot, candidatePackageNames);
  if (!releaseCandidateMatrix && mappings.size === 0) {
    throw new Error(`No packed @mango/* tarballs found in ${packageStore}`);
  }
  applyTarballMappings(frontendRoot, mappings);
  const typedPackages = listTypedPackedPackages();
  writePackageTypeSmoke(frontendRoot, typedPackages);

  console.log(
    `Installing generated consumer dependencies with ${mappings.size} local candidate tarballs and ` +
      `${releaseCandidateMatrix ? 'published registry dependencies' : 'local package overrides'}; ` +
      `${typedPackages.length} public type contracts are included in vue-tsc`,
  );
  run(
    pnpmCommand,
    [
      'install',
      ...(offline ? ['--offline'] : []),
      ...(consumerStoreDir ? [`--store-dir=${consumerStoreDir}`] : []),
      `--registry=${registry}`,
    ],
    {
      cwd: frontendRoot,
      env: {
        npm_config_registry: registry,
        NPM_CONFIG_REGISTRY: registry,
      },
    },
  );

  console.log('Running generated consumer vue-tsc type gate');
  run(pnpmCommand, ['run', 'typecheck'], { cwd: frontendRoot });

  console.log('Running generated consumer production build');
  run(pnpmCommand, ['run', 'build'], { cwd: frontendRoot });

  console.log('Generated consumer typecheck and production build passed.');
  cleanup();
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  if (keepTemp) {
    console.error(`Temporary consumer project: ${join(projectRoot, consumerName)}`);
    console.error(`Temporary package store: ${packageStore}`);
  } else {
    cleanup();
    console.error(
      'Temporary consumer project and package store were removed. Re-run with --keep-temp to inspect them.',
    );
  }
  process.exit(1);
}
