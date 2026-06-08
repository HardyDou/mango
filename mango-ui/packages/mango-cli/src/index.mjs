#!/usr/bin/env node
import { chmodSync, copyFileSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const packageRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(packageRoot, '../../..');
const templateRoot = resolve(packageRoot, 'templates/full');
const businessModuleTemplateRoot = resolve(packageRoot, 'templates/business-module');
const businessStarterRoot = existsSync(businessModuleTemplateRoot)
  ? businessModuleTemplateRoot
  : resolve(repoRoot, 'mango-business-starter');
const releaseVersions = readReleaseVersions();

const defaultVersions = {
  mangoBackend: releaseVersions.maven?.mangoBackend || '1.0.0-SNAPSHOT',
  mangoAdmin: readReleasedMangoPackageVersion('admin', '1.0.11'),
  mangoAdminPages: readReleasedMangoPackageVersion('admin-pages', '1.0.5'),
  mangoAdminShell: readReleasedMangoPackageVersion('admin-shell', '1.0.10'),
  mangoAppRuntime: readReleasedMangoPackageVersion('app-runtime', '1.0.2'),
  mangoAuth: readReleasedMangoPackageVersion('auth', '1.0.4'),
  mangoCalendar: readReleasedMangoPackageVersion('calendar', '1.0.6'),
  mangoCommon: readReleasedMangoPackageVersion('common', '1.0.7'),
  mangoFile: readReleasedMangoPackageVersion('file', '1.0.6'),
  mangoNotice: readReleasedMangoPackageVersion('notice', '1.0.6'),
  mangoNumgen: readReleasedMangoPackageVersion('numgen', '1.0.6'),
  mangoRbac: readReleasedMangoPackageVersion('rbac', '1.0.4'),
  mangoSystem: readReleasedMangoPackageVersion('system', '1.0.4'),
  mangoTemplate: readReleasedMangoPackageVersion('template', '1.0.6'),
  mangoWorkflow: readReleasedMangoPackageVersion('workflow', '1.0.6'),
  mangoWorkflowBusinessExample: readReleasedMangoPackageVersion('workflow-business-example', '1.0.6'),
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
  springdocOpenapi: '2.8.9',
  swaggerAnnotations: '2.2.30',
};

const CORE_FRONTEND_PACKAGES = [
  { name: '@mango/admin', versionKey: 'mangoAdmin' },
  { name: '@mango/admin-pages', versionKey: 'mangoAdminPages' },
  { name: '@mango/admin-shell', versionKey: 'mangoAdminShell' },
  { name: '@mango/app-runtime', versionKey: 'mangoAppRuntime' },
  { name: '@mango/auth', versionKey: 'mangoAuth' },
  { name: '@mango/common', versionKey: 'mangoCommon' },
  { name: '@mango/rbac', versionKey: 'mangoRbac' },
  { name: '@mango/system', versionKey: 'mangoSystem' },
];

const ADMIN_OPTIONAL_PEER_PACKAGES = [
  { name: '@mango/calendar', versionKey: 'mangoCalendar' },
  { name: '@mango/file', versionKey: 'mangoFile' },
  { name: '@mango/notice', versionKey: 'mangoNotice' },
  { name: '@mango/numgen', versionKey: 'mangoNumgen' },
  { name: '@mango/template', versionKey: 'mangoTemplate' },
  { name: '@mango/workflow', versionKey: 'mangoWorkflow' },
  { name: '@mango/workflow-business-example', versionKey: 'mangoWorkflowBusinessExample' },
];

const CORE_BACKEND_DEPENDENCIES = [
  { groupId: 'io.mango.common', artifactId: 'mango-common' },
  { groupId: 'io.mango.infra.module', artifactId: 'mango-infra-module-starter' },
  { groupId: 'io.mango.infra.kv', artifactId: 'mango-infra-kv-starter' },
  { groupId: 'io.mango.infra.event', artifactId: 'mango-infra-event-starter' },
  { groupId: 'io.mango.infra.ip.location', artifactId: 'mango-infra-ip-location-starter' },
  { groupId: 'io.mango.infra.realtime', artifactId: 'mango-infra-realtime-starter' },
  { groupId: 'io.mango.infra.persistence', artifactId: 'mango-infra-persistence-web-starter' },
  { groupId: 'io.mango.infra.doc', artifactId: 'mango-infra-doc-starter' },
  { groupId: 'io.mango.infra.web', artifactId: 'mango-infra-web-starter' },
  { groupId: 'io.mango.platform.auth', artifactId: 'mango-auth-starter' },
  { groupId: 'io.mango.platform.identity', artifactId: 'mango-identity-starter' },
  { groupId: 'io.mango.platform.authorization', artifactId: 'mango-authorization-starter' },
  { groupId: 'io.mango.platform.authorization', artifactId: 'mango-authorization-resource-sync-starter' },
  { groupId: 'io.mango.platform.authorization', artifactId: 'mango-authorization-resource-access-starter' },
  { groupId: 'io.mango.platform.access', artifactId: 'mango-access-web-starter' },
  { groupId: 'io.mango.platform.org', artifactId: 'mango-org-starter' },
  { groupId: 'io.mango.platform.captcha', artifactId: 'mango-captcha-starter' },
  { groupId: 'io.mango.platform.system', artifactId: 'mango-system-starter' },
];

const BUSINESS_BACKEND_MANAGED_DEPENDENCIES = [
  { groupId: 'io.mango.common', artifactId: 'mango-common' },
  { groupId: 'io.mango.infra.web', artifactId: 'mango-infra-web-starter' },
  { groupId: 'io.mango.infra.persistence', artifactId: 'mango-infra-persistence-starter' },
  { groupId: 'io.mango.infra.persistence', artifactId: 'mango-infra-persistence-web-starter' },
  { groupId: 'io.mango.infra.feign', artifactId: 'mango-infra-feign-starter' },
  {
    groupId: 'org.springdoc',
    artifactId: 'springdoc-openapi-starter-common',
    version: '${springdoc-openapi.version}',
  },
  {
    groupId: 'io.swagger.core.v3',
    artifactId: 'swagger-annotations',
    version: '${swagger-annotations.version}',
  },
];

const OPTIONAL_MODULES = [
  {
    code: 'file',
    label: '文件中心',
    feature: 'file',
    frontendPackage: '@mango/file',
    versionKey: 'mangoFile',
    styleImport: '@mango/file/style.css',
    registrarImport: "import { registerMangoFileAdminPages } from '@mango/file/admin-pages';",
    registrar: 'registerMangoFileAdminPages',
    backend: [
      { groupId: 'io.mango.platform.file', artifactId: 'mango-file-starter' },
      { groupId: 'io.mango.platform.file.preview', artifactId: 'mango-file-preview-starter' },
    ],
  },
  {
    code: 'template',
    label: '模板管理',
    feature: 'template',
    frontendPackage: '@mango/template',
    versionKey: 'mangoTemplate',
    styleImport: '@mango/template/style.css',
    registrarImport: "import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';",
    registrar: 'registerMangoTemplateAdminPages',
    runtimeModule: {
      moduleCode: 'mango-template',
      local: { mode: 'local', runtimeCode: 'mango-admin-template-local' },
      micro: { mode: 'micro', runtimeCode: 'mango-admin-template-app', entry: 'http://d.mango.io:5183/' },
    },
    backend: [
      { groupId: 'io.mango.platform.template', artifactId: 'mango-template-starter' },
    ],
  },
  {
    code: 'notice',
    label: '通知中心',
    feature: 'notice',
    frontendPackage: '@mango/notice',
    versionKey: 'mangoNotice',
    styleImport: '@mango/notice/style.css',
    registrarImport: [
      "import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';",
      "import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';",
    ],
    registrar: ['registerMangoNoticeAdminPages', 'registerMangoNoticeAdminShell'],
    backend: [
      { groupId: 'io.mango.platform.notice', artifactId: 'mango-notice-starter' },
    ],
  },
  {
    code: 'numgen',
    label: '编号规则',
    feature: 'numgen',
    frontendPackage: '@mango/numgen',
    versionKey: 'mangoNumgen',
    styleImport: '@mango/numgen/style.css',
    registrarImport: "import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';",
    registrar: 'registerMangoNumgenAdminPages',
    backend: [
      { groupId: 'io.mango.platform.numgen', artifactId: 'mango-numgen-starter' },
    ],
  },
  {
    code: 'calendar',
    label: '工作日历',
    feature: 'calendar',
    frontendPackage: '@mango/calendar',
    versionKey: 'mangoCalendar',
    styleImport: '@mango/calendar/style.css',
    registrarImport: "import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';",
    registrar: 'registerMangoCalendarAdminPages',
    backend: [
      { groupId: 'io.mango.platform.calendar', artifactId: 'mango-calendar-starter' },
    ],
  },
  {
    code: 'workflow',
    label: '审批中心',
    feature: 'workflow',
    frontendPackage: '@mango/workflow',
    versionKey: 'mangoWorkflow',
    styleImport: '@mango/workflow/style.css',
    registrarImport: "import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';",
    registrar: 'registerMangoWorkflowAdminPages',
    runtimeModule: {
      moduleCode: 'mango-workflow',
      local: { mode: 'local', runtimeCode: 'mango-admin-workflow-local' },
      micro: { mode: 'micro', runtimeCode: 'mango-admin-workflow-app', entry: 'http://c.mango.io:5182/' },
    },
    backend: [
      { groupId: 'io.mango.platform.workflow', artifactId: 'mango-workflow-starter' },
    ],
  },
  {
    code: 'workflow-example',
    label: '审批示例',
    frontendPackage: '@mango/workflow-business-example',
    versionKey: 'mangoWorkflowBusinessExample',
    styleImport: '@mango/workflow-business-example/style.css',
    registrarImport: "import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';",
    registrar: 'registerMangoWorkflowBusinessExampleAdminPages',
    dependsOn: ['workflow'],
  },
];

const MODULE_BY_CODE = new Map(OPTIONAL_MODULES.map(module => [module.code, module]));
const FULL_MODULE_CODES = OPTIONAL_MODULES.map(module => module.code);

const usage = `
Mango CLI

Usage:
  mango init <project> --preset full [options]
  mango init <project> --preset custom --modules workflow,template [options]
  mango add <module...> [options]
  mango module add <module> --aggregate <name> [--aggregate-name <name>] [options]
  mango-cli init <project> --preset full [options]
  mango-cli add <module...> [options]

Options:
  --preset <name>          Preset name. Supported: full, custom
  --modules <list>         Optional module codes for custom preset. Use comma list, all, or none
  --topology <mode>        monolith or microservice, default: monolith
  --package <name>         Java base package, default: com.example.mango
  --group-id <name>        Maven groupId, default: same as --package
  --version <version>      Generated project version, default: 1.0.0-SNAPSHOT
  --mango-version <value>  Mango Maven version, default: ${defaultVersions.mangoBackend}
  --npm-registry <url>     NPM registry written to project .npmrc
  --maven-repository <url> Maven repository URL written to generated pom.xml
  --aggregate <name>       Business aggregate name for mango module add
  --aggregate-name <name>  Business aggregate display name for mango module add
  --module-name <name>     Business module display name for mango module add
  --force                  Overwrite existing target directory
  --help                   Show help

Modules:
  ${OPTIONAL_MODULES.map(module => `${module.code.padEnd(16)} ${module.label}`).join('\n  ')}
`;

function main(argv = process.argv.slice(2)) {
  const args = normalizeArgs(argv);
  if (args.includes('--help') || args.includes('-h')) {
    process.stdout.write(usage.trimStart());
    return;
  }

  if (args[0] === 'module') {
    const subCommand = args[1];
    if (subCommand !== 'add') {
      fail(`unknown module command: ${subCommand || ''}`);
    }
    addBusinessModule(args.slice(2));
    return;
  }

  const command = args[0] === 'add' ? 'add' : 'init';
  const commandArgs = command === 'add' ? args.slice(1) : args;

  if (command === 'add') {
    addModules(commandArgs);
    return;
  }

  const options = parseArgs(commandArgs);
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
  chmodSync(join(targetDir, 'scripts/backend-dev.sh'), 0o755);
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
    modules: '',
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
      case '--modules':
        result.modules = next;
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
  if (!['full', 'custom'].includes(options.preset)) {
    fail(`unsupported preset: ${options.preset}`);
  }
  if (!['monolith', 'microservice'].includes(options.topology)) {
    fail(`invalid topology: ${options.topology}`);
  }
  if (!/^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$/.test(options.packageName)) {
    fail(`invalid Java package: ${options.packageName}`);
  }
  resolveSelectedModules(options);
}

function buildVariables(options) {
  const basePackagePath = options.packageName.replaceAll('.', '/');
  const selectedModules = resolveSelectedModules(options);
  const optionalDependencies = uniqueBy(
    [
      ...ADMIN_OPTIONAL_PEER_PACKAGES,
      ...selectedModules
        .filter(module => module.frontendPackage)
        .map(module => ({ name: module.frontendPackage, versionKey: module.versionKey })),
    ],
    dependency => dependency.name,
  );
  const frontendVersions = Object.fromEntries(
    [...CORE_FRONTEND_PACKAGES, ...optionalDependencies].map(dependency => [
      dependency.name,
      defaultVersions[dependency.versionKey],
    ]),
  );
  const variables = {
    projectKebab: options.project,
    projectPascal: toPascalCase(options.project),
    projectVersion: options.version,
    groupId: options.groupId,
    basePackage: options.packageName,
    basePackagePath,
    topology: options.topology,
    preset: options.preset,
    presetLabel: toPascalCase(options.preset),
    selectedModules,
    selectedModuleCodes: selectedModules.map(module => module.code),
    frontendVersions,
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
    springdocOpenapiVersion: defaultVersions.springdocOpenapi,
    swaggerAnnotationsVersion: defaultVersions.swaggerAnnotations,
    npmRegistry: ensureTrailingSlash(options.npmRegistry),
    mavenRepository: ensureTrailingSlash(options.mavenRepository),
    mangoBaselineCommit: readMangoBaselineCommit(),
  };
  return {
    ...variables,
    frontendModuleDependencies: renderFrontendModuleDependencies(selectedModules),
    frontendEntryImports: renderFrontendEntryImports(options.preset, selectedModules),
    frontendFeaturesExpression: renderFrontendFeaturesExpression(options.preset, selectedModules),
    frontendFeatureRegistrarsExpression: renderFrontendFeatureRegistrarsExpression(options.preset, selectedModules),
    backendManagedDependencies: renderBackendManagedDependencies(options.preset, selectedModules),
    backendDependencies: renderBackendDependencies(options.preset, selectedModules),
    runtimeModulesJson: renderRuntimeModulesJson(selectedModules, 'local'),
    runtimeModulesMicroserviceJson: renderRuntimeModulesJson(selectedModules, 'micro'),
    backendBusinessModules: '',
    backendBusinessDependencies: '',
    backendBusinessFlywayModules: '',
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

function renderTemplateToFile(templateRelativePath, targetDir, variables) {
  const source = join(templateRoot, templateRelativePath);
  const target = join(targetDir, renderTemplateFileName(templateRelativePath, variables));
  mkdirSync(dirname(target), { recursive: true });
  const buffer = readFileSync(source);
  if (isTextFile(source)) {
    writeFileSync(target, render(buffer.toString('utf8'), variables));
    return;
  }
  copyFileSync(source, target);
}

function addModules(argv) {
  const options = parseAddArgs(argv);
  const targetDir = resolve(process.cwd(), options.projectDir);
  const configPath = join(targetDir, 'mango.config.json');
  if (!existsSync(configPath)) {
    fail(`mango.config.json not found in ${targetDir}`);
  }
  const config = JSON.parse(readFileSync(configPath, 'utf8'));
  if (config.preset === 'full') {
    fail('full preset already includes all optional modules; use custom preset for additive module management');
  }
  const existingCodes = Array.isArray(config.modules?.optional) ? config.modules.optional : [];
  const modulesToAdd = resolveModuleCodes(options.modules);
  const mergedCodes = resolveModuleCodes([...existingCodes, ...modulesToAdd].join(','));
  const projectOptions = {
    project: config.project || basename(targetDir),
    preset: config.preset === 'full' ? 'full' : 'custom',
    topology: config.topology || 'monolith',
    packageName: config.basePackage || 'com.example.mango',
    groupId: config.groupId || config.basePackage || 'com.example.mango',
    version: config.projectVersion || '1.0.0-SNAPSHOT',
    mangoVersion: config.mangoBackendVersion || defaultVersions.mangoBackend,
    npmRegistry: config.npmRegistry || 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/',
    mavenRepository: config.mavenRepository || 'http://nexus.inner.yunxinbaokeji.com/repository/maven-public/',
    modules: mergedCodes.join(','),
  };
  const variables = buildVariables(projectOptions);
  updateFrontendPackage(targetDir, variables);
  updateFrontendEntry(targetDir, variables);
  updateRuntimeConfigFiles(targetDir, variables);
  updateBackendPom(targetDir, variables);
  writeMangoConfig(targetDir, variables);
  process.stdout.write(`Added Mango modules: ${modulesToAdd.join(', ')}\n`);
}

function addBusinessModule(argv) {
  const options = parseBusinessModuleArgs(argv);
  const targetDir = resolve(process.cwd(), options.projectDir);
  const configPath = join(targetDir, 'mango.config.json');
  if (!existsSync(configPath)) {
    fail(`mango.config.json not found in ${targetDir}`);
  }
  const config = JSON.parse(readFileSync(configPath, 'utf8'));
  const moduleKebab = toKebabCase(options.module);
  const aggregateKebab = toKebabCase(options.aggregate);
  if (!moduleKebab) {
    fail('missing business module name');
  }
  if (!aggregateKebab) {
    fail('missing business aggregate name');
  }
  const moduleTarget = join(targetDir, 'backend/modules', moduleKebab);
  if (existsSync(moduleTarget) && !options.force) {
    fail(`business module already exists: ${moduleKebab}`);
  }
  const variables = {
    ...buildVariables({
      project: config.project || basename(targetDir),
      preset: config.preset || 'custom',
      topology: config.topology || 'monolith',
      packageName: config.basePackage || 'com.example.mango',
      groupId: config.groupId || config.basePackage || 'com.example.mango',
      version: config.projectVersion || '1.0.0-SNAPSHOT',
      mangoVersion: config.mangoBackendVersion || defaultVersions.mangoBackend,
      npmRegistry: config.npmRegistry || 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/',
      mavenRepository: config.mavenRepository || 'http://nexus.inner.yunxinbaokeji.com/repository/maven-public/',
      modules: (config.modules?.optional || []).join(','),
    }),
    moduleKebab,
    modulePackage: toJavaSegment(moduleKebab),
    modulePascal: toPascalCase(moduleKebab),
    moduleCamel: toCamelCase(moduleKebab),
    moduleName: options.moduleName || `${toPascalCase(moduleKebab)}模块`,
    moduleKebabSnake: toSnakeCase(moduleKebab),
    aggregateKebab,
    aggregateKebabSnake: toSnakeCase(aggregateKebab),
    aggregatePascal: toPascalCase(aggregateKebab),
    aggregateCamel: toCamelCase(aggregateKebab),
    aggregateName: options.aggregateName || toPascalCase(aggregateKebab),
    backendBusinessFlywayModules: '',
  };
  copyTemplate(join(businessStarterRoot, 'backend/modules/{{moduleKebab}}'), moduleTarget, variables);
  copyTemplate(join(businessStarterRoot, 'frontend/packages/{{moduleKebab}}-api'), join(targetDir, 'frontend/packages', `${moduleKebab}-api`), variables);
  copyTemplate(join(businessStarterRoot, 'frontend/packages/{{moduleKebab}}'), join(targetDir, 'frontend/packages', moduleKebab), variables);
  assertNoUnrenderedPlaceholders(targetDir, [
    `backend/modules/${moduleKebab}`,
    `frontend/packages/${moduleKebab}-api`,
    `frontend/packages/${moduleKebab}`,
  ]);
  updateBackendBusinessIntegration(targetDir, variables);
  updateFrontendBusinessIntegration(targetDir, variables);
  updateBusinessConfig(targetDir, config, variables);
  process.stdout.write(`Added business module: ${moduleKebab} (${aggregateKebab})\n`);
}

function parseBusinessModuleArgs(argv) {
  const result = {
    projectDir: '.',
    module: '',
    aggregate: '',
    aggregateName: '',
    moduleName: '',
    force: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--force') {
      result.force = true;
      continue;
    }
    if (['--project-dir', '--aggregate', '--aggregate-name', '--module-name'].includes(arg)) {
      const next = argv[index + 1];
      if (!next || next.startsWith('--')) {
        fail(`missing value for ${arg}`);
      }
      index += 1;
      if (arg === '--project-dir') {
        result.projectDir = next;
      } else if (arg === '--aggregate') {
        result.aggregate = next;
      } else if (arg === '--aggregate-name') {
        result.aggregateName = next;
      } else {
        result.moduleName = next;
      }
      continue;
    }
    if (arg.startsWith('--')) {
      fail(`unknown option: ${arg}`);
    }
    if (!result.module) {
      result.module = arg;
      continue;
    }
    fail(`unexpected argument: ${arg}`);
  }
  return result;
}

function parseAddArgs(argv) {
  const result = {
    projectDir: '.',
    modules: [],
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--project-dir') {
      const next = argv[index + 1];
      if (!next || next.startsWith('--')) {
        fail(`missing value for ${arg}`);
      }
      result.projectDir = next;
      index += 1;
      continue;
    }
    if (arg.startsWith('--')) {
      fail(`unknown option: ${arg}`);
    }
    result.modules.push(arg);
  }
  if (result.modules.length === 0) {
    fail('missing module code for add command');
  }
  return result;
}

function writeMangoConfig(targetDir, variables) {
  const config = {
    project: variables.projectKebab,
    preset: variables.preset,
    topology: variables.topology,
    basePackage: variables.basePackage,
    groupId: variables.groupId,
    projectVersion: variables.projectVersion,
    mangoBackendVersion: variables.mangoBackendVersion,
    mangoAdminVersion: variables.mangoAdminVersion,
    modules: {
      required: ['authorization', 'system'],
      optional: variables.selectedModuleCodes,
    },
    mangoFrontendVersions: variables.frontendVersions,
    npmRegistry: variables.npmRegistry,
    mavenRepository: variables.mavenRepository,
  };
  writeFileSync(join(targetDir, 'mango.config.json'), `${JSON.stringify(config, null, 2)}\n`);
}

function updateFrontendPackage(targetDir, variables) {
  const packagePath = join(targetDir, 'frontend/package.json');
  const packageJson = JSON.parse(readFileSync(packagePath, 'utf8'));
  packageJson.dependencies = packageJson.dependencies || {};
  for (const [dependency, version] of Object.entries(variables.frontendVersions)) {
    packageJson.dependencies[dependency] = version;
  }
  writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`);
}

function updateFrontendEntry(targetDir, variables) {
  const entryPath = join(targetDir, 'frontend/src/main.ts');
  const content = readFileSync(entryPath, 'utf8');
  const nextContent = replaceManagedBlock(
    replaceManagedBlock(content, 'imports', variables.frontendEntryImports),
    'features',
    [
      `const mangoFeatures = ${variables.frontendFeaturesExpression};`,
      `const mangoFeatureRegistrars = ${variables.frontendFeatureRegistrarsExpression};`,
    ].join('\n'),
  );
  writeFileSync(entryPath, nextContent);
}

function updateRuntimeConfigFiles(targetDir, variables) {
  const configFiles = [
    ['frontend/public/runtime-config.json', variables.runtimeModulesJson],
    ['frontend/public/runtime-config.monolith.json', variables.runtimeModulesJson],
    ['frontend/public/runtime-config.microservice.json', variables.runtimeModulesMicroserviceJson],
  ];
  for (const [relativePath, modulesJson] of configFiles) {
    const configPath = join(targetDir, relativePath);
    const config = JSON.parse(readFileSync(configPath, 'utf8'));
    config.modules = JSON.parse(modulesJson);
    writeFileSync(configPath, `${JSON.stringify(config, null, 2)}\n`);
  }
}

function updateBackendPom(targetDir, variables) {
  const parentPomPath = join(targetDir, 'backend/pom.xml');
  const appPomPath = join(targetDir, 'backend/app/pom.xml');
  const nextParentPom = replaceXmlManagedBlock(
    readFileSync(parentPomPath, 'utf8'),
    'managed-dependencies',
    variables.backendManagedDependencies,
    'backend/pom.xml',
  );
  const nextAppPom = replaceXmlManagedBlock(
    readFileSync(appPomPath, 'utf8'),
    'dependencies',
    variables.backendDependencies,
    'backend/app/pom.xml',
  );
  writeFileSync(parentPomPath, nextParentPom);
  writeFileSync(appPomPath, nextAppPom);
}

function updateBackendBusinessIntegration(targetDir, variables) {
  const backendPomPath = join(targetDir, 'backend/pom.xml');
  const appPomPath = join(targetDir, 'backend/app/pom.xml');
  const moduleLine = `        <module>modules/${variables.moduleKebab}</module>`;
  const dependencyXml = [
    '        <dependency>',
    `            <groupId>${variables.groupId}</groupId>`,
    `            <artifactId>${variables.moduleKebab}-starter</artifactId>`,
    `            <version>${variables.projectVersion}</version>`,
    '        </dependency>',
  ].join('\n');
  writeFileSync(backendPomPath, appendManagedLine(readFileSync(backendPomPath, 'utf8'), 'business-modules', moduleLine));
  writeFileSync(appPomPath, appendManagedLine(readFileSync(appPomPath, 'utf8'), 'business-dependencies', dependencyXml));
  updateBackendBusinessFlywayConfig(targetDir, variables);
}

function updateBackendBusinessFlywayConfig(targetDir, variables) {
  const applicationPath = join(targetDir, 'backend/app/src/main/resources/application.yml');
  const flywayModuleBlock = [
    `        ${variables.moduleKebab}:`,
    '          enabled: true',
  ].join('\n');
  writeFileSync(
    applicationPath,
    appendYamlManagedBlock(readFileSync(applicationPath, 'utf8'), 'business-flyway-modules', flywayModuleBlock),
  );
}

function updateFrontendBusinessIntegration(targetDir, variables) {
  const packagePath = join(targetDir, 'frontend/package.json');
  const packageJson = JSON.parse(readFileSync(packagePath, 'utf8'));
  packageJson.dependencies = packageJson.dependencies || {};
  packageJson.workspaces = ensureWorkspace(packageJson.workspaces, 'packages/*');
  packageJson.dependencies[`@${variables.projectKebab}/${variables.moduleKebab}`] = variables.projectVersion;
  packageJson.dependencies[`@${variables.projectKebab}/${variables.moduleKebab}-api`] = variables.projectVersion;
  writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`);

  const entryPath = join(targetDir, 'frontend/src/main.ts');
  const content = readFileSync(entryPath, 'utf8');
  const importLine = `import { register${variables.modulePascal}Pages } from '@${variables.projectKebab}/${variables.moduleKebab}';`;
  const registerLine = `register${variables.modulePascal}Pages();`;
  const withImport = content.includes(importLine)
    ? content
    : content.replace('// mango-cli:imports:end', `${importLine}\n// mango-cli:imports:end`);
  const marker = '// mango-cli:business-registrars';
  const withMarker = withImport.includes(marker)
    ? withImport
    : withImport.replace('createMangoAdminApp({', `${marker}\n\ncreateMangoAdminApp({`);
  const next = withMarker.includes(registerLine)
    ? withMarker
    : withMarker.replace(marker, `${marker}\n${registerLine}`);
  writeFileSync(entryPath, next);
}

