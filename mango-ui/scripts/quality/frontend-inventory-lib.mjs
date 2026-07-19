import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const SOURCE_EXTENSIONS = ['.ts', '.tsx', '.js', '.mjs', '.cjs', '.vue'];
const COMPONENT_EXTENSIONS = new Set(['.vue', '.tsx']);
const SKIPPED_DIRECTORIES = new Set(['node_modules', 'dist', 'coverage', '.git', '.runtime']);

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function relative(root, value) {
  return toPosix(path.relative(root, value));
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function listFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  const results = [];
  const visit = (current) => {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.isDirectory() && SKIPPED_DIRECTORIES.has(entry.name)) continue;
      const resolved = path.join(current, entry.name);
      if (entry.isDirectory()) visit(resolved);
      else if (entry.isFile()) results.push(resolved);
    }
  };
  visit(directory);
  return results.sort((left, right) => left.localeCompare(right));
}

function discoverWorkspaces(uiRoot) {
  const workspaces = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const directory = path.join(parent, entry.name);
      const manifestFile = path.join(directory, 'package.json');
      if (!fs.existsSync(manifestFile)) continue;
      const manifest = readJson(manifestFile);
      workspaces.push({
        kind: kind === 'apps' ? 'app' : 'package',
        directory,
        manifestFile,
        manifest,
      });
    }
  }
  return workspaces.sort((left, right) => left.manifest.name.localeCompare(right.manifest.name));
}

function resolveSource(fromFile, specifier) {
  if (!specifier.startsWith('.')) return null;
  const base = path.resolve(path.dirname(fromFile), specifier);
  const candidates = [base];
  for (const extension of SOURCE_EXTENSIONS) candidates.push(`${base}${extension}`);
  for (const extension of SOURCE_EXTENSIONS) candidates.push(path.join(base, `index${extension}`));
  return candidates.find((candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile()) || null;
}

function parseBindingList(value) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => item.replace(/^type\s+/u, '').trim())
    .map((item) => {
      const [imported, exported] = item.split(/\s+as\s+/u).map((part) => part.trim());
      return { imported, exported: exported || imported };
    });
}

