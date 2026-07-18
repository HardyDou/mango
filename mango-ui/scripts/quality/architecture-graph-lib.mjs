import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import ts from 'typescript';

const LAYERS = new Set(['FE0', 'FE1', 'FE2', 'FE3', 'FE4', 'TOOLING']);
const ROLES = new Set(['contract', 'foundation', 'domain', 'composition', 'app', 'tooling']);
const ROLE_LAYERS = new Map([
  ['contract', 'FE0'],
  ['foundation', 'FE1'],
  ['domain', 'FE2'],
  ['composition', 'FE3'],
  ['app', 'FE4'],
  ['tooling', 'TOOLING'],
]);
const LAYER_ORDER = new Map([
  ['FE0', 0],
  ['FE1', 1],
  ['FE2', 2],
  ['FE3', 3],
  ['FE4', 4],
]);
const ALLOWED_RUNTIME_TARGET_LAYERS = new Map([
  ['FE0', new Set(['FE0'])],
  ['FE1', new Set(['FE0', 'FE1'])],
  ['FE2', new Set(['FE0', 'FE1', 'FE2'])],
  ['FE3', new Set(['FE0', 'FE1', 'FE2', 'FE3'])],
  ['FE4', new Set(['FE0', 'FE1', 'FE2', 'FE3', 'FE4'])],
]);
const SOURCE_EXTENSIONS = new Set(['.js', '.mjs', '.cjs', '.ts', '.tsx', '.vue', '.css', '.scss', '.sass']);
const SKIPPED_DIRECTORIES = new Set(['node_modules', 'dist', 'coverage', '.git', '.runtime']);
const DEPENDENCY_GROUPS = ['dependencies', 'optionalDependencies', 'peerDependencies'];
const ALL_DEPENDENCY_GROUPS = [...DEPENDENCY_GROUPS, 'devDependencies'];
const LEGACY_SCC_GRAPH_KINDS = new Set(['manifest', 'source-runtime', 'contract', 'combined']);

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.keys(value)
      .sort()
      .map((key) => [key, canonical(value[key])]),
  );
}

function stableHash(value) {
  return createHash('sha256')
    .update(JSON.stringify(canonical(value)))
    .digest('hex');
}

function listFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  const files = [];
  const visit = (current) => {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.isDirectory() && SKIPPED_DIRECTORIES.has(entry.name)) continue;
      const resolved = path.join(current, entry.name);
      if (entry.isDirectory()) visit(resolved);
      else if (entry.isFile()) files.push(resolved);
    }
  };
  visit(directory);
  return files.sort();
}