function updateBusinessConfig(targetDir, config, variables) {
  const businessModules = Array.isArray(config.businessModules) ? config.businessModules : [];
  const nextModules = businessModules.filter(item => item.module !== variables.moduleKebab);
  nextModules.push({
    module: variables.moduleKebab,
    aggregate: variables.aggregateKebab,
    package: variables.modulePackage,
    displayName: variables.moduleName,
    aggregateDisplayName: variables.aggregateName,
  });
  config.businessModules = nextModules;
  writeFileSync(join(targetDir, 'mango.config.json'), `${JSON.stringify(config, null, 2)}\n`);
}

function ensureWorkspace(workspaces, pattern) {
  const list = Array.isArray(workspaces) ? workspaces : [];
  return list.includes(pattern) ? list : [...list, pattern];
}

function appendManagedLine(content, name, line) {
  const start = `<!-- mango-cli:${name}:start -->`;
  const end = `<!-- mango-cli:${name}:end -->`;
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    fail(`managed block not found: ${name}`);
  }
  const block = content.slice(startIndex + start.length, endIndex);
  if (block.includes(line)) {
    return content;
  }
  const nextBlock = block.trim() ? `${block.trimEnd()}\n${line}\n` : `\n${line}\n`;
  return `${content.slice(0, startIndex + start.length)}${nextBlock}${content.slice(endIndex)}`;
}

