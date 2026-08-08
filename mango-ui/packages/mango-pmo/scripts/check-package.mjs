#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(fileURLToPath(new URL('..', import.meta.url)));
const distRoot = join(packageRoot, 'dist');
const baselineRoot = join(distRoot, 'baseline');
const manifestPath = join(distRoot, 'baseline.json');
const packageJson = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'));
const requiredFiles = [
  'rules/00-dev-flow.md',
  'README.md',
  'rules/03-ai-coding-redlines.md',
  'rules/index.json',
  'agents/03-dev-agent.md',
  'agents/05-pmo-agent.md',
  'tools/pmo-preflight.mjs',
  'tools/code-baseline.mjs',
  'tools/check-document-set.mjs',
  'tools/pin-historical-pmo-version-documents.mjs',
  'tools/check-frontend-page-baseline.mjs',
  'tools/delivery-contract-check.mjs',
  'tools/acceptance-evidence-check.mjs',
  'templates/delivery-contract.md',
  'templates/acceptance-evidence.md',
  'templates/business-pull-request-template.md',
  'contracts/delivery-assurance.json',
  'contracts/lean-documents.json',
  'contracts/business-requirements.json',
  'contracts/system-requirements.json',
  'contracts/technical-design.json',
  'contracts/implementation-plan.json',
  'contracts/document-lifecycle.json',
  'templates/delivery-l2.md',
  'templates/delivery-l3.md',
  'templates/delivery-l4.md',
  'templates/l5-business-requirements.md',
  'templates/l5-system-requirements.md',
  'templates/l5-technical-design.md',
  'templates/l5-implementation-plan.md',
  'tools/check-lean-document.mjs',
  'tools/resolve-lean-document-policy.mjs',
  'examples/lean-documents/delivery-l2-json-error.md',
  'examples/lean-documents/delivery-l3-workflow-withdraw.md',
  'examples/lean-documents/delivery-l4-third-party-login.md',
  'examples/lean-documents/l5-supplier-business-requirements.md',
  'examples/lean-documents/l5-supplier-system-requirements.md',
  'examples/lean-documents/l5-supplier-technical-design.md',
  'examples/lean-documents/l5-supplier-implementation-plan.md',
  'tools/risk-verification.mjs',
  'skills/mango-pmo-lifecycle/SKILL.md',
  'skills/mango-requirements-business/SKILL.md',
  'skills/mango-requirements-system/SKILL.md',
  'skills/mango-design-technical/SKILL.md',
  'skills/mango-plan-implementation/SKILL.md',
  'code-templates/index.json',
  'code-templates/business-module/backend/modules/{{moduleKebab}}/pom.xml',
  'code-templates/business-module/frontend/packages/{{moduleKebab}}/src/index.ts',
];

if (!existsSync(manifestPath)) {
  throw new Error('dist/baseline.json not found. Run pnpm -F @mango/pmo build first.');
}

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
validateManifestHeader(manifest);
const manifestFiles = new Map();
for (const file of manifest.files || []) {
  validateManifestFile(file);
  if (manifestFiles.has(file.path)) {
    throw new Error(`duplicate baseline manifest path: ${file.path}`);
  }
  manifestFiles.set(file.path, file);
}

for (const file of requiredFiles) {
  if (!manifestFiles.has(file)) {
    throw new Error(`baseline manifest missing required file: ${file}`);
  }
}

const diskFiles = walkFiles(baselineRoot)
  .map((path) => toPosix(relative(baselineRoot, path)))
  .sort(compareText);
const expectedFiles = [...manifestFiles.keys()].sort(compareText);
if (JSON.stringify(diskFiles) !== JSON.stringify(expectedFiles)) {
  const expected = new Set(expectedFiles);
  const actual = new Set(diskFiles);
  const missing = expectedFiles.filter((path) => !actual.has(path));
  const extra = diskFiles.filter((path) => !expected.has(path));
  throw new Error(
    `baseline tree differs from manifest; missing=${missing.join(',') || '-'} extra=${extra.join(',') || '-'}`,
  );
}

