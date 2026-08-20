#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { readProjectPmoChecks } from './lib/project-pmo-checks.mjs';

const toolDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = resolveRepositoryRoot(toolDirectory);
const defaultProjectPaths = {
  backend: 'backend',
  frontend: 'frontend',
  businessDocs: 'business-docs',
};

function resolveRepositoryRoot(start) {
  try {
    return execFileSync('git', ['rev-parse', '--show-toplevel'], {
      cwd: start,
      encoding: 'utf8',
    }).trim();
  } catch {
    return path.resolve(start, '../..');
  }
}

function matchesAny(file, patterns) {
  return patterns.some(pattern => pattern.test(file));
}

function normalizeProjectPath(value, fallback, field) {
  const candidate = typeof value === 'string' && value.trim() ? value.trim() : fallback;
  const normalized = candidate.replaceAll('\\', '/').replace(/^\.\//u, '').replace(/\/$/u, '');
  if (!normalized
    || path.posix.isAbsolute(normalized)
    || normalized.split('/').includes('..')
    || normalized.includes('\n')
    || normalized.includes('\r')) {
    throw new Error(`mango.config.json paths.${field} must be a repository-relative path`);
  }
  return normalized;
}

export function resolveProjectPaths(repositoryRoot = root) {
  const configPath = path.join(repositoryRoot, 'mango.config.json');
  let configured = {};
  if (fs.existsSync(configPath)) {
    try {
      configured = JSON.parse(fs.readFileSync(configPath, 'utf8')).paths || {};
    } catch (error) {
      throw new Error(`Cannot read mango.config.json paths: ${error.message}`);
    }
  }
  return {
    backend: normalizeProjectPath(configured.backend, defaultProjectPaths.backend, 'backend'),
    frontend: normalizeProjectPath(configured.frontend, defaultProjectPaths.frontend, 'frontend'),
    businessDocs: normalizeProjectPath(
      configured.businessDocs,
      defaultProjectPaths.businessDocs,
      'businessDocs',
    ),
  };
}

function logicalFilePath(file, paths) {
  const mappings = [
    [paths.backend, 'backend'],
    [paths.frontend, 'frontend'],
    [paths.businessDocs, 'business-docs'],
  ].sort((left, right) => right[0].length - left[0].length);
  for (const [configured, logical] of mappings) {
    if (file === configured) return logical;
    if (file.startsWith(`${configured}/`)) return `${logical}/${file.slice(configured.length + 1)}`;
  }
  if (/^\.gitea\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/u.test(file)) {
    return file.replace(/^\.gitea/u, '.github');
  }
  return file;
}

function changesGeneratedBackendBehavior(file) {
  if (matchesAny(file, [
    /(?:^|\/)README\.md$/,
    /(?:^|\/)CHANGELOG\.md$/,
    /^mango-pmo\/contracts(?:\/|$)/,
    /^mango-pmo\/(?:plugin-src\/\.codex-plugin\/plugin\.json|release-lock\.json)$/,
    /^mango-ui\/packages\/mango-pmo\/(?:package\.json|\.codex-plugin\/plugin\.json|dist\/baseline\.json)$/,
    /^mango-ui\/packages\/mango-cli\/(?:package\.json|release-versions\.json|src\/release-command\.mjs|scripts\/check-release-versions\.mjs)$/,
    /^mango-business-starter\/business-pmo\/(?:pmo-lock\.json|mango-baseline\/(?:baseline\.json|contracts(?:\/|$)))/,
    /^business-pmo\/(?:pmo-lock\.json|mango-baseline\/(?:baseline\.json|contracts(?:\/|$)))/,
  ])) {
    return false;
  }

  return matchesAny(file, [
    /^\.github\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/,
    /^mango\.config\.json$/,
    /^mango\/pom\.xml$/,
    /^mango\/mango-parent(?:\/|$)/,
    /^mango\/mango-tools\/mango-(?:architecture-rules|maven-plugin|architecture-verification)(?:\/|$)/,
    /^mango-pmo\/baselines\/(?:architecture|mango-check)(?:\/|$)/,
    /^mango-pmo\/tools\/(?:classify-pmo-check-scope|resolve-pmo-check-event-mode|check-governance-intent|check-architecture-debt-budget)\.mjs$/,
    /^(?:mango-business-starter\/)?business-pmo\/architecture-debt-budget\.json$/,
    /^(?:mango-business-starter\/)?business-pmo\/mango-baseline\/tools\/(?:classify-pmo-check-scope|resolve-pmo-check-event-mode|check-governance-intent|check-architecture-debt-budget)\.mjs$/,
    /^mango-business-starter\/backend(?:\/|$)/,
    /^mango-business-starter\/scripts\/check-template\.mjs$/,
    /^mango-ui\/packages\/mango-cli\/templates\/full\/(?:\.github|\.gitea)\/workflows\/pmo-doc-check\.yml$/,
    /^mango-ui\/packages\/mango-cli\/templates\/full\/business-pmo\/architecture-debt-budget\.json$/,
    /^mango-ui\/packages\/mango-cli\/templates\/full\/backend(?:\/|$)/,
    /^mango-ui\/packages\/mango-cli\/src(?:\/|$)/,
    /^mango-ui\/packages\/mango-cli\/scripts\/(?:check-cli|check-business-module-template|check-generated-backend-gate)\.mjs$/,
  ]);
}

export function classifyChangedFiles(files, repositoryRoot = root) {
  const paths = resolveProjectPaths(repositoryRoot);
  const normalized = [...new Set(files.map(file => logicalFilePath(file.trim(), paths)).filter(Boolean))].sort();
  const workflowChanged = normalized.some(file =>
    /^\.github\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/.test(file));
  if (workflowChanged || normalized.includes('mango.config.json')) {
    return {
      pmo: true,
      backend: true,
      projection: true,
      distribution: true,
      readmes: true,
      generated_backend: true,
      frontend: true,
    };
  }

  const packagedPmo = normalized.some(file => matchesAny(file, [
    /^mango-pmo\/(?:agents|contracts|plugin-src|rules|skills|templates|tools)(?:\/|$)/,
  ]));
  const pmo = normalized.some(file => matchesAny(file, [
    /^mango-pmo\//,
    /^business-pmo\/mango-baseline\//,
    /^\.agents\/skills\//,
    /^mango-ui\/packages\/mango-pmo\//,
    /^\.github\/workflows\/pr-contract-check\.yml$/,
    /^\.github\/(?:pull_request_template\.md|CODEOWNERS|branch-protection-policy\.json)$/,
    /^(?:AGENTS|CLAUDE|GEMINI)\.md$/,
  ]));
  const backend = normalized.some(file => matchesAny(file, [
    /^mango\/.*\.java$/,
    /^mango\/(?:pom\.xml|.*\/pom\.xml)$/,
    /^mango\/.*\/src\/main\/resources\/META-INF\/mango\/module\.properties$/,
    /^backend\/.*\.java$/,
    /^backend\/(?:pom\.xml|.*\/pom\.xml)$/,
    /^backend\/.*\/src\/main\/resources\/META-INF\/mango\/module\.properties$/,
    /^mango\/mango-tools\/mango-(?:architecture-rules|maven-plugin)(?:\/|$)/,
    /^mango-pmo\/baselines\/(?:architecture|mango-check)(?:\/|$)/,
    /^mango-pmo\/tools\/check-architecture-debt-budget\.mjs$/,
    /^(?:mango-business-starter\/)?business-pmo\/architecture-debt-budget\.json$/,
  ]));
  const distribution = normalized.some(file => matchesAny(file, [
    /^mango-business-starter\//,
    /^mango-ui\/packages\/mango-cli\//,
    /^scripts\/.*release/i,
    /^mango-pmo\/release-lock\.json$/,
  ]));
  const projection = packagedPmo || distribution || normalized.some(file =>
    /^mango-ui\/packages\/mango-pmo\//.test(file));
  const readmes = normalized.some(file => matchesAny(file, [
    /(?:^|\/)README\.md$/,
    /^CHANGELOG\.md$/,
    /^mango-docs\/(?:capabilities|guides)\//,
    /^mango\/.*\.(?:java|xml|yml|yaml|properties)$/,
    /^backend\/.*\.(?:java|xml|yml|yaml|properties)$/,
    /^mango-ui\/packages\/.*\.(?:ts|tsx|vue|js|mjs|json|css|scss|less)$/,
    /^frontend\/.*\.(?:ts|tsx|vue|js|mjs|json|css|scss|less)$/,
  ]));
  const frontend = normalized.some(file => matchesAny(file, [
    /^frontend(?:\/|$)/,
    /^mango-ui\/(?:apps|packages|scripts)(?:\/|$)/,
    /^mango-business-starter\/frontend(?:\/|$)/,
    /^mango-ui\/packages\/mango-cli\/templates\/(?:full\/frontend|business-module\/frontend)(?:\/|$)/,
    /^mango-pmo\/tools\/check-frontend-page-baseline\.mjs$/,
    /^mango-pmo\/tests\/frontend-page-baseline\.test\.mjs$/,
  ]));
  const generated_backend = normalized.some(changesGeneratedBackendBehavior);

  return { pmo, backend, projection, distribution, readmes, generated_backend, frontend };
}

function listPomProjects(directory, mangoRoot) {
  if (!fs.existsSync(directory)) return [];
  const projects = [];
  const entries = fs.readdirSync(directory, { withFileTypes: true });
  if (fs.existsSync(path.join(directory, 'pom.xml'))) {
    const relative = path.relative(mangoRoot, directory).replaceAll(path.sep, '/');
    if (relative) projects.push(relative);
  }
  for (const entry of entries) {
    if (!entry.isDirectory() || entry.name === 'target' || entry.name.startsWith('.')) continue;
    projects.push(...listPomProjects(path.join(directory, entry.name), mangoRoot));
  }
  return projects;
}

function nearestMavenProject(file, repositoryRoot, mavenRoot) {
  let current = path.dirname(path.join(repositoryRoot, file));
  while (current.startsWith(mavenRoot) && current !== mavenRoot) {
    if (fs.existsSync(path.join(current, 'pom.xml'))) return current;
    current = path.dirname(current);
  }
  return fs.existsSync(path.join(mavenRoot, 'pom.xml')) ? mavenRoot : null;
}

export function resolveMavenScope(files, repositoryRoot = root) {
  const normalized = [...new Set(files.map(file => file.trim()).filter(Boolean))].sort();
  const sourceLayout = fs.existsSync(path.join(repositoryRoot, 'mango/pom.xml'));
  const projectPaths = resolveProjectPaths(repositoryRoot);
  const mavenPrefix = sourceLayout ? 'mango' : projectPaths.backend;
  const mavenRoot = path.join(repositoryRoot, mavenPrefix);
  if (!fs.existsSync(path.join(mavenRoot, 'pom.xml'))) {
    if (fs.existsSync(path.join(repositoryRoot, 'mango.config.json'))) {
      throw new Error(`Configured backend POM does not exist: ${mavenPrefix}/pom.xml`);
    }
    return { mode: 'none', projects: [] };
  }
  const escapedMavenPrefix = mavenPrefix.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
  const mavenPomPattern = new RegExp(`^${escapedMavenPrefix}\\/(?:pom\\.xml|.*\\/pom\\.xml)$`);
  const mavenJavaPattern = new RegExp(`^${escapedMavenPrefix}\\/.*\\.java$`);
  const mavenMetadataPattern = new RegExp(
    `^${escapedMavenPrefix}\\/.*\\/src\\/main\\/resources\\/META-INF\\/mango\\/module\\.properties$`,
  );
  const architecturePattern = new RegExp(`^${escapedMavenPrefix}\\/architecture-verification(?:\\/|$)`);
  const qualityPattern = new RegExp(`^${escapedMavenPrefix}\\/(?:\\.mvn|config\\/quality)(?:\\/|$)`);
  const governanceGate = normalized.some(file => matchesAny(file, [
    /^\.github\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/,
    /^\.gitea\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/,
    /^mango\.config\.json$/,
    new RegExp(`^${escapedMavenPrefix}\\/pom\\.xml$`),
    ...(sourceLayout ? [
      /^mango\/mango-parent(?:\/|$)/,
      /^mango\/mango-tools\/mango-(?:architecture-rules|maven-plugin|architecture-verification)(?:\/|$)/,
    ] : [
      architecturePattern,
      qualityPattern,
      /^business-pmo\/global-entity-exceptions\.json$/,
    ]),
    /^mango-pmo\/baselines\/(?:architecture|mango-check)(?:\/|$)/,
    /^mango-pmo\/tools\/check-architecture-debt-budget\.mjs$/,
    /^(?:mango-business-starter\/)?business-pmo\/architecture-debt-budget\.json$/,
  ]));
  if (governanceGate) return { mode: 'governance', projects: [] };

  const backendFiles = normalized.filter(file => matchesAny(file, [
    /^mango\/.*\.java$/,
    /^mango\/(?:pom\.xml|.*\/pom\.xml)$/,
    /^mango\/.*\/src\/main\/resources\/META-INF\/mango\/module\.properties$/,
    mavenJavaPattern,
    mavenPomPattern,
    mavenMetadataPattern,
    /^mango-pmo\/baselines\/(?:architecture|mango-check)(?:\/|$)/,
  ]));
  if (backendFiles.length === 0) return { mode: 'none', projects: [] };

  const architectureProject = sourceLayout
    ? ':mango-architecture-verification'
    : 'architecture-verification';
  const projects = new Set([architectureProject]);
  for (const file of backendFiles) {
    const project = nearestMavenProject(file, repositoryRoot, mavenRoot);
    if (!project || project === mavenRoot) return { mode: 'governance', projects: [] };
    if (file.endsWith('/pom.xml')) {
      if (!fs.existsSync(path.join(repositoryRoot, file))) {
        return { mode: 'governance', projects: [] };
      }
      for (const descendant of listPomProjects(project, mavenRoot)) projects.add(descendant);
      continue;
    }
    if (file.endsWith('/META-INF/mango/module.properties')) {
      const domainRoot = fs.existsSync(path.join(path.dirname(project), 'pom.xml'))
        ? path.dirname(project)
        : project;
      for (const sibling of listPomProjects(domainRoot, mavenRoot)) projects.add(sibling);
      continue;
    }
    projects.add(path.relative(mavenRoot, project).replaceAll(path.sep, '/'));
  }
  return { mode: 'partial', projects: [...projects].sort() };
}

export function resolveMavenDependencyProjects(mavenScope) {
  if (mavenScope.mode !== 'partial') return [];
  return mavenScope.projects.filter(project => ![
    ':mango-architecture-verification',
    'architecture-verification',
    'mango-architecture-verification',
  ].includes(project));
}

function parseArgs(argv) {
  const result = { base: '', head: '', output: process.env.GITHUB_OUTPUT || '' };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (['--base', '--head', '--output'].includes(value)) {
      result[value.slice(2)] = argv[index + 1] ?? '';
      index += 1;
    }
  }
  return result;
}

function changedFiles(base, head) {
  if (!base || !head) throw new Error('Use --base <sha> and --head <sha>.');
  return execFileSync('git', ['diff', '--name-only', `${base}...${head}`], {
    cwd: root,
    encoding: 'utf8',
  }).split(/\r?\n/u).map(file => file.trim()).filter(Boolean);
}

export function runScopeClassifierCli(argv = process.argv.slice(2)) {
  try {
    const args = parseArgs(argv);
    const files = changedFiles(args.base, args.head);
    if (process.env.GITHUB_ACTIONS && files.length === 0) {
      throw new Error(`No changed files detected for ${args.base}...${args.head}.`);
    }
    const scope = classifyChangedFiles(files);
    const maven = resolveMavenScope(files);
    const mavenDependencyProjects = resolveMavenDependencyProjects(maven);
    const projectPaths = resolveProjectPaths(root);
    const projectPmoChecks = readProjectPmoChecks(root);
    if (args.output) {
      const lines = [
        ...Object.entries(scope).map(([key, value]) => `${key}=${value}`),
        `frontend_page_baseline_enabled=${projectPmoChecks.frontendPageBaseline}`,
        `backend_mode=${maven.mode}`,
        `maven_projects=${maven.projects.join(',')}`,
        `maven_dependency_projects=${mavenDependencyProjects.join(',')}`,
        `backend_root=${projectPaths.backend}`,
        `backend_pom=${projectPaths.backend}/pom.xml`,
        `frontend_root=${projectPaths.frontend}`,
        `business_docs_root=${projectPaths.businessDocs}`,
      ].join('\n');
      fs.appendFileSync(args.output, `${lines}\n`);
    }
    process.stdout.write(`${JSON.stringify({ files, scope, maven, pmoChecks: projectPmoChecks }, null, 2)}\n`);
    return 0;
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    return 1;
  }
}

const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : '';
if (invokedPath === fileURLToPath(import.meta.url)) {
  process.exitCode = runScopeClassifierCli();
}