function appendYamlManagedBlock(content, name, block) {
  const start = `# mango-cli:${name}:start`;
  const end = `# mango-cli:${name}:end`;
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    fail(`managed block not found: ${name}`);
  }
  const startLineEnd = content.indexOf('\n', startIndex);
  const endLineStart = content.lastIndexOf('\n', endIndex) + 1;
  if (startLineEnd < 0 || endLineStart <= startLineEnd) {
    fail(`invalid managed block: ${name}`);
  }
  const currentBlock = content.slice(startLineEnd + 1, endLineStart);
  if (currentBlock.includes(block)) {
    return content;
  }
  const nextBlock = currentBlock.trim() ? `${currentBlock.trimEnd()}\n${block}\n` : `${block}\n`;
  return `${content.slice(0, startLineEnd + 1)}${nextBlock}${content.slice(endLineStart)}`;
}

function replaceManagedBlock(content, name, replacement) {
  const start = `// mango-cli:${name}:start`;
  const end = `// mango-cli:${name}:end`;
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    fail(`managed block not found in frontend/src/main.ts: ${name}`);
  }
  return [
    content.slice(0, startIndex + start.length),
    '\n',
    replacement,
    '\n',
    content.slice(endIndex),
  ].join('');
}

function replaceXmlManagedBlock(content, name, replacement, fileLabel = 'backend/pom.xml') {
  const start = `<!-- mango-cli:${name}:start -->`;
  const end = `<!-- mango-cli:${name}:end -->`;
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    fail(`managed block not found in ${fileLabel}: ${name}`);
  }
  return [
    content.slice(0, startIndex + start.length),
    '\n',
    replacement,
    '\n',
    content.slice(endIndex),
  ].join('');
}

