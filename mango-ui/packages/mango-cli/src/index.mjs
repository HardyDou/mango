#!/usr/bin/env node
import { copyFileSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const packageRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(packageRoot, '../../..');
const templateRoot = resolve(packageRoot, 'templates/full');

const defaultVersions = {
  mangoBackend: '1.0.0-SNAPSHOT',
  mangoAdmin: readMangoPackageVersion('admin', '1.0.7'),
  mangoAdminPages: readMangoPackageVersion('admin-pages', '1.0.3'),
  mangoAdminShell: readMangoPackageVersion('admin-shell', '1.0.4'),
  mangoAppRuntime: readMangoPackageVersion('app-runtime', '1.0.2'),
  mangoAuth: readMangoPackageVersion('auth', '1.0.2'),
  mangoCalendar: readMangoPackageVersion('calendar', '1.0.4'),
  mangoCommon: readMangoPackageVersion('common', '1.0.2'),
  mangoFile: readMangoPackageVersion('file', '1.0.4'),
  mangoNotice: readMangoPackageVersion('notice', '1.0.4'),
  mangoNumgen: readMangoPackageVersion('numgen', '1.0.4'),
  mangoRbac: readMangoPackageVersion('rbac', '1.0.2'),
  mangoSystem: readMangoPackageVersion('system', '1.0.2'),
  mangoTemplate: readMangoPackageVersion('template', '1.0.4'),
  mangoWorkflow: readMangoPackageVersion('workflow', '1.0.4'),
  mangoWorkflowBusinessExample: readMangoPackageVersion('workflow-business-example', '1.0.4'),
  vue: '3.5.13',
  vueRouter: '^4.1.6',
  vueI18n: '9.2.2',
  pinia: '2.0.32',
  elementPlus: '2.5.5',
  iconsVue: '^2.0.10',
  vite: '^4.3.3',
  viteVue: '^4.0.0',
  typescript: '^5.9.3',
  vueTsc: '^2.1.10',
  playwright: '^1.59.1',
  nodeTypes: '^20.19.0',
  mavenCompilerPlugin: '3.15.0',
  springBoot: '3.5.14',
  springCloud: '2025.0.1',
};

const usage = `
Mango CLI

Usage:
  mango init <project> --preset full [options]
  mango-cli init <project> --preset full [options]

Options:
  --preset <name>          Preset name. Supported: full
  --topology <mode>        monolith or microservice, default: monolith
  --package <name>         Java base package, default: com.example.mango
  --group-id <name>        Maven groupId, default: same as --package
  --version <version>      Generated project version, default: 1.0.0-SNAPSHOT
  --mango-version <value>  Mango Maven version, default: ${defaultVersions.mangoBackend}
  --npm-registry <url>     NPM registry written to project .npmrc
  --maven-repository <url> Maven repository URL written to generated pom.xml
  --force                  Overwrite existing target directory
  --help                   Show help
`;

function main(argv = process.argv.slice(2)) {
  const args = normalizeArgs(argv);
  if (args.includes('--help') || args.includes('-h')) {
    process.stdout.write(usage.trimStart());
    return;
  }

  const options = parseArgs(args);
  validateOptions(options);

  const targetDir = resolve(process.cwd(), options.project);
  if (existsSync(targetDir)) {
    if (!options.force) {
      fail(`target already exists: ${targetDir}`);
    }
    rmSync(targetDir, { recursive: true, force: true });
  }

  const variables = buildVariables(options);
  copyTemplate(templateRoot, targetDir, variables);
  writeMangoConfig(targetDir, variables);
  printNextSteps(targetDir, variables);
}

function normalizeArgs(argv) {
  if (argv[0] === 'init') {
    return argv.slice(1);
  }
  return argv;
}

function parseArgs(argv) {
  const result = {
    project: '',
    preset: 'full',
    topology: 'monolith',
    packageName: 'com.example.mango',
    groupId: '',
    version: '1.0.0-SNAPSHOT',
    mangoVersion: defaultVersions.mangoBackend,
    npmRegistry: 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/',
    mavenRepository: 'http://nexus.inner.yunxinbaokeji.com/repository/maven-public/',
    force: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith('--') && !result.project) {
      result.project = toKebabCase(arg);
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
      case '--preset':
        result.preset = next;
        break;
      case '--topology':
        result.topology = next;
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
      case '--mango-version':
        result.mangoVersion = next;
        break;
      case '--npm-registry':
        result.npmRegistry = next;
        break;
      case '--maven-repository':
        result.mavenRepository = next;
        break;
      default:
        fail(`unknown option: ${arg}`);
    }
  }

  result.groupId = result.groupId || result.packageName;
  return result;
}

function validateOptions(options) {
  if (!options.project) {
    fail('missing project name');
  }
  if (options.preset !== 'full') {
    fail(`unsupported preset: ${options.preset}`);
  }
  if (!['monolith', 'microservice'].includes(options.topology)) {
    fail(`invalid topology: ${options.topology}`);
  }
  if (!/^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$/.test(options.packageName)) {
    fail(`invalid Java package: ${options.packageName}`);
  }
}

