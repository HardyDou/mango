#!/usr/bin/env node
import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const pmoRoot = resolve(dirname(currentFile), '..');
const indexPath = join(pmoRoot, 'code-templates/index.json');

export function loadCodeTemplateIndex() {
  const index = JSON.parse(readFileSync(indexPath, 'utf8'));
  if (index.schemaVersion !== 1 || !Array.isArray(index.templates)) {
    throw new Error(`invalid code template index: ${indexPath}`);
  }
  validateCodeTemplateIndex(index);
  return index;
}

export function validateCodeTemplateIndex(index) {
  const ids = new Set();
  for (const template of index.templates) {
    if (!template.id || ids.has(template.id)) {
      throw new Error(`duplicate or missing code template id: ${template.id || '<missing>'}`);
    }
    ids.add(template.id);
    if (!template.root || !template.inputs || !template.derived || !Array.isArray(template.qualityProfiles)) {
      throw new Error(`code template ${template.id} must declare inputs, derived and qualityProfiles`);
    }
    const conventionIds = new Set();
    for (const convention of template.conventions ?? []) {
      if (!convention.id || conventionIds.has(convention.id) || !convention.evidence || !convention.contains) {
        throw new Error(`invalid convention in code template ${template.id}`);
      }
      conventionIds.add(convention.id);
    }
    for (const profile of template.qualityProfiles) {
      if (!profile.id || !profile.source || !Array.isArray(profile.rules) || profile.rules.length === 0) {
        throw new Error(`invalid quality profile in code template ${template.id}`);
      }
      if (new Set(profile.rules).size !== profile.rules.length) {
        throw new Error(`duplicate quality rule in ${template.id}/${profile.id}`);
      }
    }
    if (template.id === 'business-module') {
      validateBusinessModuleContract(template);
    }
  }
  return index;
}

function validateBusinessModuleContract(template) {
  const sourceRoot = join(pmoRoot, template.root);
  const checkstyleProfile = template.qualityProfiles.find((profile) => profile.id === 'mango-java-checkstyle');
  if (!checkstyleProfile) throw new Error('business-module must declare mango-java-checkstyle');
  const checkstylePath = resolve(pmoRoot, '..', checkstyleProfile.source);
  if (existsSync(checkstylePath)) {
    const source = readFileSync(checkstylePath, 'utf8');
    const actual = [...source.matchAll(/<module name="([A-Za-z0-9]+)"/g)]
      .map((match) => match[1])
      .filter((name) => !['Checker', 'TreeWalker'].includes(name));
    const declared = new Set(checkstyleProfile.rules);
    for (const rule of new Set(actual)) {
      if (!declared.has(rule)) throw new Error(`Checkstyle rule ${rule} is not represented by business-module baseline`);
    }
    if (checkstyleProfile.projection) {
      const projectionPath = resolve(pmoRoot, '..', checkstyleProfile.projection);
      if (existsSync(projectionPath) && readFileSync(projectionPath, 'utf8') !== source) {
        throw new Error('generated-project Checkstyle projection differs from Mango Checkstyle source');
      }
    }
  }
  const architectureProfile = template.qualityProfiles.find((profile) => profile.id === 'mango-java-architecture');
  if (!architectureProfile) throw new Error('business-module must declare mango-java-architecture');
  const architectureSources = architectureProfile.sources ?? [architectureProfile.source];
  const declaredArchitectureRules = new Set(architectureProfile.rules);
  for (const sourcePath of architectureSources) {
    const architecturePath = resolve(pmoRoot, '..', sourcePath);
    if (existsSync(architecturePath)) {
      const source = readFileSync(architecturePath, 'utf8');
      const actual = new Set(source.match(/MANGO-ARCH-[A-Z0-9-]+/g) ?? []);
      for (const rule of actual) {
        if (!declaredArchitectureRules.has(rule)) {
          throw new Error(`Mango architecture rule ${rule} is not represented by business-module baseline`);
        }
      }
    }
  }
  for (const convention of template.conventions ?? []) {
    const evidence = convention.evidence;
    const evidencePath = join(sourceRoot, evidence);
    if (!existsSync(evidencePath)) {
      throw new Error(`missing convention evidence ${convention.id}: ${evidence}`);
    }
    const source = readFileSync(evidencePath, 'utf8');
    const marker = convention.contains;
    if (!source.includes(marker)) {
      throw new Error(`convention ${convention.id} is not evidenced by ${evidence}`);
    }
  }
}

export function resolveCodeBaselines({ role = '', phase = '', task = '', paths = [] } = {}) {
  const normalizedTask = String(task).toLowerCase();
  const normalizedPaths = Array.isArray(paths) ? paths : splitPaths(paths);
  return loadCodeTemplateIndex().templates
    .filter((template) => {
      if (template.roles?.length && !template.roles.includes(role)) return false;
      if (template.phases?.length && !template.phases.includes(phase)) return false;
      const keywordHit = (template.keywords ?? []).some((keyword) =>
        normalizedTask.includes(String(keyword).toLowerCase()),
      );
      const pathHit = normalizedPaths.some((inputPath) =>
        (template.paths ?? []).some((pattern) => pathMatches(inputPath, pattern)),
      );
      return keywordHit || pathHit;
    })
    .map((template) => ({
      id: template.id,
      version: template.version,
      root: template.root,
      description: template.description,
      reason: 'task or target path matches the standard Mango code template',
    }));
}

