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
  --features <list>     Comma separated Mango features, default: base,system,rbac
  --frontend-mode <mode> local, micro, or mixed, default: local
  --template <path>     Starter directory, default: mango-business-starter
  --force               Overwrite existing target directory
  --help                Show help
`;

const featureCatalog = {
  base: [
    { code: 'auth', packageName: '@mango/auth', capability: 'mangoAuthCapability', importPath: '@mango/auth/capability', moduleCode: 'mango-authorization' },
    { code: 'rbac', packageName: '@mango/rbac', capability: 'mangoRbacCapability', importPath: '@mango/rbac/capability', moduleCode: 'mango-authorization' },
  ],
  system: [
    { code: 'system', packageName: '@mango/system', capability: 'mangoSystemCapability', importPath: '@mango/system/capability', moduleCode: 'mango-system' },
  ],
  rbac: [
    { code: 'rbac', packageName: '@mango/rbac', capability: 'mangoRbacCapability', importPath: '@mango/rbac/capability', moduleCode: 'mango-authorization' },
  ],
  workflow: [
    { code: 'workflow', packageName: '@mango/workflow', capability: 'mangoWorkflowCapability', importPath: '@mango/workflow/capability', moduleCode: 'mango-workflow' },
  ],
  notice: [
    { code: 'notice', packageName: '@mango/notice', capability: 'mangoNoticeCapability', importPath: '@mango/notice/capability', moduleCode: 'mango-notice' },
  ],
  file: [
    { code: 'file', packageName: '@mango/file', capability: 'mangoFileCapability', importPath: '@mango/file/capability', moduleCode: 'mango-file' },
  ],
  template: [
    { code: 'template', packageName: '@mango/template', capability: 'mangoTemplateCapability', importPath: '@mango/template/capability', moduleCode: 'mango-template' },
  ],
  numgen: [
    { code: 'numgen', packageName: '@mango/numgen', capability: 'mangoNumgenCapability', importPath: '@mango/numgen/capability', moduleCode: 'mango-numgen' },
  ],
  calendar: [
    { code: 'calendar', packageName: '@mango/calendar', capability: 'mangoCalendarCapability', importPath: '@mango/calendar/capability', moduleCode: 'mango-calendar' },
  ],
};

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
  if (!['local', 'micro', 'mixed'].includes(parsed.frontendMode)) {
    fail(`invalid frontend mode: ${parsed.frontendMode}`);
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
    features: 'base,system,rbac',
    frontendMode: 'local',
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
      case '--features':
        result.features = next;
        break;
      case '--frontend-mode':
        result.frontendMode = next;
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
  result.features = normalizeFeatures(result.features);
  result.frontendMode = toKebabCase(result.frontendMode || 'local');
  return result;
}

function buildVariables(options) {
  const modulePackage = toPackageSegment(options.module);
  const basePackagePath = options.packageName.replaceAll('.', '/');
  const featureEntries = resolveFeatureEntries(options.features);
  const runtimeModules = buildRuntimeModules(options, featureEntries);
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
    featuresCsv: options.features.join(','),
    frontendMode: options.frontendMode,
    runtimeProfile: toRuntimeProfile(options.frontendMode),
    mangoFeatureDependencies: renderFeatureDependencies(featureEntries),
    mangoFeatureImports: renderFeatureImports(featureEntries),
    mangoFeatureCapabilities: renderFeatureCapabilities(featureEntries),
    mangoRuntimeModulesTs: renderRuntimeModulesTs(runtimeModules),
    mangoRuntimeModulesJson: renderRuntimeModulesJson(runtimeModules),
    mangoFeatureEnv: renderFeatureEnv(options, featureEntries),
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
    features: variables.featuresCsv.split(',').filter(Boolean),
    frontend: {
      app: `${variables.projectKebab}-admin`,
      defaultMode: variables.frontendMode,
      supportedModes: ['local', 'micro', 'mixed'],
      runtimeConfig: `frontend/apps/${variables.projectKebab}-admin/public/mango-runtime-config.json`,
    },
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
  return /\.(md|json|xml|java|ts|vue|html|sql|properties|mjs|sh|yml|yaml|txt)$/.test(name)
    || name === 'CODEOWNERS'
    || name === 'AGENTS.md'
    || name.endsWith('.example')
    || name.endsWith('.imports');
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
  process.stdout.write('  pnpm install\n');
  process.stdout.write('  pnpm typecheck\n');
  process.stdout.write('  pnpm build\n');
  process.stdout.write(`  Frontend mode: ${variables.frontendMode}; features: ${variables.featuresCsv}\n`);
  process.stdout.write(`  Review topologies/${variables.topology}/README.md\n`);
}

function normalizeFeatures(value) {
  const requested = (value || 'base,system,rbac')
    .split(',')
    .map(item => toKebabCase(item))
    .filter(Boolean);
  const features = requested.length ? requested : ['base'];
  const unknown = features.filter(feature => !featureCatalog[feature]);
  if (unknown.length) {
    fail(`invalid features: ${unknown.join(', ')}`);
  }
  return [...new Set(features)];
}

function resolveFeatureEntries(features) {
  const entries = [];
  const seenPackages = new Set();
  for (const feature of features) {
    for (const entry of featureCatalog[feature]) {
      if (seenPackages.has(entry.packageName)) {
        continue;
      }
      seenPackages.add(entry.packageName);
      entries.push(entry);
    }
  }
  return entries;
}

function buildRuntimeModules(options, featureEntries) {
  const modules = new Map();
  modules.set(options.module, {
    moduleCode: options.module,
    mode: options.frontendMode === 'micro' ? 'micro' : 'local',
    entry: `import.meta.env.VITE_${toSnakeCase(options.module).toUpperCase()}_ENTRY`,
    runtimeCode: `${options.project}-${options.module}`,
    appType: options.frontendMode === 'micro' ? 'MICRO_APP' : 'LOCAL',
    version: options.version,
    healthCheckUrl: `import.meta.env.VITE_${toSnakeCase(options.module).toUpperCase()}_HEALTH_CHECK_URL`,
  });

  for (const entry of featureEntries) {
    if (!modules.has(entry.moduleCode)) {
      const mode = options.frontendMode === 'micro' ? 'micro' : 'local';
      modules.set(entry.moduleCode, {
        moduleCode: entry.moduleCode,
        mode,
        entry: `import.meta.env.VITE_${toSnakeCase(entry.code).toUpperCase()}_ENTRY`,
        runtimeCode: `${entry.moduleCode}-${mode}`,
        appType: mode === 'micro' ? 'MICRO_APP' : 'LOCAL',
        version: '1.0.0',
        healthCheckUrl: `import.meta.env.VITE_${toSnakeCase(entry.code).toUpperCase()}_HEALTH_CHECK_URL`,
      });
    }
  }

  if (options.frontendMode === 'mixed') {
    for (const module of modules.values()) {
      if (module.moduleCode === options.module || module.moduleCode === 'mango-workflow' || module.moduleCode === 'mango-notice') {
        module.mode = 'micro';
        module.appType = 'MICRO_APP';
        if (module.moduleCode !== options.module) {
          module.runtimeCode = `${module.moduleCode}-micro`;
        }
      }
    }
  }

  return [...modules.values()];
}

function renderFeatureDependencies(entries) {
  return entries.map(entry => `    \"${entry.packageName}\": \"^1.0.0\"`).join(',\n');
}

