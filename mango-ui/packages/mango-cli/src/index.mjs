#!/usr/bin/env node
import { appendFileSync, chmodSync, closeSync, copyFileSync, existsSync, mkdirSync, openSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { spawn, spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
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
  mangoJob: readReleasedMangoPackageVersion('job', '1.0.0'),
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
  elementPlus: '2.14.1',
  iconsVue: '2.3.2',
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
  { name: '@mango/job', versionKey: 'mangoJob' },
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
  mango init-dev
  mango validate
  mango doctor
  mango plan [group|app...]
  mango start [group|app...]
  mango stop [app...]
  mango status
  mango logs <app>
  mango changelog
  mango pmo sync --project-dir <dir> [--dry-run] [--write-agents] [--sync-shell]
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
  --project-dir <dir>      Existing project directory for add/module/pmo commands
  --dry-run                Print PMO sync plan without modifying files
  --write-agents           Update root AGENTS.md during PMO sync when it points to an external mango-pmo
  --sync-shell             Sync generated startup shell scripts during PMO sync
  --force                  Overwrite existing target directory
  --help                   Show help

Development workspace:
  mango searches upward from the current directory for mango.dev.json.
  Project defaults stay in mango.dev.json. Local secrets and per-workspace
  ports stay in .mango/dev-workspace.env and must not be committed.

Upgrade:
  Run "mango changelog" after upgrading to review new features, upgrade notes, and verification steps.

Modules:
  ${OPTIONAL_MODULES.map(module => `${module.code.padEnd(16)} ${module.label}`).join('\n  ')}
`;

async function main(argv = process.argv.slice(2)) {
  const args = normalizeArgs(argv);
  if (args.includes('--help') || args.includes('-h')) {
    process.stdout.write(usage.trimStart());
    return;
  }

  if (args[0] === 'changelog' || args[0] === 'changes' || args[0] === 'release-notes') {
    printChangelog();
    return;
  }

  if (isDevWorkspaceCommand(args[0])) {
    await runDevWorkspaceCommand(args[0], args.slice(1));
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

  if (args[0] === 'pmo') {
    const subCommand = args[1];
    if (subCommand !== 'sync') {
      fail(`unknown pmo command: ${subCommand || ''}`);
    }
    syncPmoBaseline(args.slice(2));
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
  chmodSync(join(targetDir, 'scripts/dev-workspace.sh'), 0o755);
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
    projectKebabSnake: toSnakeCase(options.project),
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
    mangoJobVersion: defaultVersions.mangoJob,
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
    mangoCliVersion: readCliVersion(),
    mangoBaselineSyncedAt: new Date().toISOString(),
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

const DEV_WORKSPACE_COMMANDS = new Set(['init-dev', 'print', 'validate', 'doctor', 'plan', 'start', 'stop', 'status', 'logs', 'backend', 'frontend']);
const DEFAULT_SPRING_BOOT_PLUGIN = `org.springframework.boot:spring-boot-maven-plugin:${defaultVersions.springBoot}:run`;

function isDevWorkspaceCommand(command) {
  return DEV_WORKSPACE_COMMANDS.has(command);
}

async function runDevWorkspaceCommand(command, argv) {
  const context = command === 'init-dev'
    ? loadDevWorkspaceContext({ allowMissingManifest: true })
    : loadDevWorkspaceContext({ allowMissingManifest: false });
  const normalizedCommand = normalizeDevWorkspaceCommand(command);

  if (normalizedCommand === 'init-dev') {
    initDevWorkspace(context);
    return;
  }

  if (normalizedCommand === 'print') {
    printDevWorkspace(context);
    return;
  }

  if (normalizedCommand === 'validate') {
    validateDevWorkspace(context, { verbose: true });
    return;
  }

  if (normalizedCommand === 'doctor') {
    doctorDevWorkspace(context);
    return;
  }

  if (normalizedCommand === 'plan') {
    printDevWorkspacePlan(context, argv);
    return;
  }

  if (normalizedCommand === 'status') {
    printDevWorkspaceStatus(context);
    return;
  }

  if (normalizedCommand === 'logs') {
    printDevWorkspaceLogs(context, argv);
    return;
  }

  if (normalizedCommand === 'stop') {
    await stopDevWorkspace(context, argv);
    return;
  }

  if (normalizedCommand === 'start') {
    await startDevWorkspace(context, command === 'backend' || command === 'frontend' ? [command] : argv);
    return;
  }

  fail(`unknown development workspace command: ${command}`);
}

function normalizeDevWorkspaceCommand(command) {
  if (command === 'backend') {
    return 'start';
  }
  if (command === 'frontend') {
    return 'start';
  }
  return command;
}

function loadDevWorkspaceContext({ allowMissingManifest }) {
  const root = findUpFile(process.cwd(), 'mango.dev.json');
  if (!root) {
    if (allowMissingManifest) {
      return {
        root: process.cwd(),
        manifestPath: join(process.cwd(), 'mango.dev.json'),
        manifest: null,
        env: {},
        localConfig: {},
      };
    }
    fail('mango.dev.json not found. Run this command inside a Mango workspace or create mango.dev.json.');
  }
  const manifestPath = join(root, 'mango.dev.json');
  const env = readEnvFile(join(root, '.mango/dev-workspace.env'));
  const localConfigPath = join(root, '.mango/dev-workspace.local.json');
  const localConfig = existsSync(localConfigPath) ? readJsonFile(localConfigPath) : {};
  const manifest = mergeDevWorkspaceManifest(readJsonFile(manifestPath), localConfig);
  const defaultEnv = parseEnvText(defaultDevWorkspaceEnv(root));
  return {
    root,
    manifestPath,
    manifest,
    env: { ...defaultEnv, ...env },
    localConfig,
    localConfigPath,
    runDir: join(root, '.mango/run'),
    pidDir: join(root, '.mango/run/pids'),
    logDir: join(root, '.mango/run/logs'),
    statePath: join(root, '.mango/run/state.json'),
  };
}

function findUpFile(startDir, fileName) {
  let current = resolve(startDir);
  while (true) {
    if (existsSync(join(current, fileName))) {
      return current;
    }
    const parent = dirname(current);
    if (parent === current) {
      return '';
    }
    current = parent;
  }
}

function readJsonFile(path) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch (error) {
    fail(`failed to read JSON ${path}: ${error.message}`);
  }
}

function readEnvFile(path) {
  if (!existsSync(path)) {
    return {};
  }
  return parseEnvText(readFileSync(path, 'utf8'));
}

function parseEnvText(content) {
  const env = {};
  const lines = content.split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const equalIndex = trimmed.indexOf('=');
    if (equalIndex <= 0) {
      continue;
    }
    const key = trimmed.slice(0, equalIndex).trim();
    env[key] = unquoteEnvValue(trimmed.slice(equalIndex + 1).trim());
  }
  return env;
}

function unquoteEnvValue(value) {
  if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
    return value.slice(1, -1);
  }
  return value;
}

function mergeDevWorkspaceManifest(manifest, localConfig) {
  const result = {
    ...manifest,
    groups: {
      ...(manifest.groups || {}),
      ...(localConfig.groups || {}),
    },
    apps: {
      ...(manifest.apps || {}),
    },
  };
  for (const [name, app] of Object.entries(localConfig.apps || {})) {
    result.apps[name] = deepMerge(result.apps[name] || {}, app);
  }
  return result;
}

function deepMerge(base, override) {
  const result = { ...base };
  for (const [key, value] of Object.entries(override || {})) {
    if (isPlainObject(value) && isPlainObject(result[key])) {
      result[key] = deepMerge(result[key], value);
    } else {
      result[key] = value;
    }
  }
  return result;
}

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function initDevWorkspace(context) {
  const mangoDir = join(context.root, '.mango');
  const envPath = join(mangoDir, 'dev-workspace.env');
  mkdirSync(mangoDir, { recursive: true });
  if (!existsSync(envPath)) {
    writeFileSync(envPath, defaultDevWorkspaceEnv(context.root));
    process.stdout.write(`Created local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  } else {
    process.stdout.write(`Workspace env already exists: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  }

  if (!existsSync(context.manifestPath)) {
    writeFileSync(context.manifestPath, `${JSON.stringify(defaultBusinessDevManifest(basename(context.root)), null, 2)}\n`);
    process.stdout.write(`Created development manifest: ${relativeOrAbsolute(process.cwd(), context.manifestPath)}\n`);
  }
}

function defaultDevWorkspaceEnv(root) {
  const projectName = toSnakeCase(basename(root)) || 'mango_workspace';
  return [
    '# Mango local workspace configuration.',
    '# This file is generated once per workspace and must not be committed.',
    `MANGO_CRYPTO_SM4_SECRET_KEY=${randomBytes(16).toString('hex')}`,
    'MANGO_BACKEND_PORT=5555',
    'MANGO_FRONTEND_PORT=5176',
    'MANGO_FRONTEND_HOST=127.0.0.1',
    'MANGO_FRONTEND_OPEN=false',
    'MANGO_FRONTEND_AUTO_INSTALL=true',
    `MANGO_DB_NAME=${projectName}`,
    'MANGO_DB_HOST=127.0.0.1',
    'MANGO_DB_PORT=3306',
    'MANGO_DB_USERNAME=root',
    "MANGO_DB_PASSWORD=''",
    'MANGO_DB_AUTO_CREATE=true',
    'MANGO_OFFICE_PLUGIN_ENABLED=false',
    "MANGO_BACKEND_ADDITIONAL_ARGS=''",
    '',
  ].join('\n');
}

function defaultBusinessDevManifest(projectName) {
  return {
    version: 1,
    groups: {
      default: ['backend', 'frontend'],
    },
    apps: {
      backend: {
        type: 'spring-boot-maven',
        cwd: 'backend',
        pom: 'app/pom.xml',
        install: {
          command: 'mvn',
          args: ['-f', 'pom.xml', '-DskipTests', 'install'],
        },
        portEnv: 'MANGO_BACKEND_PORT',
        port: 5555,
        health: '/actuator/health',
        env: {
          MANGO_CRYPTO_SM4_SECRET_KEY: '${env.MANGO_CRYPTO_SM4_SECRET_KEY}',
        },
        args: [
          '--server.port=${port}',
          '--spring.datasource.url=jdbc:mysql://${env.MANGO_DB_HOST}:${env.MANGO_DB_PORT}/${env.MANGO_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
          '--spring.datasource.username=${env.MANGO_DB_USERNAME}',
          '--spring.datasource.password=${env.MANGO_DB_PASSWORD}',
          '--office.plugin.enabled=${env.MANGO_OFFICE_PLUGIN_ENABLED}',
          '${env.MANGO_BACKEND_ADDITIONAL_ARGS}',
        ],
      },
      frontend: {
        type: 'vite',
        cwd: 'frontend',
        dependsOn: ['backend'],
        packageManager: 'npm',
        portEnv: 'MANGO_FRONTEND_PORT',
        port: 5176,
        hostEnv: 'MANGO_FRONTEND_HOST',
        host: '127.0.0.1',
        env: {
          VITE_ADMIN_PROXY_PATH: 'http://127.0.0.1:${backend.port}',
          VITE_PORT: '${port}',
          VITE_HOST: '${host}',
          VITE_OPEN: '${env.MANGO_FRONTEND_OPEN}',
        },
        args: ['run', 'dev', '--', '--host', '${host}', '--port', '${port}'],
      },
    },
    metadata: {
      project: projectName,
    },
  };
}

function printDevWorkspace(context) {
  validateDevWorkspace(context, { verbose: false });
  process.stdout.write(`Workspace: ${context.root}\n`);
  process.stdout.write(`Manifest:  ${context.manifestPath}\n`);
  process.stdout.write(`Env file:  ${join(context.root, '.mango/dev-workspace.env')}\n`);
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    process.stdout.write(`${name.padEnd(16)} ${resolved.type.padEnd(18)} ${resolved.cwd}`);
    if (resolved.port) {
      process.stdout.write(` port=${resolved.port}`);
    }
    process.stdout.write('\n');
  }
}

function validateDevWorkspace(context, { verbose }) {
  const errors = [];
  const manifest = context.manifest;
  if (!manifest || typeof manifest !== 'object') {
    errors.push('mango.dev.json must be a JSON object');
  }
  if (manifest?.version !== 1) {
    errors.push('mango.dev.json version must be 1');
  }
  if (!isPlainObject(manifest?.apps) || Object.keys(manifest.apps).length === 0) {
    errors.push('mango.dev.json apps must contain at least one app');
  }
  for (const [name, app] of Object.entries(manifest?.apps || {})) {
    if (!/^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/.test(name)) {
      errors.push(`invalid app name: ${name}`);
    }
    if (!['spring-boot-maven', 'vite', 'command'].includes(app.type)) {
      errors.push(`${name}: unsupported type ${app.type || ''}`);
    }
    const cwdPath = app.cwd ? resolve(context.root, app.cwd) : '';
    if (!app.cwd) {
      errors.push(`${name}: cwd is required`);
    } else if (!existsSync(cwdPath)) {
      errors.push(`${name}: cwd not found: ${app.cwd}`);
    }
    for (const dependency of app.dependsOn || []) {
      if (!manifest.apps[dependency]) {
        errors.push(`${name}: unknown dependency ${dependency}`);
      }
    }
    if (app.type === 'spring-boot-maven' && app.cwd && existsSync(cwdPath)) {
      const pom = app.pom || 'pom.xml';
      const pomPath = resolve(cwdPath, pom);
      if (!existsSync(pomPath)) {
        errors.push(`${name}: pom not found: ${relativeOrAbsolute(context.root, pomPath)}`);
      }
    }
    if (app.type === 'spring-boot-maven' && app.goal && app.goal === 'spring-boot:run') {
      errors.push(`${name}: use explicit Spring Boot Maven plugin coordinate instead of spring-boot:run`);
    }
  }
  for (const [groupName, members] of Object.entries(manifest?.groups || {})) {
    if (!Array.isArray(members)) {
      errors.push(`group ${groupName}: members must be an array`);
      continue;
    }
    for (const member of members) {
      if (!manifest.apps?.[member] && !manifest.groups?.[member]) {
        errors.push(`group ${groupName}: unknown app or group ${member}`);
      }
    }
  }
  try {
    resolveDevWorkspaceTargets(context, ['default']);
  } catch (error) {
    errors.push(error.message);
  }
  if (errors.length > 0) {
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    fail('development workspace validation failed');
  }
  if (verbose) {
    process.stdout.write(`Development workspace manifest is valid: ${relativeOrAbsolute(process.cwd(), context.manifestPath)}\n`);
  }
}

function doctorDevWorkspace(context) {
  validateDevWorkspace(context, { verbose: true });
  process.stdout.write(`ok      mango runner ${currentFile}\n`);
  const checks = [
    ['node', process.execPath],
    ['mvn', 'mvn'],
    ['npm', 'npm'],
    ['pnpm', 'pnpm'],
    ['curl', 'curl'],
  ];
  for (const [label, command] of checks) {
    if (label === 'node') {
      process.stdout.write(`ok      node ${process.version}\n`);
      continue;
    }
    const result = spawnSync(command, ['--version'], { encoding: 'utf8' });
    if (result.status === 0) {
      const firstLine = `${result.stdout || result.stderr}`.split(/\r?\n/).find(Boolean) || 'available';
      process.stdout.write(`ok      ${label} ${firstLine}\n`);
    } else {
      process.stdout.write(`missing ${label}\n`);
    }
  }
  const globalMango = spawnSync('mango', ['--version'], { encoding: 'utf8' });
  if (globalMango.status === 0) {
    const firstLine = `${globalMango.stdout || globalMango.stderr}`.split(/\r?\n/).find(Boolean) || 'available';
    process.stdout.write(`ok      global mango ${firstLine}\n`);
  } else {
    process.stdout.write('optional global mango not found; current runner is available\n');
  }
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    if (resolved.type === 'spring-boot-maven') {
      const pomPath = resolve(resolved.cwd, resolved.pom || 'pom.xml');
      if (existsSync(pomPath)) {
        process.stdout.write(`ok      ${name} pom ${relativeOrAbsolute(context.root, pomPath)}\n`);
      } else {
        process.stdout.write(`missing ${name} pom ${relativeOrAbsolute(context.root, pomPath)}\n`);
      }
    }
    if (resolved.port && isPortInUse(resolved.port)) {
      process.stdout.write(`warn    ${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}\n`);
    } else if (resolved.port) {
      process.stdout.write(`ok      ${name} port ${resolved.port} is free\n`);
    }
  }
}

function printDevWorkspacePlan(context, targets) {
  validateDevWorkspace(context, { verbose: false });
  const appNames = resolveDevWorkspaceTargets(context, targets.length > 0 ? targets : ['default']);
  process.stdout.write(`Start plan for ${relativeOrAbsolute(process.cwd(), context.root)}\n`);
  for (const name of appNames) {
    const resolved = resolveDevApp(context, name, context.manifest.apps[name]);
    process.stdout.write(`  ${name}\n`);
    process.stdout.write(`    type: ${resolved.type}\n`);
    process.stdout.write(`    cwd:  ${relativeOrAbsolute(context.root, resolved.cwd)}\n`);
    process.stdout.write(`    cmd:  ${resolved.command} ${resolved.args.join(' ')}\n`);
    if (resolved.port) {
      process.stdout.write(`    url:  ${resolved.url}\n`);
    }
    if (resolved.dependsOn.length > 0) {
      process.stdout.write(`    dependsOn: ${resolved.dependsOn.join(', ')}\n`);
    }
  }
}

async function startDevWorkspace(context, targets) {
  validateDevWorkspace(context, { verbose: false });
  ensureDevWorkspaceEnv(context);
  context.env = { ...parseEnvText(defaultDevWorkspaceEnv(context.root)), ...readEnvFile(join(context.root, '.mango/dev-workspace.env')) };
  ensureRunDirs(context);
  const appNames = resolveDevWorkspaceTargets(context, targets.length > 0 ? targets : ['default']);
  for (const name of appNames) {
    const resolved = resolveDevApp(context, name, context.manifest.apps[name]);
    const current = readPidFile(context, name);
    if (current && isProcessAlive(current.pid)) {
      process.stdout.write(`${name} is already running: pid=${current.pid}\n`);
      continue;
    }
    if (resolved.port && isPortInUse(resolved.port)) {
      fail(`${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}. Stop the conflicting process or edit .mango/dev-workspace.env.`);
    }
    startDevApp(context, name, resolved);
    if (resolved.health) {
      await waitForDevApp(context, name, resolved);
    }
  }
}

function ensureDevWorkspaceEnv(context) {
  const envPath = join(context.root, '.mango/dev-workspace.env');
  if (!existsSync(envPath)) {
    initDevWorkspace(context);
    return;
  }
  const env = readEnvFile(envPath);
  if (!env.MANGO_CRYPTO_SM4_SECRET_KEY) {
    appendFileSync(envPath, `\nMANGO_CRYPTO_SM4_SECRET_KEY=${randomBytes(16).toString('hex')}\n`);
    process.stdout.write(`Added MANGO_CRYPTO_SM4_SECRET_KEY to local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  }
}

function startDevApp(context, name, app) {
  const logPath = join(context.logDir, `${name}.log`);
  appendFileSync(logPath, `\n--- ${new Date().toISOString()} start ${name} ---\n`);
  if (app.install) {
    requireCommand(app.install.command, name);
    process.stdout.write(`${name}: running install command\n`);
    const install = runForegroundCommand(app.cwd, app.install.command, app.install.args, app.env, logPath);
    if (install.status !== 0) {
      fail(`${name}: install command failed, see ${relativeOrAbsolute(process.cwd(), logPath)}`);
    }
  }
  requireCommand(app.command, name);
  const logFd = openSync(logPath, 'a');
  const child = spawn(app.command, app.args, {
    cwd: app.cwd,
    env: { ...process.env, ...app.env },
    detached: true,
    stdio: ['ignore', logFd, logFd],
  });
  child.on('error', error => {
    appendFileSync(logPath, `\n--- ${new Date().toISOString()} failed ${name}: ${error.message} ---\n`);
    closeSync(logFd);
    rmSync(pidFilePath(context, name), { force: true });
  });
  child.on('exit', (code, signal) => {
    appendFileSync(logPath, `\n--- ${new Date().toISOString()} exit ${name} code=${code ?? ''} signal=${signal ?? ''} ---\n`);
    closeSync(logFd);
  });
  child.unref();
  writePidFile(context, name, {
    pid: child.pid,
    pgid: child.pid,
    startedAt: new Date().toISOString(),
    cwd: app.cwd,
    command: app.command,
    args: app.args,
    logPath,
    port: app.port || null,
    url: app.url || null,
  });
  process.stdout.write(`${name}: started pid=${child.pid} log=${relativeOrAbsolute(process.cwd(), logPath)}\n`);
}

function requireCommand(command, appName) {
  if (command.includes('/') || command.includes('\\')) {
    if (!existsSync(command)) {
      fail(`${appName}: command not found: ${command}`);
    }
    return;
  }
  const result = spawnSync('sh', ['-c', `command -v "$1" >/dev/null 2>&1`, 'sh', command], { stdio: 'ignore' });
  if (result.status !== 0) {
    fail(`${appName}: command not found: ${command}`);
  }
}

function runForegroundCommand(cwd, command, args, env, logPath) {
  const result = spawnSync(command, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
  });
  if (result.stdout) {
    appendFileSync(logPath, result.stdout);
  }
  if (result.stderr) {
    appendFileSync(logPath, result.stderr);
  }
  return result;
}