function resolveSelectedModules(options) {
  const codes = options.preset === 'full'
    ? FULL_MODULE_CODES
    : resolveModuleCodes(options.modules || 'none');
  return codes.map(code => MODULE_BY_CODE.get(code));
}

function resolveModuleCodes(value) {
  const rawCodes = Array.isArray(value)
    ? value.flatMap(item => String(item).split(','))
    : String(value || '').split(',');
  const normalized = rawCodes
    .map(item => item.trim())
    .filter(Boolean)
    .flatMap(item => item === 'all' ? FULL_MODULE_CODES : item === 'none' ? [] : [item]);
  const result = [];
  for (const code of normalized) {
    if (!MODULE_BY_CODE.has(code)) {
      fail(`unknown module: ${code}`);
    }
    addModuleCode(result, code);
  }
  return result;
}

function addModuleCode(result, code) {
  const module = MODULE_BY_CODE.get(code);
  for (const dependency of module.dependsOn || []) {
    addModuleCode(result, dependency);
  }
  if (!result.includes(code)) {
    result.push(code);
  }
}

function renderFrontendModuleDependencies(selectedModules) {
  const dependencies = uniqueBy(
    [
      ...ADMIN_OPTIONAL_PEER_PACKAGES,
      ...selectedModules
        .filter(module => module.frontendPackage)
        .map(module => ({ name: module.frontendPackage, versionKey: module.versionKey })),
    ],
    dependency => dependency.name,
  );
  return dependencies
    .map(dependency => `    "${dependency.name}": "${defaultVersions[dependency.versionKey]}",`)
    .join('\n');
}