function listRuntimeSourceFiles(workspace) {
  const metadata = workspace.manifest.mangoArchitecture;
  const candidates =
    metadata?.architectureLayer === 'TOOLING'
      ? ['src', 'scripts'].map((directory) => path.join(workspace.root, directory))
      : workspace.kind === 'app'
        ? [path.join(workspace.root, 'src')]
        : fs.existsSync(path.join(workspace.root, 'src'))
          ? [path.join(workspace.root, 'src')]
          : ['api', 'components', 'hooks', 'utils'].map((directory) => path.join(workspace.root, directory));
  const files = candidates.flatMap(listFiles);
  for (const config of [
    ...Object.values(metadata?.sourceExports || {}),
    ...Object.values(metadata?.nonCodeExports || {}),
  ]) {
    const source = config.source || config.sourcePattern;
    if (!source) continue;
    if (source.includes('*')) {
      files.push(...matchingFiles(workspace.root, source).map((file) => path.resolve(workspace.root, file)));
      continue;
    }
    const resolved = path.resolve(workspace.root, source);
    if (fs.existsSync(resolved) && fs.statSync(resolved).isFile()) files.push(resolved);
  }
  const rootEntry = path.join(workspace.root, 'index.ts');
  if (workspace.kind === 'package' && fs.existsSync(rootEntry)) files.push(rootEntry);
  const runtimeFiles = [...new Set(files)]
    .filter((file) => SOURCE_EXTENSIONS.has(path.extname(file)))
    .filter((file) => !toPosix(file).includes('/__tests__/'))
    .sort();
  const seen = new Set(runtimeFiles);
  for (let index = 0; index < runtimeFiles.length; index += 1) {
    const file = runtimeFiles[index];
    if (!['.css', '.scss', '.sass'].includes(path.extname(file))) continue;
    const content = fs.readFileSync(file, 'utf8');
    for (const match of content.matchAll(/@import\s+(?:url\(\s*)?['"]([^'"]+)['"]/gu)) {
      const importPath = match[1].split(/[?#]/u, 1)[0];
      if (!importPath.startsWith('.')) continue;
      const resolved = path.resolve(path.dirname(file), importPath);
      if (
        insideRoot(workspace.root, resolved) &&
        fs.existsSync(resolved) &&
        fs.statSync(resolved).isFile() &&
        ['.css', '.scss', '.sass'].includes(path.extname(resolved)) &&
        !seen.has(resolved)
      ) {
        seen.add(resolved);
        runtimeFiles.push(resolved);
      }
    }
  }
  return runtimeFiles.sort();
}

function discoverWorkspaces(uiRoot) {
  const workspaces = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const root = path.join(parent, entry.name);
      const manifestFile = path.join(root, 'package.json');
      if (!fs.existsSync(manifestFile)) continue;
      workspaces.push({
        kind: kind === 'apps' ? 'app' : 'package',
        root,
        manifestFile,
        manifest: readJson(manifestFile),
      });
    }
  }
  return workspaces.sort((left, right) => left.manifest.name.localeCompare(right.manifest.name));
}

function findMissingWorkspaceManifests(uiRoot) {
  const missing = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (entry.isDirectory() && !fs.existsSync(path.join(parent, entry.name, 'package.json'))) {
        missing.push(`${kind}/${entry.name}`);
      }
    }
  }
  return missing.sort();
}

function targetImport(value) {
  if (typeof value === 'string') return value;
  return value?.import || value?.default || null;
}

function wildcardToRegExp(pattern) {
  const escaped = pattern.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`^${escaped.replaceAll('*', '.+')}$`);
}

