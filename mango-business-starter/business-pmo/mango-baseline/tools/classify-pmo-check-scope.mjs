#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const toolDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = resolveRepositoryRoot(toolDirectory);

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

export function classifyChangedFiles(files) {
  const normalized = [...new Set(files.map(file => file.trim()).filter(Boolean))].sort();
  const workflowChanged = normalized.some(file =>
    /^\.github\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/.test(file));
  if (workflowChanged) {
    return { pmo: true, backend: true, projection: true, distribution: true, readmes: true };
  }

  const packagedPmo = normalized.some(file => matchesAny(file, [
    /^mango-pmo\/(?:agents|contracts|plugin-src|rules|skills|templates|tools)(?:\/|$)/,
  ]));
  const pmo = normalized.some(file => matchesAny(file, [
    /^mango-pmo\//,
    /^business-pmo\/mango-baseline\//,
    /^\.agents\/skills\//,
    /^mango-ui\/packages\/mango-pmo\//,
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

  return { pmo, backend, projection, distribution, readmes };
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
  const mavenPrefix = sourceLayout ? 'mango' : 'backend';
  const mavenRoot = path.join(repositoryRoot, mavenPrefix);
  if (!fs.existsSync(path.join(mavenRoot, 'pom.xml'))) return { mode: 'none', projects: [] };
  const governanceGate = normalized.some(file => matchesAny(file, [
    /^\.github\/workflows\/(?:pmo-doc-check|architecture-debt-inventory)\.yml$/,
    new RegExp(`^${mavenPrefix}\\/pom\\.xml$`),
    ...(sourceLayout ? [
      /^mango\/mango-parent(?:\/|$)/,
      /^mango\/mango-tools\/mango-(?:architecture-rules|maven-plugin|architecture-verification)(?:\/|$)/,
    ] : [
      /^backend\/architecture-verification(?:\/|$)/,
      /^backend\/(?:\.mvn|config\/quality)(?:\/|$)/,
      /^business-pmo\/global-entity-exceptions\.json$/,
    ]),
    /^mango-pmo\/baselines\/(?:architecture|mango-check)(?:\/|$)/,
  ]));
  if (governanceGate) return { mode: 'governance', projects: [] };

  const backendFiles = normalized.filter(file => matchesAny(file, [
    /^mango\/.*\.java$/,
    /^mango\/(?:pom\.xml|.*\/pom\.xml)$/,
    /^mango\/.*\/src\/main\/resources\/META-INF\/mango\/module\.properties$/,
    /^backend\/.*\.java$/,
    /^backend\/(?:pom\.xml|.*\/pom\.xml)$/,
    /^backend\/.*\/src\/main\/resources\/META-INF\/mango\/module\.properties$/,
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
    if (args.output) {
      const lines = [
        ...Object.entries(scope).map(([key, value]) => `${key}=${value}`),
        `backend_mode=${maven.mode}`,
        `maven_projects=${maven.projects.join(',')}`,
      ].join('\n');
      fs.appendFileSync(args.output, `${lines}\n`);
    }
    process.stdout.write(`${JSON.stringify({ files, scope, maven }, null, 2)}\n`);
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