function renderFrontendEntryImports(preset, selectedModules) {
  if (preset === 'full') {
    return [
      "import { createMangoAdminApp, mangoFullAdminFeatureRegistrars } from '@mango/admin/full';",
      "import '@mango/admin/style-full.css';",
    ].join('\n');
  }
  const imports = ["import { createMangoAdminApp } from '@mango/admin';", "import '@mango/admin/style.css';"];
  for (const module of selectedModules) {
    imports.push(...toArray(module.registrarImport));
  }
  for (const module of selectedModules) {
    if (module.styleImport) {
      imports.push(`import '${module.styleImport}';`);
    }
  }
  return uniqueBy(imports, item => item).join('\n');
}

function renderFrontendFeaturesExpression(preset, selectedModules) {
  if (preset === 'full') {
    return "'full'";
  }
  const features = selectedModules.map(module => module.feature).filter(Boolean);
  return `${JSON.stringify(features)} as const`;
}

function renderFrontendFeatureRegistrarsExpression(preset, selectedModules) {
  if (preset === 'full') {
    return 'mangoFullAdminFeatureRegistrars';
  }
  const registrars = selectedModules.flatMap(module => toArray(module.registrar));
  if (registrars.length === 0) {
    return '[]';
  }
  return `[${registrars.join(', ')}]`;
}

