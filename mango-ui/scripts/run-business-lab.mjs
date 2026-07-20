#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  applyTarballMappings,
  assertGeneratedProjectBoundary,
  readJson,
  sha256File,
  writeJson,
} from './quality/business-lab-lib.mjs';

const uiRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(uiRoot, '..');
const runtimeRoot = resolve(readArgument('--runtime-root=') || join(repoRoot, '.runtime'));
const projectName = 'frontend-standards-business-lab';
const projectRoot = join(runtimeRoot, 'projects', projectName);
const frontendRoot = join(projectRoot, 'frontend');
const labRoot = join(runtimeRoot, 'business-lab');
const tarballRoot = join(labRoot, 'artifacts', 'tarballs');
const bootstrapRoot = join(labRoot, 'bootstrap');
const dependencyStore = join(runtimeRoot, 'package-store', projectName);
const reportRoot = join(runtimeRoot, 'frontend-quality', 'business-lab');
const preparationReportPath = join(reportRoot, 'preparation-report.json');
const sealedReportPath = join(reportRoot, 'sealed-report.json');
const registry = readArgument('--registry=') || 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';
const image = readArgument('--image=') || 'mango/frontend-quality:node22-pnpm11.14';
const runtimeMount = readArgument('--runtime-mount=') || runtimeRoot;
const reuseBuild = process.argv.includes('--reuse-build');
const prepareOnly = process.argv.includes('--prepare-only');
const sealedOnly = process.argv.includes('--sealed-only');
const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

function shouldUseShellForCommand(command) {
  return process.platform === 'win32' && /\.cmd$/iu.test(command);
}

function readArgument(prefix) {
  return process.argv.find((argument) => argument.startsWith(prefix))?.slice(prefix.length) || '';
}

function resolveGitSha() {
  const providedGitSha = process.env.MANGO_BUSINESS_LAB_GIT_COMMIT?.trim();
  if (providedGitSha) {
    if (!/^(?:[a-f\d]{40}|[a-f\d]{64})$/i.test(providedGitSha)) {
      throw new Error('MANGO_BUSINESS_LAB_GIT_COMMIT must be a full 40- or 64-character Git SHA');
    }
    return providedGitSha.toLowerCase();
  }
  return run('git', ['rev-parse', 'HEAD'], { capture: true, cwd: repoRoot }).stdout.trim();
}

function resolveGitTree() {
  const providedGitTree = process.env.MANGO_BUSINESS_LAB_GIT_TREE?.trim();
  if (providedGitTree) {
    if (!/^(?:[a-f\d]{40}|[a-f\d]{64})$/iu.test(providedGitTree)) {
      throw new Error('MANGO_BUSINESS_LAB_GIT_TREE must be a full 40- or 64-character Git tree SHA');
    }
    return providedGitTree.toLowerCase();
  }
  return run('git', ['rev-parse', 'HEAD^{tree}'], { capture: true, cwd: repoRoot }).stdout.trim();
}

function run(command, args, options = {}) {
  const startedAt = Date.now();
  const result = spawnSync(command, args, {
    cwd: options.cwd || uiRoot,
    encoding: 'utf8',
    stdio: options.capture ? 'pipe' : 'inherit',
    shell: shouldUseShellForCommand(command),
    env: {
      ...process.env,
      FORCE_COLOR: '0',
      npm_config_registry: registry,
      NPM_CONFIG_REGISTRY: registry,
      ...options.env,
    },
    maxBuffer: 64 * 1024 * 1024,
  });
  if (result.status !== 0) {
    const output = options.capture ? `\n${result.stdout || ''}\n${result.stderr || ''}` : '';
    throw new Error(`${command} ${args.join(' ')} failed with status ${result.status ?? 1}${output}`);
  }
  return { ...result, durationMs: Date.now() - startedAt };
}

function readPackedPackage(tarballPath) {
  const result = run('tar', ['-xOf', tarballPath, 'package/package.json'], { capture: true });
  return JSON.parse(result.stdout);
}

function listPackablePackages() {
  return readdirSync(join(uiRoot, 'packages'))
    .map((entry) => join(uiRoot, 'packages', entry))
    .filter((packageRoot) => existsSync(join(packageRoot, 'package.json')))
    .filter((packageRoot) => {
      const packageJson = readJson(join(packageRoot, 'package.json'));
      return packageJson.name?.startsWith('@mango/') && !packageJson.private;
    })
    .sort();
}

function packPackages() {
  const tarballsByName = new Map();
  for (const packageRoot of listPackablePackages()) {
    run(pnpmCommand, ['--config.ignore-scripts=true', 'pack', '--pack-destination', tarballRoot], {
      cwd: packageRoot,
    });
  }
  for (const file of readdirSync(tarballRoot)
    .filter((entry) => entry.endsWith('.tgz'))
    .sort()) {
    const tarballPath = join(tarballRoot, file);
    const packageJson = readPackedPackage(tarballPath);
    if (tarballsByName.has(packageJson.name)) {
      throw new Error(`duplicate packed Mango package: ${packageJson.name}`);
    }
    tarballsByName.set(packageJson.name, tarballPath);
  }
  if (tarballsByName.size === 0) {
    throw new Error('business lab packed zero Mango packages');
  }
  return tarballsByName;
}

