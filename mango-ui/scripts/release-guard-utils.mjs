import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';

export const HOSTED_REGISTRY = 'http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/';
export const GROUP_REGISTRY = 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';

export function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

export function run(command, args, options = {}) {
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

export function normalizePackageName(input) {
  if (!input) {
    return '';
  }
  if (input.startsWith('@')) {
    return input;
  }
  if (input === 'cli' || input === 'mango-cli') {
    return '@mango/cli';
  }
  if (input === 'pmo' || input === 'mango-pmo') {
    return '@mango/pmo';
  }
  return `@mango/${input.replace(/^mango-/, '')}`;
}

export function indexWorkspacePackages(workspaceRoot = process.cwd()) {
  const packagesRoot = join(workspaceRoot, 'packages');
  const packages = new Map();
  for (const dir of readdirSync(packagesRoot)) {
    const packageJsonPath = join(packagesRoot, dir, 'package.json');
    if (!existsSync(packageJsonPath)) {
      continue;
    }
    const packageJson = readJson(packageJsonPath);
    if (packageJson.name?.startsWith('@mango/')) {
      packages.set(packageJson.name, {
        dir,
        root: join(packagesRoot, dir),
        packageJson,
        packageJsonPath,
      });
    }
  }
  return packages;
}

export function findPackage(packageName, workspaceRoot = process.cwd()) {
  return indexWorkspacePackages(workspaceRoot).get(packageName) ?? null;
}

export function npmView(packageName, registry) {
  return spawnSync('npm', ['view', packageName, 'version', `--registry=${registry}`], {
    stdio: 'pipe',
    encoding: 'utf8',
  });
}

export function parsePackageJsonAllowingTemplates(packageJsonPath) {
  const content = readFileSync(packageJsonPath, 'utf8')
    .split('\n')
    .filter((line) => !line.trim().match(/^{{[a-zA-Z0-9]+}}$/))
    .join('\n');
  return JSON.parse(content);
}

export function collectPackageJsonFiles(roots) {
  const packageJsonFiles = [];
  for (const root of roots) {
    collectPackageJsonFilesFromRoot(root, packageJsonFiles);
  }
  return packageJsonFiles;
}

function collectPackageJsonFilesFromRoot(root, packageJsonFiles) {
  if (!existsSync(root)) {
    return;
  }
  const stats = statSync(root);
  if (stats.isFile()) {
    if (root.endsWith('package.json') || root.endsWith('package.json.template')) {
      packageJsonFiles.push(root);
    }
    return;
  }
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name === 'coverage') {
      continue;
    }
    const entryPath = join(root, entry.name);
    if (entry.isDirectory()) {
      collectPackageJsonFilesFromRoot(entryPath, packageJsonFiles);
      continue;
    }
    if (entry.isFile() && (entry.name === 'package.json' || entry.name === 'package.json.template')) {
      packageJsonFiles.push(entryPath);
    }
  }
}

export function verifyPackageTree(packageName, packageRoot, sourcePackageJson, options = {}) {
  const requiredFiles = new Set();
  for (const entry of sourcePackageJson.files ?? []) {
    requiredFiles.add(entry);
  }
  for (const path of exportedPackageFiles(sourcePackageJson)) {
    requiredFiles.add(path);
  }
  for (const path of options.contract?.files ?? []) {
    requiredFiles.add(path);
  }

  for (const entry of requiredFiles) {
    if (!existsSync(join(packageRoot, entry))) {
      throw new Error(`Published tarball for ${packageName} is missing required file: ${entry}`);
    }
  }

  const styleExport = sourcePackageJson.exports?.['./style.css'];
  if (styleExport) {
    const stylePath = typeof styleExport === 'string' ? styleExport : styleExport.import;
    if (!stylePath || !existsSync(join(packageRoot, stripDotSlash(stylePath)))) {
      throw new Error(`Published tarball for ${packageName} is missing exported style.css: ${stylePath || '<unknown>'}.`);
    }
    verifyPublishedStyleContent(packageName, join(packageRoot, stripDotSlash(stylePath)));
  }

  for (const check of options.contract?.contains ?? []) {
    const contentPath = join(packageRoot, check.path);
    if (!existsSync(contentPath)) {
      throw new Error(`Published tarball for ${packageName} is missing contract content file: ${check.path}`);
    }
    const content = readFileSync(contentPath, 'utf8');
    if (!content.includes(check.text)) {
      throw new Error(`Published tarball for ${packageName} ${check.path} does not contain required text: ${check.text}`);
    }
  }
}

