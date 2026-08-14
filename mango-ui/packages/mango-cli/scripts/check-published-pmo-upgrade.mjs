import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';

const options = parseArgs(process.argv.slice(2));
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-cli-pmo-upgrade-consumer-'));
const consumerRoot = join(tempRoot, 'consumer');
const projectRoot = join(tempRoot, 'project');

try {
  mkdirSync(consumerRoot, { recursive: true });
  writeFileSync(
    join(consumerRoot, 'package.json'),
    `${JSON.stringify(
      {
        name: 'mango-cli-pmo-upgrade-consumer',
        private: true,
        version: '1.0.0',
        dependencies: {
          '@mango/cli': options.cliSpec || options.version,
        },
      },
      null,
      2,
    )}\n`,
  );
  run(
    'npm',
    ['install', '--ignore-scripts', '--prefer-online', '--registry', options.registry],
    consumerRoot,
    'install clean CLI consumer',
  );

  const cliRoot = join(consumerRoot, 'node_modules/@mango/cli');
  const cliPackage = readJson(join(cliRoot, 'package.json'));
  if (cliPackage.version !== options.version) {
    throw new Error(`installed CLI version mismatch: expected ${options.version}, got ${cliPackage.version}`);
  }
  const cli = join(cliRoot, 'src/index.mjs');
  run(
    process.execPath,
    [cli, 'init', 'project', '--preset', 'custom', '--modules', 'none'],
    tempRoot,
    'create clean business consumer',
  );

  const manifestPath = join(projectRoot, 'business-pmo/mango-baseline/baseline.json');
  const lockPath = join(projectRoot, 'business-pmo/pmo-lock.json');
  const legacyPath = join(projectRoot, 'business-pmo/mango-baseline/code-templates/README.md');
  const legacyContent = '# Legacy code templates\n';
  mkdirSync(dirname(legacyPath), { recursive: true });
  writeFileSync(legacyPath, legacyContent);

  const manifest = readJson(manifestPath);
  manifest.packageVersion = '1.3.11';
  manifest.files.push({
    path: 'code-templates/README.md',
    sha256: createHash('sha256').update(legacyContent).digest('hex'),
    size: Buffer.byteLength(legacyContent),
    kind: 'code-template',
    mode: '0644',
  });
  manifest.bundleSha256 = createHash('sha256')
    .update(JSON.stringify({ files: manifest.files, contracts: manifest.contracts, plugin: manifest.plugin ?? null }))
    .digest('hex');
  writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);

  const lock = readJson(lockPath);
  lock.packageVersion = manifest.packageVersion;
  lock.bundleSha256 = manifest.bundleSha256;
  writeFileSync(lockPath, `${JSON.stringify(lock, null, 2)}\n`);

  run(
    process.execPath,
    [cli, 'pmo', 'upgrade', '--project-dir', projectRoot, '--to', '1.3.13'],
    projectRoot,
    'upgrade historical PMO manifest',
  );
  if (existsSync(legacyPath)) {
    throw new Error('obsolete code-templates/README.md remains after PMO upgrade');
  }
  const upgradedManifest = readJson(manifestPath);
  if (upgradedManifest.packageVersion !== '1.3.13') {
    throw new Error(`upgraded PMO version mismatch: ${upgradedManifest.packageVersion}`);
  }
  run(
    process.execPath,
    [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'],
    projectRoot,
    'verify upgraded PMO lock',
  );
  process.stdout.write(
    `${JSON.stringify({ result: 'PASS', cliVersion: cliPackage.version, pmoVersion: upgradedManifest.packageVersion })}\n`,
  );
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function parseArgs(args) {
  const result = { version: '', registry: '', cliSpec: '' };
  for (const arg of args) {
    if (arg.startsWith('--version=')) result.version = arg.slice('--version='.length);
    else if (arg.startsWith('--registry=')) result.registry = arg.slice('--registry='.length);
    else if (arg.startsWith('--cli-spec=')) result.cliSpec = resolve(arg.slice('--cli-spec='.length));
    else throw new Error(`unknown argument: ${arg}`);
  }
  if (!/^\d+\.\d+\.\d+$/.test(result.version)) throw new Error('--version must be a semantic version');
  if (!/^https?:\/\//.test(result.registry)) throw new Error('--registry must be an HTTP(S) URL');
  return result;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function run(command, args, cwd, label) {
  const result = spawnSync(command, args, { cwd, encoding: 'utf8' });
  if (result.status !== 0) {
    throw new Error(`${label} failed:\n${result.stdout}\n${result.stderr}`);
  }
}