function installPackedCli(tarballsByName) {
  const cliTarball = tarballsByName.get('@mango/cli');
  const pmoTarball = tarballsByName.get('@mango/pmo');
  if (!cliTarball || !pmoTarball) {
    throw new Error('business lab requires packed @mango/cli and @mango/pmo artifacts');
  }
  const toRelativeTarball = (path) => `file:${relative(bootstrapRoot, path).split('\\').join('/')}`;
  writeJson(join(bootstrapRoot, 'package.json'), {
    name: 'mango-business-lab-bootstrap',
    private: true,
    packageManager: readJson(join(uiRoot, 'package.json')).packageManager,
    dependencies: {
      '@mango/cli': toRelativeTarball(cliTarball),
      '@mango/pmo': toRelativeTarball(pmoTarball),
    },
  });
  writeFileSync(
    join(bootstrapRoot, 'pnpm-workspace.yaml'),
    `packages: []\noverrides:\n  "@mango/pmo": "${toRelativeTarball(pmoTarball)}"\n`,
  );
  writeFileSync(join(bootstrapRoot, '.npmrc'), `registry=${registry}\n`);
  run(pnpmCommand, ['install', '--frozen-lockfile=false', `--store-dir=${dependencyStore}`, `--registry=${registry}`], {
    cwd: bootstrapRoot,
  });
  return join(bootstrapRoot, 'node_modules', '.bin', process.platform === 'win32' ? 'mango.cmd' : 'mango');
}

function generateProject(cliPath) {
  mkdirSync(dirname(projectRoot), { recursive: true });
  run(
    cliPath,
    ['init', projectName, '--preset', 'full', '--topology', 'monolith', '--npm-registry', registry, '--force'],
    { cwd: dirname(projectRoot) },
  );
  if (!existsSync(join(frontendRoot, 'package.json'))) {
    throw new Error(`packed CLI did not generate ${frontendRoot}`);
  }
  run(
    cliPath,
    [
      'module',
      'add',
      'orders',
      '--aggregate',
      'sales-order',
      '--aggregate-name',
      '销售订单',
      '--module-name',
      '订单模块',
      '--project-dir',
      projectRoot,
    ],
    { cwd: projectRoot },
  );
}

function prepareOfflineInstall(tarballsByName) {
  const mappings = applyTarballMappings(frontendRoot, tarballsByName, registry);
  assertGeneratedProjectBoundary(projectRoot, [repoRoot]);
  run(pnpmCommand, ['install', '--lockfile-only', `--store-dir=${dependencyStore}`, `--registry=${registry}`], {
    cwd: frontendRoot,
  });
  run(pnpmCommand, ['fetch', '--frozen-lockfile', `--store-dir=${dependencyStore}`, `--registry=${registry}`], {
    cwd: frontendRoot,
  });
  rmSync(join(frontendRoot, 'node_modules'), { recursive: true, force: true });
  assertGeneratedProjectBoundary(projectRoot, [repoRoot]);
  return mappings;
}

function imageIdentity() {
  const result = run('docker', ['image', 'inspect', image, '--format', '{{.Id}}'], { capture: true });
  return result.stdout.trim();
}

function runSealedPhase(currentImageIdentity) {
  const sealedRunner = join(uiRoot, 'scripts', 'run-business-lab-sealed.mjs');
  const library = join(uiRoot, 'scripts', 'quality', 'business-lab-lib.mjs');
  run('docker', [
    'run',
    '--rm',
    '--network',
    'none',
    '--cap-drop',
    'ALL',
    '--security-opt',
    'no-new-privileges',
    '--pids-limit',
    '512',
    '--tmpfs',
    '/tmp:rw,noexec,nosuid,size=512m',
    '-e',
    'CI=true',
    '-e',
    'HOME=/runtime/business-lab/sealed-home',
    '-e',
    'COREPACK_HOME=/root/.cache/node/corepack',
    '-e',
    'COREPACK_ENABLE_DOWNLOAD_PROMPT=0',
    '-e',
    'MANGO_WORKSPACE_REGISTRY=/runtime/business-lab/workspaces.json',
    '-e',
    `MANGO_BUSINESS_LAB_FORBIDDEN_ROOT=${repoRoot}`,
    '-e',
    `MANGO_BUSINESS_LAB_IMAGE_IDENTITY=${currentImageIdentity}`,
    '-e',
    'HTTP_PROXY=',
    '-e',
    'HTTPS_PROXY=',
    '-e',
    'ALL_PROXY=',
    '-e',
    'NO_PROXY=',
    '-e',
    'NPM_CONFIG_USERCONFIG=/dev/null',
    '-v',
    `${runtimeMount}:/runtime`,
    '-v',
    `${sealedRunner}:/runner.mjs:ro`,
    '-v',
    `${library}:/business-lab-lib.mjs:ro`,
    '-w',
    `/runtime/projects/${projectName}`,
    image,
    'node',
    '/runner.mjs',
  ]);
}

