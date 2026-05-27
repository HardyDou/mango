#!/usr/bin/env node
import { copyFileSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const packageRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(packageRoot, '../../..');
const packagedTemplateRoot = resolve(packageRoot, 'templates/mango-business-starter');
const repoTemplateRoot = resolve(repoRoot, 'mango-business-starter');
const defaultTemplateRoot = existsSync(packagedTemplateRoot) ? packagedTemplateRoot : repoTemplateRoot;

const usage = `
Mango Initializr

Usage:
  npm create mango-app@latest <project> -- [options]
  mango init <project> [options]

Options:
  --module <name>       Business module name, default: project name
  --aggregate <name>    First aggregate name, default: item
  --package <name>      Java base package, default: com.example.business
  --group-id <name>     Maven groupId, default: same as --package
  --version <version>   Project version, default: 1.0.0-SNAPSHOT
  --topology <mode>     monolith or microservice, default: monolith
  --template <path>     Starter directory, default: mango-business-starter
  --force               Overwrite existing target directory
  --help                Show help
`;

function main(argv = process.argv.slice(2)) {
  const args = normalizeCreateArgs(argv);
  if (args.includes('--help') || args.includes('-h')) {
    process.stdout.write(usage.trimStart());
    return;
  }

  const parsed = parseArgs(args);
  if (!parsed.project) {
    fail('missing project name');
  }
  if (!['monolith', 'microservice'].includes(parsed.topology)) {
    fail(`invalid topology: ${parsed.topology}`);
  }

  const targetDir = resolve(process.cwd(), parsed.project);
  const templateRoot = parsed.template ? resolvePath(parsed.template) : defaultTemplateRoot;
  if (!existsSync(templateRoot)) {
    fail(`template directory not found: ${templateRoot}`);
  }
  if (existsSync(targetDir)) {
    if (!parsed.force) {
      fail(`target already exists: ${targetDir}`);
    }
    rmSync(targetDir, { recursive: true, force: true });
  }

  const variables = buildVariables(parsed);
  copyTemplate(templateRoot, targetDir, variables);
  writeMangoConfig(targetDir, variables);
  printNextSteps(targetDir, variables);
}

function normalizeCreateArgs(argv) {
  if (argv[0] === 'init') {
    return argv.slice(1);
  }
  return argv;
}

function parseArgs(argv) {
  const result = {
    project: '',
    module: '',
    aggregate: 'item',
    packageName: 'com.example.business',
    groupId: '',
    version: '1.0.0-SNAPSHOT',
    topology: 'monolith',
    template: '',
    force: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith('--') && !result.project) {
      result.project = arg;
      continue;
    }
    if (arg === '--force') {
      result.force = true;
      continue;
    }
    const next = argv[index + 1];
    if (!next || next.startsWith('--')) {
      fail(`missing value for ${arg}`);
    }
    index += 1;
    switch (arg) {
      case '--module':
        result.module = next;
        break;
      case '--aggregate':
        result.aggregate = next;
        break;
      case '--package':
        result.packageName = next;
        break;
      case '--group-id':
        result.groupId = next;
        break;
      case '--version':
        result.version = next;
        break;
      case '--topology':
        result.topology = next;
        break;
      case '--template':
        result.template = next;
        break;
      default:
        fail(`unknown option: ${arg}`);
    }
  }

  result.project = toKebabCase(result.project || '');
  result.module = toKebabCase(result.module || result.project);
  result.aggregate = toKebabCase(result.aggregate);
  result.groupId = result.groupId || result.packageName;
  return result;
}

function buildVariables(options) {
  const modulePackage = toPackageSegment(options.module);
  const basePackagePath = options.packageName.replaceAll('.', '/');
  return {
    projectKebab: options.project,
    projectPascal: toPascalCase(options.project),
    moduleKebab: options.module,
    modulePackage,
    moduleCamel: toCamelCase(options.module),
    modulePascal: toPascalCase(options.module),
    moduleName: `${toPascalCase(options.module)} Module`,
    moduleKebabSnake: toSnakeCase(options.module),
    aggregateKebab: options.aggregate,
    aggregateCamel: toCamelCase(options.aggregate),
    aggregatePascal: toPascalCase(options.aggregate),
    aggregateKebabSnake: toSnakeCase(options.aggregate),
    basePackage: options.packageName,
    basePackagePath,
    groupId: options.groupId,
    projectVersion: options.version,
    topology: options.topology,
  };
}

function copyTemplate(sourceDir, targetDir, variables) {
  mkdirSync(targetDir, { recursive: true });
  for (const entry of readdirSync(sourceDir)) {
    const source = join(sourceDir, entry);
    const renderedName = render(entry, variables);
    const target = join(targetDir, renderedName);
    const stat = statSync(source);
    if (stat.isDirectory()) {
      copyTemplate(source, target, variables);
      continue;
    }
    const buffer = readFileSync(source);
    if (isTextFile(source)) {
      writeFileSync(target, render(buffer.toString('utf8'), variables));
    } else {
      copyFileSync(source, target);
    }
  }
}

function writeMangoConfig(targetDir, variables) {
  const config = {
    project: variables.projectKebab,
    module: variables.moduleKebab,
    aggregate: variables.aggregateKebab,
    basePackage: variables.basePackage,
    groupId: variables.groupId,
    version: variables.projectVersion,
    topology: variables.topology,
  };
  writeFileSync(join(targetDir, 'mango.config.json'), `${JSON.stringify(config, null, 2)}\n`);
}

function render(value, variables) {
  return Object.entries(variables).reduce((content, [key, replacement]) => {
    return content.replaceAll(`{{${key}}}`, String(replacement));
  }, value);
}

function isTextFile(file) {
  const name = basename(file);
  return /\.(md|json|xml|java|ts|vue|html|sql|properties|mjs|yml|yaml|txt)$/.test(name)
    || name === 'CODEOWNERS'
    || name === 'AGENTS.md';
}

function resolvePath(path) {
  return isAbsolute(path) ? path : resolve(process.cwd(), path);
}

function toKebabCase(value) {
  return value
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase();
}

function toPascalCase(value) {
  return toKebabCase(value)
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
}

function toCamelCase(value) {
  const pascal = toPascalCase(value);
  return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

function toSnakeCase(value) {
  return toKebabCase(value).replaceAll('-', '_');
}

function toPackageSegment(value) {
  return toKebabCase(value).replaceAll('-', '');
}

function printNextSteps(targetDir, variables) {
  const relativeTarget = relativeOrAbsolute(process.cwd(), targetDir);
  process.stdout.write(`Created Mango business project: ${relativeTarget}\n\n`);
  process.stdout.write('Next steps:\n');
  process.stdout.write(`  cd ${relativeTarget}\n`);
  process.stdout.write('  node scripts/check-template.mjs\n');
  process.stdout.write(`  Review topologies/${variables.topology}/README.md\n`);
}

function relativeOrAbsolute(from, to) {
  const relativePath = relative(from, to);
  return relativePath && !relativePath.startsWith('..') ? relativePath : to;
}

function fail(message) {
  console.error(`Error: ${message}`);
  console.error('Run with --help for usage.');
  process.exit(1);
}

main();