for (const file of manifest.files) {
  const path = join(baselineRoot, file.path);
  const content = readFileSync(path);
  if (content.toString('utf8').endsWith('\n\n')) {
    throw new Error(`baseline file has trailing blank line at EOF: ${file.path}`);
  }
  const actual = sha256(content);
  if (actual !== file.sha256) {
    throw new Error(`manifest hash mismatch: ${file.path}`);
  }
  if (content.length !== file.size) {
    throw new Error(`manifest size mismatch: ${file.path}`);
  }
  if (process.platform !== 'win32') {
    const actualMode = statSync(path).mode & 0o111 ? '0755' : '0644';
    if (actualMode !== file.mode) {
      throw new Error(`manifest mode mismatch: ${file.path}`);
    }
  }
}

validateContracts(manifest, manifestFiles);
validatePullRequestTemplate(manifest);
validatePluginProjection(manifest, manifestFiles);
validatePublishExecutableFiles(manifest);
const expectedBundleSha = sha256(
  Buffer.from(
    JSON.stringify({
      files: manifest.files,
      contracts: manifest.contracts,
      plugin: manifest.plugin,
    }),
    'utf8',
  ),
);
if (manifest.bundleSha256 !== expectedBundleSha) {
  throw new Error(`bundle hash mismatch: expected ${expectedBundleSha}, got ${manifest.bundleSha256}`);
}

const preflight = spawnSync(
  process.execPath,
  [
    'tools/pmo-preflight.mjs',
    '--role',
    'dev',
    '--phase',
    'develop',
    '--task',
    '新增业务模块并验证 @mango/pmo baseline package',
    '--paths',
    'backend/modules/order,frontend/packages/order',
  ],
  {
    cwd: baselineRoot,
    encoding: 'utf8',
  },
);

if (preflight.status !== 0) {
  throw new Error(`packaged baseline preflight failed:\n${preflight.stdout}\n${preflight.stderr}`);
}
if (!preflight.stdout.includes('Code baselines:') || !preflight.stdout.includes('business-module@1')) {
  throw new Error(`packaged baseline preflight did not resolve code baseline:\n${preflight.stdout}`);
}

validatePackedPackage(manifest);

process.stdout.write(
  `Checked ${manifest.packageName}@${manifest.packageVersion} bundle ${manifest.bundleSha256.slice(0, 12)}.\n`,
);

function validateManifestHeader(value) {
  if (value.packageName !== packageJson.name || value.packageVersion !== packageJson.version) {
    throw new Error('baseline manifest package identity does not match package.json');
  }
  if (value.schemaVersion !== 2) {
    throw new Error(`unsupported baseline manifest schemaVersion: ${value.schemaVersion}`);
  }
  if (value.generatedAt !== undefined) {
    throw new Error('baseline manifest must be reproducible and cannot contain generatedAt');
  }
  if (typeof value.sourceCommit !== 'string' || !/^[0-9a-f]{40}$/i.test(value.sourceCommit)) {
    throw new Error('baseline manifest sourceCommit must be a full Git commit SHA');
  }
  if (typeof value.bundleSha256 !== 'string' || !/^[0-9a-f]{64}$/i.test(value.bundleSha256)) {
    throw new Error('baseline manifest bundleSha256 must be SHA-256');
  }
  if (!Array.isArray(value.files) || value.files.length === 0 || !Array.isArray(value.contracts)) {
    throw new Error('baseline manifest must contain files and contracts arrays');
  }
}

function validateManifestFile(file) {
  if (!isSafeRelativePath(file.path)) {
    throw new Error(`unsafe baseline manifest path: ${file.path}`);
  }
  if (!Number.isInteger(file.size) || file.size < 0) {
    throw new Error(`invalid baseline manifest size: ${file.path}`);
  }
  if (typeof file.sha256 !== 'string' || !/^[0-9a-f]{64}$/i.test(file.sha256)) {
    throw new Error(`invalid baseline manifest sha256: ${file.path}`);
  }
  if (!['0644', '0755'].includes(file.mode)) {
    throw new Error(`invalid baseline manifest mode: ${file.path}`);
  }
  if (
    ![
      'agent',
      'rule',
      'template',
      'code-template',
      'contract',
      'tool',
      'skill',
      'documentation',
      'asset',
      'plugin',
    ].includes(file.kind)
  ) {
    throw new Error(`invalid baseline manifest kind: ${file.path}`);
  }
}