function matchingFiles(root, pattern) {
  const relativePattern = pattern.replace(/^\.\//u, '');
  const matcher = wildcardToRegExp(relativePattern);
  const wildcardIndex = relativePattern.indexOf('*');
  const prefix = wildcardIndex === -1 ? relativePattern : relativePattern.slice(0, wildcardIndex);
  const resolvedPrefix = path.resolve(root, prefix || '.');
  const searchRoot =
    fs.existsSync(resolvedPrefix) && fs.statSync(resolvedPrefix).isDirectory()
      ? resolvedPrefix
      : path.dirname(resolvedPrefix);
  return listFiles(searchRoot)
    .map((file) => toPosix(path.relative(root, file)))
    .filter((file) => matcher.test(file));
}

function insideRoot(root, target) {
  const relative = path.relative(root, target);
  return relative === '' || (!relative.startsWith(`..${path.sep}`) && relative !== '..' && !path.isAbsolute(relative));
}

function exportedSubpath(manifest, exportKey) {
  const keys = Object.keys(manifest.exports || {});
  if (keys.includes(exportKey)) return true;
  return keys.some((key) => key.includes('*') && wildcardToRegExp(key).test(exportKey));
}

function parseInternalSpecifier(specifier, workspaceNames) {
  if (workspaceNames.has(specifier)) return { packageName: specifier, exportKey: '.' };
  const candidates = [...workspaceNames]
    .filter((name) => specifier.startsWith(`${name}/`))
    .sort((left, right) => right.length - left.length);
  if (candidates.length === 0) return null;
  const packageName = candidates[0];
  return { packageName, exportKey: `./${specifier.slice(packageName.length + 1)}` };
}

function sourceImports(content, file, workspaceName, errors) {
  const imports = [];
  const seen = new Set();
  const add = (kind, specifier) => {
    const identity = `${kind}\0${specifier}`;
    if (!seen.has(identity)) imports.push({ kind, specifier });
    seen.add(identity);
  };
  if (['.css', '.scss', '.sass'].includes(path.extname(file))) {
    for (const match of content.matchAll(/@import\s+(?:url\()?['"]([^'"]+)['"]/gu)) add('runtime', match[1]);
    return imports;
  }
  const script =
    path.extname(file) === '.vue'
      ? [...content.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/giu)].map((match) => match[1]).join('\n')
      : content;
  const sourceFile = ts.createSourceFile(file, script, ts.ScriptTarget.Latest, false, ts.ScriptKind.TSX);
  if (sourceFile.parseDiagnostics.length > 0) {
    errors.push(
      `source:${workspaceName}: AST parse failed for ${toPosix(file)} (${sourceFile.parseDiagnostics[0].messageText})`,
    );
    return imports;
  }
  const visit = (node) => {
    if (
      (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) &&
      node.moduleSpecifier &&
      ts.isStringLiteral(node.moduleSpecifier)
    ) {
      const typeOnly = ts.isImportDeclaration(node) ? node.importClause?.isTypeOnly === true : node.isTypeOnly === true;
      add(typeOnly ? 'contract' : 'runtime', node.moduleSpecifier.text);
    } else if (ts.isCallExpression(node)) {
      const isDynamicImport = node.expression.kind === ts.SyntaxKind.ImportKeyword;
      const isRequire = ts.isIdentifier(node.expression) && node.expression.text === 'require';
      if (isDynamicImport || isRequire) {
        if (node.arguments.length === 1 && ts.isStringLiteral(node.arguments[0]))
          add('runtime', node.arguments[0].text);
        else
          errors.push(
            `source:${workspaceName}: non-literal ${isDynamicImport ? 'import' : 'require'} is not analyzable in ${toPosix(file)}`,
          );
      }
    }
    ts.forEachChild(node, visit);
  };
  visit(sourceFile);
  return imports;
}

function edgeIdentity(edge) {
  return `${edge.from}->${edge.to}:${edge.kind}`;
}

function uniqueEdges(edges) {
  const byIdentity = new Map();
  for (const edge of edges) byIdentity.set(edgeIdentity(edge), edge);
  return [...byIdentity.values()].sort((left, right) => edgeIdentity(left).localeCompare(edgeIdentity(right)));
}

function stronglyConnectedComponents(nodes, edges) {
  const adjacency = new Map([...nodes].map((node) => [node, []]));
  for (const edge of edges) adjacency.get(edge.from)?.push(edge.to);
  for (const targets of adjacency.values()) targets.sort();
  let index = 0;
  const indices = new Map();
  const lowLinks = new Map();
  const stack = [];
  const onStack = new Set();
  const components = [];

  const visit = (node) => {
    indices.set(node, index);
    lowLinks.set(node, index);
    index += 1;
    stack.push(node);
    onStack.add(node);
    for (const target of adjacency.get(node) || []) {
      if (!indices.has(target)) {
        visit(target);
        lowLinks.set(node, Math.min(lowLinks.get(node), lowLinks.get(target)));
      } else if (onStack.has(target)) {
        lowLinks.set(node, Math.min(lowLinks.get(node), indices.get(target)));
      }
    }
    if (lowLinks.get(node) !== indices.get(node)) return;
    const component = [];
    let current;
    do {
      current = stack.pop();
      onStack.delete(current);
      component.push(current);
    } while (current !== node);
    if (component.length > 1 || (adjacency.get(node) || []).includes(node)) components.push(component.sort());
  };

  for (const node of [...nodes].sort()) if (!indices.has(node)) visit(node);
  return components.sort((left, right) => left.join('\0').localeCompare(right.join('\0')));
}

function createSccs(graphKind, nodes, edges) {
  return stronglyConnectedComponents(nodes, edges).map((members) => {
    const memberSet = new Set(members);
    const componentEdges = edges
      .filter((edge) => memberSet.has(edge.from) && memberSet.has(edge.to))
      .map((edge) => ({ from: edge.from, to: edge.to, kind: edge.kind }))
      .sort((left, right) => edgeIdentity(left).localeCompare(edgeIdentity(right)));
    const value = { graphKind, members, edges: componentEdges };
    return { ...value, hash: stableHash(value) };
  });
}

function exceptionMatches(exceptions, from, to) {
  return exceptions.some((exception) => exception.from === from && exception.to === to);
}

function validateExpiryDate(expiresAt, identity, subject, errors) {
  const validDate = /^\d{4}-\d{2}-\d{2}$/u.test(expiresAt);
  const parsedDate = validDate ? new Date(`${expiresAt}T00:00:00.000Z`) : null;
  if (!validDate || Number.isNaN(parsedDate.getTime()) || parsedDate.toISOString().slice(0, 10) !== expiresAt) {
    errors.push(`${identity}: expiresAt must be a valid YYYY-MM-DD date`);
    return;
  }
  const today = new Date().toISOString().slice(0, 10);
  if (expiresAt < today) errors.push(`${identity}: ${subject} expired on ${expiresAt}`);
}

function validateArchitectureMetadata(workspace, errors) {
  const { manifest, root, kind } = workspace;
  const metadata = manifest.mangoArchitecture;
  if (!metadata || typeof metadata !== 'object') {
    errors.push(`metadata:${manifest.name}: missing package.json#mangoArchitecture`);
    return;
  }
  if (!LAYERS.has(metadata.architectureLayer))
    errors.push(`metadata:${manifest.name}: unknown architectureLayer ${metadata.architectureLayer}`);
  if (!ROLES.has(metadata.role)) errors.push(`metadata:${manifest.name}: unknown role ${metadata.role}`);
  if (ROLE_LAYERS.get(metadata.role) !== metadata.architectureLayer) {
    errors.push(`metadata:${manifest.name}: role ${metadata.role} does not match ${metadata.architectureLayer}`);
  }
  if (!/^[a-z][a-z0-9-]*$/u.test(metadata.domain || '')) errors.push(`metadata:${manifest.name}: invalid domain`);
  if (!metadata.ownerRole) errors.push(`metadata:${manifest.name}: ownerRole is required`);
  if (kind === 'app' && manifest.private !== true)
    errors.push(`metadata:${manifest.name}: apps must declare private=true`);
  for (const key of ['sourceExports', 'nonCodeExports']) {
    if (!metadata[key] || typeof metadata[key] !== 'object' || Array.isArray(metadata[key]))
      errors.push(`metadata:${manifest.name}: ${key} must be an object`);
  }
  if (!metadata.sourceExports || !metadata.nonCodeExports) return;
  const sourceKeys = Object.keys(metadata.sourceExports);
  const nonCodeKeys = Object.keys(metadata.nonCodeExports);
  const overlap = sourceKeys.filter((key) => nonCodeKeys.includes(key));
  if (overlap.length) errors.push(`exports:${manifest.name}: overlapping metadata keys ${overlap.join(',')}`);
  const declared = Object.keys(manifest.exports || {}).sort();
  const covered = [...sourceKeys, ...nonCodeKeys].sort();
  if (JSON.stringify(declared) !== JSON.stringify(covered)) {
    errors.push(`exports:${manifest.name}: metadata keys must exactly cover package exports`);
  }
  for (const [exportKey, config] of Object.entries(metadata.sourceExports)) {
    const source = config.source || config.sourcePattern;
    if (config.kind !== 'code' || !source) {
      errors.push(`exports:${manifest.name}:${exportKey}: code export requires kind=code and source/sourcePattern`);
      continue;
    }
    if (source.includes('*')) {
      const identity = `exports:${manifest.name}:${exportKey}`;
      if (!config.expiresAt) errors.push(`${identity}: wildcard source expiresAt is required`);
      else validateExpiryDate(config.expiresAt, identity, 'wildcard source', errors);
      if (matchingFiles(root, source).length === 0)
        errors.push(`${identity}: wildcard source must match at least one file`);
      continue;
    }
    const resolved = path.resolve(root, source);
    if (!insideRoot(root, resolved) || !fs.existsSync(resolved) || !fs.statSync(resolved).isFile()) {
      errors.push(`exports:${manifest.name}:${exportKey}: source does not resolve inside package (${source})`);
    }
  }
  for (const [exportKey, config] of Object.entries(metadata.nonCodeExports)) {
    if (!config.source || !config.dist || !['static', 'build'].includes(config.generation)) {
      errors.push(`exports:${manifest.name}:${exportKey}: non-code export requires source/dist/generation`);
      continue;
    }
    const matches = config.source.includes('*')
      ? matchingFiles(root, config.source)
      : [config.source].filter((source) => fs.existsSync(path.resolve(root, source)));
    if (matches.length === 0)
      errors.push(
        `exports:${manifest.name}:${exportKey}: ${config.generation} source does not exist (${config.source})`,
      );
    const resolvedSource = path.resolve(
      root,
      config.source.includes('*') ? config.source.slice(0, config.source.indexOf('*')) : config.source,
    );
    if (!insideRoot(root, resolvedSource))
      errors.push(`exports:${manifest.name}:${exportKey}: non-code source escapes package (${config.source})`);
    const resolvedDist = path.resolve(root, config.dist);
    if (!insideRoot(root, resolvedDist))
      errors.push(`exports:${manifest.name}:${exportKey}: dist target escapes package (${config.dist})`);
    const declaredTarget = targetImport(manifest.exports?.[exportKey]);
    if (declaredTarget !== config.dist)
      errors.push(
        `exports:${manifest.name}:${exportKey}: metadata dist ${config.dist} does not match export target ${declaredTarget}`,
      );
  }
}

function validateExceptions(uiRoot, baseline, workspaceNames, errors) {
  if (baseline.schemaVersion !== 1 || !Array.isArray(baseline.exceptions) || !Array.isArray(baseline.legacySccs)) {
    errors.push('baseline: architecture-exceptions.json must contain schemaVersion=1, exceptions[], legacySccs[]');
    return;
  }
  const identities = new Set();
  for (const exception of baseline.exceptions) {
    const identity = `${exception.from}->${exception.to}`;
    if (identities.has(identity)) errors.push(`baseline: duplicate exception ${identity}`);
    identities.add(identity);
    if (!workspaceNames.has(exception.from) || !workspaceNames.has(exception.to))
      errors.push(`baseline: unknown exception endpoint ${identity}`);
    for (const field of ['reason', 'ownerRole', 'adr', 'decisionEvidence', 'expiresAt']) {
      if (!exception[field]) errors.push(`baseline:${identity}: ${field} is required`);
    }
    if (exception.expiresAt) validateExpiryDate(exception.expiresAt, `baseline:${identity}`, 'exception', errors);
    if (exception.decisionEvidence && !fs.existsSync(path.resolve(uiRoot, '..', exception.decisionEvidence))) {
      errors.push(`baseline:${identity}: decisionEvidence does not exist (${exception.decisionEvidence})`);
    }
  }
  const sccIds = new Set();
  for (const legacy of baseline.legacySccs) {
    const identity = legacy.id || '<missing-id>';
    if (!legacy.id) errors.push('baseline: legacy SCC id is required');
    if (sccIds.has(identity)) errors.push(`baseline: duplicate legacy SCC id ${identity}`);
    sccIds.add(identity);
    if (!LEGACY_SCC_GRAPH_KINDS.has(legacy.graphKind))
      errors.push(`baseline:${identity}: invalid graphKind ${legacy.graphKind}`);
    for (const field of ['ownerRole', 'adr'])
      if (!legacy[field]) errors.push(`baseline:${identity}: ${field} is required`);
    if (!Number.isInteger(legacy.targetPhase) || legacy.targetPhase < 1)
      errors.push(`baseline:${identity}: targetPhase must be a positive integer`);
    if (!Array.isArray(legacy.members) || legacy.members.length < 2) {
      errors.push(`baseline:${identity}: members must contain at least two workspaces`);
      continue;
    }
    if (JSON.stringify(legacy.members) !== JSON.stringify([...legacy.members].sort()))
      errors.push(`baseline:${identity}: members must be sorted`);
    for (const member of legacy.members)
      if (!workspaceNames.has(member)) errors.push(`baseline:${identity}: unknown member ${member}`);
    if (!Array.isArray(legacy.edges) || legacy.edges.length < 2) {
      errors.push(`baseline:${identity}: edges must contain the recorded cycle`);
      continue;
    }
    const sortedEdges = [...legacy.edges].sort((left, right) => edgeIdentity(left).localeCompare(edgeIdentity(right)));
    if (JSON.stringify(legacy.edges) !== JSON.stringify(sortedEdges))
      errors.push(`baseline:${identity}: edges must be sorted`);
    const members = new Set(legacy.members);
    for (const edge of legacy.edges) {
      if (!edge?.from || !edge?.to || !edge?.kind)
        errors.push(`baseline:${identity}: every edge requires from/to/kind`);
      else if (!members.has(edge.from) || !members.has(edge.to))
        errors.push(`baseline:${identity}: edge endpoint must belong to members`);
    }
  }
}

export function analyzeArchitecture(uiRoot, options = {}) {
  const resolvedRoot = path.resolve(uiRoot);
  const workspaces = discoverWorkspaces(resolvedRoot);
  if (workspaces.length === 0) throw new Error(`No frontend workspaces found under ${resolvedRoot}`);
  const errors = [];
  for (const location of findMissingWorkspaceManifests(resolvedRoot))
    errors.push(`metadata:${location}: missing package.json`);
  const baselineFile = options.baselineFile || path.join(resolvedRoot, 'architecture-exceptions.json');
  if (!fs.existsSync(baselineFile)) throw new Error(`Architecture baseline not found: ${baselineFile}`);
  const baseline = readJson(baselineFile);
  const byName = new Map();
  for (const workspace of workspaces) {
    if (!workspace.manifest.name)
      errors.push(`metadata:${toPosix(path.relative(resolvedRoot, workspace.root))}: package name is required`);
    else if (byName.has(workspace.manifest.name))
      errors.push(`metadata:${workspace.manifest.name}: duplicate workspace name`);
    else byName.set(workspace.manifest.name, workspace);
    validateArchitectureMetadata(workspace, errors);
  }
  const workspaceNames = new Set(byName.keys());
  validateExceptions(resolvedRoot, baseline, workspaceNames, errors);
  const manifestEdges = [];
  const toolingEdges = [];
  const sourceRuntimeEdges = [];
  const contractEdges = [];
  const dependencyViolations = [];

  for (const workspace of workspaces) {
    const metadata = workspace.manifest.mangoArchitecture;
    if (!metadata) continue;
    const declaredRuntimeDependencies = new Set(
      DEPENDENCY_GROUPS.flatMap((group) => Object.keys(workspace.manifest[group] || {})),
    );
    const declaredDependencies = new Set(
      ALL_DEPENDENCY_GROUPS.flatMap((group) => Object.keys(workspace.manifest[group] || {})),
    );
    for (const group of DEPENDENCY_GROUPS) {
      for (const dependency of Object.keys(workspace.manifest[group] || {})) {
        if (!workspaceNames.has(dependency)) continue;
        manifestEdges.push({ from: workspace.manifest.name, to: dependency, kind: group });
      }
    }
    for (const dependency of Object.keys(workspace.manifest.devDependencies || {})) {
      if (workspaceNames.has(dependency))
        toolingEdges.push({ from: workspace.manifest.name, to: dependency, kind: 'devDependencies' });
    }
    for (const file of listRuntimeSourceFiles(workspace)) {
      const content = fs.readFileSync(file, 'utf8');
      for (const item of sourceImports(content, file, workspace.manifest.name, errors)) {
        if (item.specifier.startsWith('.')) {
          const resolved = path.resolve(path.dirname(file), item.specifier);
          if (!insideRoot(workspace.root, resolved))
            errors.push(
              `source:${workspace.manifest.name}: relative import escapes package (${toPosix(path.relative(workspace.root, file))} -> ${item.specifier})`,
            );
          continue;
        }
        const target = parseInternalSpecifier(item.specifier, workspaceNames);
        if (!target || target.packageName === workspace.manifest.name) continue;
        if (!declaredDependencies.has(target.packageName)) {
          errors.push(
            `source:${workspace.manifest.name}: undeclared internal dependency ${target.packageName} in ${toPosix(path.relative(workspace.root, file))}`,
          );
        } else if (item.kind === 'runtime' && !declaredRuntimeDependencies.has(target.packageName)) {
          errors.push(
            `source:${workspace.manifest.name}: runtime dependency ${target.packageName} is declared only for development in ${toPosix(path.relative(workspace.root, file))}`,
          );
        }
        const targetWorkspace = byName.get(target.packageName);
        if (!exportedSubpath(targetWorkspace.manifest, target.exportKey)) {
          errors.push(
            `source:${workspace.manifest.name}: ${item.specifier} is not a public export of ${target.packageName}`,
          );
        }
        const edge = { from: workspace.manifest.name, to: target.packageName, kind: item.kind };
        (item.kind === 'contract' ? contractEdges : sourceRuntimeEdges).push(edge);
      }
    }
  }

  const runtimeEdges = uniqueEdges([...manifestEdges, ...sourceRuntimeEdges]);
  const normalizedContractEdges = uniqueEdges(contractEdges);
  const combinedEdges = uniqueEdges([...runtimeEdges, ...normalizedContractEdges]);
  const actualViolationPairs = new Set();
  for (const edge of combinedEdges) {
    const from = byName.get(edge.from)?.manifest.mangoArchitecture;
    const to = byName.get(edge.to)?.manifest.mangoArchitecture;
    if (!from || !to) continue;
    let reason = null;
    const targetWorkspace = byName.get(edge.to);
    if (byName.get(edge.from)?.kind === 'package' && targetWorkspace?.kind === 'app') reason = 'package depends on app';
    else if (from.architectureLayer !== 'TOOLING' && to.architectureLayer === 'TOOLING')
      reason = 'runtime package depends on TOOLING';
    else if (from.architectureLayer !== 'TOOLING' && to.architectureLayer !== 'TOOLING') {
      const allowedTargets = ALLOWED_RUNTIME_TARGET_LAYERS.get(from.architectureLayer);
      if (!allowedTargets?.has(to.architectureLayer))
        reason = `reverse layer dependency ${from.architectureLayer}->${to.architectureLayer}`;
      const fromOrder = LAYER_ORDER.get(from.architectureLayer);
      const toOrder = LAYER_ORDER.get(to.architectureLayer);
      if (!reason && fromOrder === toOrder && from.domain !== to.domain)
        reason = `same-layer cross-domain dependency ${from.domain}->${to.domain}`;
    }
    if (reason) actualViolationPairs.add(`${edge.from}->${edge.to}`);
    if (reason && !exceptionMatches(baseline.exceptions || [], edge.from, edge.to)) {
      dependencyViolations.push({ ...edge, reason });
      errors.push(`dependency:${edgeIdentity(edge)}: ${reason}`);
    }
  }
  for (const exception of baseline.exceptions || []) {
    const identity = `${exception.from}->${exception.to}`;
    if (!actualViolationPairs.has(identity))
      errors.push(`baseline:${identity}: exception does not match a current dependency violation`);
  }

  const normalizedToolingEdges = uniqueEdges(toolingEdges);
  for (const edge of normalizedToolingEdges) {
    const from = byName.get(edge.from)?.manifest.mangoArchitecture;
    const to = byName.get(edge.to)?.manifest.mangoArchitecture;
    if (!from || !to) continue;
    let reason = null;
    if (from.architectureLayer === 'TOOLING' && to.architectureLayer !== 'TOOLING' && to.role !== 'contract') {
      reason = 'TOOLING devDependency must target TOOLING or a public contract';
    } else if (from.architectureLayer !== 'TOOLING' && to.architectureLayer !== 'TOOLING') {
      const fromOrder = LAYER_ORDER.get(from.architectureLayer);
      const toOrder = LAYER_ORDER.get(to.architectureLayer);
      if (fromOrder < toOrder) reason = `reverse tooling dependency ${from.architectureLayer}->${to.architectureLayer}`;
    }
    if (reason) errors.push(`tooling:${edgeIdentity(edge)}: ${reason}`);
  }
  for (const scc of createSccs('tooling', workspaceNames, normalizedToolingEdges)) {
    errors.push(`tooling-scc:${scc.members.join(',')}: tooling dependency cycle`);
  }

  const graphEdges = {
    manifest: uniqueEdges(manifestEdges),
    'source-runtime': uniqueEdges(sourceRuntimeEdges),
    contract: normalizedContractEdges,
    combined: combinedEdges,
    tooling: normalizedToolingEdges,
  };
  const sccs = Object.entries(graphEdges).flatMap(([graphKind, edges]) => createSccs(graphKind, workspaceNames, edges));
  for (const scc of sccs) {
    const matched = (baseline.legacySccs || []).some((legacy) => {
      if (legacy.graphKind !== scc.graphKind) return false;
      const legacyMembers = new Set(legacy.members || []);
      const legacyEdges = new Set((legacy.edges || []).map(edgeIdentity));
      return (
        scc.members.every((member) => legacyMembers.has(member)) &&
        scc.edges.every((edge) => legacyEdges.has(edgeIdentity(edge)))
      );
    });
    if (!matched) errors.push(`scc:${scc.graphKind}:${scc.members.join(',')}: new or expanded cycle`);
  }

  const report = {
    schemaVersion: 1,
    summary: {
      workspaceCount: workspaces.length,
      metadataCoverage: workspaces.filter((workspace) => workspace.manifest.mangoArchitecture).length,
      manifestEdgeCount: graphEdges.manifest.length,
      sourceRuntimeEdgeCount: graphEdges['source-runtime'].length,
      contractEdgeCount: graphEdges.contract.length,
      toolingEdgeCount: graphEdges.tooling.length,
      exceptionCount: baseline.exceptions?.length || 0,
      legacySccCount: baseline.legacySccs?.length || 0,
      detectedSccCount: sccs.length,
      errorCount: errors.length,
    },
    workspaces: workspaces.map((workspace) => ({
      name: workspace.manifest.name,
      path: toPosix(path.relative(resolvedRoot, workspace.root)),
      ...(workspace.manifest.mangoArchitecture || {}),
    })),
    edges: graphEdges,
    dependencyViolations,
    sccs,
    errors: [...new Set(errors)].sort(),
  };
  report.reportSha256 = stableHash({ ...report, reportSha256: undefined });
  return report;
}

export function assertArchitecture(report) {
  if (report.summary.workspaceCount === 0) throw new Error('Architecture workspace input is zero');
  if (report.summary.metadataCoverage !== report.summary.workspaceCount)
    throw new Error('Architecture metadata coverage is incomplete');
  if (report.errors.length)
    throw new Error(
      `Frontend architecture check failed with ${report.errors.length} issue(s):\n${report.errors.join('\n')}`,
    );
}