function renderBackendManagedDependencies(preset, selectedModules) {
  if (preset === 'full') {
    return renderDependencyXml(
      [
        { groupId: 'io.mango', artifactId: 'mango-admin-starter' },
        ...BUSINESS_BACKEND_MANAGED_DEPENDENCIES,
      ],
      true,
      12,
    );
  }
  return renderDependencyXml(
    [...CORE_BACKEND_DEPENDENCIES, ...BUSINESS_BACKEND_MANAGED_DEPENDENCIES, ...selectedModules.flatMap(module => module.backend || [])],
    true,
    12,
  );
}

function renderBackendDependencies(preset, selectedModules) {
  if (preset === 'full') {
    return renderDependencyXml([
      { groupId: 'io.mango', artifactId: 'mango-admin-starter' },
    ], false, 8);
  }
  return renderDependencyXml(
    [...CORE_BACKEND_DEPENDENCIES, ...selectedModules.flatMap(module => module.backend || [])],
    false,
    8,
  );
}

function renderDependencyXml(dependencies, includeVersion, indentSize) {
  const indent = ' '.repeat(indentSize);
  const childIndent = `${indent}    `;
  return uniqueBy(dependencies, dependency => `${dependency.groupId}:${dependency.artifactId}`)
    .map(dependency => [
      `${indent}<dependency>`,
      `${childIndent}<groupId>${dependency.groupId}</groupId>`,
      `${childIndent}<artifactId>${dependency.artifactId}</artifactId>`,
      ...(includeVersion ? [`${childIndent}<version>${dependency.version || '${mango.version}'}</version>`] : []),
      `${indent}</dependency>`,
    ].join('\n'))
    .join('\n');
}