async function waitForDevApp(context, name, app) {
  const timeoutMs = Number(app.waitTimeoutMs || 120000);
  const startedAt = Date.now();
  process.stdout.write(`${name}: waiting for ${app.healthUrl}\n`);
  const tick = () => new Promise(resolvePromise => setTimeout(resolvePromise, 2000));
  while (Date.now() - startedAt < timeoutMs) {
    const pidInfo = readPidFile(context, name);
    if (!pidInfo || !isProcessAlive(pidInfo.pid)) {
      failWithDevAppLog(context, name, `${name} exited before becoming healthy`);
    }
    if (await httpOk(app.healthUrl)) {
      process.stdout.write(`${name}: ready ${app.healthUrl}\n`);
      return;
    }
    await tick();
  }
  failWithDevAppLog(context, name, `${name}: timed out waiting for ${app.healthUrl}`);
}

function failWithDevAppLog(context, name, message) {
  const pidInfo = readPidFile(context, name);
  if (pidInfo?.logPath) {
    process.stderr.write(`${message}. Last log lines:\n${tailFile(pidInfo.logPath, 80)}\n`);
  }
  fail(message);
}

function printDevWorkspaceStatus(context) {
  validateDevWorkspace(context, { verbose: false });
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    const pidInfo = readPidFile(context, name);
    const alive = pidInfo && isProcessAlive(pidInfo.pid);
    const occupied = !alive && resolved.port && isPortInUse(resolved.port);
    const status = alive ? 'running' : occupied ? 'occupied' : 'stopped';
    const pidText = alive ? ` pid=${pidInfo.pid}` : '';
    const urlText = resolved.url ? ` ${resolved.url}` : '';
    const occupantText = occupied ? formatPortOccupants(resolved.port) : '';
    process.stdout.write(`${status.padEnd(8)} ${name}${pidText}${urlText}${occupantText}\n`);
  }
}

