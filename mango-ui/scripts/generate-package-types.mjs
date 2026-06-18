#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const uiRoot = resolve(scriptDir, '..');
const defaultPackageRoot = process.cwd();
const packageRoot = resolve(process.argv[2] || defaultPackageRoot);
const packageJsonPath = join(packageRoot, 'package.json');
const generatedDeclarations = new Set();

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
  if (normalized.startsWith('src/')) {
    return join(packageRoot, 'dist', normalized.replace(/^src\//, '').replace(/\.ts$/, '.d.ts').replace(/\.vue$/, '.d.ts'));
  }
  return join(packageRoot, 'dist', normalized.replace(/\.ts$/, '.d.ts').replace(/\.vue$/, '.d.ts'));
}

function resolveLocalSource(fromSource, modulePath) {
  if (!modulePath.startsWith('.')) {
    return undefined;
  }
  const base = resolve(packageRoot, dirname(fromSource), modulePath);
  const candidates = [
    `${base}.ts`,
    `${base}.vue`,
    join(base, 'index.ts'),
    join(base, 'index.vue'),
  ];
  const match = candidates.find((candidate) => existsSync(candidate));
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

function readExportStatement(lines, startIndex) {
  const statement = [];
  let braceDepth = 0;
  let sawBrace = false;
  let sawEquals = false;

  for (let index = startIndex; index < lines.length; index += 1) {
    const line = lines[index];
    statement.push(line);
    for (const char of line) {
      if (char === '{') {
        braceDepth += 1;
        sawBrace = true;
      } else if (char === '}') {
        braceDepth -= 1;
      } else if (char === '=') {
        sawEquals = true;
      }
    }
    const trimmed = line.trim();
    if (
      trimmed.endsWith(';')
      || (!sawEquals && sawBrace && braceDepth <= 0 && trimmed.endsWith('}'))
      || (!sawEquals && !sawBrace && trimmed.endsWith('}'))
    ) {
      break;
    }
  }

  return statement;
}

function extractTypeParams(declaration) {
  const match = declaration.match(/^[A-Za-z0-9_$]+\s*(<[^>{}=;]+>)?/);
  return match?.[1] || '';
}

function generateLooseTypeDeclaration(kind, declaration) {
  const nameMatch = declaration.match(/^([A-Za-z0-9_$]+)/);
  if (!nameMatch) {
    return undefined;
  }
  const name = nameMatch[1];
  const typeParams = extractTypeParams(declaration);
  if (kind === 'interface') {
    return `export interface ${name}${typeParams} {\n  [key: string]: unknown;\n}`;
  }
  return `export type ${name}${typeParams} = unknown;`;
}

function generateTsDeclaration(source, declarationPath) {
  const sourcePath = join(packageRoot, source);
  if (!existsSync(sourcePath)) {
    return 'export {};';
  }
  const content = readFileSync(sourcePath, 'utf8');
  const sourceLines = content.split(/\r?\n/);
  const lines = [];
  const componentExports = [];
  const starExports = [];
  const namedTypeExports = [];
  const namedValueExports = [];
  const functionExports = [];
  const typeBlocks = [];
  const valueExports = [];

  for (let index = 0; index < sourceLines.length; index += 1) {
    const line = sourceLines[index];
    const componentMatch = line.match(/^export\s+\{\s*default\s+as\s+([A-Za-z0-9_$]+)\s*\}\s+from\s+['"](.+\.vue)['"];?$/);
    if (componentMatch) {
      componentExports.push(componentMatch[1]);
      continue;
    }

    const starMatch = line.match(/^export\s+\*\s+from\s+['"](.+)['"];?$/);
    if (starMatch) {
      starExports.push(starMatch[1]);
      continue;
    }

    const namedTypeMatch = line.match(/^export\s+type\s+\{(.+)\}\s+from\s+['"](.+)['"];?$/);
    if (namedTypeMatch) {
      namedTypeExports.push({ names: namedTypeMatch[1].trim(), module: namedTypeMatch[2] });
      continue;
    }

    const namedValueMatch = line.match(/^export\s+\{(.+)\}\s+from\s+['"](.+)['"];?$/);
    if (namedValueMatch) {
      namedValueExports.push({ names: namedValueMatch[1].trim(), module: namedValueMatch[2] });
      continue;
    }

    const functionMatch = line.match(/^export\s+(?:async\s+)?function\s+([A-Za-z0-9_$]+)\s*\(/);
    if (functionMatch) {
      functionExports.push(functionMatch[1]);
      continue;
    }

    const typeDeclarationMatch = line.match(/^export\s+(interface|type)\s+(.+)/);
    if (typeDeclarationMatch) {
      const statement = readExportStatement(sourceLines, index);
      const looseDeclaration = generateLooseTypeDeclaration(typeDeclarationMatch[1], typeDeclarationMatch[2]);
      if (looseDeclaration) {
        typeBlocks.push(looseDeclaration);
      }
      index += statement.length - 1;
      continue;
    }

    const classMatch = line.match(/^export\s+class\s+([A-Za-z0-9_$]+)/);
    if (classMatch) {
      valueExports.push(`export declare const ${classMatch[1]}: unknown;`);
      continue;
    }

    const enumMatch = line.match(/^export\s+enum\s+([A-Za-z0-9_$]+)/);
    if (enumMatch) {
      valueExports.push(`export declare const ${enumMatch[1]}: Record<string, string | number>;`);
      continue;
    }

    const constMatch = line.match(/^export\s+const\s+([A-Za-z0-9_$]+)/);
    if (constMatch) {
      valueExports.push(`export declare const ${constMatch[1]}: unknown;`);
    }
  }

  if (componentExports.length > 0) {
    lines.push("import type { DefineComponent } from 'vue';");
    for (const name of componentExports) {
      lines.push(`export declare const ${name}: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;`);
    }
  }

  for (const block of typeBlocks) {
    lines.push(block);
  }
  for (const valueExport of valueExports) {
    lines.push(valueExport);
  }
  for (const item of namedTypeExports) {
    const localSource = resolveLocalSource(source, item.module);
    if (localSource) {
      generateDeclarationForSource(localSource);
      lines.push(`export type { ${item.names} } from '${declarationImportPath(declarationPath, localSource)}';`);
    } else {
      lines.push(`export type { ${item.names} } from '${item.module}';`);
    }
  }
  for (const item of namedValueExports) {
    const localSource = resolveLocalSource(source, item.module);
    if (localSource) {
      generateDeclarationForSource(localSource);
      lines.push(`export { ${item.names} } from '${declarationImportPath(declarationPath, localSource)}';`);
    } else {
      lines.push(`export { ${item.names} } from '${item.module}';`);
    }
  }
  for (const modulePath of starExports) {
    const localSource = resolveLocalSource(source, modulePath);
    if (localSource) {
      generateDeclarationForSource(localSource);
      lines.push(`export * from '${declarationImportPath(declarationPath, localSource)}';`);
    } else {
      lines.push(`export * from '${modulePath}';`);
    }
  }
  for (const name of functionExports) {
    lines.push(`export declare function ${name}(...args: unknown[]): unknown;`);
  }

  if (lines.length === 0) {
    lines.push('export {};');
  }

  lines.push(`// Source entry: ${source}`);
  return lines.join('\n');
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
    writeFile(join(distDir, 'utils/realtime.d.ts'), "export * from './realtime/index';");
    writeFile(join(distDir, 'utils/realtime.js'), "export * from '../index.js';");
  }

  const componentNames = ['Pagination', 'IconSelector', 'DictTag', 'DictSelect', 'MangoDialog'];
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