export function renderCodeBaseline({ templateId, targetDir, variables }) {
  const index = loadCodeTemplateIndex();
  const template = index.templates.find((item) => item.id === templateId);
  if (!template) throw new Error(`unknown code template: ${templateId}`);
  validateVariables(template, variables);

  const sourceRoot = join(pmoRoot, template.root);
  const resolvedTarget = resolve(targetDir);
  if (!existsSync(sourceRoot)) throw new Error(`code template root is missing: ${sourceRoot}`);
  mkdirSync(resolvedTarget, { recursive: true });
  renderTree(sourceRoot, resolvedTarget, variables, resolvedTarget);
  assertNoUnresolvedPlaceholders(resolvedTarget);
  return {
    templateId,
    templateVersion: template.version,
    targetDir: resolvedTarget,
    files: walkFiles(resolvedTarget).map((file) => relative(resolvedTarget, file).split(sep).join('/')).sort(),
  };
}

function validateVariables(template, variables) {
  if (!variables || typeof variables !== 'object' || Array.isArray(variables)) {
    throw new Error('template variables must be an object');
  }
  const contracts = {
    ...(template.inputs ?? {}),
    ...(Array.isArray(template.derived)
      ? Object.fromEntries(template.derived.map((name) => [name, { pattern: '^[^/]+$' }]))
      : (template.derived ?? {})),
  };
  const unknown = Object.keys(variables).filter((name) => !Object.hasOwn(contracts, name));
  if (unknown.length > 0) throw new Error(`unknown template variables: ${unknown.join(', ')}`);
  for (const [name, contract] of Object.entries(contracts)) {
    const value = variables[name];
    if (typeof value !== 'string' || value.includes('\0') || !new RegExp(contract.pattern).test(value)) {
      throw new Error(`invalid template variable ${name}`);
    }
  }
}

function renderTree(sourceDir, targetDir, variables, targetRoot) {
  mkdirSync(targetDir, { recursive: true });
  for (const entry of readdirSync(sourceDir, { withFileTypes: true })) {
    const renderedName = renderTemplateFileName(entry.name, variables);
    const target = resolve(targetDir, renderedName);
    if (target !== targetRoot && !target.startsWith(`${targetRoot}${sep}`)) {
      throw new Error(`rendered template path escapes target: ${renderedName}`);
    }
    const source = join(sourceDir, entry.name);
    if (entry.isDirectory()) {
      renderTree(source, target, variables, targetRoot);
      continue;
    }
    if (!entry.isFile()) throw new Error(`unsupported template entry: ${source}`);
    const content = readFileSync(source);
    if (content.includes(0)) {
      copyFileSync(source, target);
    } else {
      writeFileSync(target, render(content.toString('utf8'), variables));
    }
    chmodSync(target, statSync(source).mode & 0o111 ? 0o755 : 0o644);
  }
}

function render(value, variables) {
  return Object.entries(variables).reduce(
    (content, [name, replacement]) => content.replaceAll(`{{${name}}}`, String(replacement)),
    value,
  );
}

function renderTemplateFileName(value, variables) {
  const rendered = render(value, variables);
  if (rendered === 'npmrc.template') return '.npmrc';
  if (rendered === 'gitignore.template') return '.gitignore';
  return rendered.endsWith('.template') ? rendered.slice(0, -'.template'.length) : rendered;
}

function assertNoUnresolvedPlaceholders(root) {
  for (const file of walkFiles(root)) {
    const relativePath = relative(root, file);
    const content = readFileSync(file);
    if (/\{\{[^}]+}}/.test(relativePath) || (!content.includes(0) && /\{\{[^}]+}}/.test(content.toString('utf8')))) {
      throw new Error(`unresolved template placeholder: ${relativePath}`);
    }
  }
}

function walkFiles(root) {
  if (!existsSync(root)) return [];
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) files.push(...walkFiles(path));
    else if (entry.isFile()) files.push(path);
  }
  return files;
}

function splitPaths(value) {
  return String(value).split(',').map((item) => item.trim()).filter(Boolean);
}

function pathMatches(inputPath, pattern) {
  const input = String(inputPath).replaceAll('\\', '/');
  const normalizedPattern = String(pattern).replaceAll('\\', '/');
  if (normalizedPattern.endsWith('/**')) {
    const prefix = normalizedPattern.slice(0, -3);
    return input === prefix || input.startsWith(`${prefix}/`);
  }
  return input === normalizedPattern;
}

function parseArgs(argv) {
  const [command = '', ...rest] = argv;
  const args = { command, paths: '' };
  for (let index = 0; index < rest.length; index += 1) {
    const arg = rest[index];
    if (!arg.startsWith('--')) throw new Error(`unknown argument: ${arg}`);
    const key = arg.slice(2);
    const value = rest[index + 1];
    if (!value || value.startsWith('--')) throw new Error(`missing value for ${arg}`);
    args[key] = value;
    index += 1;
  }
  return args;
}

if (process.argv[1] && resolve(process.argv[1]) === currentFile) {
  try {
    const args = parseArgs(process.argv.slice(2));
    let result;
    if (args.command === 'resolve') {
      result = resolveCodeBaselines({
        role: args.role,
        phase: args.phase,
        task: args.task,
        paths: splitPaths(args.paths),
      });
    } else if (args.command === 'render') {
      if (!args.vars) throw new Error('render requires --vars <json-file>');
      result = renderCodeBaseline({
        templateId: args.template,
        targetDir: args.target,
        variables: JSON.parse(readFileSync(resolve(args.vars), 'utf8')),
      });
    } else {
      throw new Error('usage: code-baseline.mjs resolve|render ...');
    }
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    process.stderr.write(`Code baseline failed: ${error.message}\n`);
    process.exitCode = 1;
  }
}
