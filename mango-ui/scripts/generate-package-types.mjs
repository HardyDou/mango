#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const uiRoot = resolve(scriptDir, '..');
const defaultPackageRoot = process.cwd();
const packageRoot = resolve(process.argv[2] || defaultPackageRoot);
const packageJsonPath = join(packageRoot, 'package.json');
const generatedDeclarations = new Set();
const declarationCompilerOptions = {
  target: ts.ScriptTarget.ES2020,
  module: ts.ModuleKind.ESNext,
  moduleResolution: ts.ModuleResolutionKind.Bundler,
  strict: true,
  skipLibCheck: true,
};

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function writeFile(path, content) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${content.trimEnd()}\n`);
}

function normalizeEntryName(exportPath) {
  if (exportPath === '.') {
    return 'index';
  }
  return exportPath.replace(/^\.\//, '');
}

function toPosixPath(path) {
  return path.split('\\').join('/');
}

function declarationImportPath(fromDeclarationPath, source) {
  const targetDeclarationPath = declarationPathForSource(source);
  const raw = toPosixPath(relative(dirname(fromDeclarationPath), targetDeclarationPath)).replace(/\.d\.ts$/, '');
  return raw.startsWith('.') ? raw : `./${raw}`;
}

function declarationPathForSource(source) {
  const normalized = toPosixPath(source).replace(/^\.\//, '');
  if (normalized === 'index.ts') {
    return join(packageRoot, 'dist', 'index.d.ts');
  }
  const indexSourceMatch = normalized.match(/^(?:src\/)?(.+)\/index\.(ts|vue)$/);
  if (indexSourceMatch) {
    return join(packageRoot, 'dist', `${indexSourceMatch[1]}.d.ts`);
  }
  if (normalized.startsWith('src/')) {
    return join(packageRoot, 'dist', normalized.replace(/^src\//, '').replace(/\.ts$/, '.d.ts').replace(/\.vue$/, '.d.ts'));
  }
  return join(packageRoot, 'dist', normalized.replace(/\.ts$/, '.d.ts').replace(/\.vue$/, '.d.ts'));
}

function resolveLocalSource(fromSource, modulePath) {
  if (!modulePath.startsWith('.')) {
    return undefined;
  }
  if (modulePath.match(/\.(?:css|scss|sass|less)$/)) {
    return undefined;
  }
  const base = resolve(packageRoot, dirname(fromSource), modulePath);
  const candidates = [
    base,
    `${base}.ts`,
    `${base}.vue`,
    join(base, 'index.ts'),
    join(base, 'index.vue'),
  ];
  const match = candidates.find((candidate) => existsSync(candidate) && statSync(candidate).isFile());
  if (!match) {
    return undefined;
  }
  return toPosixPath(relative(packageRoot, match));
}

function generateVueDeclaration(source) {
  return [
    "import type { DefineComponent } from 'vue';",
    'declare const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;',
    'export default component;',
    `// Source component: ${source}`,
  ].join('\n');
}

function generateTsDeclaration(source, declarationPath) {
  const sourcePath = join(packageRoot, source);
  if (!existsSync(sourcePath)) {
    return 'export {};';
  }
  const content = readFileSync(sourcePath, 'utf8');
  const result = ts.transpileDeclaration(content, {
    fileName: sourcePath,
    compilerOptions: declarationCompilerOptions,
  });
  const output = removeStyleImports(rewriteDeclarationModuleSpecifiers(result.outputText, source, declarationPath));
  return `${output.trimEnd()}\n// Source entry: ${source}`;
}

function removeStyleImports(content) {
  return content.replace(/^\s*import\s+['"][^'"]+\.(?:css|scss|sass|less)['"];\s*$/gm, '');
}

