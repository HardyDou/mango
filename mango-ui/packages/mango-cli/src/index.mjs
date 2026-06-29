#!/usr/bin/env node
import { appendFileSync, chmodSync, closeSync, copyFileSync, existsSync, mkdirSync, openSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { spawn, spawnSync } from 'node:child_process';
import { createHash, randomBytes } from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
import { createRequire } from 'node:module';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const requireFromCli = createRequire(import.meta.url);
const currentFile = fileURLToPath(import.meta.url);
const packageRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(packageRoot, '../../..');
const templateRoot = resolve(packageRoot, 'templates/full');
const bundledPmoPackageRoot = resolve(packageRoot, '../mango-pmo');
const businessModuleTemplateRoot = resolve(packageRoot, 'templates/business-module');
const businessStarterRoot = existsSync(businessModuleTemplateRoot)
  ? businessModuleTemplateRoot
  : resolve(repoRoot, 'mango-business-starter');
const releaseVersions = readReleaseVersions();
const adminModulesManifest = readAdminModulesManifest();

const defaultVersions = {
  mangoBackend: releaseVersions.maven?.mangoBackend || '1.0.0-SNAPSHOT',
  mangoAdmin: readReleasedMangoPackageVersion('admin', '1.0.11'),
  mangoAdminPages: readReleasedMangoPackageVersion('admin-pages', '1.0.5'),
  mangoAdminShell: readReleasedMangoPackageVersion('admin-shell', '1.0.10'),
  mangoAppRuntime: readReleasedMangoPackageVersion('app-runtime', '1.0.2'),
  mangoAuth: readReleasedMangoPackageVersion('auth', '1.0.4'),
  mangoCalendar: readReleasedMangoPackageVersion('calendar', '1.0.6'),
  mangoCms: readReleasedMangoPackageVersion('cms', '1.0.0'),
  mangoCommon: readReleasedMangoPackageVersion('common', '1.0.7'),
  mangoFile: readReleasedMangoPackageVersion('file', '1.0.6'),
  mangoGridLayout: readReleasedMangoPackageVersion('grid-layout', '1.0.0'),
  mangoGridWidgets: readReleasedMangoPackageVersion('grid-widgets', '1.0.0'),
  mangoJob: readReleasedMangoPackageVersion('job', '1.0.0'),
  mangoNotice: readReleasedMangoPackageVersion('notice', '1.0.6'),
  mangoNumgen: readReleasedMangoPackageVersion('numgen', '1.0.6'),
  mangoPayment: readReleasedMangoPackageVersion('payment', '1.0.1'),
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

const ADMIN_DEFAULT_MODULES = normalizeAdminModules(adminModulesManifest.defaultPackages);
const ADMIN_FULL_MODULES = normalizeAdminModules(adminModulesManifest.fullPackages);

const CORE_FRONTEND_PACKAGES = uniqueBy([
  { name: '@mango/admin', versionKey: 'mangoAdmin' },
  { name: '@mango/admin-pages', versionKey: 'mangoAdminPages' },
  { name: '@mango/app-runtime', versionKey: 'mangoAppRuntime' },
  ...ADMIN_DEFAULT_MODULES.map(toFrontendDependency),
], dependency => dependency.name);

const ADMIN_OPTIONAL_PEER_PACKAGES = ADMIN_FULL_MODULES.map(toFrontendDependency);

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

const BUSINESS_BACKEND_API_MANAGED_DEPENDENCIES = [
  { groupId: 'io.mango.infra.context', artifactId: 'mango-infra-context-api' },
  { groupId: 'io.mango.infra.event', artifactId: 'mango-infra-event-api' },
  { groupId: 'io.mango.infra.fileproc', artifactId: 'mango-infra-fileproc-api' },
  { groupId: 'io.mango.infra.ip.location', artifactId: 'mango-infra-ip-location-api' },
  { groupId: 'io.mango.infra.kv', artifactId: 'mango-infra-kv-api' },
  { groupId: 'io.mango.infra.log', artifactId: 'mango-infra-log-api' },
  { groupId: 'io.mango.infra.module', artifactId: 'mango-infra-module-api' },
  { groupId: 'io.mango.infra.persistence', artifactId: 'mango-infra-persistence-api' },
  { groupId: 'io.mango.infra.realtime', artifactId: 'mango-infra-realtime-api' },
  { groupId: 'io.mango.infra.sensitive', artifactId: 'mango-infra-sensitive-api' },
  { groupId: 'io.mango.infra.web', artifactId: 'mango-infra-web-api' },
  { groupId: 'io.mango.platform.access', artifactId: 'mango-access-api' },
  { groupId: 'io.mango.platform.auth', artifactId: 'mango-auth-api' },
  { groupId: 'io.mango.platform.authorization', artifactId: 'mango-authorization-api' },
  { groupId: 'io.mango.platform.calendar', artifactId: 'mango-calendar-api' },
  { groupId: 'io.mango.platform.captcha', artifactId: 'mango-captcha-api' },
  { groupId: 'io.mango.platform.cms', artifactId: 'mango-cms-api' },
  { groupId: 'io.mango.platform.domain', artifactId: 'mango-domain-api' },
  { groupId: 'io.mango.platform.file', artifactId: 'mango-file-api' },
  { groupId: 'io.mango.platform.file.preview', artifactId: 'mango-file-preview-api' },
  { groupId: 'io.mango.platform.gridlayout', artifactId: 'mango-grid-layout-api' },
  { groupId: 'io.mango.platform.identity', artifactId: 'mango-identity-api' },
  { groupId: 'io.mango.platform.job', artifactId: 'mango-job-api' },
  { groupId: 'io.mango.platform.notice', artifactId: 'mango-notice-api' },
  { groupId: 'io.mango.platform.numgen', artifactId: 'mango-numgen-api' },
  { groupId: 'io.mango.platform.org', artifactId: 'mango-org-api' },
  { groupId: 'io.mango.platform.payment', artifactId: 'mango-payment-api' },
  { groupId: 'io.mango.platform.resource', artifactId: 'mango-resource-api' },
  { groupId: 'io.mango.platform.system', artifactId: 'mango-system-api' },
  { groupId: 'io.mango.platform.template', artifactId: 'mango-template-api' },
  { groupId: 'io.mango.platform.workflow', artifactId: 'mango-workflow-api' },
];

const OPTIONAL_MODULE_OVERLAYS = [
  {
    code: 'file',
    label: '文件中心',
    feature: 'file',
    backend: [
      { groupId: 'io.mango.platform.file', artifactId: 'mango-file-starter' },
      { groupId: 'io.mango.platform.file.preview', artifactId: 'mango-file-preview-starter' },
    ],
  },
  {
    code: 'template',
    label: '模板管理',
    feature: 'template',
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
    code: 'cms',
    label: '内容中心',
    feature: 'cms',
    runtimeModule: {
      moduleCode: 'mango-cms',
      local: { mode: 'local', runtimeCode: 'mango-admin-cms-local' },
      micro: { mode: 'micro', runtimeCode: 'mango-admin-cms-app', entry: 'http://e.mango.io:5184/' },
    },
    backend: [
      { groupId: 'io.mango.platform.cms', artifactId: 'mango-cms-starter' },
    ],
  },
  {
    code: 'notice',
    label: '通知中心',
    feature: 'notice',
    backend: [
      { groupId: 'io.mango.platform.notice', artifactId: 'mango-notice-starter' },
    ],
  },
  {
    code: 'numgen',
    label: '编号规则',
    feature: 'numgen',
    backend: [
      { groupId: 'io.mango.platform.numgen', artifactId: 'mango-numgen-starter' },
    ],
  },
  {
    code: 'calendar',
    label: '工作日历',
    feature: 'calendar',
    backend: [
      { groupId: 'io.mango.platform.calendar', artifactId: 'mango-calendar-starter' },
    ],
  },
  {
    code: 'payment',
    label: '支付中心',
    feature: 'payment',
    backend: [
      { groupId: 'io.mango.platform.payment', artifactId: 'mango-payment-starter' },
    ],
  },
  {
    code: 'workflow',
    label: '审批中心',
    feature: 'workflow',
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
    dependsOn: ['workflow'],
  },
];

const OPTIONAL_MODULES = buildOptionalModules(ADMIN_FULL_MODULES, OPTIONAL_MODULE_OVERLAYS);
const MODULE_BY_CODE = new Map(OPTIONAL_MODULES.map(module => [module.code, module]));
const FULL_MODULE_CODES = OPTIONAL_MODULES.map(module => module.code);

const usage = `
Mango CLI

Usage:
  mango init <project> --preset full [options]
  mango init <project> --preset custom --modules workflow,template [options]
  mango add <module...> [options]
  mango workspace init
  mango workspace status
  mango workspace doctor
  mango workspace release [--workspace <path>]
  mango dev start [group|app...]
  mango dev stop [app...]
  mango dev status
  mango dev doctor
  mango dev logs <app>
  mango frontend prepare
  mango frontend doctor
  mango changelog
  mango pmo status --project-dir <dir>
  mango pmo check --project-dir <dir>
  mango pmo sync --project-dir <dir> [--dry-run] [--write-agents] [--sync-shell]
  mango pmo upgrade --project-dir <dir> [--dry-run] [--write-agents] [--sync-shell]
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
  ports stay in .mango/workspace.json and .mango/dev-workspace.env and must not be committed.
  scripts/dev-workspace.sh is a deprecated compatibility entry; use mango workspace/dev/frontend commands.

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

  if (args[0] === 'workspace') {
    await runWorkspaceCommand(args[1], args.slice(2));
    return;
  }

  if (args[0] === 'dev') {
    await runDevCommand(args[1], args.slice(2));
    return;
  }

  if (args[0] === 'frontend') {
    await runFrontendCommand(args[1], args.slice(2));
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
    runPmoCommand(args[1], args.slice(2));
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
  installPmoBaseline(targetDir);
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
  const frontendVersions = Object.fromEntries(
    [...CORE_FRONTEND_PACKAGES, ...ADMIN_OPTIONAL_PEER_PACKAGES].map(dependency => [
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
    mangoGridLayoutVersion: defaultVersions.mangoGridLayout,
    mangoGridWidgetsVersion: defaultVersions.mangoGridWidgets,
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
    frontendPackageDependencies: renderFrontendPackageDependencies(frontendVersions),
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
const WORKSPACE_SLOT_COUNT = 200;
const BACKEND_PORT_BASE = 18000;
const FRONTEND_PORT_BASE = 8600;
const FRONTEND_SLOT_SIZE = 20;
const DEFAULT_FRONTEND_APP_PORT_OFFSETS = {
  MANGO_ADMIN_SHELL_PORT: 1,
  MANGO_ADMIN_RBAC_APP_PORT: 6,
  MANGO_ADMIN_WORKFLOW_APP_PORT: 7,
  MANGO_ADMIN_TEMPLATE_APP_PORT: 8,
  MANGO_ADMIN_CMS_APP_PORT: 9,
  MANGO_SITE_ENTERPRISE_APP_PORT: 16,
  MANGO_SITE_HELP_APP_PORT: 17,
  MANGO_SITE_DEMO_APP_PORT: 18,
};

function isDevWorkspaceCommand(command) {
  return DEV_WORKSPACE_COMMANDS.has(command);
}

async function runWorkspaceCommand(command = 'status', argv = []) {
  const normalized = command === 'init-dev' ? 'init' : command;
  if (normalized === 'list') {
    listWorkspaceRegistry(process.cwd());
    return;
  }
  const context = normalized === 'init'
    ? loadDevWorkspaceContext({ allowMissingManifest: true })
    : loadDevWorkspaceContext({ allowMissingManifest: false });

  if (normalized === 'init') {
    initDevWorkspace(context);
    return;
  }
  if (normalized === 'status' || normalized === 'print') {
    printDevWorkspace(context);
    return;
  }
  if (normalized === 'doctor') {
    doctorDevWorkspace(context);
    return;
  }
  if (normalized === 'release') {
    releaseWorkspaceCommand(context, argv);
    return;
  }
  fail(`unknown workspace command: ${command || ''}`);
}

async function runDevCommand(command = 'start', argv = []) {
  const normalized = command || 'start';
  const context = loadDevWorkspaceContext({ allowMissingManifest: false });
  if (normalized === 'start') {
    await startDevWorkspace(context, argv);
    return;
  }
  if (normalized === 'stop') {
    await stopDevWorkspace(context, argv);
    return;
  }
  if (normalized === 'status') {
    printDevWorkspaceStatus(context);
    return;
  }
  if (normalized === 'doctor') {
    doctorDevWorkspace(context);
    return;
  }
  if (normalized === 'plan') {
    printDevWorkspacePlan(context, argv);
    return;
  }
  if (normalized === 'logs') {
    printDevWorkspaceLogs(context, argv);
    return;
  }
  if (normalized === 'backend') {
    await startDevWorkspace(context, ['backend', ...argv]);
    return;
  }
  if (normalized === 'frontend') {
    await startDevWorkspace(context, ['frontend', ...argv]);
    return;
  }
  fail(`unknown dev command: ${command || ''}`);
}

async function runFrontendCommand(command = 'prepare', argv = []) {
  const normalized = command || 'prepare';
  const context = loadDevWorkspaceContext({ allowMissingManifest: false });
  if (normalized === 'prepare') {
    prepareFrontendWorkspace(context, { checkOnly: false });
    return;
  }
  if (normalized === 'doctor') {
    prepareFrontendWorkspace(context, { checkOnly: true });
    return;
  }
  if (normalized === 'start') {
    await startDevWorkspace(context, ['frontend', ...argv]);
    return;
  }
  fail(`unknown frontend command: ${command || ''}`);
}

async function runDevWorkspaceCommand(command, argv) {
  if (command === 'frontend') {
    await runDevCommand('frontend', argv);
    return;
  }
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
        workspacePath: join(process.cwd(), '.mango/workspace.json'),
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
    workspacePath: join(root, '.mango/workspace.json'),
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
  const workspace = ensureWorkspaceConfig(context.root);
  if (!existsSync(envPath)) {
    writeFileSync(envPath, defaultDevWorkspaceEnv(context.root, workspace));
    process.stdout.write(`Created local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  } else {
    ensureDevWorkspaceEnv(context);
    process.stdout.write(`Workspace env already exists: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  }
  process.stdout.write(`Workspace slot ${workspace.slot}: ${relativeOrAbsolute(process.cwd(), join(context.root, '.mango/workspace.json'))}\n`);

  if (!existsSync(context.manifestPath)) {
    const { manifest, warnings } = createBusinessDevManifest(context.root, basename(context.root));
    writeFileSync(context.manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
    process.stdout.write(`Created development manifest: ${relativeOrAbsolute(process.cwd(), context.manifestPath)}\n`);
    for (const warning of warnings) {
      process.stdout.write(`warn    mango.dev.json ${warning}\n`);
    }
  }
}

function defaultDevWorkspaceEnv(root, workspace = ensureWorkspaceConfig(root)) {
  const frontendPorts = workspace.frontendApps || buildFrontendAppPorts(workspace.frontendPort);
  return [
    '# Mango local workspace configuration.',
    '# This file is generated once per workspace and must not be committed.',
    `MANGO_WORKSPACE_ID=${workspace.workspaceId}`,
    `MANGO_CRYPTO_SM4_SECRET_KEY=${randomBytes(16).toString('hex')}`,
    `MANGO_BACKEND_PORT=${workspace.backendPort}`,
    `MANGO_FRONTEND_PORT=${workspace.frontendPort}`,
    ...Object.entries(frontendPorts).map(([key, value]) => `${key}=${value}`),
    'MANGO_FRONTEND_HOST=127.0.0.1',
    'MANGO_FRONTEND_OPEN=false',
    'MANGO_FRONTEND_AUTO_INSTALL=true',
    'MANGO_FRONTEND_MODE=source',
    `MANGO_DB_NAME=${workspace.dbName}`,
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

function ensureWorkspaceConfig(root) {
  const workspacePath = join(root, '.mango/workspace.json');
  if (existsSync(workspacePath)) {
    const workspace = readJsonFile(workspacePath);
    registerWorkspace(root, workspace);
    return workspace;
  }
  const workspace = allocateDevWorkspace(root);
  mkdirSync(dirname(workspacePath), { recursive: true });
  writeFileSync(workspacePath, `${JSON.stringify(workspace, null, 2)}\n`);
  return workspace;
}

function allocateDevWorkspace(root) {
  const normalizedRoot = resolve(root);
  const registry = readWorkspaceRegistry(normalizedRoot);
  const existing = registry.find(entry => entry.root === normalizedRoot);
  if (existing) {
    return existing;
  }
  const usedSlots = new Set(registry.map(entry => Number(entry.slot)).filter(Number.isFinite));
  const usedPorts = new Set(registry.flatMap(entry => workspacePorts(entry)));
  const usedDbNames = new Set(registry.map(entry => entry.dbName));
  for (let slot = 1; slot <= WORKSPACE_SLOT_COUNT; slot += 1) {
    const candidate = buildWorkspaceConfig(normalizedRoot, slot);
    if (usedSlots.has(slot)
      || workspacePorts(candidate).some(port => usedPorts.has(port) || isPortInUse(port))
      || usedDbNames.has(candidate.dbName)) {
      continue;
    }
    writeWorkspaceRegistry(normalizedRoot, [...registry, candidate]);
    return candidate;
  }
  fail('unable to allocate free Mango workspace slot. Run mango workspace list, stop conflicting services, or set MANGO_WORKSPACE_REGISTRY.');
}

function buildWorkspaceConfig(root, slot) {
  const frontendPort = FRONTEND_PORT_BASE + slot * FRONTEND_SLOT_SIZE;
  return {
    version: 1,
    root,
    workspaceId: `mango_${slot.toString().padStart(3, '0')}`,
    slot,
    backendPort: BACKEND_PORT_BASE + slot,
    frontendPort,
    frontendApps: buildFrontendAppPorts(frontendPort),
    dbName: `mango_dev_${slot.toString().padStart(3, '0')}`,
  };
}

function buildFrontendAppPorts(frontendPort) {
  return Object.fromEntries(
    Object.entries(DEFAULT_FRONTEND_APP_PORT_OFFSETS)
      .map(([key, offset]) => [key, frontendPort + offset]),
  );
}

function workspacePorts(workspace) {
  return [
    Number(workspace.backendPort),
    Number(workspace.frontendPort),
    ...Object.values(workspace.frontendApps || {}).map(Number),
  ].filter(Number.isFinite);
}

function registerWorkspace(root, workspace) {
  const normalizedRoot = resolve(root);
  const registry = readWorkspaceRegistry(normalizedRoot).filter(entry => entry.root !== normalizedRoot);
  writeWorkspaceRegistry(normalizedRoot, [
    ...registry,
    {
      ...workspace,
      root: normalizedRoot,
      lastActiveAt: new Date().toISOString(),
    },
  ]);
}

function workspaceRegistryPath(root) {
  if (process.env.MANGO_WORKSPACE_REGISTRY) {
    return resolve(process.env.MANGO_WORKSPACE_REGISTRY);
  }
  const home = process.env.HOME || root;
  return join(home, '.mango/workspaces.json');
}

function readWorkspaceRegistry(root) {
  const path = workspaceRegistryPath(root);
  if (!existsSync(path)) {
    return [];
  }
  const content = readFileSync(path, 'utf8').trim();
  if (!content) {
    return [];
  }
  if (content.startsWith('[')) {
    return JSON.parse(content).filter(isValidWorkspaceRegistryEntry);
  }
  return content
    .split(/\r?\n/)
    .filter(Boolean)
    .map(line => {
      const [entryRoot, workspaceId, backendPort, frontendPort, dbName] = line.split('\t');
      const slot = Number(String(workspaceId || '').replace(/\D/g, ''));
      const migrated = buildWorkspaceConfig(entryRoot, Number.isFinite(slot) && slot > 0 ? slot : 1);
      return {
        ...migrated,
        workspaceId: workspaceId || migrated.workspaceId,
        backendPort: Number(backendPort) || migrated.backendPort,
        frontendPort: Number(frontendPort) || migrated.frontendPort,
        dbName: dbName || migrated.dbName,
      };
    })
    .filter(isValidWorkspaceRegistryEntry);
}

function isValidWorkspaceRegistryEntry(entry) {
  return Boolean(entry?.root && entry.workspaceId && entry.slot && entry.backendPort && entry.frontendPort && entry.dbName);
}

function writeWorkspaceRegistry(root, entries) {
  const path = workspaceRegistryPath(root);
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${JSON.stringify(entries, null, 2)}\n`);
}

function listWorkspaceRegistry(root) {
  const registry = readWorkspaceRegistry(root);
  if (registry.length === 0) {
    process.stdout.write('No Mango workspaces registered on this machine.\n');
    return;
  }
  for (const entry of registry.sort((left, right) => Number(left.slot) - Number(right.slot))) {
    process.stdout.write([
      `slot=${entry.slot}`,
      `id=${entry.workspaceId}`,
      `backend=${entry.backendPort}`,
      `frontend=${entry.frontendPort}`,
      `db=${entry.dbName}`,
      entry.root,
    ].join(' ') + '\n');
  }
}

function releaseWorkspaceCommand(context, argv) {
  const workspacePath = readOptionValue(argv, '--workspace') || context.root;
  const targetRoot = resolve(workspacePath);
  const registry = readWorkspaceRegistry(context.root);
  const next = registry.filter(entry => resolve(entry.root) !== targetRoot);
  writeWorkspaceRegistry(context.root, next);
  process.stdout.write(`Released Mango workspace registration: ${targetRoot}\n`);
}

function readOptionValue(argv, name) {
  const index = argv.indexOf(name);
  if (index < 0) {
    return '';
  }
  const value = argv[index + 1];
  if (!value || value.startsWith('--')) {
    fail(`missing value for ${name}`);
  }
  return value;
}

function numericHash(value) {
  return Number.parseInt(createHash('sha256').update(value).digest('hex').slice(0, 8), 16);
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
          VITE_ADMIN_PROXY_PATH: 'http://127.0.0.1:${apps.backend.port}',
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

function createBusinessDevManifest(targetDir, projectName) {
  const discovery = discoverBusinessDevApps(targetDir);
  if (discovery.apps.length === 0) {
    return {
      manifest: defaultBusinessDevManifest(projectName),
      warnings: [
        'used default backend/frontend layout; confirm backend/app/pom.xml and frontend exist before starting',
      ],
    };
  }
  const apps = Object.fromEntries(discovery.apps.map(app => [app.name, app.config]));
  const backendNames = discovery.apps.filter(app => app.kind === 'backend').map(app => app.name);
  const frontendNames = discovery.apps.filter(app => app.kind === 'frontend').map(app => app.name);
  const defaultGroup = [
    ...(backendNames.length > 0 ? [backendNames[0]] : []),
    ...(frontendNames.length > 0 ? [frontendNames[0]] : []),
  ];
  const groups = {};
  if (defaultGroup.length > 0) {
    groups.default = defaultGroup;
  }
  if (backendNames.length > 0) {
    groups.backend = backendNames;
  }
  if (frontendNames.length > 0) {
    groups.frontend = frontendNames;
  }
  const warnings = [];
  if (backendNames.length === 0) {
    warnings.push('no Spring Boot Maven app POM detected; add backend app manually if needed');
  }
  if (frontendNames.length === 0) {
    warnings.push('no Vite frontend app detected; add frontend app manually if needed');
  }
  if (backendNames.length > 1 || frontendNames.length > 1) {
    warnings.push(`detected ${backendNames.length} backend app(s) and ${frontendNames.length} frontend app(s); confirm groups before starting`);
  }
  for (const warning of discovery.warnings) {
    warnings.push(warning);
  }
  return {
    manifest: {
      version: 1,
      groups,
      apps,
      metadata: {
        project: projectName,
        generatedFrom: 'discovered-project-layout',
      },
    },
    warnings,
  };
}

function discoverBusinessDevApps(root) {
  const files = walkProjectFiles(root, {
    fileNames: new Set(['pom.xml', 'package.json']),
    maxDepth: 6,
  });
  const pomFiles = files.filter(file => basename(file) === 'pom.xml');
  const packageFiles = files.filter(file => basename(file) === 'package.json');
  const backendApps = discoverSpringBootApps(root, pomFiles);
  const frontendApps = discoverViteApps(root, packageFiles);
  const apps = [];
  const usedNames = new Set();
  backendApps.forEach((app, index) => {
    const name = uniqueAppName(toAppName(app.relativeDir, 'backend'), usedNames);
    apps.push({
      kind: 'backend',
      name,
      config: buildSpringBootDevApp(app.relativeDir, index),
    });
  });
  const firstBackend = apps.find(app => app.kind === 'backend')?.name || '';
  frontendApps.forEach((app, index) => {
    const name = uniqueAppName(toAppName(app.relativeDir, 'frontend'), usedNames);
    apps.push({
      kind: 'frontend',
      name,
      config: buildViteDevApp(root, app.relativeDir, index, firstBackend),
    });
  });
  return {
    apps,
    warnings: buildDiscoveryWarnings(root, pomFiles, backendApps, packageFiles, frontendApps),
  };
}

function discoverSpringBootApps(root, pomFiles) {
  return pomFiles
    .map(file => {
      const content = readFileSync(file, 'utf8');
      return {
        file,
        relativeDir: toPosix(relative(root, dirname(file))) || '.',
        content,
      };
    })
    .filter(item => isSpringBootAppPom(item.content))
    .sort((left, right) => left.relativeDir.localeCompare(right.relativeDir));
}

function isSpringBootAppPom(content) {
  if (isAggregatorPom(content)) {
    return false;
  }
  return content.includes('<artifactId>spring-boot-maven-plugin</artifactId>')
    || content.includes('<artifactId>spring-boot-starter-web</artifactId>')
    || content.includes('<artifactId>spring-boot-starter-webflux</artifactId>')
    || content.includes('<artifactId>mango-admin-starter</artifactId>')
    || content.includes('<artifactId>mango-monolith-starter</artifactId>');
}

function isAggregatorPom(content) {
  return content.includes('<packaging>pom</packaging>') && content.includes('<modules>');
}

function discoverViteApps(root, packageFiles) {
  return packageFiles
    .map(file => {
      const directory = dirname(file);
      const packageJson = readJsonFile(file);
      return {
        file,
        relativeDir: toPosix(relative(root, directory)) || '.',
        packageJson,
        hasViteConfig: hasViteConfig(directory),
      };
    })
    .filter(item => isVitePackage(item.packageJson, item.hasViteConfig))
    .sort((left, right) => left.relativeDir.localeCompare(right.relativeDir));
}

function isVitePackage(packageJson, hasViteConfigFile) {
  const scripts = packageJson.scripts || {};
  const dependencies = {
    ...(packageJson.dependencies || {}),
    ...(packageJson.devDependencies || {}),
  };
  return Boolean(scripts.dev) && (Boolean(dependencies.vite) || hasViteConfigFile);
}

function hasViteConfig(directory) {
  return ['vite.config.ts', 'vite.config.js', 'vite.config.mjs', 'vite.config.mts']
    .some(file => existsSync(join(directory, file)));
}

function buildSpringBootDevApp(cwd, index) {
  const app = {
    type: 'spring-boot-maven',
    cwd,
    pom: 'pom.xml',
    port: 5555 + index,
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
  };
  if (index === 0) {
    app.portEnv = 'MANGO_BACKEND_PORT';
  }
  return app;
}

function buildViteDevApp(root, cwd, index, backendName) {
  const appRoot = resolve(root, cwd);
  const app = {
    type: 'vite',
    cwd,
    packageManager: detectPackageManager(appRoot, root),
    port: 5176 + index,
    hostEnv: 'MANGO_FRONTEND_HOST',
    host: '127.0.0.1',
    env: {
      VITE_PORT: '${port}',
      VITE_HOST: '${host}',
      VITE_OPEN: '${env.MANGO_FRONTEND_OPEN}',
    },
    args: ['run', 'dev', '--', '--host', '${host}', '--port', '${port}'],
  };
  if (backendName) {
    app.dependsOn = [backendName];
    app.env.VITE_ADMIN_PROXY_PATH = `http://127.0.0.1:\${apps.${backendName}.port}`;
  }
  if (index === 0) {
    app.portEnv = 'MANGO_FRONTEND_PORT';
  }
  return app;
}

function buildDiscoveryWarnings(root, pomFiles, backendApps, packageFiles, frontendApps) {
  const warnings = [];
  const springAppPaths = new Set(backendApps.map(app => app.file));
  const skippedAggregatorPoms = pomFiles
    .filter(file => !springAppPaths.has(file))
    .filter(file => isAggregatorPom(readFileSync(file, 'utf8')))
    .map(file => toPosix(relative(root, file)));
  if (skippedAggregatorPoms.length > 0) {
    warnings.push(`skipped aggregator POM(s): ${skippedAggregatorPoms.join(', ')}`);
  }
  if (packageFiles.length > 0 && frontendApps.length === 0) {
    warnings.push('package.json files exist but no Vite dev app was detected');
  }
  return warnings;
}

function walkProjectFiles(root, options) {
  const result = [];
  const excludedDirectories = new Set([
    '.git',
    '.mango',
    '.runtime',
    'node_modules',
    'target',
    'dist',
    'build',
    'coverage',
    'business-pmo',
    'business-docs',
  ]);
  const visit = (directory, depth) => {
    if (depth > options.maxDepth) {
      return;
    }
    for (const entry of readdirSync(directory)) {
      const path = join(directory, entry);
      const stats = statSync(path);
      if (stats.isDirectory()) {
        if (!excludedDirectories.has(entry)) {
          visit(path, depth + 1);
        }
        continue;
      }
      if (stats.isFile() && options.fileNames.has(entry)) {
        result.push(path);
      }
    }
  };
  visit(root, 0);
  return result;
}

function uniqueAppName(baseName, usedNames) {
  let name = baseName;
  let index = 2;
  while (usedNames.has(name)) {
    name = `${baseName}-${index}`;
    index += 1;
  }
  usedNames.add(name);
  return name;
}

function toAppName(relativeDir, fallback) {
  const normalized = relativeDir === '.' ? fallback : relativeDir.split('/').filter(Boolean).pop() || fallback;
  const name = normalized
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/[^a-zA-Z0-9-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase();
  return /^[a-zA-Z0-9]/.test(name) ? name : fallback;
}

function printDevWorkspace(context) {
  validateDevWorkspace(context, { verbose: false });
  const workspace = ensureWorkspaceConfig(context.root);
  process.stdout.write(`Workspace: ${context.root}\n`);
  process.stdout.write(`Workspace ID: ${workspace.workspaceId} slot=${workspace.slot}\n`);
  process.stdout.write(`Manifest:  ${context.manifestPath}\n`);
  process.stdout.write(`Workspace: ${context.workspacePath}\n`);
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
      process.stdout.write(`warn    ${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}\n`);
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
      fail(`${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}.`);
    }
    if (resolved.type === 'vite') {
      prepareFrontendWorkspace(context, { checkOnly: false });
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
  const workspace = ensureWorkspaceConfig(context.root);
  if (!env.MANGO_CRYPTO_SM4_SECRET_KEY) {
    appendFileSync(envPath, `\nMANGO_CRYPTO_SM4_SECRET_KEY=${randomBytes(16).toString('hex')}\n`);
    process.stdout.write(`Added MANGO_CRYPTO_SM4_SECRET_KEY to local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  }
  const requiredValues = {
    MANGO_WORKSPACE_ID: workspace.workspaceId,
    MANGO_BACKEND_PORT: workspace.backendPort,
    MANGO_FRONTEND_PORT: workspace.frontendPort,
    MANGO_DB_NAME: workspace.dbName,
    MANGO_FRONTEND_MODE: 'source',
    ...workspace.frontendApps,
  };
  const missing = Object.entries(requiredValues).filter(([key]) => !env[key]);
  if (missing.length > 0) {
    appendFileSync(envPath, `\n${missing.map(([key, value]) => `${key}=${value}`).join('\n')}\n`);
    process.stdout.write(`Added workspace ownership values to local env: ${missing.map(([key]) => key).join(', ')}\n`);
  }
}

function startDevApp(context, name, app) {
  const logPath = join(context.logDir, `${name}.log`);
  appendFileSync(logPath, `\n--- ${new Date().toISOString()} start ${name} ---\n`);
  if (shouldEnsureWorkspaceDatabase(context, app)) {
    ensureWorkspaceDatabase(context, name, logPath);
  }
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
    workspaceId: context.env.MANGO_WORKSPACE_ID || '',
    workspaceRoot: context.root,
  });
  process.stdout.write(`${name}: started pid=${child.pid} log=${relativeOrAbsolute(process.cwd(), logPath)}\n`);
}

function shouldEnsureWorkspaceDatabase(context, app) {
  if (app.type === 'spring-boot-maven') {
    return true;
  }
  const dbName = context.env.MANGO_DB_NAME || '';
  const values = [
    app.command,
    ...(app.args || []),
    ...Object.values(app.env || {}),
    app.install?.command,
    ...(app.install?.args || []),
  ].filter(Boolean);
  return values.some(value => {
    const text = String(value);
    return text.includes('MANGO_DB_NAME') || (dbName && text.includes(dbName));
  });
}

function ensureWorkspaceDatabase(context, appName, logPath) {
  if (!isTruthy(context.env.MANGO_DB_AUTO_CREATE)) {
    return;
  }
  const dbName = context.env.MANGO_DB_NAME || '';
  if (!/^mango_dev_[a-zA-Z0-9_]+$/.test(dbName)) {
    fail(`${appName}: refuse to auto-create non-workspace database: ${dbName || '<empty>'}`);
  }
  const mysqlArgs = [
    '--protocol=TCP',
    '-h', context.env.MANGO_DB_HOST || '127.0.0.1',
    '-P', String(context.env.MANGO_DB_PORT || '3306'),
    '-u', context.env.MANGO_DB_USERNAME || 'root',
    '-e', `CREATE DATABASE IF NOT EXISTS \`${dbName}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`,
  ];
  const env = { ...process.env };
  if (context.env.MANGO_DB_PASSWORD) {
    env.MYSQL_PWD = context.env.MANGO_DB_PASSWORD;
  }
  const result = spawnSync('mysql', mysqlArgs, {
    cwd: context.root,
    env,
    encoding: 'utf8',
  });
  if (result.stdout) {
    appendFileSync(logPath, result.stdout);
  }
  if (result.stderr) {
    appendFileSync(logPath, result.stderr);
  }
  if (result.error) {
    appendFileSync(logPath, `${result.error.message}\n`);
  }
  if (result.status !== 0) {
    const reason = result.error ? `: ${result.error.message}` : '';
    fail(`${appName}: failed to auto-create database ${dbName}${reason}, see ${relativeOrAbsolute(process.cwd(), logPath)}`);
  }
  process.stdout.write(`${appName}: ensured database ${dbName}\n`);
}

function isTruthy(value) {
  return ['1', 'true', 'yes', 'on'].includes(String(value || '').trim().toLowerCase());
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
  ensureDevWorkspaceEnv(context);
  context.env = { ...parseEnvText(defaultDevWorkspaceEnv(context.root)), ...readEnvFile(join(context.root, '.mango/dev-workspace.env')) };
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    const pidInfo = readPidFile(context, name);
    const alive = pidInfo && isProcessAlive(pidInfo.pid);
    const occupied = !alive && resolved.port && isPortInUse(resolved.port);
    const status = alive ? 'running' : occupied ? 'occupied' : 'stopped';
    const pidText = alive ? ` pid=${pidInfo.pid}` : '';
    const urlText = resolved.url ? ` ${resolved.url}` : '';
    const ownerText = alive && pidInfo.workspaceRoot ? ` owner=${relativeOrAbsolute(process.cwd(), pidInfo.workspaceRoot)}` : '';
    const occupantText = occupied ? `${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}` : '';
    process.stdout.write(`${status.padEnd(8)} ${name}${pidText}${urlText}${ownerText}${occupantText}\n`);
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

function prepareFrontendWorkspace(context, { checkOnly }) {
  const frontendRoots = uniqueBy(
    Object.values(context.manifest.apps || {})
      .filter(app => app.type === 'vite')
      .map(app => resolve(context.root, app.cwd || '.')),
    item => item,
  );
  if (frontendRoots.length === 0) {
    process.stdout.write('No Vite frontend apps found.\n');
    return;
  }
  for (const appRoot of frontendRoots) {
    const uiRoot = findFrontendWorkspaceRoot(appRoot, context.root);
    const packageManager = detectPackageManager(appRoot, context.root);
    const mode = context.env.MANGO_FRONTEND_MODE || 'source';
    if (mode === 'package') {
      process.stdout.write(`frontend package mode: ${relativeOrAbsolute(context.root, appRoot)} expects built package artifacts\n`);
      continue;
    }
    if (!existsSync(join(uiRoot, 'package.json'))) {
      process.stdout.write(`warn    frontend root not found for ${relativeOrAbsolute(context.root, appRoot)}\n`);
      continue;
    }
    const adminStylesScript = packageJsonHasScript(join(uiRoot, 'package.json'), 'admin:styles');
    if (adminStylesScript) {
      if (checkOnly) {
        process.stdout.write(`ok      frontend prepare can run ${packageManager} admin:styles in ${relativeOrAbsolute(process.cwd(), uiRoot)}\n`);
      } else {
        runCheckedCommand(uiRoot, packageManager, ['admin:styles'], 'frontend style aggregation');
      }
    }
    const adminPackagePath = join(uiRoot, 'packages/admin/package.json');
    if (existsSync(adminPackagePath)) {
      const missing = requiredAdminSourceModeArtifacts(uiRoot).filter(path => !existsSync(path));
      if (missing.length === 0) {
        process.stdout.write(`ok      frontend source artifacts ready in ${relativeOrAbsolute(process.cwd(), uiRoot)}\n`);
        continue;
      }
      if (checkOnly) {
        process.stdout.write(`warn    missing source-mode artifacts:\n${missing.map(path => `        ${relativeOrAbsolute(process.cwd(), path)}`).join('\n')}\n`);
      } else if (!adminStylesScript) {
        fail('frontend source-mode artifacts are missing and admin:styles script is unavailable');
      }
    }
  }
}

function findFrontendWorkspaceRoot(appRoot, stopRoot) {
  let current = appRoot;
  const boundary = resolve(stopRoot);
  while (true) {
    if (existsSync(join(current, 'pnpm-workspace.yaml')) || existsSync(join(current, 'packages/admin/package.json'))) {
      return current;
    }
    if (current === boundary || current === dirname(current)) {
      return appRoot;
    }
    current = dirname(current);
  }
}

function packageJsonHasScript(packageJsonPath, scriptName) {
  if (!existsSync(packageJsonPath)) {
    return false;
  }
  const packageJson = readJsonFile(packageJsonPath);
  return Boolean(packageJson.scripts?.[scriptName]);
}

function requiredAdminSourceModeArtifacts(uiRoot) {
  return [
    join(uiRoot, 'packages/admin/generated-package-styles.css'),
    join(uiRoot, 'packages/admin/style-full.css'),
  ];
}

function runCheckedCommand(cwd, command, args, label) {
  requireCommand(command, label);
  process.stdout.write(`${label}: ${command} ${args.join(' ')}\n`);
  const result = spawnSync(command, args, {
    cwd,
    stdio: 'inherit',
    env: { ...process.env },
  });
  if (result.status !== 0) {
    fail(`${label} failed`);
  }
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

function detectPackageManager(start, stopRoot = start) {
  let current = resolve(start);
  const boundary = resolve(stopRoot);
  while (true) {
    if (existsSync(join(current, 'pnpm-lock.yaml'))) {
      return 'pnpm';
    }
    if (existsSync(join(current, 'yarn.lock'))) {
      return 'yarn';
    }
    if (current === boundary || current === dirname(current)) {
      break;
    }
    current = dirname(current);
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

function formatPortOwnerHint(root, port) {
  const owner = findRegisteredPortOwner(root, port);
  if (!owner || resolve(owner.root) === resolve(root)) {
    return '';
  }
  return ` owner=${owner.root}`;
}

function findRegisteredPortOwner(root, port) {
  return readWorkspaceRegistry(root)
    .find(entry => workspacePorts(entry).includes(Number(port)));
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

function runPmoCommand(command, argv) {
  if (command === 'status') {
    const options = parsePmoArgs(argv);
    const targetDir = resolve(process.cwd(), options.projectDir);
    const status = getPmoStatus(targetDir);
    printPmoStatus(status);
    return;
  }
  if (command === 'check') {
    const options = parsePmoArgs(argv);
    const targetDir = resolve(process.cwd(), options.projectDir);
    const status = getPmoStatus(targetDir);
    printPmoStatus(status);
    if (status.errors.length > 0 || status.warnings.length > 0) {
      process.exit(1);
    }
    return;
  }
  if (command === 'sync' || command === 'upgrade') {
    syncPmoBaseline(argv, { command });
    return;
  }
  fail(`unknown pmo command: ${command || ''}`);
}

function syncPmoBaseline(argv, { command = 'sync' } = {}) {
  const options = parsePmoArgs(argv);
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
  const baseline = loadPmoPackageBaseline();
  const plan = [
    ...planPmoBaselineSync(targetDir, baseline),
    ...planTemplateSync('business-pmo/README.md', targetDir, variables),
    ...planBusinessDocsSync(targetDir, variables),
    planAgentsSync(targetDir, variables, options.writeAgents),
    ...planShellSync(targetDir, variables, options.syncShell),
  ].filter(Boolean);

  const summary = summarizeSyncPlan(plan);
  printPmoSyncPlan(targetDir, plan, options.dryRun, command);
  if (options.dryRun) {
    return;
  }
  for (const item of plan) {
    if (item.action === 'skip' || item.action === 'warn') {
      continue;
    }
    writePlannedFile(item);
  }
  writePmoBaselineManifest(targetDir, baseline.manifest);
  const synced = summary.add + summary.update;
  process.stdout.write(`PMO baseline ${command} complete: ${synced} files written, ${summary.skip} skipped.\n`);
}

function parsePmoArgs(argv) {
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

function loadPmoPackageBaseline() {
  const resolvedPackageBaseline = resolveInstalledPmoPackageBaseline();
  if (resolvedPackageBaseline) {
    return resolvedPackageBaseline;
  }

  const candidates = [
    bundledPmoPackageRoot,
    resolve(packageRoot, 'node_modules/@mango/pmo'),
    resolve(packageRoot, '../../node_modules/@mango/pmo'),
    resolve(process.cwd(), 'node_modules/@mango/pmo'),
  ];
  for (const candidate of candidates) {
    const manifestPath = join(candidate, 'dist/baseline.json');
    const baselineRoot = join(candidate, 'dist/baseline');
    if (existsSync(manifestPath) && existsSync(baselineRoot)) {
      return {
        root: baselineRoot,
        manifest: JSON.parse(readFileSync(manifestPath, 'utf8')),
      };
    }
  }
  const templateBaselineRoot = join(templateRoot, 'business-pmo/mango-baseline');
  return {
    root: templateBaselineRoot,
    manifest: buildLegacyPmoManifest(templateBaselineRoot),
  };
}

function resolveInstalledPmoPackageBaseline() {
  try {
    const manifestPath = requireFromCli.resolve('@mango/pmo/baseline.json');
    const baselineRoot = join(dirname(manifestPath), 'baseline');
    if (existsSync(manifestPath) && existsSync(baselineRoot)) {
      return {
        root: baselineRoot,
        manifest: JSON.parse(readFileSync(manifestPath, 'utf8')),
      };
    }
  } catch (error) {
    if (error?.code !== 'MODULE_NOT_FOUND' && error?.code !== 'ERR_PACKAGE_PATH_NOT_EXPORTED') {
      throw error;
    }
  }
  return null;
}

function buildLegacyPmoManifest(root) {
  return {
    packageName: '@mango/cli-template',
    packageVersion: readCliVersion(),
    schemaVersion: 1,
    source: 'mango-cli-template',
    generatedAt: new Date().toISOString(),
    files: walkFiles(root)
      .map(file => {
        const content = readFileSync(file);
        return {
          path: toPosix(relative(root, file)),
          sha256: createHash('sha256').update(content).digest('hex'),
          size: content.length,
        };
      })
      .sort((left, right) => left.path.localeCompare(right.path)),
  };
}

function planPmoBaselineSync(targetDir, baseline) {
  const plan = [];
  for (const file of baseline.manifest.files || []) {
    const targetRelative = `business-pmo/mango-baseline/${file.path}`;
    const sourceFile = join(baseline.root, file.path);
    plan.push(buildFilePlanItem(targetRelative, join(targetDir, targetRelative), readRenderedBaselineFile(sourceFile)));
  }
  plan.push(buildFilePlanItem(
    'business-pmo/mango-baseline/baseline.json',
    join(targetDir, 'business-pmo/mango-baseline/baseline.json'),
    `${JSON.stringify(baseline.manifest, null, 2)}\n`,
  ));
  return plan;
}

function readRenderedBaselineFile(sourceFile) {
  return readFileSync(sourceFile, 'utf8');
}

function writePmoBaselineManifest(targetDir, manifest) {
  const manifestPath = join(targetDir, 'business-pmo/mango-baseline/baseline.json');
  mkdirSync(dirname(manifestPath), { recursive: true });
  writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
}

function installPmoBaseline(targetDir) {
  const baseline = loadPmoPackageBaseline();
  const baselineDir = join(targetDir, 'business-pmo/mango-baseline');
  rmSync(baselineDir, { recursive: true, force: true });
  for (const file of baseline.manifest.files || []) {
    const sourceFile = join(baseline.root, file.path);
    const targetPath = join(baselineDir, file.path);
    mkdirSync(dirname(targetPath), { recursive: true });
    copyFileSync(sourceFile, targetPath);
    if (file.path.startsWith('tools/') && file.path.endsWith('.mjs')) {
      chmodSync(targetPath, 0o755);
    }
  }
  writePmoBaselineManifest(targetDir, baseline.manifest);
}

function getPmoStatus(targetDir) {
  const baseline = loadPmoPackageBaseline();
  const baselineDir = join(targetDir, 'business-pmo/mango-baseline');
  const installedManifestPath = join(baselineDir, 'baseline.json');
  const errors = [];
  const warnings = [];
  if (!existsSync(targetDir) || !statSync(targetDir).isDirectory()) {
    errors.push(`project directory not found: ${targetDir}`);
    return { targetDir, baseline, baselineDir, errors, warnings, missing: [], changed: [], extra: [] };
  }
  if (!existsSync(baselineDir)) {
    errors.push('business-pmo/mango-baseline is missing. Run mango pmo sync --project-dir .');
    return { targetDir, baseline, baselineDir, errors, warnings, missing: [], changed: [], extra: [] };
  }
  if (!existsSync(installedManifestPath)) {
    warnings.push('baseline.json is missing. Run mango pmo sync --project-dir . to install a versioned manifest.');
  }
  const comparison = comparePmoBaselineFiles(baselineDir, baseline.manifest);
  if (comparison.missing.length > 0) {
    errors.push(`${comparison.missing.length} baseline files are missing`);
  }
  if (comparison.changed.length > 0) {
    warnings.push(`${comparison.changed.length} baseline files differ from ${baseline.manifest.packageName}@${baseline.manifest.packageVersion}`);
  }
  return {
    targetDir,
    baseline,
    baselineDir,
    errors,
    warnings,
    ...comparison,
  };
}

function comparePmoBaselineFiles(baselineDir, manifest) {
  const expected = new Map((manifest.files || []).map(file => [file.path, file]));
  const missing = [];
  const changed = [];
  for (const [filePath, file] of expected.entries()) {
    const targetPath = join(baselineDir, filePath);
    if (!existsSync(targetPath)) {
      missing.push(filePath);
      continue;
    }
    const actualHash = hashFile(targetPath);
    if (actualHash !== file.sha256) {
      changed.push(filePath);
    }
  }
  const expectedPaths = new Set([...expected.keys(), 'baseline.json']);
  const extra = walkFiles(baselineDir)
    .map(file => toPosix(relative(baselineDir, file)))
    .filter(file => !expectedPaths.has(file));
  return { missing, changed, extra };
}

function printPmoStatus(status) {
  process.stdout.write(`Project: ${status.targetDir}\n`);
  process.stdout.write(`Baseline: ${status.baseline.manifest.packageName}@${status.baseline.manifest.packageVersion}\n`);
  process.stdout.write(`Files: ${status.baseline.manifest.files?.length || 0} expected, ${status.missing.length} missing, ${status.changed.length} changed, ${status.extra.length} extra\n`);
  for (const warning of status.warnings) {
    process.stdout.write(`warn    ${warning}\n`);
  }
  for (const error of status.errors) {
    process.stdout.write(`error   ${error}\n`);
  }
  if (status.missing.length > 0) {
    process.stdout.write(`Missing: ${status.missing.slice(0, 10).join(', ')}${status.missing.length > 10 ? ', ...' : ''}\n`);
  }
  if (status.changed.length > 0) {
    process.stdout.write(`Changed: ${status.changed.slice(0, 10).join(', ')}${status.changed.length > 10 ? ', ...' : ''}\n`);
  }
  if (status.errors.length === 0 && status.warnings.length === 0) {
    process.stdout.write('PMO baseline is current.\n');
  }
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
    const { manifest, warnings } = createBusinessDevManifest(targetDir, variables.projectKebab);
    plan.push(buildFilePlanItem(
      'mango.dev.json',
      manifestPath,
      `${JSON.stringify(manifest, null, 2)}\n`,
    ));
    for (const warning of warnings) {
      plan.push({
        action: 'warn',
        reason: warning,
        path: 'mango.dev.json',
        targetPath: manifestPath,
      });
    }
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

function printPmoSyncPlan(targetDir, plan, dryRun, command = 'sync') {
  const summary = summarizeSyncPlan(plan);
  process.stdout.write(`${dryRun ? 'PMO baseline dry-run plan' : `PMO baseline ${command} plan`} for ${relativeOrAbsolute(process.cwd(), targetDir)}\n`);
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

function normalizeAdminModules(items) {
  if (!Array.isArray(items)) {
    return [];
  }
  return items.map(item => ({
    ...item,
    packageName: item.packageName || item.name,
    registrars: item.registrars || [],
  }));
}

function toFrontendDependency(module) {
  if (!module.packageName || !module.cliVersionKey) {
    fail(`admin module ${module.code || module.packageName || '<unknown>'} must declare packageName and cliVersionKey for CLI`);
  }
  return {
    name: module.packageName,
    versionKey: module.cliVersionKey,
  };
}

function buildOptionalModules(adminFullModules, overlays) {
  const overlayByCode = new Map(overlays.map(overlay => [overlay.code, overlay]));
  return adminFullModules
    .filter(module => module.cliOptional !== false)
    .map(module => {
      const overlay = overlayByCode.get(module.code);
      if (!overlay) {
        fail(`mango-cli optional module overlay missing for ${module.code}`);
      }
      return {
        ...overlay,
        frontendPackage: module.packageName,
        versionKey: module.cliVersionKey,
        styleImport: module.style,
        registrarImport: module.registrars.map(registrar => `import { ${registrar.name} } from '${registrar.import}';`),
        registrar: module.registrars.map(registrar => registrar.name),
      };
    });
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

function renderFrontendPackageDependencies(frontendVersions) {
  return Object.entries(frontendVersions)
    .map(([dependency, version]) => `    "${dependency}": "${version}",`)
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
        ...BUSINESS_BACKEND_API_MANAGED_DEPENDENCIES,
        ...BUSINESS_BACKEND_MANAGED_DEPENDENCIES,
      ],
      true,
      12,
    );
  }
  return renderDependencyXml(
    [
      ...CORE_BACKEND_DEPENDENCIES,
      ...BUSINESS_BACKEND_API_MANAGED_DEPENDENCIES,
      ...BUSINESS_BACKEND_MANAGED_DEPENDENCIES,
      ...selectedModules.flatMap(module => module.backend || []),
    ],
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

function hashFile(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function toPosix(path) {
  return path.split('\\').join('/');
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

function readAdminModulesManifest() {
  const manifestPath = join(packageRoot, 'admin-modules.json');
  if (!existsSync(manifestPath)) {
    fail('admin-modules.json is missing from @mango/cli package');
  }
  return readJsonFile(manifestPath);
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
  process.stdout.write('  mango workspace init\n');
  process.stdout.write('  mango dev start\n');
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
