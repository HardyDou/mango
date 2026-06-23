#!/usr/bin/env node
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const uiRoot = resolve(scriptDir, '..');
const packagesRoot = join(uiRoot, 'packages');
const failures = [];

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function addFailure(packageName, message) {
  failures.push(`${packageName}: ${message}`);
}

function assertPath(packageName, packageRoot, field, value) {
  if (!value) {
    return;
  }
  if (
    value.includes('/src/')
    || value.startsWith('./src')
    || (value.endsWith('.ts') && !value.endsWith('.d.ts'))
    || value.endsWith('.vue')
  ) {
    addFailure(packageName, `${field} must point to published dist output, got ${value}`);
    return;
  }
  const target = join(packageRoot, value.replace(/^\.\//, ''));
  if (!existsSync(target)) {
    addFailure(packageName, `${field} points to missing file ${value}`);
  }
}

function assertPublishedFiles(packageName, packageJson) {
  const files = packageJson.files ?? [];
  const sourceEntries = ['src', 'api', 'components', 'hooks', 'types', 'utils', 'views', 'index.ts'];
  for (const entry of files) {
    if (sourceEntries.includes(entry) || entry.startsWith('src/') || entry.endsWith('.ts') || entry.endsWith('.vue')) {
      addFailure(packageName, `files must not publish source entry ${entry}`);
    }
  }
  if (!files.includes('dist')) {
    addFailure(packageName, 'files must include dist');
  }
}

function findDeclarationFiles(dir) {
  if (!existsSync(dir)) {
    return [];
  }
  const files = [];
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) {
      files.push(...findDeclarationFiles(path));
      continue;
    }
    if (path.endsWith('.d.ts')) {
      files.push(path);
    }
  }
  return files;
}

function resolveDeclarationImport(fromFile, modulePath) {
  const base = resolve(dirname(fromFile), modulePath);
  const candidates = [
    `${base}.d.ts`,
    join(base, 'index.d.ts'),
  ];
  return candidates.find((candidate) => existsSync(candidate));
}

function assertDeclaration(packageName, packageRoot, declarationPath) {
  const content = readFileSync(declarationPath, 'utf8');
  const imports = content.matchAll(/\b(?:from|import)\s+['"]([^'"]+)['"]/g);

  for (const match of imports) {
    const modulePath = match[1];
    if (modulePath.includes('/src/') || modulePath.startsWith('src/') || modulePath.startsWith('../src')) {
      addFailure(packageName, `${declarationPath} must not reference source module ${modulePath}`);
      continue;
    }
    if (!modulePath.startsWith('.')) {
      continue;
    }
    const resolved = resolveDeclarationImport(declarationPath, modulePath);
    if (!resolved) {
      addFailure(packageName, `${declarationPath} references missing declaration ${modulePath}`);
      continue;
    }
    const relativeToPackage = resolved.slice(packageRoot.length + 1).split('\\').join('/');
    if (!relativeToPackage.startsWith('dist/')) {
      addFailure(packageName, `${declarationPath} references declaration outside dist: ${modulePath}`);
    }
  }
}

for (const packageDir of readdirSync(packagesRoot)) {
  const packageRoot = join(packagesRoot, packageDir);
  const packageJsonPath = join(packageRoot, 'package.json');
  if (!existsSync(packageJsonPath)) {
    continue;
  }
  const packageJson = readJson(packageJsonPath);
  if (!packageJson.name?.startsWith('@mango/') || packageJson.name === '@mango/cli') {
    continue;
  }
  if (packageJson.private) {
    continue;
  }
  assertPublishedFiles(packageJson.name, packageJson);
  assertPath(packageJson.name, packageRoot, 'main', packageJson.main);
  assertPath(packageJson.name, packageRoot, 'module', packageJson.module);
  assertPath(packageJson.name, packageRoot, 'types', packageJson.types);

  if (!packageJson.exports) {
    addFailure(packageJson.name, 'missing exports');
    continue;
  }
  for (const [exportPath, exportConfig] of Object.entries(packageJson.exports)) {
    if (exportPath.includes('*')) {
      continue;
    }
    if (exportPath.endsWith('.css') || exportPath.endsWith('.scss')) {
      assertPath(
        packageJson.name,
        packageRoot,
        `exports.${exportPath}`,
        typeof exportConfig === 'string' ? exportConfig : exportConfig.default || exportConfig.import,
      );
      continue;
    }
    if (typeof exportConfig === 'string') {
      assertPath(packageJson.name, packageRoot, `exports.${exportPath}`, exportConfig);
      continue;
    }
    assertPath(packageJson.name, packageRoot, `exports.${exportPath}.types`, exportConfig.types);
    assertPath(packageJson.name, packageRoot, `exports.${exportPath}.import`, exportConfig.import);
  }

  for (const declarationPath of findDeclarationFiles(join(packageRoot, 'dist'))) {
    assertDeclaration(packageJson.name, packageRoot, declarationPath);
  }
}

if (failures.length > 0) {
  console.error('Package export check failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log('Package export check passed.');