function printDevWorkspaceLogs(context, argv) {
  const appName = argv[0];
  if (!appName) {
    fail('missing app name for logs command');
  }
  if (!context.manifest.apps?.[appName]) {
    fail(`unknown app: ${appName}`);
  }
  const logPath = join(context.logDir, `${appName}.log`);
  if (!existsSync(logPath)) {
    fail(`log file not found: ${relativeOrAbsolute(process.cwd(), logPath)}`);
  }
  process.stdout.write(tailFile(logPath, 200));
}

async function stopDevWorkspace(context, targets) {
  validateDevWorkspace(context, { verbose: false });
  const appNames = resolveStopTargets(context, targets);
  for (const name of appNames.reverse()) {
    const pidInfo = readPidFile(context, name);
    if (!pidInfo) {
      process.stdout.write(`${name}: no pid file\n`);
      continue;
    }
    if (!isProcessAlive(pidInfo.pid)) {
      rmSync(pidFilePath(context, name), { force: true });
      process.stdout.write(`${name}: stale pid removed\n`);
      continue;
    }
    await stopProcessGroup(pidInfo.pid);
    rmSync(pidFilePath(context, name), { force: true });
    process.stdout.write(`${name}: stopped pid=${pidInfo.pid}\n`);
  }
}

function resolveStopTargets(context, targets) {
  if (targets.length > 0) {
    return resolveDevWorkspaceTargets(context, targets);
  }
  return resolveDevWorkspaceTargets(context, ['default']);
}