function validatePluginProjection(value, baselineFiles) {
  const plugin = value.plugin;
  if (!plugin || plugin.path !== 'package-root' || !Array.isArray(plugin.files) || !isSha256(plugin.sha256)) {
    throw new Error('baseline manifest is missing the Codex plugin projection');
  }
  const files = new Map();
  for (const file of plugin.files) {
    validateManifestFile(file);
    if (files.has(file.path)) {
      throw new Error(`duplicate plugin projection path: ${file.path}`);
    }
    files.set(file.path, file);
  }
  const diskFiles = [
    ...walkFiles(join(packageRoot, '.codex-plugin')).map(
      (path) => `.codex-plugin/${toPosix(relative(join(packageRoot, '.codex-plugin'), path))}`,
    ),
    ...walkFiles(join(packageRoot, 'skills')).map(
      (path) => `skills/${toPosix(relative(join(packageRoot, 'skills'), path))}`,
    ),
  ].sort(compareText);
  const expectedFiles = [...files.keys()].sort(compareText);
  if (JSON.stringify(diskFiles) !== JSON.stringify(expectedFiles)) {
    throw new Error('Codex plugin projection tree differs from its manifest');
  }
  for (const file of files.values()) {
    const content = readFileSync(join(packageRoot, file.path));
    if (content.length !== file.size || sha256(content) !== file.sha256) {
      throw new Error(`Codex plugin projection hash mismatch: ${file.path}`);
    }
  }
  if (sha256(Buffer.from(JSON.stringify(plugin.files), 'utf8')) !== plugin.sha256) {
    throw new Error('Codex plugin projection aggregate hash mismatch');
  }
  const pluginManifest = JSON.parse(readFileSync(join(packageRoot, '.codex-plugin/plugin.json'), 'utf8'));
  if (pluginManifest.version !== packageJson.version || pluginManifest.skills !== './skills/') {
    throw new Error('Codex plugin projection metadata does not match the package version or skill path');
  }
  for (const [path, file] of baselineFiles) {
    if (!path.startsWith('skills/')) {
      continue;
    }
    const projected = files.get(path);
    if (!projected || projected.sha256 !== file.sha256 || projected.size !== file.size) {
      throw new Error(`Codex plugin skill projection differs from baseline: ${path}`);
    }
  }
}

function validatePublishExecutableFiles(value) {
  if (process.platform === 'win32') {
    return;
  }
  const expected = [
    ...value.files.filter((file) => file.mode === '0755').map((file) => `dist/baseline/${file.path}`),
    ...value.plugin.files.filter((file) => file.mode === '0755').map((file) => file.path),
  ].sort(compareText);
  const declared = [...(packageJson.publishConfig?.executableFiles || [])].sort(compareText);
  if (JSON.stringify(declared) !== JSON.stringify(expected)) {
    throw new Error(
      `publishConfig.executableFiles differs from manifest; expected=${expected.join(',') || '-'} declared=${declared.join(',') || '-'}`,
    );
  }
}

function validatePackedPackage(value) {
  if (process.platform === 'win32') {
    return;
  }
  const temporaryRoot = mkdtempSync(join(tmpdir(), 'mango-pmo-pack-check-'));
  try {
    const pnpm = spawnSync('pnpm', ['pack', '--pack-destination', temporaryRoot], {
      cwd: packageRoot,
      encoding: 'utf8',
    });
    if (pnpm.error) {
      throw new Error(`failed to execute pnpm pack: ${pnpm.error.message}`);
    }
    if (pnpm.status !== 0) {
      throw new Error(`pnpm pack failed:\n${pnpm.stdout}\n${pnpm.stderr}`);
    }
    const tarballs = readdirSync(temporaryRoot).filter((file) => file.endsWith('.tgz'));
    if (tarballs.length !== 1) {
      throw new Error(`pnpm pack must create exactly one tarball, got ${tarballs.length}`);
    }
    const extract = spawnSync('tar', ['-xzf', join(temporaryRoot, tarballs[0]), '-C', temporaryRoot], {
      encoding: 'utf8',
    });
    if (extract.status !== 0) {
      throw new Error(`failed to extract pnpm tarball:\n${extract.stdout}\n${extract.stderr}`);
    }
    const packedRoot = join(temporaryRoot, 'package');
    const packedManifest = JSON.parse(readFileSync(join(packedRoot, 'dist/baseline.json'), 'utf8'));
    if (packedManifest.bundleSha256 !== value.bundleSha256) {
      throw new Error('pnpm tarball baseline manifest differs from the built manifest');
    }
    for (const file of value.files) {
      validatePackedFile(join(packedRoot, 'dist/baseline', file.path), file);
    }
    for (const file of value.plugin.files) {
      validatePackedFile(join(packedRoot, file.path), file);
    }
  } finally {
    rmSync(temporaryRoot, { recursive: true, force: true });
  }
}