export function verifyPublishedPackage(packageName, version, foundPackage, options = {}) {
  const registry = options.registry || HOSTED_REGISTRY;
  const tempDir = mkdtempSync(join(tmpdir(), 'mango-npm-publish-verify-'));
  try {
    console.log(`Verifying published tarball ${packageName}@${version} from ${registry}`);
    run('npm', [
      'pack',
      `${packageName}@${version}`,
      `--registry=${registry}`,
      '--pack-destination',
      tempDir,
    ]);
    const tarball = readdirSync(tempDir).find((file) => file.endsWith('.tgz'));
    if (!tarball) {
      throw new Error(`Published tarball not found for ${packageName}@${version}.`);
    }
    run('tar', ['-xzf', join(tempDir, tarball), '-C', tempDir]);
    const packageRoot = join(tempDir, 'package');
    const publishedPackageJsonPath = join(packageRoot, 'package.json');
    if (!existsSync(publishedPackageJsonPath)) {
      throw new Error(`Published tarball for ${packageName}@${version} does not contain package.json.`);
    }
    const publishedPackageJson = readJson(publishedPackageJsonPath);
    if (publishedPackageJson.name !== packageName || publishedPackageJson.version !== version) {
      throw new Error(
        `Published tarball metadata mismatch: expected ${packageName}@${version}, got ${publishedPackageJson.name}@${publishedPackageJson.version}.`,
      );
    }
    verifyPackageTree(packageName, packageRoot, foundPackage.packageJson, options);
    options.afterExtract?.(packageRoot);
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

export function readReleaseContracts(workspaceRoot = process.cwd()) {
  const contractsPath = join(workspaceRoot, 'release-contracts.json');
  if (!existsSync(contractsPath)) {
    return {};
  }
  return readJson(contractsPath).npm ?? {};
}

export function relativePath(fromRoot, path) {
  return relative(fromRoot, path).split('\\').join('/');
}

function exportedPackageFiles(packageJson) {
  const files = [];
  for (const exportConfig of Object.values(packageJson.exports ?? {})) {
    collectExportFiles(exportConfig, files);
  }
  if (packageJson.main) {
    files.push(stripDotSlash(packageJson.main));
  }
  if (packageJson.module) {
    files.push(stripDotSlash(packageJson.module));
  }
  if (packageJson.types) {
    files.push(stripDotSlash(packageJson.types));
  }
  return files.filter((file) => file && !file.includes('*'));
}

function collectExportFiles(exportConfig, files) {
  if (typeof exportConfig === 'string') {
    files.push(stripDotSlash(exportConfig));
    return;
  }
  if (!exportConfig || typeof exportConfig !== 'object') {
    return;
  }
  for (const value of Object.values(exportConfig)) {
    collectExportFiles(value, files);
  }
}

function verifyPublishedStyleContent(packageName, stylePath) {
  const content = readFileSync(stylePath, 'utf8').trim();
  const hasCssRule = content.includes('{') && content.includes('}');
  const hasCssImport = /^\s*@import\s+['"][^'"]+['"]\s*;/m.test(content);
  if (content.length < 16 || content === 'export {};' || (!hasCssRule && !hasCssImport)) {
    throw new Error(`Published tarball for ${packageName} has invalid exported style.css content.`);
  }
}

function stripDotSlash(path) {
  return path.replace(/^\.\//, '');
}