function resolveDevWorkspaceTargets(context, targets) {
  const manifest = context.manifest;
  const selected = [];
  const visiting = new Set();
  const visited = new Set();
  const expandTarget = (target) => {
    if (manifest.apps[target]) {
      visitApp(target);
      return;
    }
    const group = manifest.groups?.[target];
    if (Array.isArray(group)) {
      for (const member of group) {
        expandTarget(member);
      }
      return;
    }
    throw new Error(`unknown app or group: ${target}`);
  };
  const visitApp = (name) => {
    if (visited.has(name)) {
      return;
    }
    if (visiting.has(name)) {
      throw new Error(`cyclic app dependency detected at ${name}`);
    }
    visiting.add(name);
    for (const dependency of manifest.apps[name].dependsOn || []) {
      visitApp(dependency);
    }
    visiting.delete(name);
    visited.add(name);
    selected.push(name);
  };
  for (const target of targets) {
    expandTarget(target);
  }
  return selected;
}

function resolveDevApp(context, name, app) {
  const base = {
    ...app,
    name,
    dependsOn: app.dependsOn || [],
  };
  const port = resolveAppPort(context, app);
  const host = resolveAppHost(context, app);
  const vars = buildDevInterpolationVars(context, name, base, port, host);
  const cwd = resolve(context.root, interpolateValue(app.cwd, vars));
  const env = Object.fromEntries(
    Object.entries(app.env || {}).map(([key, value]) => [key, interpolateValue(String(value), vars)]),
  );
  const resolved = {
    ...base,
    cwd,
    port,
    host,
    env,
    health: app.health || '',
  };
  const commandSpec = resolveDevCommand(context, resolved, vars);
  resolved.command = commandSpec.command;
  resolved.args = commandSpec.args;
  resolved.url = port ? `http://${host || '127.0.0.1'}:${port}` : '';
  resolved.healthUrl = resolved.health
    ? resolveHealthUrl(resolved.url, resolved.health)
    : '';
  if (app.install) {
    resolved.install = {
      command: app.install.command,
      args: (app.install.args || []).map(arg => interpolateValue(String(arg), vars)),
    };
  }
  return resolved;
}

