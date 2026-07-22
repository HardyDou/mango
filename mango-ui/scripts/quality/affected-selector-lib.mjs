import fs from 'node:fs';
import path from 'node:path';

const DEPENDENCY_FIELDS = ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies'];
const FULL_PATTERNS = [
  /^\.github\/workflows\/frontend-quality\.yml$/u,
  /^mango-business-starter\/frontend\//u,
  /^mango-pmo\/(?:rules\/frontend\/|tools\/pmo-preflight)/u,
  /^mango-ui\/(?:package\.json|pnpm-lock\.yaml|pnpm-workspace\.yaml|quality-baseline\.json|architecture-exceptions\.json|frontend-boundary-baseline\.json)$/u,
  /^mango-ui\/(?:eslint\.config\.|prettier\.config\.|stylelint\.config\.|tsconfig|vite\.config\.)/u,
  /^mango-ui\/scripts\/(?:quality\/|check-|generate-package-styles|run-business-lab)/u,
];
const FRONTEND_PREFIX = /^mango-ui\/(?:apps|packages)\//u;
const KNOWN_NON_FRONTEND_PREFIX =
  /^(?:\.github\/(?!workflows\/frontend-quality\.yml$)|mango\/(?!ui\/)|mango-business-starter\/(?!frontend\/)|mango-docs\/|mango-pmo\/(?!rules\/frontend\/|tools\/pmo-preflight)|CHANGELOG(?:\.|$)|README(?:\.|$)|\.gitignore$)/u;

function posix(value) {
  return value.split(path.sep).join('/');
}

export function readAffectedWorkspaces(uiRoot) {
  const records = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const manifestFile = path.join(parent, entry.name, 'package.json');
      if (!fs.existsSync(manifestFile)) throw new Error(`${kind}/${entry.name} is missing package.json`);
      const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
      if (!manifest.name) throw new Error(`${posix(path.relative(uiRoot, manifestFile))} is missing name`);
      records.push({
        name: manifest.name,
        kind,
        relativeRoot: `mango-ui/${kind}/${entry.name}`,
        private: manifest.private === true,
        scripts: manifest.scripts || {},
        dependencies: Object.fromEntries(
          DEPENDENCY_FIELDS.flatMap((field) => Object.keys(manifest[field] || {}).map((name) => [name, field])),
        ),
      });
    }
  }
  if (records.length === 0) throw new Error('affected workspace graph is empty');
  return records.sort((left, right) => left.name.localeCompare(right.name));
}

function full(reason, records = []) {
  return { mode: 'full', reason, selected: records.map((item) => item.name).sort(), publishableChanged: [] };
}

export function selectAffectedWorkspaces(records, changedPaths, options = {}) {
  if (options.scopeKnown === false) return full(options.reason || 'change scope is unknown', records);
  const normalized = [...new Set(changedPaths.map(posix).filter(Boolean))].sort();
  if (normalized.length === 0) return full('change set is empty', records);
  const fullTrigger = normalized.find((file) => FULL_PATTERNS.some((pattern) => pattern.test(file)));
  if (fullTrigger) return full(`shared frontend input changed: ${fullTrigger}`, records);

  const byRoot = [...records].sort((left, right) => right.relativeRoot.length - left.relativeRoot.length);
  const directlyChanged = new Set();
  for (const file of normalized) {
    if (!FRONTEND_PREFIX.test(file)) {
      if (KNOWN_NON_FRONTEND_PREFIX.test(file)) continue;
      return full(`unclassified repository path changed: ${file}`, records);
    }
    const workspace = byRoot.find((item) => file === item.relativeRoot || file.startsWith(`${item.relativeRoot}/`));
    if (!workspace) return full(`frontend path has no workspace owner: ${file}`, records);
    directlyChanged.add(workspace.name);
  }
  if (directlyChanged.size === 0) {
    return { mode: 'none', reason: 'no frontend-owned path changed', selected: [], publishableChanged: [] };
  }

  const selected = new Set(directlyChanged);
  let changed = true;
  while (changed) {
    changed = false;
    for (const record of records) {
      if (selected.has(record.name)) continue;
      if (Object.keys(record.dependencies).some((dependency) => selected.has(dependency))) {
        selected.add(record.name);
        changed = true;
      }
    }
  }
  const publishableChanged = records
    .filter((item) => directlyChanged.has(item.name) && !item.private)
    .map((item) => item.name)
    .sort();
  return {
    mode: 'affected',
    reason: `${directlyChanged.size} directly changed workspace(s) plus transitive consumers`,
    directlyChanged: [...directlyChanged].sort(),
    selected: [...selected].sort(),
    publishableChanged,
  };
}