function buildVariables(options) {
  const basePackagePath = options.packageName.replaceAll('.', '/');
  return {
    projectKebab: options.project,
    projectPascal: toPascalCase(options.project),
    projectVersion: options.version,
    groupId: options.groupId,
    basePackage: options.packageName,
    basePackagePath,
    topology: options.topology,
    mangoBackendVersion: options.mangoVersion,
    mangoAdminVersion: defaultVersions.mangoAdmin,
    mangoAdminPagesVersion: defaultVersions.mangoAdminPages,
    mangoAdminShellVersion: defaultVersions.mangoAdminShell,
    mangoAppRuntimeVersion: defaultVersions.mangoAppRuntime,
    mangoAuthVersion: defaultVersions.mangoAuth,
    mangoCalendarVersion: defaultVersions.mangoCalendar,
    mangoCommonVersion: defaultVersions.mangoCommon,
    mangoFileVersion: defaultVersions.mangoFile,
    mangoNoticeVersion: defaultVersions.mangoNotice,
    mangoNumgenVersion: defaultVersions.mangoNumgen,
    mangoRbacVersion: defaultVersions.mangoRbac,
    mangoSystemVersion: defaultVersions.mangoSystem,
    mangoTemplateVersion: defaultVersions.mangoTemplate,
    mangoWorkflowVersion: defaultVersions.mangoWorkflow,
    mangoWorkflowBusinessExampleVersion: defaultVersions.mangoWorkflowBusinessExample,
    vueVersion: defaultVersions.vue,
    vueRouterVersion: defaultVersions.vueRouter,
    vueI18nVersion: defaultVersions.vueI18n,
    piniaVersion: defaultVersions.pinia,
    elementPlusVersion: defaultVersions.elementPlus,
    iconsVueVersion: defaultVersions.iconsVue,
    viteVersion: defaultVersions.vite,
    viteVueVersion: defaultVersions.viteVue,
    typescriptVersion: defaultVersions.typescript,
    vueTscVersion: defaultVersions.vueTsc,
    playwrightVersion: defaultVersions.playwright,
    nodeTypesVersion: defaultVersions.nodeTypes,
    mavenCompilerPluginVersion: defaultVersions.mavenCompilerPlugin,
    springBootVersion: defaultVersions.springBoot,
    springCloudVersion: defaultVersions.springCloud,
    npmRegistry: ensureTrailingSlash(options.npmRegistry),
    mavenRepository: ensureTrailingSlash(options.mavenRepository),
  };
}

function copyTemplate(sourceDir, targetDir, variables) {
  mkdirSync(targetDir, { recursive: true });
  for (const entry of readdirSync(sourceDir)) {
    const source = join(sourceDir, entry);
    const renderedName = renderTemplateFileName(entry, variables);
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
    preset: 'full',
    topology: variables.topology,
    basePackage: variables.basePackage,
    groupId: variables.groupId,
    projectVersion: variables.projectVersion,
    mangoBackendVersion: variables.mangoBackendVersion,
    mangoAdminVersion: variables.mangoAdminVersion,
    mangoFrontendVersions: {
      '@mango/admin': variables.mangoAdminVersion,
      '@mango/admin-pages': variables.mangoAdminPagesVersion,
      '@mango/admin-shell': variables.mangoAdminShellVersion,
      '@mango/app-runtime': variables.mangoAppRuntimeVersion,
      '@mango/auth': variables.mangoAuthVersion,
      '@mango/calendar': variables.mangoCalendarVersion,
      '@mango/common': variables.mangoCommonVersion,
      '@mango/file': variables.mangoFileVersion,
      '@mango/notice': variables.mangoNoticeVersion,
      '@mango/numgen': variables.mangoNumgenVersion,
      '@mango/rbac': variables.mangoRbacVersion,
      '@mango/system': variables.mangoSystemVersion,
      '@mango/template': variables.mangoTemplateVersion,
      '@mango/workflow': variables.mangoWorkflowVersion,
      '@mango/workflow-business-example': variables.mangoWorkflowBusinessExampleVersion,
    },
    npmRegistry: variables.npmRegistry,
    mavenRepository: variables.mavenRepository,
  };
  writeFileSync(join(targetDir, 'mango.config.json'), `${JSON.stringify(config, null, 2)}\n`);
}

function render(value, variables) {
  return Object.entries(variables).reduce((content, [key, replacement]) => {
    return content.replaceAll(`{{${key}}}`, String(replacement));
  }, value);
}

function renderTemplateFileName(value, variables) {
  const rendered = render(value, variables);
  if (rendered === 'npmrc.template') {
    return '.npmrc';
  }
  if (rendered === 'gitignore.template') {
    return '.gitignore';
  }
  return rendered.endsWith('.template') ? rendered.slice(0, -'.template'.length) : rendered;
}

function isTextFile(file) {
  const name = basename(file);
  return /\.(md|json|xml|java|ts|vue|html|mjs|yml|yaml|txt|gitignore|npmrc|properties)$/.test(name)
    || name.endsWith('.template')
    || name === 'CODEOWNERS'
    || name === 'AGENTS.md';
}

function readPackageVersion(path, fallback) {
  const packagePath = resolve(repoRoot, path);
  if (!existsSync(packagePath)) {
    return fallback;
  }
  const content = JSON.parse(readFileSync(packagePath, 'utf8'));
  return content.version || fallback;
}

function readMangoPackageVersion(packageName, fallback) {
  return readPackageVersion(`mango-ui/packages/${packageName}/package.json`, fallback);
}

function ensureTrailingSlash(value) {
  return value.endsWith('/') ? value : `${value}/`;
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

function printNextSteps(targetDir, variables) {
  const relativeTarget = relativeOrAbsolute(process.cwd(), targetDir);
  process.stdout.write(`Created Mango full project: ${relativeTarget}\n\n`);
  process.stdout.write('Next steps:\n');
  process.stdout.write(`  cd ${relativeTarget}\n`);
  process.stdout.write('  npm --prefix frontend install\n');
  process.stdout.write('  npm --prefix frontend run build\n');
  process.stdout.write('  mvn -f backend/pom.xml -DskipTests package\n');
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