function parseModule(file) {
  const content = fs.readFileSync(file, 'utf8');
  const reexports = [];
  const imports = new Map();
  const localExports = [];

  for (const match of content.matchAll(/export\s+(?:type\s+)?\{([\s\S]*?)\}\s+from\s+['"]([^'"]+)['"]/gu)) {
    reexports.push({ specifier: match[2], bindings: parseBindingList(match[1]), exportAll: false });
  }
  for (const match of content.matchAll(/export\s+\*\s+from\s+['"]([^'"]+)['"]/gu)) {
    reexports.push({ specifier: match[1], bindings: [], exportAll: true });
  }
  for (const match of content.matchAll(/import\s+([A-Za-z_$][\w$]*)\s+from\s+['"]([^'"]+)['"]/gu)) {
    imports.set(match[1], { imported: 'default', specifier: match[2] });
  }
  for (const match of content.matchAll(/import\s+\{([\s\S]*?)\}\s+from\s+['"]([^'"]+)['"]/gu)) {
    for (const binding of parseBindingList(match[1])) {
      imports.set(binding.exported, { imported: binding.imported, specifier: match[2] });
    }
  }
  const withoutFromExports = content.replace(/export\s+(?:type\s+)?\{[\s\S]*?\}\s+from\s+['"][^'"]+['"]/gu, '');
  for (const match of withoutFromExports.matchAll(/export\s+\{([\s\S]*?)\}/gu)) {
    localExports.push(...parseBindingList(match[1]));
  }

  const dynamicImports = [...content.matchAll(/import\(\s*['"]([^'"]+)['"]\s*\)/gu)].map((match) => match[1]);
  const globPatterns = [...content.matchAll(/import\.meta\.glob(?:Eager)?\(\s*(['"`])([^'"`]+)\1/gu)].map(
    (match) => match[2],
  );
  const registrarNames = [
    ...content.matchAll(/export\s+(?:async\s+)?function\s+(register[A-Za-z0-9_$]*Pages)\s*\(/gu),
  ].map((match) => match[1]);
  const widgetTypes = [...content.matchAll(/\btype\s*:\s*['"]([^'"]+)['"]/gu)].map((match) => match[1]);

  return { content, reexports, imports, localExports, dynamicImports, globPatterns, registrarNames, widgetTypes };
}

function collectVueExports(entryFile, exportKey) {
  const visited = new Set();
  const results = [];

  const visit = (file, inheritedName = null) => {
    const identity = `${file}\0${inheritedName || '*'}`;
    if (visited.has(identity)) return;
    visited.add(identity);
    if (COMPONENT_EXTENSIONS.has(path.extname(file))) {
      results.push({ file, exportName: inheritedName || 'default', exportKey });
      return;
    }

    const parsed = parseModule(file);
    for (const item of parsed.reexports) {
      const target = resolveSource(file, item.specifier);
      if (!target) continue;
      if (item.exportAll) {
        visit(target, inheritedName);
        continue;
      }
      for (const binding of item.bindings) {
        if (binding.imported === 'default' && COMPONENT_EXTENSIONS.has(path.extname(target))) {
          results.push({ file: target, exportName: inheritedName || binding.exported, exportKey });
        } else {
          visit(target, inheritedName || binding.exported);
        }
      }
    }
    for (const binding of parsed.localExports) {
      const imported = parsed.imports.get(binding.imported);
      if (!imported) continue;
      const target = resolveSource(file, imported.specifier);
      if (!target) continue;
      visit(target, inheritedName || binding.exported);
    }
  };

  visit(entryFile);
  return results;
}

function extractViteEntries(workspace) {
  const configFile = ['vite.config.ts', 'vite.config.js', 'vite.config.mjs']
    .map((name) => path.join(workspace.directory, name))
    .find(fs.existsSync);
  if (!configFile) return new Map();
  const content = fs.readFileSync(configFile, 'utf8');
  const entryStart = content.search(/\bentry\s*:/u);
  if (entryStart < 0) return new Map();
  const entryTail = content.slice(entryStart);
  const entryEnd = entryTail.search(/\bformats\s*:/u);
  const section = entryEnd >= 0 ? entryTail.slice(0, entryEnd) : entryTail.slice(0, 2000);
  const entries = new Map();
  for (const match of section.matchAll(/(?:['"]([^'"]+)['"]|([A-Za-z0-9_-]+))\s*:\s*['"]([^'"]+)['"]/gu)) {
    entries.set(match[1] || match[2], path.resolve(workspace.directory, match[3]));
  }
  const scalar = section.match(/entry\s*:\s*['"]([^'"]+)['"]/u);
  if (scalar) entries.set('index', path.resolve(workspace.directory, scalar[1]));
  return entries;
}

function targetImport(value) {
  if (typeof value === 'string') return value;
  if (!value || typeof value !== 'object') return null;
  return value.import || value.default || null;
}

function inferEntry(workspace, exportKey, exportValue, viteEntries) {
  const importTarget = targetImport(exportValue);
  if (!importTarget || /\.(?:css|scss|sass|less)$/u.test(importTarget)) return null;
  if (exportKey.endsWith('.vue')) {
    const direct = path.resolve(workspace.directory, exportKey.replace(/^\.\//u, ''));
    if (fs.existsSync(direct)) return direct;
  }
  const outputKey = importTarget.replace(/^\.\/dist\//u, '').replace(/\.(?:m?js|cjs)$/u, '');
  if (viteEntries.has(outputKey)) return viteEntries.get(outputKey);
  if (exportKey === '.' && viteEntries.has('index')) return viteEntries.get('index');
  const logical = exportKey === '.' ? 'index' : exportKey.replace(/^\.\//u, '');
  const candidates = [
    path.join(workspace.directory, `${logical}.ts`),
    path.join(workspace.directory, 'src', `${logical}.ts`),
    path.join(workspace.directory, logical, 'index.ts'),
    path.join(workspace.directory, 'src', logical, 'index.ts'),
  ];
  return candidates.find(fs.existsSync) || null;
}

function classifyComponent(file, workspaceDirectory) {
  const value = relative(workspaceDirectory, file).toLowerCase();
  if (/(^|\/)widgets?\//u.test(value)) return 'widget';
  if (/(^|\/)(views?|pages?)\//u.test(value)) return 'page';
  if (/(^|\/)layouts?\//u.test(value) || /(^|\/)layout\//u.test(value)) return 'layout';
  if (/(^|\/)components?\//u.test(value)) return 'component';
  if (/(^|\/)(main|app)\.(?:vue|tsx)$/u.test(value)) return 'runtime-entry';
  return 'component-candidate';
}

function testKind(file) {
  const value = toPosix(file);
  if (/\/e2e\/specs\//u.test(value) || /playwright/u.test(path.basename(value))) return 'e2e';
  if (/\.(?:spec|test)\.(?:[cm]?[jt]sx?)$/u.test(value)) return 'unit-or-component';
  return null;
}

function dependencyEntries(manifest) {
  const groups = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
  return groups.flatMap((group) =>
    Object.entries(manifest[group] || {}).map(([name, version]) => ({ group, name, version })),
  );
}

export function createFrontendInventory(uiRoot) {
  const resolvedRoot = path.resolve(uiRoot);
  const workspaces = discoverWorkspaces(resolvedRoot);
  if (workspaces.length === 0) throw new Error(`No workspaces found under ${resolvedRoot}`);
  const workspaceNames = new Set(workspaces.map((item) => item.manifest.name));
  const components = [];
  const publicVueExports = [];
  const registrars = [];
  const widgets = [];
  const globImports = [];
  const dynamicImports = [];
  const tests = [];
  const sourceFiles = [];
  const unresolvedCodeExportEntries = [];

  for (const workspace of workspaces) {
    const files = listFiles(workspace.directory);
    const source = files.filter((file) => SOURCE_EXTENSIONS.includes(path.extname(file)));
    for (const file of source) {
      const content = fs.readFileSync(file);
      sourceFiles.push({ file: relative(resolvedRoot, file), sha256: sha256(content), bytes: content.length });
      const kind = testKind(file);
      if (kind) tests.push({ workspace: workspace.manifest.name, file: relative(resolvedRoot, file), kind });
      const parsed = parseModule(file);
      for (const pattern of parsed.globPatterns)
        globImports.push({ workspace: workspace.manifest.name, file: relative(resolvedRoot, file), pattern });
      for (const specifier of parsed.dynamicImports)
        dynamicImports.push({ workspace: workspace.manifest.name, file: relative(resolvedRoot, file), specifier });
      for (const name of parsed.registrarNames)
        registrars.push({
          workspace: workspace.manifest.name,
          source: 'source',
          name,
          file: relative(resolvedRoot, file),
        });
      const isWidgetMetadata =
        /(^|\/)widgets?\//u.test(relative(workspace.directory, file)) ||
        /\bMango(?:Grid)?WidgetDefinition\b/u.test(parsed.content);
      if (isWidgetMetadata) {
        for (const type of parsed.widgetTypes)
          widgets.push({
            workspace: workspace.manifest.name,
            source: 'source-metadata',
            type,
            file: relative(resolvedRoot, file),
          });
      }
      if (COMPONENT_EXTENSIONS.has(path.extname(file))) {
        components.push({
          workspace: workspace.manifest.name,
          file: relative(resolvedRoot, file),
          kind: classifyComponent(file, workspace.directory),
          exportedBy: [],
        });
      }
    }

    for (const registrar of workspace.manifest.mangoAdmin?.registrars || []) {
      registrars.push({ workspace: workspace.manifest.name, source: 'manifest', ...registrar });
    }
    const viteEntries = extractViteEntries(workspace);
    for (const [exportKey, exportValue] of Object.entries(workspace.manifest.exports || {})) {
      if (exportKey.startsWith('./widgets/')) {
        widgets.push({ workspace: workspace.manifest.name, source: 'package-export', exportKey });
      }
      const entry = inferEntry(workspace, exportKey, exportValue, viteEntries);
      const importTarget = targetImport(exportValue);
      if (!entry) {
        if (importTarget && !exportKey.includes('*') && /\.(?:m?js|cjs)$/u.test(importTarget)) {
          unresolvedCodeExportEntries.push({ workspace: workspace.manifest.name, exportKey, importTarget });
        }
        continue;
      }
      for (const item of collectVueExports(entry, exportKey)) {
        publicVueExports.push({
          workspace: workspace.manifest.name,
          exportKey,
          exportName: item.exportName,
          entry: relative(resolvedRoot, entry),
          file: relative(resolvedRoot, item.file),
        });
      }
    }
  }

  const componentByFile = new Map(components.map((item) => [item.file, item]));
  const unresolvedPublicVueExports = [];
  for (const item of publicVueExports) {
    const component = componentByFile.get(item.file);
    if (!component) unresolvedPublicVueExports.push(item);
    else
      component.exportedBy.push({ workspace: item.workspace, exportKey: item.exportKey, exportName: item.exportName });
  }

  const workspaceReport = workspaces.map((workspace) => {
    const dependencies = dependencyEntries(workspace.manifest);
    return {
      name: workspace.manifest.name,
      version: workspace.manifest.version || null,
      kind: workspace.kind,
      private: workspace.manifest.private === true,
      directory: relative(resolvedRoot, workspace.directory),
      scripts: Object.fromEntries(Object.entries(workspace.manifest.scripts || {}).sort()),
      dependencies,
      localDependencies: dependencies.filter((item) => workspaceNames.has(item.name)),
      exports: Object.keys(workspace.manifest.exports || {}).sort(),
    };
  });

  const sortByIdentity = (list) =>
    list.sort((left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right)));
  for (const component of components) sortByIdentity(component.exportedBy);
  const report = {
    schemaVersion: 1,
    root: '.',
    summary: {
      workspaceCount: workspaceReport.length,
      appCount: workspaceReport.filter((item) => item.kind === 'app').length,
      packageCount: workspaceReport.filter((item) => item.kind === 'package').length,
      sourceFileCount: sourceFiles.length,
      componentCandidateCount: components.length,
      publicVueExportCount: publicVueExports.length,
      publicVueExportCoverage:
        publicVueExports.length === 0
          ? 0
          : (publicVueExports.length - unresolvedPublicVueExports.length) / publicVueExports.length,
      registrarCount: registrars.length,
      widgetMetadataCount: widgets.length,
      testFileCount: tests.length,
      globImportCount: globImports.length,
      dynamicImportCount: dynamicImports.length,
    },
    workspaces: workspaceReport,
    sourceFiles: sortByIdentity(sourceFiles),
    components: sortByIdentity(components),
    publicVueExports: sortByIdentity(publicVueExports),
    unresolvedPublicVueExports: sortByIdentity(unresolvedPublicVueExports),
    unresolvedCodeExportEntries: sortByIdentity(unresolvedCodeExportEntries),
    registrars: sortByIdentity(registrars),
    widgets: sortByIdentity(widgets),
    globImports: sortByIdentity(globImports),
    dynamicImports: sortByIdentity(dynamicImports),
    tests: sortByIdentity(tests),
  };
  report.inventorySha256 = sha256(JSON.stringify(report));

  if (report.summary.sourceFileCount === 0) throw new Error('Frontend inventory source file count is zero');
  if (report.summary.componentCandidateCount === 0) throw new Error('Frontend component candidate count is zero');
  if (unresolvedCodeExportEntries.length > 0) {
    const identities = unresolvedCodeExportEntries.map((item) => `${item.workspace}:${item.exportKey}`).join(', ');
    throw new Error(
      `Frontend code export source entry coverage is incomplete: ${unresolvedCodeExportEntries.length} unresolved (${identities})`,
    );
  }
  if (report.summary.publicVueExportCount === 0) throw new Error('Frontend public Vue export count is zero');
  if (unresolvedPublicVueExports.length > 0) {
    throw new Error(
      `Frontend public Vue export coverage is incomplete: ${unresolvedPublicVueExports.length} unresolved`,
    );
  }
  return report;
}

export function writeFrontendInventory(report, outputFile) {
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);
}