function validatePackedFile(path, descriptor) {
  if (!existsSync(path)) {
    throw new Error(`pnpm tarball is missing manifest file: ${descriptor.path}`);
  }
  const content = readFileSync(path);
  const actualMode = statSync(path).mode & 0o111 ? '0755' : '0644';
  if (content.length !== descriptor.size || sha256(content) !== descriptor.sha256 || actualMode !== descriptor.mode) {
    throw new Error(
      `pnpm tarball file differs from manifest: ${descriptor.path} expectedMode=${descriptor.mode} actualMode=${actualMode}`,
    );
  }
}

function validateContracts(value, files) {
  const ids = new Set();
  for (const contract of value.contracts) {
    if (!contract.contractId || !Number.isInteger(contract.schemaRevision) || contract.schemaRevision < 1) {
      throw new Error('invalid contract descriptor in baseline manifest');
    }
    if (ids.has(contract.contractId)) {
      throw new Error(`duplicate contract descriptor: ${contract.contractId}`);
    }
    ids.add(contract.contractId);
    const file = files.get(contract.path);
    if (!file || file.kind !== 'contract') {
      throw new Error(`contract descriptor points to missing contract file: ${contract.path}`);
    }
  }
  for (const contractId of [
    'delivery-assurance',
    'lean-documents',
    'business-requirements',
    'system-requirements',
    'technical-design',
    'implementation-plan',
    'document-lifecycle',
  ]) {
    if (!ids.has(contractId)) {
      throw new Error(`baseline manifest is missing required contract: ${contractId}`);
    }
  }
}

function validatePullRequestTemplate(value) {
  const descriptor = value.contracts.find((contract) => contract.contractId === 'delivery-assurance');
  if (!descriptor) {
    throw new Error('baseline manifest is missing delivery-assurance contract');
  }
  const contract = JSON.parse(readFileSync(join(baselineRoot, descriptor.path), 'utf8'));
  if (
    contract.schemaRevision !== descriptor.schemaRevision ||
    typeof contract.pullRequestBody?.templatePath !== 'string'
  ) {
    throw new Error('delivery-assurance PR template metadata differs from the baseline manifest');
  }
  const templatePath = join(baselineRoot, contract.pullRequestBody.templatePath);
  const checkerPath = join(baselineRoot, 'tools/risk-verification.mjs');
  const result = spawnSync(process.execPath, [checkerPath, '--template', '--body', templatePath], {
    cwd: baselineRoot,
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    throw new Error(`packaged business PR template contract failed:\n${result.stdout}\n${result.stderr}`);
  }
  if (!result.stdout.includes(`schema revision ${descriptor.schemaRevision}`)) {
    throw new Error(`packaged business PR template did not report schema revision ${descriptor.schemaRevision}`);
  }
}

function walkFiles(root) {
  if (!existsSync(root)) {
    throw new Error(`baseline directory missing: ${root}`);
  }
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`symbolic link found in baseline package: ${path}`);
    }
    if (entry.isDirectory()) {
      files.push(...walkFiles(path));
    } else if (entry.isFile()) {
      files.push(path);
    } else {
      throw new Error(`unsupported baseline package entry: ${path}`);
    }
  }
  return files;
}

function isSafeRelativePath(path) {
  return (
    typeof path === 'string' &&
    path.length > 0 &&
    !path.startsWith('/') &&
    !path.includes('\\') &&
    path.split('/').every((segment) => segment && segment !== '.' && segment !== '..')
  );
}

function isSha256(value) {
  return typeof value === 'string' && /^[0-9a-f]{64}$/i.test(value);
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function compareText(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function toPosix(path) {
  return path.split('\\').join('/');
}