function renderRuntimeModulesJson(selectedModules, mode) {
  const modules = {
    'mango-authorization': {
      mode: mode === 'micro' ? 'micro' : 'local',
      runtimeCode: mode === 'micro' ? 'mango-admin-rbac-app' : 'mango-admin-rbac-local',
      ...(mode === 'micro' ? { entry: 'http://b.mango.io:5181/' } : {}),
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
  };
  for (const module of selectedModules) {
    if (module.runtimeModule) {
      modules[module.runtimeModule.moduleCode] = mode === 'micro'
        ? module.runtimeModule.micro
        : module.runtimeModule.local;
    }
  }
  return JSON.stringify(modules, null, 4).replace(/\n/g, '\n  ');
}

function uniqueBy(items, getKey) {
  const seen = new Set();
  const result = [];
  for (const item of items) {
    const key = getKey(item);
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    result.push(item);
  }
  return result;
}

function toArray(value) {
  if (!value) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
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

function assertNoUnrenderedPlaceholders(targetDir, relativePaths) {
  for (const relativePath of relativePaths) {
    const root = join(targetDir, relativePath);
    for (const file of walkFiles(root)) {
      const rel = relative(targetDir, file);
      if (/\{\{[^}]+}}/.test(rel)) {
        fail(`unrendered placeholder in path: ${rel}`);
      }
      if (!isTextFile(file)) {
        continue;
      }
      const content = readFileSync(file, 'utf8');
      const match = content.match(/\{\{[^}]+}}/);
      if (match) {
        fail(`unrendered placeholder ${match[0]} in ${rel}`);
      }
    }
  }
}