function resolveAppPort(context, app) {
  const raw = app.portEnv && context.env[app.portEnv] ? context.env[app.portEnv] : app.port;
  return raw === undefined || raw === null || raw === '' ? 0 : Number(raw);
}

function resolveAppHost(context, app) {
  return app.hostEnv && context.env[app.hostEnv] ? context.env[app.hostEnv] : app.host || '127.0.0.1';
}

function buildDevInterpolationVars(context, name, app, port, host) {
  const apps = {};
  for (const [appName, item] of Object.entries(context.manifest.apps || {})) {
    apps[appName] = {
      port: resolveAppPort(context, item),
      host: resolveAppHost(context, item),
    };
  }
  return {
    env: { ...context.env },
    app: { ...app, port, host },
    current: { ...app, port, host },
    apps,
    [name]: { ...app, port, host },
    port,
    host,
  };
}

function resolveDevCommand(context, app, vars) {
  if (app.command) {
    return {
      command: app.command,
      args: (app.args || []).map(arg => interpolateValue(String(arg), vars)).filter(Boolean),
    };
  }
  if (app.type === 'spring-boot-maven') {
    const pom = app.pom || 'pom.xml';
    const goal = app.goal || DEFAULT_SPRING_BOOT_PLUGIN;
    const springArgs = (app.args || []).map(arg => interpolateValue(String(arg), vars)).filter(Boolean).join(' ');
    return {
      command: 'mvn',
      args: ['-f', pom, `-Dspring-boot.run.arguments=${springArgs}`, goal],
    };
  }
  if (app.type === 'vite') {
    const packageManager = app.packageManager || detectPackageManager(context.root);
    return {
      command: packageManager,
      args: (app.args || ['run', 'dev', '--', '--host', '${host}', '--port', '${port}'])
        .map(arg => interpolateValue(String(arg), vars))
        .filter(Boolean),
    };
  }
  if (app.type === 'command') {
    fail(`${app.name}: command type requires command`);
  }
  fail(`${app.name}: unsupported app type ${app.type}`);
}

