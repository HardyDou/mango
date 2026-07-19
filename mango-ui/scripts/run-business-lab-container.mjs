#!/usr/bin/env node
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const uiRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(uiRoot, '..');
const image = readArgument('--image=') || 'mango/frontend-quality:node22-pnpm11.14';
const keepVolume = process.argv.includes('--keep-volume');
const suffix = `${Date.now()}-${process.pid}`;
const runtimeVolume = `mango-frontend-candidate-${suffix}`;
const modulesVolume = `mango-frontend-candidate-modules-${suffix}`;
const gitSha = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: repoRoot, encoding: 'utf8' }).trim();
const gitCommonDir = resolve(
  repoRoot,
  execFileSync('git', ['rev-parse', '--git-common-dir'], { cwd: repoRoot, encoding: 'utf8' }).trim(),
);
const reportRoot = join(repoRoot, '.runtime', 'frontend-quality', 'business-lab');
let passed = false;

function readArgument(prefix) {
  return process.argv.find((argument) => argument.startsWith(prefix))?.slice(prefix.length) || '';
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || repoRoot,
    encoding: 'utf8',
    stdio: options.capture ? 'pipe' : 'inherit',
    env: { ...process.env, FORCE_COLOR: '0', ...options.env },
    maxBuffer: 64 * 1024 * 1024,
  });
  if (result.status !== 0) {
    const output = options.capture ? `\n${result.stdout || ''}\n${result.stderr || ''}` : '';
    throw new Error(`${command} ${args.join(' ')} failed with status ${result.status ?? 1}${output}`);
  }
  return result;
}

function readReport(path) {
  return run('docker', ['run', '--rm', '-v', `${runtimeVolume}:/runtime:ro`, image, 'cat', path], { capture: true })
    .stdout;
}

function removeVolume(name) {
  spawnSync('docker', ['volume', 'rm', '-f', name], { cwd: repoRoot, stdio: 'ignore' });
}

try {
  run('docker', ['image', 'inspect', image]);
  run('docker', ['volume', 'create', runtimeVolume]);
  run('docker', ['volume', 'create', modulesVolume]);
  run('docker', [
    'run',
    '--rm',
    '-e',
    `MANGO_BUSINESS_LAB_GIT_COMMIT=${gitSha}`,
    '-e',
    `MANGO_PMO_SOURCE_COMMIT=${gitSha}`,
    '-v',
    `${repoRoot}:/workspace`,
    '-v',
    `${gitCommonDir}:${gitCommonDir}:ro`,
    '-v',
    `${modulesVolume}:/workspace/mango-ui/node_modules`,
    '-v',
    `${runtimeVolume}:/runtime`,
    '-w',
    '/workspace/mango-ui',
    image,
    'sh',
    '-lc',
    'pnpm install --frozen-lockfile && pnpm check:full && node scripts/run-business-lab.mjs --prepare-only --runtime-mount=/runtime',
  ]);
  run(
    process.execPath,
    [
      join(uiRoot, 'scripts', 'run-business-lab.mjs'),
      '--sealed-only',
      `--runtime-mount=${runtimeVolume}`,
      `--image=${image}`,
    ],
    { cwd: uiRoot },
  );

  mkdirSync(reportRoot, { recursive: true });
  for (const file of ['preparation-report.json', 'sealed-report.json']) {
    writeFileSync(join(reportRoot, file), readReport(`/runtime/frontend-quality/business-lab/${file}`));
  }
  const sealed = JSON.parse(readReport('/runtime/frontend-quality/business-lab/sealed-report.json'));
  if (sealed.status !== 'passed') throw new Error(`sealed report status is ${sealed.status}`);
  passed = true;
  console.log(`Mango container Business Lab PASS: ${reportRoot}`);
} catch (error) {
  console.error(error instanceof Error ? error.stack || error.message : error);
  console.error(`Business Lab runtime volume retained for diagnosis: ${runtimeVolume}`);
  process.exitCode = 1;
} finally {
  removeVolume(modulesVolume);
  if (passed && !keepVolume) removeVolume(runtimeVolume);
  else if (passed) console.log(`Business Lab runtime volume retained: ${runtimeVolume}`);
}