function walkFiles(root) {
  if (!existsSync(root)) {
    return [];
  }
  const result = [];
  for (const entry of readdirSync(root)) {
    const fullPath = join(root, entry);
    if (statSync(fullPath).isDirectory()) {
      result.push(...walkFiles(fullPath));
    } else {
      result.push(fullPath);
    }
  }
  return result;
}

function isTextFile(file) {
  const name = basename(file);
  return /\.(md|json|xml|java|ts|vue|html|mjs|sh|yml|yaml|txt|gitignore|npmrc|properties|imports|sql)$/.test(name)
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

function readReleaseVersions() {
  const releaseVersionsPath = join(packageRoot, 'release-versions.json');
  if (!existsSync(releaseVersionsPath)) {
    return {};
  }
  return JSON.parse(readFileSync(releaseVersionsPath, 'utf8'));
}

function readReleasedMangoPackageVersion(packageName, fallback) {
  return releaseVersions.npm?.[`@mango/${packageName}`] || fallback;
}

function readMangoBaselineCommit() {
  const gitDir = resolveGitDir();
  if (!gitDir) {
    return 'unknown';
  }
  const headPath = resolve(gitDir, 'HEAD');
  if (!existsSync(headPath)) {
    return 'unknown';
  }
  const head = readFileSync(headPath, 'utf8').trim();
  if (!head.startsWith('ref: ')) {
    return head.slice(0, 12) || 'unknown';
  }
  const ref = head.slice('ref: '.length);
  const commonGitDir = resolveCommonGitDir(gitDir);
  return readGitRef(gitDir, ref)
    || readGitRef(commonGitDir, ref)
    || readPackedGitRef(commonGitDir, ref)
    || 'unknown';
}

function resolveGitDir() {
  const dotGitPath = resolve(repoRoot, '.git');
  if (!existsSync(dotGitPath)) {
    return '';
  }
  const stat = statSync(dotGitPath);
  if (stat.isDirectory()) {
    return dotGitPath;
  }
  const content = readFileSync(dotGitPath, 'utf8').trim();
  if (!content.startsWith('gitdir: ')) {
    return '';
  }
  const gitDir = content.slice('gitdir: '.length).trim();
  return isAbsolute(gitDir) ? gitDir : resolve(repoRoot, gitDir);
}

function resolveCommonGitDir(gitDir) {
  const commonDirPath = resolve(gitDir, 'commondir');
  if (!existsSync(commonDirPath)) {
    return gitDir;
  }
  const commonDir = readFileSync(commonDirPath, 'utf8').trim();
  return isAbsolute(commonDir) ? commonDir : resolve(gitDir, commonDir);
}

function readGitRef(gitDir, ref) {
  const refPath = resolve(gitDir, ref);
  if (!existsSync(refPath)) {
    return '';
  }
  return readFileSync(refPath, 'utf8').trim().slice(0, 12);
}

function readPackedGitRef(gitDir, ref) {
  const packedRefsPath = resolve(gitDir, 'packed-refs');
  if (!existsSync(packedRefsPath)) {
    return '';
  }
  const lines = readFileSync(packedRefsPath, 'utf8').split(/\r?\n/);
  const packedLine = lines.find(line => line.endsWith(` ${ref}`));
  return packedLine ? packedLine.split(' ')[0].slice(0, 12) : '';
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

function toCamelCase(value) {
  const pascal = toPascalCase(value);
  return pascal ? pascal.charAt(0).toLowerCase() + pascal.slice(1) : '';
}

function toSnakeCase(value) {
  return toKebabCase(value).replaceAll('-', '_');
}

function toJavaSegment(value) {
  const segment = toCamelCase(value).replace(/[^a-zA-Z0-9_]/g, '');
  if (!segment || !/^[a-zA-Z_]/.test(segment)) {
    fail(`invalid Java package segment: ${value}`);
  }
  return segment;
}

function printNextSteps(targetDir, variables) {
  const relativeTarget = relativeOrAbsolute(process.cwd(), targetDir);
  process.stdout.write(`Created Mango ${variables.preset} project: ${relativeTarget}\n\n`);
  process.stdout.write('Next steps:\n');
  process.stdout.write(`  cd ${relativeTarget}\n`);
  process.stdout.write('  npm --prefix frontend install\n');
  process.stdout.write('  npm --prefix frontend run build\n');
  process.stdout.write('  scripts/backend-dev.sh\n');
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