function readSealedReport() {
  if (runtimeMount === runtimeRoot) {
    return readJson(sealedReportPath);
  }
  const result = run(
    'docker',
    [
      'run',
      '--rm',
      '-v',
      `${runtimeMount}:/runtime:ro`,
      image,
      'cat',
      '/runtime/frontend-quality/business-lab/sealed-report.json',
    ],
    { capture: true },
  );
  return JSON.parse(result.stdout);
}

if (sealedOnly) {
  try {
    if (
      runtimeMount === runtimeRoot &&
      (!existsSync(preparationReportPath) || !existsSync(join(frontendRoot, 'pnpm-lock.yaml')))
    ) {
      throw new Error('Business Lab must be prepared before --sealed-only can run');
    }
    const currentImageIdentity = imageIdentity();
    if (runtimeMount === runtimeRoot) {
      const preparationReport = readJson(preparationReportPath);
      preparationReport.imageIdentity = currentImageIdentity;
      writeJson(preparationReportPath, preparationReport);
    }
    runSealedPhase(currentImageIdentity);
    const sealedReport = readSealedReport();
    if (sealedReport.status !== 'passed') {
      throw new Error(`Business Lab sealed report is not passed: ${sealedReport.status}`);
    }
    console.log(`Mango Business Lab PASS: ${sealedReportPath}`);
    process.exit(0);
  } catch (error) {
    console.error(error instanceof Error ? error.stack || error.message : error);
    process.exit(1);
  }
}

try {
  rmSync(projectRoot, { recursive: true, force: true });
  rmSync(labRoot, { recursive: true, force: true });
  rmSync(dependencyStore, { recursive: true, force: true });
  rmSync(reportRoot, { recursive: true, force: true });
  mkdirSync(tarballRoot, { recursive: true });
  mkdirSync(bootstrapRoot, { recursive: true });
  mkdirSync(dependencyStore, { recursive: true });
  mkdirSync(reportRoot, { recursive: true });

  if (!reuseBuild) {
    console.log('Building Mango package candidates for Business Lab');
    run(pnpmCommand, ['admin:styles']);
    run(pnpmCommand, ['--filter', '@mango/admin', 'run', 'build']);
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
  }
  run(pnpmCommand, ['package-exports:check']);

  console.log('Packing Mango package candidates for Business Lab');
  const tarballsByName = packPackages();
  const cliPath = installPackedCli(tarballsByName);
  console.log('Generating full-preset Business Lab from packed CLI');
  generateProject(cliPath);
  const mappings = prepareOfflineInstall(tarballsByName);
  const gitSha = resolveGitSha();
  const gitTree = resolveGitTree();
  const tarballs = [...tarballsByName]
    .map(([name, path]) => ({
      name,
      file: relative(runtimeRoot, path).split('\\').join('/'),
      sha256: sha256File(path),
    }))
    .sort((left, right) => left.name.localeCompare(right.name));
  const preparationReport = {
    schemaVersion: 1,
    status: 'prepared',
    generatedAt: new Date().toISOString(),
    gitSha,
    gitTree,
    sourceMode: 'git-archive-exact-commit',
    node: process.version,
    platform: process.platform,
    arch: process.arch,
    registry,
    image,
    project: relative(runtimeRoot, projectRoot).split('\\').join('/'),
    packageStore: relative(runtimeRoot, dependencyStore).split('\\').join('/'),
    lockfileSha256: sha256File(join(frontendRoot, 'pnpm-lock.yaml')),
    packageManager: readJson(join(frontendRoot, 'package.json')).packageManager,
    packedPackageCount: tarballs.length,
    mappedPackageCount: mappings.size,
    tarballs,
  };
  writeJson(preparationReportPath, preparationReport);
  console.log(`Business Lab preparation report: ${preparationReportPath}`);

  if (prepareOnly) {
    console.log('Business Lab preparation completed; sealed phase was explicitly skipped.');
  } else {
    preparationReport.imageIdentity = imageIdentity();
    writeJson(preparationReportPath, preparationReport);
    runSealedPhase(preparationReport.imageIdentity);
    const sealedReport = readSealedReport();
    if (sealedReport.status !== 'passed') {
      throw new Error(`Business Lab sealed report is not passed: ${sealedReport.status}`);
    }
    console.log(`Mango Business Lab PASS: ${sealedReportPath}`);
  }
} catch (error) {
  console.error(error instanceof Error ? error.stack || error.message : error);
  process.exit(1);
}