function detectPackageManager(root) {
  if (existsSync(join(root, 'pnpm-lock.yaml'))) {
    return 'pnpm';
  }
  if (existsSync(join(root, 'yarn.lock'))) {
    return 'yarn';
  }
  return 'npm';
}

function interpolateValue(value, vars) {
  return value.replace(/\$\{([^}]+)}/g, (_, expression) => {
    const resolved = readPath(vars, expression.trim());
    return resolved === undefined || resolved === null ? '' : String(resolved);
  });
}

function readPath(root, pathExpression) {
  const parts = pathExpression.split('.');
  let current = root;
  for (const part of parts) {
    if (current === undefined || current === null) {
      return undefined;
    }
    current = current[part];
  }
  return current;
}

function resolveHealthUrl(baseUrl, health) {
  if (/^https?:\/\//.test(health)) {
    return health;
  }
  return `${baseUrl}${health.startsWith('/') ? health : `/${health}`}`;
}

function ensureRunDirs(context) {
  mkdirSync(context.pidDir, { recursive: true });
  mkdirSync(context.logDir, { recursive: true });
}

function pidFilePath(context, name) {
  return join(context.pidDir, `${name}.json`);
}

function readPidFile(context, name) {
  const path = pidFilePath(context, name);
  if (!existsSync(path)) {
    return null;
  }
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    return null;
  }
}

function writePidFile(context, name, data) {
  mkdirSync(context.pidDir, { recursive: true });
  writeFileSync(pidFilePath(context, name), `${JSON.stringify(data, null, 2)}\n`);
}

function isProcessAlive(pid) {
  if (!pid) {
    return false;
  }
  try {
    process.kill(Number(pid), 0);
    const stat = spawnSync('ps', ['-o', 'stat=', '-p', String(pid)], { encoding: 'utf8' });
    return stat.status === 0 && !stat.stdout.trim().startsWith('Z');
  } catch {
    return false;
  }
}

async function stopProcessGroup(pid) {
  try {
    process.kill(-Number(pid), 'SIGTERM');
  } catch {
    try {
      process.kill(Number(pid), 'SIGTERM');
    } catch {
      return;
    }
  }
  for (let index = 0; index < 20; index += 1) {
    if (!isProcessAlive(pid)) {
      return;
    }
    await new Promise(resolvePromise => setTimeout(resolvePromise, 100));
  }
  try {
    process.kill(-Number(pid), 'SIGKILL');
  } catch {
    try {
      process.kill(Number(pid), 'SIGKILL');
    } catch {
      return;
    }
  }
}

function isPortInUse(port) {
  if (!port) {
    return false;
  }
  const result = spawnSync('sh', ['-c', `lsof -nP -iTCP:${Number(port)} -sTCP:LISTEN >/dev/null 2>&1`], { stdio: 'ignore' });
  return result.status === 0;
}