function renderFeatureImports(entries) {
  return entries.map(entry => `import { ${entry.capability} } from '${entry.importPath}';`).join('\n');
}

function renderFeatureCapabilities(entries) {
  return entries.map(entry => `  ${entry.capability}`).join(',\n');
}

function renderRuntimeModulesTs(modules) {
  return modules.map(module => {
    const entryLine = module.appType === 'MICRO_APP' ? `\n      entry: ${module.entry},` : '';
    const healthCheckLine = module.appType === 'MICRO_APP' ? `\n      healthCheckUrl: ${module.healthCheckUrl},` : '';
    return `    '${module.moduleCode}': {\n      mode: '${module.mode}',${entryLine}\n      runtimeCode: '${module.runtimeCode}',\n      appType: '${module.appType}',\n      framework: 'vue3',\n      version: '${module.version}',${healthCheckLine}\n    }`;
  }).join(',\n');
}

function renderRuntimeModulesJson(modules) {
  return modules.map(module => {
    const lines = [
      `    \"${module.moduleCode}\": {`,
      `      \"mode\": \"${module.mode}\",`,
      ...(module.appType === 'MICRO_APP' ? [`      \"entry\": \"${renderRuntimeJsonEntry(module)}\",`] : []),
      `      \"runtimeCode\": \"${module.runtimeCode}\",`,
      `      \"appType\": \"${module.appType}\",`,
      '      "framework": "vue3",',
      `      \"version\": \"${module.version}\"${module.appType === 'MICRO_APP' ? ',' : ''}`,
      ...(module.appType === 'MICRO_APP' ? [`      \"healthCheckUrl\": \"${renderRuntimeJsonHealthCheck(module)}\"`] : []),
      '    }',
    ];
    return lines.join('\n');
  }).join(',\n');
}

function renderFeatureEnv(options, entries) {
  const env = [
    `VITE_${toSnakeCase(options.module).toUpperCase()}_MODE=${resolveRuntimeModuleMode(options, options.module)}`,
    `VITE_${toSnakeCase(options.module).toUpperCase()}_ENTRY=http://127.0.0.1:5190/`,
    `VITE_${toSnakeCase(options.module).toUpperCase()}_HEALTH_CHECK_URL=http://127.0.0.1:5190/health.json`,
  ];
  for (const entry of entries) {
    env.push(`VITE_${toSnakeCase(entry.code).toUpperCase()}_MODE=${resolveRuntimeModuleMode(options, entry.moduleCode)}`);
    env.push(`VITE_${toSnakeCase(entry.code).toUpperCase()}_ENTRY=http://127.0.0.1:5190/`);
    env.push(`VITE_${toSnakeCase(entry.code).toUpperCase()}_HEALTH_CHECK_URL=http://127.0.0.1:5190/health.json`);
  }
  return [...new Set(env)].join('\n');
}

function renderRuntimeJsonEntry(module) {
  void module;
  return 'http://127.0.0.1:5190/';
}

function renderRuntimeJsonHealthCheck(module) {
  void module;
  return 'http://127.0.0.1:5190/health.json';
}

function resolveRuntimeModuleMode(options, moduleCode) {
  if (options.frontendMode === 'micro') {
    return 'micro';
  }
  if (options.frontendMode === 'mixed' && (moduleCode === options.module || moduleCode === 'mango-workflow' || moduleCode === 'mango-notice')) {
    return 'micro';
  }
  return 'local';
}

function toRuntimeProfile(frontendMode) {
  if (frontendMode === 'micro') {
    return 'micro';
  }
  if (frontendMode === 'mixed') {
    return 'hybrid';
  }
  return 'monolith';
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