function rewriteDeclarationModuleSpecifiers(content, source, declarationPath) {
  return content.replace(/(['"])(\.[^'"]+)\1/g, (match, quote, modulePath) => {
    const localSource = resolveLocalSource(source, modulePath);
    if (!localSource) {
      return match;
    }
    generateDeclarationForSource(localSource);
    return `${quote}${declarationImportPath(declarationPath, localSource)}${quote}`;
  });
}

function declarationForSource(source, declarationPath = declarationPathForSource(source)) {
  if (source.endsWith('.vue')) {
    return generateVueDeclaration(source);
  }
  if (source.endsWith('.ts')) {
    return generateTsDeclaration(source, declarationPath);
  }
  return 'export {};';
}

function generateDeclarationForSource(source) {
  const declarationPath = declarationPathForSource(source);
  if (generatedDeclarations.has(declarationPath)) {
    return;
  }
  generatedDeclarations.add(declarationPath);
  writeFile(declarationPath, declarationForSource(source));
}

function generatePackageTypes() {
  const packageJson = readJson(packageJsonPath);
  const entries = readViteEntries(packageRoot, packageJson);
  const distDir = join(packageRoot, 'dist');
  mkdirSync(distDir, { recursive: true });

  for (const [entryName, source] of entries) {
    const declarationPath = join(distDir, `${entryName}.d.ts`);
    generatedDeclarations.add(declarationPath);
    writeFile(declarationPath, declarationForSource(source, declarationPath));
  }

  if (packageJson.name === '@mango/api-schema') {
    writeFile(join(distDir, 'index.d.ts'), declarationForSource('src/index.ts'));
    writeFile(join(distDir, 'index.js'), 'export {};');
  }
  if (packageJson.name === '@mango/common') {
    generateCommonSubpathTypes(packageRoot);
  }
}

function generateCommonSubpathTypes(root) {
  const distDir = join(root, 'dist');
  writeFile(join(distDir, 'theme/index.scss.d.ts'), 'declare const stylesheet: string;\nexport default stylesheet;');

  for (const section of ['utils', 'hooks', 'api']) {
    const sectionDir = join(root, section);
    if (!existsSync(sectionDir)) {
      continue;
    }
    for (const file of readdirSync(sectionDir)) {
      if (!file.endsWith('.ts')) {
        continue;
      }
      const name = file.replace(/\.ts$/, '');
      generateDeclarationForSource(`${section}/${name}.ts`);
      writeFile(join(distDir, section, `${name}.js`), `export * from '../index.js';`);
    }
  }

  const realtimeDir = join(root, 'utils/realtime');
  if (existsSync(realtimeDir)) {
    for (const file of readdirSync(realtimeDir)) {
      if (!file.endsWith('.ts')) {
        continue;
      }
      const name = file.replace(/\.ts$/, '');
      generateDeclarationForSource(`utils/realtime/${name}.ts`);
      writeFile(join(distDir, 'utils/realtime', `${name}.js`), "export * from '../../index.js';");
    }
    generateDeclarationForSource('utils/realtime/index.ts');
    writeFile(join(distDir, 'utils/realtime.d.ts'), "export * from './realtime';");
    writeFile(join(distDir, 'utils/realtime.js'), "export * from '../index.js';");
  }

  const componentNames = ['Pagination', 'IconSelector', 'DictTag', 'DictSelect', 'PasswordPolicyHint', 'MangoDialog'];
  for (const name of componentNames) {
    writeFile(
      join(distDir, 'components', name, 'index.d.ts'),
      [
        "import type { DefineComponent } from 'vue';",
        'declare const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;',
        'export default component;',
      ].join('\n'),
    );
    writeFile(join(distDir, 'components', name, 'index.js'), `export { ${name} as default } from '../../index.js';`);
  }
}

function removeDeclarations(dir) {
  if (!existsSync(dir)) {
    return;
  }
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) {
      removeDeclarations(path);
      continue;
    }
    if (path.endsWith('.d.ts')) {
      rmSync(path, { force: true });
    }
  }
}

function readViteEntries(root, packageJson) {
  const viteConfigPath = join(root, 'vite.config.ts');
  if (!existsSync(viteConfigPath)) {
    return readPackageEntries(packageJson);
  }
  const content = readFileSync(viteConfigPath, 'utf8');
  const objectMatch = content.match(/entry:\s*\{([\s\S]*?)\}\s*,\s*formats:/m);
  if (objectMatch) {
    const entries = [];
    for (const line of objectMatch[1].split(/\r?\n/)) {
      const match = line.match(/^\s*['"]?([^'":]+)['"]?\s*:\s*['"]([^'"]+)['"]/);
      if (match) {
        entries.push([match[1], match[2]]);
      }
    }
    if (entries.length > 0) {
      return entries;
    }
  }
  const stringMatch = content.match(/entry:\s*['"]([^'"]+)['"]/);
  if (stringMatch) {
    return [['index', stringMatch[1]]];
  }
  return readPackageEntries(packageJson);
}

function readPackageEntries(packageJson) {
  if (packageJson.name === '@mango/api-schema') {
    return [['index', 'src/index.ts']];
  }
  const exportsConfig = packageJson.exports || {};
  const entries = [];
  for (const [exportPath, exportConfig] of Object.entries(exportsConfig)) {
    if (exportPath.endsWith('.css') || exportPath.includes('*')) {
      continue;
    }
    const source = typeof exportConfig === 'string' ? exportConfig : exportConfig?.import || exportConfig?.types;
    if (source?.startsWith('./src/') || source === './src/index.ts') {
      entries.push([normalizeEntryName(exportPath), source.replace(/^\.\//, '')]);
    }
  }
  if (entries.length > 0) {
    return entries;
  }
  return [['index', 'src/index.ts']];
}

if (!existsSync(packageJsonPath)) {
  throw new Error(`package.json not found: ${packageJsonPath}`);
}

removeDeclarations(join(packageRoot, 'dist'));
generatePackageTypes();