function getPortOccupants(port) {
  if (!port) {
    return [];
  }
  const result = spawnSync('lsof', ['-nP', `-iTCP:${Number(port)}`, '-sTCP:LISTEN', '-F', 'pc'], { encoding: 'utf8' });
  if (result.status !== 0) {
    return [];
  }
  const occupants = [];
  let current = {};
  for (const line of result.stdout.split(/\r?\n/)) {
    if (!line) {
      continue;
    }
    if (line.startsWith('p')) {
      if (current.pid) {
        occupants.push(current);
      }
      current = { pid: line.slice(1) };
    } else if (line.startsWith('c')) {
      current.command = line.slice(1);
    }
  }
  if (current.pid) {
    occupants.push(current);
  }
  return occupants;
}

function formatPortOccupants(port) {
  const occupants = getPortOccupants(port);
  if (occupants.length === 0) {
    return '';
  }
  const text = occupants
    .map(item => `pid=${item.pid}${item.command ? ` command=${item.command}` : ''}`)
    .join(', ');
  return ` (${text})`;
}

function httpOk(url) {
  return new Promise(resolvePromise => {
    const client = url.startsWith('https:') ? https : http;
    const request = client.get(url, { timeout: 2000 }, response => {
      response.resume();
      resolvePromise(response.statusCode >= 200 && response.statusCode < 500);
    });
    request.on('timeout', () => {
      request.destroy();
      resolvePromise(false);
    });
    request.on('error', () => resolvePromise(false));
  });
}

function tailFile(path, lineCount) {
  if (!existsSync(path)) {
    return '';
  }
  const lines = readFileSync(path, 'utf8').split(/\r?\n/);
  return `${lines.slice(Math.max(0, lines.length - lineCount)).join('\n')}\n`;
}

function syncPmoBaseline(argv) {
  const options = parsePmoSyncArgs(argv);
  const targetDir = resolve(process.cwd(), options.projectDir);
  if (!existsSync(targetDir) || !statSync(targetDir).isDirectory()) {
    fail(`project directory not found: ${targetDir}`);
  }
  const variables = buildVariables({
    project: basename(targetDir),
    preset: 'custom',
    topology: 'monolith',
    packageName: 'com.example.mango',
    groupId: 'com.example.mango',
    version: '1.0.0-SNAPSHOT',
    mangoVersion: defaultVersions.mangoBackend,
    npmRegistry: 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/',
    mavenRepository: 'http://nexus.inner.yunxinbaokeji.com/repository/maven-public/',
    modules: 'none',
  });
  const plan = [
    ...planTemplateSync('business-pmo/mango-baseline', targetDir, variables),
    ...planTemplateSync('business-pmo/README.md', targetDir, variables),
    ...planBusinessDocsSync(targetDir, variables),
    planAgentsSync(targetDir, variables, options.writeAgents),
    ...planShellSync(targetDir, variables, options.syncShell),
  ].filter(Boolean);

  const summary = summarizeSyncPlan(plan);
  printPmoSyncPlan(targetDir, plan, options.dryRun);
  if (options.dryRun) {
    return;
  }
  for (const item of plan) {
    if (item.action === 'skip' || item.action === 'warn') {
      continue;
    }
    writePlannedFile(item);
  }
  const synced = summary.add + summary.update;
  process.stdout.write(`PMO baseline sync complete: ${synced} files written, ${summary.skip} skipped.\n`);
}

function parsePmoSyncArgs(argv) {
  const result = {
    projectDir: '.',
    dryRun: false,
    writeAgents: false,
    syncShell: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--dry-run') {
      result.dryRun = true;
      continue;
    }
    if (arg === '--write-agents') {
      result.writeAgents = true;
      continue;
    }
    if (arg === '--sync-shell') {
      result.syncShell = true;
      continue;
    }
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
    fail(`unexpected argument: ${arg}`);
  }
  return result;
}

function planShellSync(targetDir, variables, syncShell) {
  const shellFiles = [
    'scripts/dev-workspace.sh',
    'scripts/backend-dev.sh',
  ];
  if (!syncShell) {
    return [...shellFiles, 'mango.dev.json'].map(path => ({
      action: 'skip',
      reason: 'rerun with --sync-shell to update generated startup entries',
      path,
      targetPath: join(targetDir, path),
    }));
  }
  const plan = shellFiles.map(path => {
    const sourceFile = join(templateRoot, path);
    const targetPath = join(targetDir, path);
    return buildFilePlanItem(path, targetPath, readRenderedTemplateFile(sourceFile, variables));
  });
  const manifestPath = join(targetDir, 'mango.dev.json');
  if (existsSync(manifestPath)) {
    plan.push({
      action: 'skip',
      reason: 'business-owned manifest exists',
      path: 'mango.dev.json',
      targetPath: manifestPath,
    });
  } else {
    plan.push(buildFilePlanItem(
      'mango.dev.json',
      manifestPath,
      readRenderedTemplateFile(join(templateRoot, 'mango.dev.json'), variables),
    ));
  }
  return plan;
}

function planTemplateSync(templateRelativePath, targetDir, variables) {
  const source = join(templateRoot, templateRelativePath);
  const plan = [];
  const sourceFiles = statSync(source).isDirectory() ? walkFiles(source) : [source];
  for (const sourceFile of sourceFiles) {
    const relativeSource = relative(templateRoot, sourceFile);
    const targetRelative = renderTemplateFileName(relativeSource, variables);
    const targetPath = join(targetDir, targetRelative);
    const content = readRenderedTemplateFile(sourceFile, variables);
    plan.push(buildFilePlanItem(targetRelative, targetPath, content));
  }
  return plan;
}

function planBusinessDocsSync(targetDir, variables) {
  const source = join(templateRoot, 'business-docs/plans');
  if (!existsSync(source)) {
    return [];
  }
  const plan = [];
  for (const sourceFile of walkFiles(source)) {
    const relativeSource = relative(templateRoot, sourceFile);
    const targetRelative = renderTemplateFileName(relativeSource, variables);
    const targetPath = join(targetDir, targetRelative);
    if (existsSync(targetPath)) {
      plan.push({
        action: 'skip',
        reason: 'business-doc exists',
        path: targetRelative,
        targetPath,
      });
      continue;
    }
    plan.push(buildFilePlanItem(targetRelative, targetPath, readRenderedTemplateFile(sourceFile, variables)));
  }
  return plan;
}

function planAgentsSync(targetDir, variables, writeAgents) {
  const targetPath = join(targetDir, 'AGENTS.md');
  if (!existsSync(targetPath)) {
    return buildFilePlanItem('AGENTS.md', targetPath, renderBusinessAgents(variables));
  }
  const content = readFileSync(targetPath, 'utf8');
  if (!containsExternalMangoPmoReference(content)) {
    return {
      action: 'skip',
      reason: 'AGENTS.md already business-owned',
      path: 'AGENTS.md',
      targetPath,
    };
  }
  if (!writeAgents) {
    return {
      action: 'warn',
      reason: 'AGENTS.md references external mango-pmo; rerun with --write-agents to migrate',
      path: 'AGENTS.md',
      targetPath,
    };
  }
  return buildFilePlanItem('AGENTS.md', targetPath, renderBusinessAgents(variables));
}

function readRenderedTemplateFile(sourceFile, variables) {
  const buffer = readFileSync(sourceFile);
  return isTextFile(sourceFile) ? render(buffer.toString('utf8'), variables) : buffer;
}

function renderBusinessAgents(variables) {
  const source = join(templateRoot, 'AGENTS.md');
  return render(readFileSync(source, 'utf8'), variables);
}

function containsExternalMangoPmoReference(content) {
  return /\/Users\/[^\s`'"]*\/mango-pmo/.test(content)
    || /\b[A-Za-z]:\\Users\\[^\s`'"]*\\mango-pmo/.test(content);
}

function buildFilePlanItem(path, targetPath, content) {
  if (!existsSync(targetPath)) {
    return {
      action: 'add',
      path,
      targetPath,
      content,
    };
  }
  const current = readFileSync(targetPath);
  const next = Buffer.isBuffer(content) ? content : Buffer.from(content, 'utf8');
  if (Buffer.compare(current, next) === 0) {
    return {
      action: 'skip',
      reason: 'unchanged',
      path,
      targetPath,
    };
  }
  return {
    action: 'update',
    path,
    targetPath,
    content,
  };
}

function writePlannedFile(item) {
  mkdirSync(dirname(item.targetPath), { recursive: true });
  writeFileSync(item.targetPath, item.content);
  if (item.path.endsWith('/tools/pmo-preflight.mjs') || item.path.endsWith('/tools/acceptance-evidence-check.mjs')) {
    chmodSync(item.targetPath, 0o755);
  }
  if (item.path === 'scripts/dev-workspace.sh' || item.path === 'scripts/backend-dev.sh') {
    chmodSync(item.targetPath, 0o755);
  }
}

function summarizeSyncPlan(plan) {
  return plan.reduce((summary, item) => {
    summary[item.action] = (summary[item.action] || 0) + 1;
    return summary;
  }, { add: 0, update: 0, skip: 0, warn: 0 });
}

function printPmoSyncPlan(targetDir, plan, dryRun) {
  const summary = summarizeSyncPlan(plan);
  process.stdout.write(`${dryRun ? 'PMO baseline dry-run plan' : 'PMO baseline sync plan'} for ${relativeOrAbsolute(process.cwd(), targetDir)}\n`);
  process.stdout.write(`  add: ${summary.add}, update: ${summary.update}, skip: ${summary.skip}, warn: ${summary.warn}\n`);
  for (const item of plan) {
    const reason = item.reason ? ` (${item.reason})` : '';
    process.stdout.write(`  ${item.action.padEnd(6)} ${item.path}${reason}\n`);
  }
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

function printChangelog() {
  const changelogPath = join(packageRoot, 'CHANGELOG.md');
  if (!existsSync(changelogPath)) {
    fail('CHANGELOG.md is missing from @mango/cli package');
  }
  process.stdout.write(readFileSync(changelogPath, 'utf8').trimEnd());
  process.stdout.write('\n');
}

function readReleasedMangoPackageVersion(packageName, fallback) {
  return releaseVersions.npm?.[`@mango/${packageName}`] || fallback;
}

function readCliVersion() {
  const packagePath = join(packageRoot, 'package.json');
  if (!existsSync(packagePath)) {
    return 'unknown';
  }
  return JSON.parse(readFileSync(packagePath, 'utf8')).version || 'unknown';
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
  process.stdout.write('  scripts/dev-workspace.sh init\n');
  process.stdout.write('  scripts/dev-workspace.sh start\n');
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

main().catch(error => {
  console.error(`Error: ${error.message}`);
  process.exit(1);
});
