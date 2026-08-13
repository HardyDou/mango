#!/usr/bin/env node
import {
  appendFileSync,
  chmodSync,
  closeSync,
  copyFileSync,
  existsSync,
  lstatSync,
  mkdirSync,
  openSync,
  readlinkSync,
  readdirSync,
  readFileSync,
  realpathSync,
  renameSync,
  rmSync,
  statSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs';
import { spawn, spawnSync } from 'node:child_process';
import { createHash, randomBytes } from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
import { createRequire } from 'node:module';
import { homedir } from 'node:os';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { inspectProjectPullRequestTemplate, synchronizeProjectPullRequestTemplate } from './pmo-project-template.mjs';
import { resolveHealthPollIntervalMs } from './dev-health-policy.mjs';
import { shouldRunDevInstall } from './dev-install-policy.mjs';
import { buildSpringBootReactorArgs, resolveSpringBootMavenReactor } from './dev-maven-reactor.mjs';
import { injectStableBootstrapIdentity, readStableBootstrapReceipt } from './dev-bootstrap-receipt.mjs';
import {
  buildWorkspaceMavenRevisionQualifier,
  isMavenCommand,
  qualifyWorkspaceMavenRevision,
  readCiFriendlyMavenRevision,
} from './dev-maven-revision.mjs';
import { isProcessAlive, isProcessGroupAlive, stopProcessGroup } from './process-control.mjs';
import { runReleaseCli } from './release-command.mjs';
import { runModuleDoctorCli } from './module-doctor.mjs';

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
const DEFAULT_MAVEN_REPOSITORY = 'https://nexus.inner.yunxinbaokeji.com/repository/maven-public/';
const DOCS_BUNDLE_GROUP_ID = 'io.mango';
const DOCS_BUNDLE_ARTIFACT_ID = 'mango-docs-bundle';

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
  mangoHttpClient: readReleasedMangoPackageVersion('http-client', '1.0.0'),
  mangoFile: readReleasedMangoPackageVersion('file', '1.0.6'),
  mangoGridLayout: readReleasedMangoPackageVersion('grid-layout', '1.0.0'),
  mangoGridWidgets: readReleasedMangoPackageVersion('grid-widgets', '1.0.0'),
  mangoJob: readReleasedMangoPackageVersion('job', '1.0.0'),
  mangoLink: readReleasedMangoPackageVersion('link', '1.0.0'),
  mangoNotice: readReleasedMangoPackageVersion('notice', '1.0.6'),
  mangoNumgen: readReleasedMangoPackageVersion('numgen', '1.0.6'),
  mangoPayment: readReleasedMangoPackageVersion('payment', '1.0.1'),
  mangoRbac: readReleasedMangoPackageVersion('rbac', '1.0.4'),
  mangoSystem: readReleasedMangoPackageVersion('system', '1.0.4'),
  mangoTemplate: readReleasedMangoPackageVersion('template', '1.0.6'),
  mangoWorkflow: readReleasedMangoPackageVersion('workflow', '1.0.6'),
  mangoWorkflowBusinessExample: readReleasedMangoPackageVersion('workflow-business-example', '1.0.6'),
  vue: '3.5.13',
  vueRouter: '4.6.4',
  vueI18n: '9.2.2',
  pinia: '2.0.32',
  elementPlus: '2.14.1',
  iconsVue: '2.3.2',
  vite: '7.3.6',
  viteVue: '6.0.8',
  typescript: '5.9.3',
  vueTsc: '3.3.7',
  playwright: '1.61.1',
  nodeTypes: '22.20.1',
  eslintJs: '10.0.1',
  eslint: '10.7.0',
  eslintConfigPrettier: '10.1.8',
  eslintPluginVue: '10.9.2',
  globals: '17.7.0',
  postcssHtml: '1.8.1',
  prettier: '3.9.5',
  stylelint: '17.14.0',
  stylelintConfigRecommendedVue: '1.6.1',
  stylelintConfigStandard: '40.0.0',
  stylelintConfigStandardScss: '17.0.0',
  typescriptEslint: '8.64.0',
  vitest: '4.1.10',
  mavenCompilerPlugin: '3.15.0',
  springBoot: '3.5.14',
  flyway: '11.20.3',
  springCloud: '2025.0.1',
  springdocOpenapi: '2.8.9',
  swaggerAnnotations: '2.2.30',
};

const ADMIN_DEFAULT_MODULES = normalizeAdminModules(adminModulesManifest.defaultPackages);
const ADMIN_FULL_MODULES = normalizeAdminModules(adminModulesManifest.fullPackages);

const CORE_FRONTEND_PACKAGES = uniqueBy(
  [
    { name: '@mango/admin', versionKey: 'mangoAdmin' },
    { name: '@mango/admin-pages', versionKey: 'mangoAdminPages' },
    { name: '@mango/app-runtime', versionKey: 'mangoAppRuntime' },
    { name: '@mango/http-client', versionKey: 'mangoHttpClient' },
    ...ADMIN_DEFAULT_MODULES.map(toFrontendDependency),
  ],
  (dependency) => dependency.name,
);

const ADMIN_OPTIONAL_PEER_PACKAGES = ADMIN_FULL_MODULES.map(toFrontendDependency);

const CORE_BACKEND_DEPENDENCIES = [
  { groupId: 'io.mango.common', artifactId: 'mango-common' },
  { groupId: 'io.mango.infra.bootstrap', artifactId: 'mango-infra-bootstrap-starter' },
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
  { groupId: 'io.mango.platform.notice', artifactId: 'mango-notice-starter' },
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
    label: '文件管理',
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
    backend: [{ groupId: 'io.mango.platform.template', artifactId: 'mango-template-starter' }],
  },
  {
    code: 'cms',
    label: '内容运营',
    feature: 'cms',
    runtimeModule: {
      moduleCode: 'mango-cms',
      local: { mode: 'local', runtimeCode: 'mango-admin-cms-local' },
      micro: { mode: 'micro', runtimeCode: 'mango-admin-cms-app', entry: 'http://e.mango.io:5184/' },
    },
    backend: [{ groupId: 'io.mango.platform.cms', artifactId: 'mango-cms-starter' }],
  },
  {
    code: 'notice',
    label: '通知管理',
    feature: 'notice',
    backend: [{ groupId: 'io.mango.platform.notice', artifactId: 'mango-notice-starter' }],
  },
  {
    code: 'numgen',
    label: '编号管理',
    feature: 'numgen',
    backend: [{ groupId: 'io.mango.platform.numgen', artifactId: 'mango-numgen-starter' }],
  },
  {
    code: 'calendar',
    label: '工作日历',
    feature: 'calendar',
    backend: [{ groupId: 'io.mango.platform.calendar', artifactId: 'mango-calendar-starter' }],
  },
  {
    code: 'payment',
    label: '支付管理',
    feature: 'payment',
    backend: [{ groupId: 'io.mango.platform.payment', artifactId: 'mango-payment-starter' }],
  },
  {
    code: 'workflow',
    label: '审批管理',
    feature: 'workflow',
    runtimeModule: {
      moduleCode: 'mango-workflow',
      local: { mode: 'local', runtimeCode: 'mango-admin-workflow-local' },
      micro: { mode: 'micro', runtimeCode: 'mango-admin-workflow-app', entry: 'http://c.mango.io:5182/' },
    },
    backend: [{ groupId: 'io.mango.platform.workflow', artifactId: 'mango-workflow-starter' }],
  },
  {
    code: 'workflow-example',
    label: '审批示例',
    dependsOn: ['workflow'],
  },
];

const OPTIONAL_MODULES = buildOptionalModules(ADMIN_FULL_MODULES, OPTIONAL_MODULE_OVERLAYS);
const MODULE_BY_CODE = new Map(OPTIONAL_MODULES.map((module) => [module.code, module]));
const FULL_MODULE_CODES = OPTIONAL_MODULES.map((module) => module.code);

const usage = `
Mango CLI

Usage:
  mango init <project> --preset full [options]
  mango init <project> --preset custom --modules workflow,template [options]
  mango add <module...> [options]
  mango workspace init
  mango workspace status
  mango workspace doctor
  mango workspace release [--workspace <path>] [--keep-db]
  mango dev start [group|app...]
  mango dev stop [app...]
  mango dev restart [group|app...]
  mango dev status
  mango dev doctor
  mango dev logs <app>
  mango frontend prepare
  mango frontend doctor
  mango changelog
  mango docs pull [--project-dir <dir>] [--version <version>] [--maven-repository <url>] [--force]
  mango docs status [--project-dir <dir>]
  mango docs path [--project-dir <dir>]
  mango pmo status --project-dir <dir> [--locked]
  mango pmo check --project-dir <dir> [--locked]
  mango pmo sync --project-dir <dir> [--dry-run] [--write-agents] [--sync-shell] [--adopt-governance]
  mango pmo upgrade --project-dir <dir> [--to <version>] [--dry-run] [--write-agents] [--sync-shell] [--adopt-governance]
  mango pmo rollback --project-dir <dir> [--to <version>] [--dry-run]
  mango release publish --version <version> [--authorize]
  mango release status --version <version>
  mango release verify --version <version>
  mango release repair --version <version> [--authorize]
  mango release registry doctor
  mango module doctor mango-link --app <app> --backend-url <url> --frontend-url <url> [--project-dir <dir>] [--json] [--strict]
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
  --app <code>             Application code for module runtime diagnosis
  --backend-url <url>      Loopback backend base origin for module runtime diagnosis
  --frontend-url <url>     Loopback Admin Shell base origin for module runtime diagnosis
  --json                   Emit one machine-readable result object
  --strict                 Treat degraded or incomplete optional evidence as failure
  --dry-run                Print PMO sync plan without modifying files
  --write-agents           Update root AGENTS.md during PMO sync when it points to an external mango-pmo
  --sync-shell             Sync generated startup shell scripts during PMO sync
  --locked                 Check the project lock instead of the CLI's available PMO package
  --to <version>           Exact @mango/pmo version for upgrade or rollback
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
  ${OPTIONAL_MODULES.map((module) => `${module.code.padEnd(16)} ${module.label}`).join('\n  ')}
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

  if (args[0] === 'docs') {
    await runDocsCommand(args[1], args.slice(2));
    return;
  }

  if (args[0] === 'init-dev') {
    fail('mango init-dev has been removed. Use "mango workspace init".');
  }

  if (isDevWorkspaceCommand(args[0])) {
    await runDevWorkspaceCommand(args[0], args.slice(1));
    return;
  }

  if (args[0] === 'module') {
    const subCommand = args[1];
    if (subCommand === 'doctor') {
      process.exitCode = await runModuleDoctorCli(args.slice(2));
      return;
    }
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

  if (args[0] === 'release') {
    await runReleaseCli(args.slice(1), { cwd: process.cwd() });
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
    mavenRepository: DEFAULT_MAVEN_REPOSITORY,
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
    [...CORE_FRONTEND_PACKAGES, ...ADMIN_OPTIONAL_PEER_PACKAGES].map((dependency) => [
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
    selectedModuleCodes: selectedModules.map((module) => module.code),
    frontendVersions,
    mangoBackendVersion: options.mangoVersion,
    mangoAdminVersion: defaultVersions.mangoAdmin,
    mangoAdminPagesVersion: defaultVersions.mangoAdminPages,
    mangoAdminShellVersion: defaultVersions.mangoAdminShell,
    mangoAppRuntimeVersion: defaultVersions.mangoAppRuntime,
    mangoAuthVersion: defaultVersions.mangoAuth,
    mangoCalendarVersion: defaultVersions.mangoCalendar,
    mangoCommonVersion: defaultVersions.mangoCommon,
    mangoHttpClientVersion: defaultVersions.mangoHttpClient,
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
    eslintJsVersion: defaultVersions.eslintJs,
    eslintVersion: defaultVersions.eslint,
    eslintConfigPrettierVersion: defaultVersions.eslintConfigPrettier,
    eslintPluginVueVersion: defaultVersions.eslintPluginVue,
    globalsVersion: defaultVersions.globals,
    postcssHtmlVersion: defaultVersions.postcssHtml,
    prettierVersion: defaultVersions.prettier,
    stylelintVersion: defaultVersions.stylelint,
    stylelintConfigRecommendedVueVersion: defaultVersions.stylelintConfigRecommendedVue,
    stylelintConfigStandardVersion: defaultVersions.stylelintConfigStandard,
    stylelintConfigStandardScssVersion: defaultVersions.stylelintConfigStandardScss,
    typescriptEslintVersion: defaultVersions.typescriptEslint,
    vitestVersion: defaultVersions.vitest,
    mavenCompilerPluginVersion: defaultVersions.mavenCompilerPlugin,
    springBootVersion: defaultVersions.springBoot,
    flywayVersion: defaultVersions.flyway,
    springCloudVersion: defaultVersions.springCloud,
    springdocOpenapiVersion: defaultVersions.springdocOpenapi,
    swaggerAnnotationsVersion: defaultVersions.swaggerAnnotations,
    npmRegistry: ensureTrailingSlash(options.npmRegistry),
    mavenRepository: ensureTrailingSlash(options.mavenRepository),
    mangoBaselineCommit: readMangoBaselineCommit(),
    mangoCliVersion: readCliVersion(),
    mangoPmoVersion: releaseVersions.npm?.['@mango/pmo'] || 'unknown',
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
    mavenRepository: config.mavenRepository || DEFAULT_MAVEN_REPOSITORY,
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
  const moduleDisplayName = requireChineseDisplayName(
    options.moduleName || `${toPascalCase(moduleKebab)}模块`,
    '--module-name',
  );
  const aggregateDisplayName = options.aggregateName
    ? requireChineseDisplayName(options.aggregateName, '--aggregate-name')
    : toPascalCase(aggregateKebab);
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
      mavenRepository: config.mavenRepository || DEFAULT_MAVEN_REPOSITORY,
      modules: (config.modules?.optional || []).join(','),
    }),
    moduleKebab,
    modulePackage: toJavaSegment(moduleKebab),
    modulePascal: toPascalCase(moduleKebab),
    moduleCamel: toCamelCase(moduleKebab),
    moduleName: moduleDisplayName,
    moduleBusinessDomainCode: toSnakeCase(moduleKebab).toUpperCase(),
    moduleKebabSnake: toSnakeCase(moduleKebab),
    aggregateKebab,
    aggregateKebabSnake: toSnakeCase(aggregateKebab),
    aggregatePascal: toPascalCase(aggregateKebab),
    aggregateCamel: toCamelCase(aggregateKebab),
    aggregateName: aggregateDisplayName,
    businessResourceMenuId: stableResourceDeclarationId(
      config.groupId || config.basePackage || 'com.example.mango',
      config.project || basename(targetDir),
      moduleKebab,
      'auth-menu',
    ),
    backendBusinessFlywayModules: '',
  };
  const frontendApiTarget = join(targetDir, 'frontend/packages', `${moduleKebab}-api`);
  const frontendModuleTarget = join(targetDir, 'frontend/packages', moduleKebab);
  const transactionPaths = [
    moduleTarget,
    frontendApiTarget,
    frontendModuleTarget,
    join(targetDir, 'backend/pom.xml'),
    join(targetDir, 'backend/app/pom.xml'),
    join(targetDir, 'backend/app/src/main/resources/application.yml'),
    join(targetDir, 'frontend/package.json'),
    join(targetDir, 'frontend/src/main.ts'),
    join(targetDir, 'frontend/pnpm-lock.yaml'),
    configPath,
  ];
  const snapshots = transactionPaths.map(captureBusinessModuleSnapshot);
  let stage = 'render';
  try {
    if (options.force) {
      for (const path of [moduleTarget, frontendApiTarget, frontendModuleTarget]) {
        rmSync(path, { recursive: true, force: true });
      }
    }
    copyTemplate(join(businessStarterRoot, 'backend/modules/{{moduleKebab}}'), moduleTarget, variables);
    copyTemplate(join(businessStarterRoot, 'frontend/packages/{{moduleKebab}}-api'), frontendApiTarget, variables);
    copyTemplate(join(businessStarterRoot, 'frontend/packages/{{moduleKebab}}'), frontendModuleTarget, variables);
    assertNoUnrenderedPlaceholders(targetDir, [
      `backend/modules/${moduleKebab}`,
      `frontend/packages/${moduleKebab}-api`,
      `frontend/packages/${moduleKebab}`,
    ]);
    stage = 'integrate';
    updateBackendBusinessIntegration(targetDir, variables);
    updateFrontendBusinessIntegration(targetDir, variables);
    updateBusinessConfig(targetDir, config, variables);
    stage = 'format';
    formatBusinessModuleFrontend(targetDir, variables);
    stage = 'lockfile';
    synchronizeBusinessModuleLockfile(targetDir, variables);
  } catch (error) {
    const recoveryFailures = [];
    for (const snapshot of [...snapshots].reverse()) {
      try {
        restoreBusinessModuleSnapshot(snapshot);
      } catch (recoveryError) {
        recoveryFailures.push(`${snapshot.path}: ${recoveryError.message}`);
      }
    }
    const recovery =
      recoveryFailures.length === 0 ? 'restored byte-for-byte' : `restore failures: ${recoveryFailures.join('; ')}`;
    throw new Error(`MODULE_ADD_TRANSACTION_FAILED stage=${stage}; ${recovery}; cause=${error.message}`, {
      cause: error,
    });
  }
  process.stdout.write(`Added business module: ${moduleKebab} (${aggregateKebab})\n`);
}

function captureBusinessModuleSnapshot(path) {
  if (!existsSync(path)) {
    return { path, node: null };
  }
  return { path, node: captureBusinessModuleNode(path) };
}

function captureBusinessModuleNode(path) {
  const stat = lstatSync(path);
  if (stat.isSymbolicLink()) {
    return { type: 'symlink', target: readlinkSync(path) };
  }
  if (stat.isDirectory()) {
    return {
      type: 'directory',
      mode: stat.mode,
      children: readdirSync(path).map((name) => ({ name, node: captureBusinessModuleNode(join(path, name)) })),
    };
  }
  if (!stat.isFile()) {
    throw new Error(`unsupported module transaction path type: ${path}`);
  }
  return { type: 'file', mode: stat.mode, content: readFileSync(path) };
}

function restoreBusinessModuleSnapshot(snapshot) {
  rmSync(snapshot.path, { recursive: true, force: true });
  if (snapshot.node) {
    restoreBusinessModuleNode(snapshot.path, snapshot.node);
  }
}

function restoreBusinessModuleNode(path, node) {
  if (node.type === 'symlink') {
    mkdirSync(dirname(path), { recursive: true });
    symlinkSync(node.target, path);
    return;
  }
  if (node.type === 'directory') {
    mkdirSync(path, { recursive: true, mode: node.mode });
    for (const child of node.children) {
      restoreBusinessModuleNode(join(path, child.name), child.node);
    }
    chmodSync(path, node.mode);
    return;
  }
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, node.content, { mode: node.mode });
  chmodSync(path, node.mode);
}

function formatBusinessModuleFrontend(targetDir, variables) {
  const frontendRoot = join(targetDir, 'frontend');
  const packageJson = JSON.parse(readFileSync(join(frontendRoot, 'package.json'), 'utf8'));
  const prettierVersion = packageJson.devDependencies?.prettier;
  if (!/^\d+\.\d+\.\d+$/u.test(prettierVersion ?? '')) {
    throw new Error(`generated frontend must declare an exact Prettier version, got ${prettierVersion ?? 'missing'}`);
  }
  let cliPrettierPackage;
  let cliPrettier;
  try {
    cliPrettierPackage = requireFromCli.resolve('prettier/package.json');
    cliPrettier = requireFromCli.resolve('prettier/bin/prettier.cjs');
  } catch (error) {
    throw new Error('the installed @mango/cli is missing its Prettier runtime dependency; reinstall @mango/cli', {
      cause: error,
    });
  }
  const cliPrettierVersion = JSON.parse(readFileSync(cliPrettierPackage, 'utf8')).version;
  if (cliPrettierVersion !== prettierVersion) {
    throw new Error(
      `Prettier version mismatch: @mango/cli provides ${cliPrettierVersion}, generated frontend requires ${prettierVersion}`,
    );
  }
  const targets = [
    'package.json',
    'src/main.ts',
    `packages/${variables.moduleKebab}-api`,
    `packages/${variables.moduleKebab}`,
  ];
  runBusinessModuleCommand(process.execPath, [cliPrettier, '--write', ...targets], frontendRoot, 'Prettier');
}

function synchronizeBusinessModuleLockfile(targetDir, variables) {
  const frontendRoot = join(targetDir, 'frontend');
  const pnpm = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  runBusinessModuleCommand(
    pnpm,
    ['install', '--lockfile-only', '--ignore-scripts'],
    frontendRoot,
    'pnpm lockfile update',
  );
  runBusinessModuleCommand(
    pnpm,
    ['install', '--frozen-lockfile', '--lockfile-only', '--ignore-scripts'],
    frontendRoot,
    'pnpm frozen lockfile verification',
  );
  const lockfilePath = join(frontendRoot, 'pnpm-lock.yaml');
  if (!existsSync(lockfilePath)) {
    throw new Error('pnpm did not create frontend/pnpm-lock.yaml');
  }
  const lockfile = readFileSync(lockfilePath, 'utf8');
  for (const importer of [`packages/${variables.moduleKebab}`, `packages/${variables.moduleKebab}-api`]) {
    if (!lockfile.includes(`  ${importer}:`)) {
      throw new Error(`pnpm lockfile is missing workspace importer ${importer}`);
    }
  }
}

function runBusinessModuleCommand(command, args, cwd, label) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024,
    shell: process.platform === 'win32' && command.endsWith('.cmd'),
  });
  if (result.error) {
    throw new Error(`${label} could not start: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error(`${label} failed (exit ${result.status}):\n${result.stdout ?? ''}\n${result.stderr ?? ''}`);
  }
}

function stableResourceDeclarationId(...parts) {
  const digest = createHash('sha256').update(parts.join(':')).digest('hex');
  const suffix = (BigInt(`0x${digest.slice(0, 16)}`) % 10_000_000_000_000n).toString().padStart(13, '0');
  return `269069${suffix}`;
}

const DEV_WORKSPACE_COMMANDS = new Set([
  'print',
  'validate',
  'doctor',
  'plan',
  'start',
  'stop',
  'status',
  'logs',
  'backend',
  'frontend',
]);
const DEFAULT_SPRING_BOOT_PLUGIN = `org.springframework.boot:spring-boot-maven-plugin:${defaultVersions.springBoot}:run`;
const WORKSPACE_SLOT_COUNT = 200;
const BACKEND_PORT_BASE = 18000;
const FRONTEND_PORT_BASE = 30000;
const DEFAULT_FRONTEND_APP_PORT_OFFSETS = {
  MANGO_ADMIN_SHELL_PORT: 1000,
  MANGO_ADMIN_RBAC_APP_PORT: 2000,
  MANGO_ADMIN_WORKFLOW_APP_PORT: 3000,
  MANGO_ADMIN_TEMPLATE_APP_PORT: 4000,
  MANGO_ADMIN_CMS_APP_PORT: 5000,
  MANGO_SITE_ENTERPRISE_APP_PORT: 6000,
  MANGO_SITE_HELP_APP_PORT: 7000,
  MANGO_SITE_DEMO_APP_PORT: 8000,
};

function isDevWorkspaceCommand(command) {
  return DEV_WORKSPACE_COMMANDS.has(command);
}

async function runWorkspaceCommand(command = 'status', argv = []) {
  const normalized = command;
  if (normalized === 'list') {
    listWorkspaceRegistry(process.cwd());
    return;
  }
  if (!['init', 'status', 'print', 'doctor', 'release'].includes(normalized)) {
    fail(`unknown workspace command: ${command || ''}`);
  }
  const context =
    normalized === 'init'
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
  if (normalized === 'restart') {
    await stopDevWorkspace(context, argv);
    await startDevWorkspace(context, argv);
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
  const context = loadDevWorkspaceContext({ allowMissingManifest: false });
  const normalizedCommand = normalizeDevWorkspaceCommand(command);

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
  ensureWorkspaceMavenRepository(context.root);
  const workspace = ensureWorkspaceConfig(context.root);
  if (!existsSync(envPath)) {
    writeFileSync(envPath, defaultDevWorkspaceEnv(context.root, workspace));
    process.stdout.write(`Created local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  } else {
    ensureDevWorkspaceEnv(context);
    process.stdout.write(`Workspace env already exists: ${relativeOrAbsolute(process.cwd(), envPath)}\n`);
  }
  process.stdout.write(
    `Workspace slot ${workspace.slot}: ${relativeOrAbsolute(process.cwd(), join(context.root, '.mango/workspace.json'))}\n`,
  );

  if (!existsSync(context.manifestPath)) {
    const { manifest, warnings } = createBusinessDevManifest(context.root, basename(context.root));
    writeFileSync(context.manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
    process.stdout.write(`Created development manifest: ${relativeOrAbsolute(process.cwd(), context.manifestPath)}\n`);
    for (const warning of warnings) {
      process.stdout.write(`warn    mango.dev.json ${warning}\n`);
    }
  }
}

function ensureWorkspaceMavenRepository(root) {
  const sharedRepository = join(process.env.HOME || process.env.USERPROFILE || homedir(), '.m2', 'repository');
  const workspaceRepository = join(root, '.mango', 'm2', 'repository');
  mkdirSync(sharedRepository, { recursive: true });
  mkdirSync(dirname(workspaceRepository), { recursive: true });

  let workspaceRepositoryStat;
  try {
    workspaceRepositoryStat = lstatSync(workspaceRepository);
  } catch (error) {
    if (error?.code !== 'ENOENT') {
      throw error;
    }
  }

  if (workspaceRepositoryStat) {
    if (workspaceRepositoryStat.isSymbolicLink() && haveSameRealPath(workspaceRepository, sharedRepository)) {
      process.stdout.write(
        `Shared Maven repository already initialized: ${relativeOrAbsolute(process.cwd(), workspaceRepository)}\n`,
      );
      return;
    }
    process.stdout.write(
      `warn    preserving existing Maven repository: ${relativeOrAbsolute(process.cwd(), workspaceRepository)}\n`,
    );
    return;
  }

  symlinkSync(sharedRepository, workspaceRepository, process.platform === 'win32' ? 'junction' : 'dir');
  process.stdout.write(
    `Created shared Maven repository: ${relativeOrAbsolute(process.cwd(), workspaceRepository)} -> ${sharedRepository}\n`,
  );
}

function haveSameRealPath(firstPath, secondPath) {
  try {
    const first = realpathSync(firstPath);
    const second = realpathSync(secondPath);
    return process.platform === 'win32' ? first.toLowerCase() === second.toLowerCase() : first === second;
  } catch {
    return false;
  }
}

function defaultDevWorkspaceEnv(root, workspace = ensureWorkspaceConfig(root)) {
  const frontendPorts = workspace.frontendApps || buildFrontendAppPorts(workspace.frontendPort);
  return [
    '# Mango local workspace configuration.',
    '# This file is generated once per workspace and must not be committed.',
    `MANGO_WORKSPACE_ID=${workspace.workspaceId}`,
    `MANGO_MAVEN_REVISION_QUALIFIER=${workspace.mavenRevisionQualifier}`,
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
    const persistedWorkspace = readJsonFile(workspacePath);
    const workspace = withMavenRevisionQualifier(persistedWorkspace);
    if (persistedWorkspace.mavenRevisionQualifier !== workspace.mavenRevisionQualifier) {
      writeFileSync(workspacePath, `${JSON.stringify(workspace, null, 2)}\n`);
    }
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
  const existing = registry.find((entry) => entry.root === normalizedRoot);
  if (existing) {
    return withMavenRevisionQualifier(existing);
  }
  const usedSlots = new Set(registry.map((entry) => Number(entry.slot)).filter(Number.isFinite));
  const usedPorts = new Set(registry.flatMap((entry) => workspacePorts(entry)));
  const usedDbNames = new Set(registry.map((entry) => entry.dbName));
  for (let slot = 1; slot <= WORKSPACE_SLOT_COUNT; slot += 1) {
    const candidate = buildWorkspaceConfig(normalizedRoot, slot);
    if (
      usedSlots.has(slot) ||
      workspacePorts(candidate).some((port) => usedPorts.has(port) || isPortInUse(port)) ||
      usedDbNames.has(candidate.dbName) ||
      workspaceDatabaseExists(candidate.dbName, readEnvFile(join(normalizedRoot, '.mango/dev-workspace.env')))
    ) {
      continue;
    }
    writeWorkspaceRegistry(normalizedRoot, [...registry, candidate]);
    return candidate;
  }
  fail(
    'unable to allocate free Mango workspace slot. Run mango workspace list, stop conflicting services, or set MANGO_WORKSPACE_REGISTRY.',
  );
}

function buildWorkspaceConfig(root, slot) {
  const frontendPort = FRONTEND_PORT_BASE + slot;
  const slotText = slot.toString().padStart(3, '0');
  return {
    version: 1,
    root,
    workspaceId: `mango_${slotText}`,
    mavenRevisionQualifier: `mango-${slotText}`,
    slot,
    backendPort: BACKEND_PORT_BASE + slot,
    frontendPort,
    frontendApps: buildFrontendAppPorts(frontendPort),
    dbName: `mango_dev_${workspaceProjectSlug(root)}_${slotText}`,
  };
}

function withMavenRevisionQualifier(workspace) {
  return {
    ...workspace,
    mavenRevisionQualifier: buildWorkspaceMavenRevisionQualifier(workspace.workspaceId),
  };
}

function workspaceProjectSlug(root) {
  const slug = basename(resolve(root))
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_');
  return slug || 'workspace';
}

function buildFrontendAppPorts(frontendPort) {
  return Object.fromEntries(
    Object.entries(DEFAULT_FRONTEND_APP_PORT_OFFSETS).map(([key, offset]) => [key, frontendPort + offset]),
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
  const registry = readWorkspaceRegistry(normalizedRoot).filter((entry) => entry.root !== normalizedRoot);
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
    .map((line) => {
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
  return Boolean(
    entry?.root && entry.workspaceId && entry.slot && entry.backendPort && entry.frontendPort && entry.dbName,
  );
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
    process.stdout.write(
      [
        `slot=${entry.slot}`,
        `id=${entry.workspaceId}`,
        `backend=${entry.backendPort}`,
        `frontend=${entry.frontendPort}`,
        `db=${entry.dbName}`,
        entry.root,
      ].join(' ') + '\n',
    );
  }
}

function releaseWorkspaceCommand(context, argv) {
  const workspacePath = readOptionValue(argv, '--workspace') || context.root;
  const targetRoot = resolve(workspacePath);
  const registry = readWorkspaceRegistry(context.root);
  const workspace = readReleaseWorkspace(targetRoot, registry);
  if (!argv.includes('--keep-db')) {
    dropWorkspaceDatabase(targetRoot, workspace);
  }
  const next = registry.filter((entry) => resolve(entry.root) !== targetRoot);
  writeWorkspaceRegistry(context.root, next);
  process.stdout.write(`Released Mango workspace registration: ${targetRoot}\n`);
}

function readReleaseWorkspace(targetRoot, registry) {
  const workspacePath = join(targetRoot, '.mango/workspace.json');
  if (existsSync(workspacePath)) {
    return readJsonFile(workspacePath);
  }
  return registry.find((entry) => resolve(entry.root) === targetRoot) || null;
}

function dropWorkspaceDatabase(targetRoot, workspace) {
  const dbName = workspace?.dbName || '';
  if (!dbName) {
    process.stdout.write(`No Mango workspace database recorded for ${targetRoot}; skipped DB cleanup.\n`);
    return;
  }
  if (!/^mango_dev_[a-zA-Z0-9_]+$/.test(dbName)) {
    fail(`refuse to drop non-workspace database: ${dbName}`);
  }
  const env = readEnvFile(join(targetRoot, '.mango/dev-workspace.env'));
  runMysqlStatement(env, `DROP DATABASE IF EXISTS \`${dbName}\`;`, {
    cwd: targetRoot,
    errorMessage: `failed to drop workspace database ${dbName}`,
  });
  process.stdout.write(`Dropped Mango workspace database: ${dbName}\n`);
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
        processMode: 'runtime',
        cwd: 'backend',
        pom: 'app/pom.xml',
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
  const apps = Object.fromEntries(discovery.apps.map((app) => [app.name, app.config]));
  const backendNames = discovery.apps.filter((app) => app.kind === 'backend').map((app) => app.name);
  const frontendNames = discovery.apps.filter((app) => app.kind === 'frontend').map((app) => app.name);
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
    warnings.push(
      `detected ${backendNames.length} backend app(s) and ${frontendNames.length} frontend app(s); confirm groups before starting`,
    );
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
  const pomFiles = files.filter((file) => basename(file) === 'pom.xml');
  const packageFiles = files.filter((file) => basename(file) === 'package.json');
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
  const firstBackend = apps.find((app) => app.kind === 'backend')?.name || '';
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
    .map((file) => {
      const content = readFileSync(file, 'utf8');
      return {
        file,
        relativeDir: toPosix(relative(root, dirname(file))) || '.',
        content,
      };
    })
    .filter((item) => isSpringBootAppPom(item.content))
    .sort((left, right) => left.relativeDir.localeCompare(right.relativeDir));
}

function isSpringBootAppPom(content) {
  if (isAggregatorPom(content)) {
    return false;
  }
  return (
    content.includes('<artifactId>spring-boot-maven-plugin</artifactId>') ||
    content.includes('<artifactId>spring-boot-starter-web</artifactId>') ||
    content.includes('<artifactId>spring-boot-starter-webflux</artifactId>') ||
    content.includes('<artifactId>mango-admin-starter</artifactId>') ||
    content.includes('<artifactId>mango-monolith-starter</artifactId>')
  );
}

function isAggregatorPom(content) {
  return content.includes('<packaging>pom</packaging>') && content.includes('<modules>');
}

function discoverViteApps(root, packageFiles) {
  return packageFiles
    .map((file) => {
      const directory = dirname(file);
      const packageJson = readJsonFile(file);
      return {
        file,
        relativeDir: toPosix(relative(root, directory)) || '.',
        packageJson,
        hasViteConfig: hasViteConfig(directory),
      };
    })
    .filter((item) => isVitePackage(item.packageJson, item.hasViteConfig))
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
  return ['vite.config.ts', 'vite.config.js', 'vite.config.mjs', 'vite.config.mts'].some((file) =>
    existsSync(join(directory, file)),
  );
}

function buildSpringBootDevApp(cwd, index) {
  const app = {
    type: 'spring-boot-maven',
    processMode: 'runtime',
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
  const springAppPaths = new Set(backendApps.map((app) => app.file));
  const skippedAggregatorPoms = pomFiles
    .filter((file) => !springAppPaths.has(file))
    .filter((file) => isAggregatorPom(readFileSync(file, 'utf8')))
    .map((file) => toPosix(relative(root, file)));
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
  process.stdout.write(`Maven revision qualifier: ${workspace.mavenRevisionQualifier}\n`);
  process.stdout.write(`Manifest:  ${context.manifestPath}\n`);
  process.stdout.write(`Workspace: ${context.workspacePath}\n`);
  process.stdout.write(`Env file:  ${join(context.root, '.mango/dev-workspace.env')}\n`);
  const databaseStatus = printWorkspaceDatabaseStatus(context, workspace);
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    process.stdout.write(`${name.padEnd(16)} ${resolved.type.padEnd(18)} ${resolved.cwd}`);
    if (resolved.port) {
      process.stdout.write(` port=${resolved.port}`);
    }
    process.stdout.write('\n');
  }
  if (databaseStatus.envMatch === false) {
    fail('workspace database does not match MANGO_DB_NAME in the workspace env');
  }
}

function printWorkspaceDatabaseStatus(context, workspace = ensureWorkspaceConfig(context.root)) {
  const envPath = join(context.root, '.mango/dev-workspace.env');
  const fileEnv = readEnvFile(envPath);
  const databaseName = workspace.dbName || '';
  const envDatabaseName = fileEnv.MANGO_DB_NAME || '';
  const envMatch = databaseName && envDatabaseName ? databaseName === envDatabaseName : null;
  const probe = probeWorkspaceDatabase(databaseName, { ...context.env, ...fileEnv });
  process.stdout.write(`Database:  ${databaseName || '<missing>'}\n`);
  process.stdout.write(`DB exists: ${probe.state}\n`);
  process.stdout.write(`Init source: mango workspace init -> ${context.workspacePath} -> ${envPath}\n`);
  process.stdout.write(`DB env match: ${envMatch === null ? 'UNKNOWN' : envMatch ? 'PASS' : 'FAIL'}\n`);
  process.stdout.write(
    `DB auto-create: ${isTruthy(fileEnv.MANGO_DB_AUTO_CREATE) ? 'enabled' : 'disabled'} (mango dev start)\n`,
  );
  return {
    databaseName,
    envDatabaseName,
    envMatch,
    probe,
  };
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
      if (app.bootstrapReceiptDirectory) {
        const receiptDirectory = resolve(cwdPath, app.bootstrapReceiptDirectory);
        if (!isPathInside(receiptDirectory, context.root)) {
          errors.push(`${name}: bootstrapReceiptDirectory must stay inside the workspace`);
        }
      }
    }
    if (app.type === 'spring-boot-maven' && app.goal && app.goal === 'spring-boot:run') {
      errors.push(`${name}: use explicit Spring Boot Maven plugin coordinate instead of spring-boot:run`);
    }
    if (app.type === 'spring-boot-maven') {
      if (app.command) {
        errors.push(
          `${name}: spring-boot-maven command overrides are unsupported; configure pom, goal and args instead`,
        );
      }
      const processMode = app.processMode || 'runtime';
      if (!['bootstrap', 'runtime'].includes(processMode)) {
        errors.push(`${name}: unsupported Mango processMode ${processMode}; expected bootstrap or runtime`);
      } else if (processMode !== 'runtime') {
        errors.push(`${name}: mango dev requires processMode runtime; use the Bootstrap CLI for initialization`);
      }
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
    process.stdout.write(
      `Development workspace manifest is valid: ${relativeOrAbsolute(process.cwd(), context.manifestPath)}\n`,
    );
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
      process.stdout.write(
        `warn    ${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}\n`,
      );
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
    applyStableBootstrapReceipt(context, resolved);
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
  context.env = {
    ...parseEnvText(defaultDevWorkspaceEnv(context.root)),
    ...readEnvFile(join(context.root, '.mango/dev-workspace.env')),
  };
  ensureRunDirs(context);
  const appNames = resolveDevWorkspaceTargets(context, targets.length > 0 ? targets : ['default']);
  for (const name of appNames) {
    const resolved = resolveDevApp(context, name, context.manifest.apps[name]);
    applyStableBootstrapReceipt(context, resolved);
    const current = readPidFile(context, name);
    if (current && isProcessAlive(current.pid)) {
      process.stdout.write(`${name} is already running: pid=${current.pid}\n`);
      continue;
    }
    if (resolved.port && isPortInUse(resolved.port)) {
      fail(
        `${name} port ${resolved.port} is already in use${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}.`,
      );
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
    process.stdout.write(
      `Added MANGO_CRYPTO_SM4_SECRET_KEY to local workspace env: ${relativeOrAbsolute(process.cwd(), envPath)}\n`,
    );
  }
  const requiredValues = {
    MANGO_WORKSPACE_ID: workspace.workspaceId,
    MANGO_MAVEN_REVISION_QUALIFIER: workspace.mavenRevisionQualifier,
    MANGO_BACKEND_PORT: workspace.backendPort,
    MANGO_FRONTEND_PORT: workspace.frontendPort,
    MANGO_DB_NAME: workspace.dbName,
    MANGO_FRONTEND_MODE: 'source',
    ...workspace.frontendApps,
  };
  const changes = syncEnvFileValues(envPath, requiredValues);
  if (changes.added.length > 0) {
    process.stdout.write(`Added workspace ownership values to local env: ${changes.added.join(', ')}\n`);
  }
  if (changes.updated.length > 0) {
    process.stdout.write(`Synchronized workspace ownership values in local env: ${changes.updated.join(', ')}\n`);
  }
}

function syncEnvFileValues(envPath, requiredValues) {
  const original = existsSync(envPath) ? readFileSync(envPath, 'utf8') : '';
  const lines = original.split(/\r?\n/);
  const seen = new Set();
  const updated = [];
  const nextLines = lines.map((line) => {
    const match = line.match(/^(\s*([A-Za-z_][A-Za-z0-9_]*)\s*=)(.*)$/);
    if (!match) {
      return line;
    }
    const key = match[2];
    if (!Object.prototype.hasOwnProperty.call(requiredValues, key)) {
      return line;
    }
    seen.add(key);
    const value = String(requiredValues[key]);
    if (unquoteEnvValue(match[3].trim()) !== value) {
      updated.push(key);
      return `${key}=${value}`;
    }
    return line;
  });
  const added = [];
  for (const [key, value] of Object.entries(requiredValues)) {
    if (!seen.has(key)) {
      added.push(key);
      nextLines.push(`${key}=${value}`);
    }
  }
  if (added.length > 0 || updated.length > 0 || !original.endsWith('\n')) {
    writeFileSync(envPath, `${nextLines.join('\n').replace(/\n+$/u, '')}\n`);
  }
  return { added, updated };
}

function startDevApp(context, name, app) {
  const logPath = join(context.logDir, `${name}.log`);
  appendFileSync(logPath, `\n--- ${new Date().toISOString()} start ${name} ---\n`);
  if (shouldEnsureWorkspaceDatabase(context, app)) {
    ensureWorkspaceDatabase(context, name, logPath);
  }
  if (shouldRunDevInstall(app, context.env)) {
    requireCommand(app.install.command, name);
    process.stdout.write(`${name}: running install command\n`);
    const install = runForegroundCommand(app.cwd, app.install.command, app.install.args, app.env, logPath);
    if (install.status !== 0) {
      fail(`${name}: install command failed, see ${relativeOrAbsolute(process.cwd(), logPath)}`);
    }
  } else if (app.install) {
    process.stdout.write(
      `${name}: ignored legacy install command; Spring Boot development now runs from the Maven reactor\n`,
    );
  }
  if (app.lifecycleManaged) {
    prepareManagedMangoLifecycle(context, name, app, logPath);
  }
  requireCommand(app.command, name);
  const logFd = openSync(logPath, 'a');
  const child = spawn(app.command, app.args, {
    cwd: app.runCwd || app.cwd,
    env: { ...process.env, ...app.env },
    detached: true,
    stdio: ['ignore', logFd, logFd],
  });
  child.on('error', (error) => {
    appendFileSync(logPath, `\n--- ${new Date().toISOString()} failed ${name}: ${error.message} ---\n`);
    closeSync(logFd);
    rmSync(pidFilePath(context, name), { force: true });
  });
  child.on('exit', (code, signal) => {
    appendFileSync(
      logPath,
      `\n--- ${new Date().toISOString()} exit ${name} code=${code ?? ''} signal=${signal ?? ''} ---\n`,
    );
    closeSync(logFd);
  });
  child.unref();
  writePidFile(context, name, {
    pid: child.pid,
    pgid: child.pid,
    startedAt: new Date().toISOString(),
    cwd: app.runCwd || app.cwd,
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
  const values = [app.command, ...(app.args || []), ...Object.values(app.env || {})].filter(Boolean);
  return values.some((value) => {
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
  runMysqlStatement(
    context.env,
    `CREATE DATABASE IF NOT EXISTS \`${dbName}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`,
    {
      cwd: context.root,
      logPath,
      errorMessage: `${appName}: failed to auto-create database ${dbName}`,
    },
  );
  process.stdout.write(`${appName}: ensured database ${dbName}\n`);
}

function workspaceDatabaseExists(dbName, env = {}) {
  return probeWorkspaceDatabase(dbName, env).state === 'YES';
}

function probeWorkspaceDatabase(dbName, env = {}) {
  if (!/^mango_dev_[a-zA-Z0-9_]+$/.test(dbName)) {
    return { state: 'UNKNOWN' };
  }
  const statement = [
    'SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA',
    `WHERE SCHEMA_NAME = '${dbName.replace(/'/g, "''")}'`,
  ].join(' ');
  const result = runMysqlStatement(env, statement, {
    cwd: process.cwd(),
    capture: true,
    allowFailure: true,
  });
  if (result.status !== 0) {
    return { state: 'UNKNOWN' };
  }
  return {
    state: String(result.stdout || '')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .includes(dbName)
      ? 'YES'
      : 'NO',
  };
}

function runMysqlStatement(dbEnv = {}, statement, options = {}) {
  const mysqlArgs = [
    '--protocol=TCP',
    '-h',
    dbEnv.MANGO_DB_HOST || '127.0.0.1',
    '-P',
    String(dbEnv.MANGO_DB_PORT || '3306'),
    '-u',
    dbEnv.MANGO_DB_USERNAME || 'root',
    '-e',
    statement,
  ];
  const env = { ...process.env };
  if (dbEnv.MANGO_DB_PASSWORD) {
    env.MYSQL_PWD = dbEnv.MANGO_DB_PASSWORD;
  }
  const result = spawnSync('mysql', mysqlArgs, {
    cwd: options.cwd || process.cwd(),
    env,
    stdio: 'pipe',
    encoding: 'utf8',
  });
  if (options.logPath && result.stdout) {
    appendFileSync(options.logPath, result.stdout);
  }
  if (options.logPath && result.stderr) {
    appendFileSync(options.logPath, result.stderr);
  }
  if (options.logPath && result.error) {
    appendFileSync(options.logPath, `${result.error.message}\n`);
  }
  if (!options.allowFailure && result.status !== 0) {
    const reason = result.error ? `: ${result.error.message}` : '';
    const logHint = options.logPath ? `, see ${relativeOrAbsolute(process.cwd(), options.logPath)}` : '';
    fail(`${options.errorMessage || 'mysql command failed'}${reason}${logHint}`);
  }
  return result;
}

function prepareManagedMangoLifecycle(context, appName, app, logPath) {
  assertManagedLifecycleWorkspaceDatabase(context, appName, app);
  const lifecycle = buildLocalLifecycleIdentity(context, appName, app);
  const control = readBootstrapGenerationState(context.env, lifecycle.environmentKey);
  const highestGeneration = Math.max(control.stableGeneration, control.candidateGeneration);
  let generation = control.stableGeneration;
  if (control.stableGeneration > 0) {
    const verifyLogOffset = existsSync(logPath) ? readFileSync(logPath, 'utf8').length : 0;
    const verifyResult = runManagedMangoLifecycleCommand(
      app,
      buildManagedLifecycleSpringArgs(app.springArgs, 'bootstrap', lifecycle, generation, 'verify'),
      logPath,
      `${appName}: verifying bootstrap generation ${generation}`,
    );
    if (verifyResult.status !== 0) {
      const failure = readFileSync(logPath, 'utf8').slice(verifyLogOffset);
      if (!failure.includes('BOOTSTRAP_FINGERPRINT_MISMATCH')) {
        fail(`${appName}: bootstrap verify failed, see ${relativeOrAbsolute(process.cwd(), logPath)}`);
      }
      generation = highestGeneration + 1;
      runColdBootstrap(appName, app, lifecycle, generation, logPath, false);
    }
  } else {
    generation = Math.max(1, control.candidateGeneration);
    if (!runColdBootstrap(appName, app, lifecycle, generation, logPath, true)) {
      generation = highestGeneration + 1;
      runColdBootstrap(appName, app, lifecycle, generation, logPath, false);
    }
  }
  app.springArgs = buildManagedLifecycleSpringArgs(app.springArgs, 'runtime', lifecycle, generation);
  app.args = replaceSpringBootRunArguments(app.args, app.springArgs);
  process.stdout.write(`${appName}: using Mango runtime generation ${generation}\n`);
}

function assertManagedLifecycleWorkspaceDatabase(context, appName, app) {
  const workspaceDatabase = context.env.MANGO_DB_NAME || '';
  if (!/^mango_dev_[a-zA-Z0-9_]+$/.test(workspaceDatabase)) {
    fail(`${appName}: refuse to auto-bootstrap non-workspace database: ${workspaceDatabase || '<empty>'}`);
  }
  const datasourceDatabase = extractDatasourceDatabase(app.springArgs);
  if (!datasourceDatabase) {
    fail(`${appName}: managed local bootstrap requires an explicit MySQL spring.datasource.url`);
  }
  if (datasourceDatabase !== workspaceDatabase) {
    fail(
      `${appName}: refuse to auto-bootstrap datasource ${datasourceDatabase}; ` +
        `expected workspace database ${workspaceDatabase}`,
    );
  }
}

function runColdBootstrap(appName, app, lifecycle, generation, logPath, allowFingerprintMismatch) {
  const logOffset = existsSync(logPath) ? readFileSync(logPath, 'utf8').length : 0;
  const result = runManagedMangoLifecycleCommand(
    app,
    buildManagedLifecycleSpringArgs(app.springArgs, 'bootstrap', lifecycle, generation, 'apply'),
    logPath,
    `${appName}: applying cold bootstrap generation ${generation}`,
  );
  if (result.status === 0) {
    return true;
  }
  const failure = readFileSync(logPath, 'utf8').slice(logOffset);
  if (allowFingerprintMismatch && failure.includes('BOOTSTRAP_FINGERPRINT_MISMATCH')) {
    return false;
  }
  fail(`${appName}: bootstrap apply failed, see ${relativeOrAbsolute(process.cwd(), logPath)}`);
}

function runManagedMangoLifecycleCommand(app, springArgs, logPath, message) {
  process.stdout.write(`${message}\n`);
  appendFileSync(logPath, `\n--- ${new Date().toISOString()} ${message} ---\n`);
  return runForegroundCommand(
    app.runCwd || app.cwd,
    app.command,
    replaceSpringBootRunArguments(app.args, springArgs),
    app.env,
    logPath,
  );
}

function buildLocalLifecycleIdentity(context, appName, app) {
  const workspaceId = context.env.MANGO_WORKSPACE_ID || ensureWorkspaceConfig(context.root).workspaceId;
  const suffix = `${workspaceId}-${appName}`
    .toLowerCase()
    .replace(/[^a-z0-9_.-]+/gu, '-')
    .replace(/^-+|-+$/gu, '');
  return {
    environmentKey: `local-${suffix}`,
    releaseId: `local-${suffix}`,
    revision: app.mavenRevision || context.env.MANGO_MAVEN_REVISION_QUALIFIER || workspaceId,
  };
}

function readBootstrapGenerationState(dbEnv, environmentKey) {
  const dbName = dbEnv.MANGO_DB_NAME || '';
  if (!/^mango_dev_[a-zA-Z0-9_]+$/.test(dbName)) {
    return { stableGeneration: 0, candidateGeneration: 0 };
  }
  const escapedEnvironmentKey = environmentKey.replace(/'/gu, "''");
  const result = runMysqlStatement(
    dbEnv,
    `SELECT stable_generation, COALESCE(candidate_generation, 0) FROM \`${dbName}\`.mango_bootstrap_control ` +
      `WHERE environment_key = '${escapedEnvironmentKey}' LIMIT 1;`,
    {
      cwd: process.cwd(),
      capture: true,
      allowFailure: true,
    },
  );
  if (result.status !== 0) {
    return { stableGeneration: 0, candidateGeneration: 0 };
  }
  const row = String(result.stdout || '')
    .split(/\r?\n/u)
    .map((line) =>
      line
        .trim()
        .split(/\s+/u)
        .map((value) => Number.parseInt(value, 10)),
    )
    .find((values) => values.length >= 2 && values.every((value) => Number.isInteger(value)));
  return {
    stableGeneration: Math.max(0, row?.[0] || 0),
    candidateGeneration: Math.max(0, row?.[1] || 0),
  };
}

function buildManagedLifecycleSpringArgs(configuredArgs, mode, lifecycle, generation, action = '') {
  const remaining = [...configuredArgs];
  if (isMangoLifecycleMode(remaining[0])) {
    const configuredMode = remaining.shift();
    if (configuredMode === 'bootstrap' && remaining[0] && !remaining[0].startsWith('--')) {
      remaining.shift();
    }
  }
  const filtered = remaining.filter(
    (argument) =>
      !argument.startsWith('--mango.release.id=') &&
      !argument.startsWith('--mango.release.revision=') &&
      !argument.startsWith('--mango.release.generation=') &&
      !argument.startsWith('--mango.bootstrap.environment-key=') &&
      !argument.startsWith('--mango.bootstrap.strategy='),
  );
  const prefix = mode === 'bootstrap' ? ['bootstrap', action] : ['runtime'];
  if (mode === 'bootstrap' && action === 'apply') {
    prefix.push('--mango.bootstrap.strategy=cold');
  }
  return [
    ...prefix,
    `--mango.bootstrap.environment-key=${lifecycle.environmentKey}`,
    `--mango.release.id=${lifecycle.releaseId}`,
    `--mango.release.revision=${lifecycle.revision}`,
    `--mango.release.generation=${generation}`,
    ...filtered,
  ];
}

function replaceSpringBootRunArguments(commandArgs, springArgs) {
  const property = `-Dspring-boot.run.arguments=${springArgs.join(' ')}`;
  let replaced = false;
  const nextArgs = commandArgs.map((argument) => {
    if (argument.startsWith('-Dspring-boot.run.arguments=')) {
      replaced = true;
      return property;
    }
    return argument;
  });
  if (!replaced) {
    throw new Error('Spring Boot Maven command is missing spring-boot.run.arguments');
  }
  return nextArgs;
}

function isTruthy(value) {
  return ['1', 'true', 'yes', 'on'].includes(
    String(value || '')
      .trim()
      .toLowerCase(),
  );
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
  const logFd = openSync(logPath, 'a');
  try {
    return spawnSync(command, args, {
      cwd,
      env: { ...process.env, ...env },
      stdio: ['ignore', logFd, logFd],
    });
  } finally {
    closeSync(logFd);
  }
}

async function waitForDevApp(context, name, app) {
  const timeoutMs = Number(app.waitTimeoutMs || 120000);
  const pollIntervalMs = resolveHealthPollIntervalMs(app.waitPollIntervalMs);
  const startedAt = Date.now();
  process.stdout.write(`${name}: waiting for ${app.healthUrl}\n`);
  const tick = () => new Promise((resolvePromise) => setTimeout(resolvePromise, pollIntervalMs));
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
  const workspace = ensureWorkspaceConfig(context.root);
  const databaseStatus = printWorkspaceDatabaseStatus(context, workspace);
  const datasourceMismatches = [];
  for (const [name, app] of Object.entries(context.manifest.apps || {})) {
    const resolved = resolveDevApp(context, name, app);
    const pidInfo = readPidFile(context, name);
    const alive = pidInfo && isProcessAlive(pidInfo.pid);
    const occupied = !alive && resolved.port && isPortInUse(resolved.port);
    const status = alive ? 'running' : occupied ? 'occupied' : 'stopped';
    const pidText = alive ? ` pid=${pidInfo.pid}` : '';
    const urlText = resolved.url ? ` ${resolved.url}` : '';
    const ownerText =
      alive && pidInfo.workspaceRoot ? ` owner=${relativeOrAbsolute(process.cwd(), pidInfo.workspaceRoot)}` : '';
    const occupantText = occupied
      ? `${formatPortOccupants(resolved.port)}${formatPortOwnerHint(context.root, resolved.port)}`
      : '';
    let datasourceText = '';
    if (alive) {
      const datasourceDatabase = extractDatasourceDatabase(pidInfo.args || []);
      if (resolved.type === 'spring-boot-maven' || datasourceDatabase) {
        const datasourceMatch = datasourceDatabase ? datasourceDatabase === databaseStatus.databaseName : null;
        datasourceText = ` datasourceDb=${datasourceDatabase || 'UNKNOWN'} dbMatch=${
          datasourceMatch === null ? 'UNKNOWN' : datasourceMatch ? 'PASS' : 'FAIL'
        }`;
        if (datasourceMatch === false) {
          datasourceMismatches.push(`${name}: ${datasourceDatabase}`);
        }
      }
    }
    process.stdout.write(
      `${status.padEnd(8)} ${name}${pidText}${urlText}${ownerText}${occupantText}${datasourceText}\n`,
    );
  }
  if (databaseStatus.envMatch === false) {
    fail('workspace database does not match MANGO_DB_NAME in the workspace env');
  }
  if (datasourceMismatches.length > 0) {
    fail(
      `running backend datasource does not match workspace database ${databaseStatus.databaseName}: ${datasourceMismatches.join(', ')}`,
    );
  }
}

function extractDatasourceDatabase(args) {
  for (const value of args || []) {
    const match = String(value).match(/(?:--|\b)spring\.datasource\.url=([^\s]+)/u);
    if (!match) {
      continue;
    }
    const jdbcUrl = match[1].replace(/^["']|["']$/g, '');
    const databaseMatch = jdbcUrl.match(/^jdbc:mysql:\/\/[^/]+\/([^?;\s]+)/iu);
    if (!databaseMatch) {
      return '';
    }
    try {
      return decodeURIComponent(databaseMatch[1]);
    } catch {
      return databaseMatch[1];
    }
  }
  return '';
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
      .filter((app) => app.type === 'vite')
      .map((app) => resolve(context.root, app.cwd || '.')),
    (item) => item,
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
      process.stdout.write(
        `frontend package mode: ${relativeOrAbsolute(context.root, appRoot)} expects built package artifacts\n`,
      );
      continue;
    }
    if (!existsSync(join(uiRoot, 'package.json'))) {
      process.stdout.write(`warn    frontend root not found for ${relativeOrAbsolute(context.root, appRoot)}\n`);
      continue;
    }
    const adminStylesScript = packageJsonHasScript(join(uiRoot, 'package.json'), 'admin:styles');
    if (adminStylesScript) {
      if (checkOnly) {
        process.stdout.write(
          `ok      frontend prepare can run ${packageManager} admin:styles in ${relativeOrAbsolute(process.cwd(), uiRoot)}\n`,
        );
      } else {
        runCheckedCommand(uiRoot, packageManager, ['admin:styles'], 'frontend style aggregation');
      }
    }
    const adminPackagePath = join(uiRoot, 'packages/admin/package.json');
    if (existsSync(adminPackagePath)) {
      const missing = requiredAdminSourceModeArtifacts(uiRoot).filter((path) => !existsSync(path));
      if (missing.length === 0) {
        process.stdout.write(
          `ok      frontend source artifacts ready in ${relativeOrAbsolute(process.cwd(), uiRoot)}\n`,
        );
        continue;
      }
      if (checkOnly) {
        process.stdout.write(
          `warn    missing source-mode artifacts:\n${missing.map((path) => `        ${relativeOrAbsolute(process.cwd(), path)}`).join('\n')}\n`,
        );
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
  return [join(uiRoot, 'packages/admin/generated-package-styles.css'), join(uiRoot, 'packages/admin/style-full.css')];
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
    if (!isProcessAlive(pidInfo.pid) && !isProcessGroupAlive(pidInfo.pid)) {
      rmSync(pidFilePath(context, name), { force: true });
      process.stdout.write(`${name}: stale pid removed\n`);
      continue;
    }
    const app = context.manifest.apps[name] || {};
    const outcome = await stopProcessGroup(pidInfo.pid, {
      graceMs: app.stopTimeoutMs,
      killWaitMs: app.stopKillWaitMs,
    });
    if (!outcome.stopped) {
      fail(`${name}: process group ${pidInfo.pid} is still alive after SIGKILL`);
    }
    rmSync(pidFilePath(context, name), { force: true });
    process.stdout.write(`${name}: stopped pid=${pidInfo.pid}${outcome.forced ? ' forced=true' : ''}\n`);
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
  resolved.springArgs = commandSpec.springArgs || [];
  resolved.lifecycleManaged = Boolean(commandSpec.lifecycleManaged);
  resolved.url = port ? `http://${host || '127.0.0.1'}:${port}` : '';
  resolved.healthUrl = resolved.health ? resolveHealthUrl(resolved.url, resolved.health) : '';
  if (app.install) {
    resolved.install = {
      command: app.install.command,
      args: (app.install.args || []).map((arg) => interpolateValue(String(arg), vars)),
    };
  }
  applyWorkspaceMavenRevision(context, resolved);
  return resolved;
}

function applyWorkspaceMavenRevision(context, app) {
  const runUsesMaven = isMavenCommand(app.command);
  if (app.type !== 'spring-boot-maven' || !runUsesMaven) {
    return;
  }
  let reactor;
  try {
    reactor = resolveSpringBootMavenReactor({
      workspaceRoot: context.root,
      appPomPath: resolve(app.cwd, app.pom || 'pom.xml'),
    });
  } catch (error) {
    fail(`${app.name}: ${error.message}`);
  }
  const rootPom = reactor.rootPom;
  const baseRevision = readCiFriendlyMavenRevision(readFileSync(rootPom, 'utf8'));
  const qualifier = ensureWorkspaceConfig(context.root).mavenRevisionQualifier;
  const revision = qualifyWorkspaceMavenRevision(baseRevision, qualifier);
  app.mavenRevision = revision;
  app.mavenRevisionPom = rootPom;
  app.runCwd = reactor.cwd;
  app.args = buildSpringBootReactorArgs({
    rootPom,
    selector: reactor.selector,
    revision,
    springArgs: app.springArgs,
    goal: app.goal || DEFAULT_SPRING_BOOT_PLUGIN,
  });
}

function applyStableBootstrapReceipt(context, app) {
  if (
    app.type !== 'spring-boot-maven' ||
    (app.processMode || 'runtime') !== 'runtime' ||
    app.lifecycleManaged ||
    !usesMangoApplication(app)
  ) {
    return;
  }
  const workspace = ensureWorkspaceConfig(context.root);
  const directories = resolveBootstrapReceiptDirectories(context, app);
  try {
    const receipt = readStableBootstrapReceipt({
      directories,
      workspaceRoot: context.root,
      workspaceId: workspace.workspaceId,
      databaseName: workspace.dbName,
      expectedRevision: app.mavenRevision,
    });
    app.args = injectStableBootstrapIdentity(app.args, receipt);
    app.bootstrapReceipt = receipt;
  } catch (error) {
    fail(`${app.name}: ${error.code || 'BOOTSTRAP_RUNTIME_RECEIPT_INVALID'} ${error.message}`);
  }
}

function resolveBootstrapReceiptDirectories(context, app) {
  const configured = app.bootstrapReceiptDirectory
    ? [resolve(app.cwd, app.bootstrapReceiptDirectory)]
    : [join(context.root, '.mango/bootstrap'), join(app.cwd, '.mango/bootstrap')];
  const unique = [...new Set(configured.map((directory) => resolve(directory)))];
  for (const directory of unique) {
    if (!isPathInside(directory, context.root)) {
      fail(`${app.name}: bootstrap receipt directory must stay inside the workspace: ${directory}`);
    }
  }
  return unique;
}

function isPathInside(path, root) {
  const relativePath = relative(resolve(root), resolve(path));
  return relativePath === '' || (!relativePath.startsWith('..') && !isAbsolute(relativePath));
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
      args: (app.args || []).map((arg) => interpolateValue(String(arg), vars)).filter(Boolean),
    };
  }
  if (app.type === 'spring-boot-maven') {
    const pom = app.pom || 'pom.xml';
    const goal = app.goal || DEFAULT_SPRING_BOOT_PLUGIN;
    const configuredSpringArgs = (app.args || []).map((arg) => interpolateValue(String(arg), vars)).filter(Boolean);
    const lifecycleManaged = app.mangoLifecycle === true || usesMangoApplication(app);
    const processMode = app.processMode || 'runtime';
    const resolvedArgs = (app.args || []).map((arg) => interpolateValue(String(arg), vars)).filter(Boolean);
    const modeArguments = configuredSpringArgs
      .flatMap((arg) => arg.trim().split(/\s+/u))
      .filter((arg) => arg === 'bootstrap' || arg === 'runtime');
    if (modeArguments.length > 0) {
      fail(
        `${app.name}: process mode must be declared only through processMode; ` +
          `remove ${modeArguments.join(', ')} from app.args`,
      );
    }
    const springArgs = [processMode, ...configuredSpringArgs];
    return {
      command: 'mvn',
      args: ['-f', pom, `-Dspring-boot.run.arguments=${springArgs.join(' ')}`, goal],
      springArgs,
      lifecycleManaged,
    };
  }
  if (app.type === 'vite') {
    const packageManager = app.packageManager || detectPackageManager(context.root);
    return {
      command: packageManager,
      args: (app.args || ['run', 'dev', '--', '--host', '${host}', '--port', '${port}'])
        .map((arg) => interpolateValue(String(arg), vars))
        .filter(Boolean),
    };
  }
  if (app.type === 'command') {
    fail(`${app.name}: command type requires command`);
  }
  fail(`${app.name}: unsupported app type ${app.type}`);
}

function isMangoLifecycleMode(value) {
  return value === 'runtime' || value === 'bootstrap';
}

function usesMangoApplication(app) {
  const pomPath = resolve(app.cwd, app.pom || 'pom.xml');
  const sourceRoot = join(dirname(pomPath), 'src/main/java');
  return directoryContainsText(sourceRoot, 'MangoApplication.run(', 10);
}

function directoryContainsText(directory, expected, remainingDepth) {
  if (remainingDepth < 0 || !existsSync(directory)) {
    return false;
  }
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      if (directoryContainsText(path, expected, remainingDepth - 1)) {
        return true;
      }
      continue;
    }
    if (entry.isFile() && entry.name.endsWith('.java') && readFileSync(path, 'utf8').includes(expected)) {
      return true;
    }
  }
  return false;
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

function isPortInUse(port) {
  if (!port) {
    return false;
  }
  const result = spawnSync('sh', ['-c', `lsof -nP -iTCP:${Number(port)} -sTCP:LISTEN >/dev/null 2>&1`], {
    stdio: 'ignore',
  });
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
  const text = occupants.map((item) => `pid=${item.pid}${item.command ? ` command=${item.command}` : ''}`).join(', ');
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
  return readWorkspaceRegistry(root).find((entry) => workspacePorts(entry).includes(Number(port)));
}

function httpOk(url) {
  return new Promise((resolvePromise) => {
    const client = url.startsWith('https:') ? https : http;
    const request = client.get(url, { timeout: 2000 }, (response) => {
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

async function runDocsCommand(command = 'status', argv = []) {
  const normalized = command || 'status';
  if (!['pull', 'status', 'path'].includes(normalized)) {
    fail(`unknown docs command: ${command || ''}`);
  }
  const options = parseDocsArgs(argv);
  const context = resolveDocsContext(options);
  if (normalized === 'pull') {
    await pullDocsBundle(context, options);
    return;
  }
  if (normalized === 'path') {
    printDocsPath(context);
    return;
  }
  printDocsStatus(context);
}

function parseDocsArgs(argv) {
  const result = {
    projectDir: '.',
    version: '',
    mavenRepository: '',
    force: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--force') {
      result.force = true;
      continue;
    }
    if (['--project-dir', '--version', '--maven-repository'].includes(arg)) {
      const next = argv[index + 1];
      if (!next || next.startsWith('--')) {
        fail(`missing value for ${arg}`);
      }
      if (arg === '--project-dir') {
        result.projectDir = next;
      } else if (arg === '--version') {
        result.version = next;
      } else {
        result.mavenRepository = next;
      }
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

function resolveDocsContext(options) {
  const projectDir = resolve(process.cwd(), options.projectDir);
  if (!existsSync(projectDir) || !statSync(projectDir).isDirectory()) {
    fail(`project directory not found: ${projectDir}`);
  }
  const config = readOptionalJson(join(projectDir, 'mango.config.json'));
  const version =
    options.version ||
    config?.mangoBackendVersion ||
    readMangoVersionFromPom(projectDir) ||
    defaultVersions.mangoBackend;
  if (!version || /[\\/]/.test(version)) {
    fail(`invalid Mango docs version: ${version || ''}`);
  }
  const mavenRepository = ensureTrailingSlash(
    options.mavenRepository || config?.mavenRepository || DEFAULT_MAVEN_REPOSITORY,
  );
  const docsBaseDir = join(projectDir, '.mango/docs');
  const currentPath = join(docsBaseDir, 'current.json');
  const current = readOptionalJson(currentPath);
  const installRoot = join(docsBaseDir, version);
  const docsRoot = resolveDocsContentRoot(installRoot);
  return {
    projectDir,
    version,
    mavenRepository,
    docsBaseDir,
    currentPath,
    current,
    installRoot,
    docsRoot,
    sourceUrl: buildMavenArtifactUrl(mavenRepository, DOCS_BUNDLE_GROUP_ID, DOCS_BUNDLE_ARTIFACT_ID, version),
  };
}

async function pullDocsBundle(context, options) {
  mkdirSync(context.docsBaseDir, { recursive: true });
  if (existsSync(context.installRoot) && !options.force) {
    writeCurrentDocsMetadata(context, {
      status: 'available',
      sourceUrl: context.current?.sourceUrl || context.sourceUrl,
      sha256: context.current?.sha256 || '',
    });
    process.stdout.write(
      `Mango docs ${context.version} already available: ${resolveDocsContentRoot(context.installRoot)}\n`,
    );
    return;
  }

  const tempSuffix = randomBytes(6).toString('hex');
  const tempArchive = join(context.docsBaseDir, `${DOCS_BUNDLE_ARTIFACT_ID}-${context.version}-${tempSuffix}.jar`);
  const tempExtractRoot = join(context.docsBaseDir, `${context.version}.tmp-${tempSuffix}`);
  await downloadFile(context.sourceUrl, tempArchive);
  rmSync(tempExtractRoot, { recursive: true, force: true });
  mkdirSync(tempExtractRoot, { recursive: true });
  extractArchive(tempArchive, tempExtractRoot);
  const extractedDocsRoot = resolveDocsContentRoot(tempExtractRoot);
  assertDocsBundleContent(extractedDocsRoot, context.sourceUrl);
  rmSync(context.installRoot, { recursive: true, force: true });
  renameSync(tempExtractRoot, context.installRoot);
  writeCurrentDocsMetadata(context, {
    status: 'available',
    sourceUrl: context.sourceUrl,
    sha256: hashFile(tempArchive),
  });
  rmSync(tempArchive, { force: true });
  process.stdout.write(`Pulled Mango docs ${context.version}: ${resolveDocsContentRoot(context.installRoot)}\n`);
}

function printDocsStatus(context) {
  const installed = existsSync(context.installRoot);
  const currentVersion = context.current?.version || '';
  process.stdout.write(`projectDir: ${context.projectDir}\n`);
  process.stdout.write(`mangoVersion: ${context.version}\n`);
  process.stdout.write(`mavenRepository: ${context.mavenRepository}\n`);
  process.stdout.write(`docsBundle: ${DOCS_BUNDLE_GROUP_ID}:${DOCS_BUNDLE_ARTIFACT_ID}:${context.version}\n`);
  process.stdout.write(`installed: ${installed ? 'yes' : 'no'}\n`);
  process.stdout.write(`currentVersion: ${currentVersion || 'none'}\n`);
  if (installed) {
    process.stdout.write(`path: ${resolveDocsContentRoot(context.installRoot)}\n`);
  } else {
    process.stdout.write(`path: none\n`);
    process.stdout.write(
      `next: mango docs pull --project-dir ${relativeOrAbsolute(process.cwd(), context.projectDir)}\n`,
    );
  }
  if (currentVersion && currentVersion !== context.version) {
    process.stdout.write(
      `warning: current docs version ${currentVersion} differs from project Mango version ${context.version}\n`,
    );
  }
}

function printDocsPath(context) {
  if (!existsSync(context.installRoot)) {
    fail(
      `Mango docs ${context.version} are not installed. Run "mango docs pull --project-dir ${relativeOrAbsolute(process.cwd(), context.projectDir)}".`,
    );
  }
  process.stdout.write(`${resolveDocsContentRoot(context.installRoot)}\n`);
}

function writeCurrentDocsMetadata(context, extra) {
  mkdirSync(context.docsBaseDir, { recursive: true });
  const metadata = {
    version: context.version,
    artifact: `${DOCS_BUNDLE_GROUP_ID}:${DOCS_BUNDLE_ARTIFACT_ID}:${context.version}`,
    sourceUrl: extra.sourceUrl,
    sha256: extra.sha256 || '',
    path: resolveDocsContentRoot(context.installRoot),
    status: extra.status,
    fetchedAt: new Date().toISOString(),
  };
  writeFileSync(context.currentPath, `${JSON.stringify(metadata, null, 2)}\n`);
}

function buildMavenArtifactUrl(repository, groupId, artifactId, version) {
  const base = ensureTrailingSlash(repository);
  const groupPath = groupId.replaceAll('.', '/');
  return `${base}${groupPath}/${artifactId}/${version}/${artifactId}-${version}.jar`;
}

async function downloadFile(url, targetPath) {
  if (url.startsWith('file:')) {
    const sourcePath = fileURLToPath(url);
    if (!existsSync(sourcePath)) {
      fail(`docs bundle not found: ${sourcePath}`);
    }
    copyFileSync(sourcePath, targetPath);
    return;
  }
  if (!/^https?:\/\//.test(url)) {
    fail(`unsupported docs bundle URL: ${url}`);
  }
  await downloadHttpFile(url, targetPath);
}

function downloadHttpFile(url, targetPath, redirects = 0) {
  return new Promise((resolvePromise, reject) => {
    const client = url.startsWith('https:') ? https : http;
    const request = client.get(url, (response) => {
      const statusCode = response.statusCode || 0;
      const location = response.headers.location;
      if ([301, 302, 303, 307, 308].includes(statusCode) && location) {
        response.resume();
        if (redirects >= 5) {
          reject(new Error(`too many redirects while downloading ${url}`));
          return;
        }
        const nextUrl = new URL(location, url).toString();
        downloadHttpFile(nextUrl, targetPath, redirects + 1).then(resolvePromise, reject);
        return;
      }
      if (statusCode < 200 || statusCode >= 300) {
        response.resume();
        reject(new Error(`download failed ${statusCode}: ${url}`));
        return;
      }
      const file = openSync(targetPath, 'w');
      response.on('data', (chunk) => {
        writeFileSync(file, chunk);
      });
      response.on('end', () => {
        closeSync(file);
        resolvePromise();
      });
      response.on('error', (error) => {
        closeSync(file);
        reject(error);
      });
    });
    request.on('error', reject);
  }).catch((error) => fail(error.message));
}

function extractArchive(archivePath, targetDir) {
  const jarResult = spawnSync('jar', ['xf', archivePath], {
    cwd: targetDir,
    encoding: 'utf8',
  });
  if (jarResult.status === 0) {
    return;
  }
  const unzipResult = spawnSync('unzip', ['-q', archivePath, '-d', targetDir], {
    encoding: 'utf8',
  });
  if (unzipResult.status === 0) {
    return;
  }
  fail(
    `cannot extract docs bundle. jar: ${jarResult.stderr || jarResult.stdout}; unzip: ${unzipResult.stderr || unzipResult.stdout}`,
  );
}

function resolveDocsContentRoot(installRoot) {
  const nestedRoot = join(installRoot, 'META-INF/mango-docs');
  return existsSync(nestedRoot) ? nestedRoot : installRoot;
}

function assertDocsBundleContent(docsRoot, sourceUrl) {
  if (!existsSync(docsRoot) || !statSync(docsRoot).isDirectory()) {
    fail(`docs bundle has no docs root: ${sourceUrl}`);
  }
  const hasReadme = existsSync(join(docsRoot, 'README.md'));
  const hasManifest = existsSync(join(docsRoot, 'index.json'));
  const hasCapabilities = existsSync(join(docsRoot, 'capabilities/README.md'));
  if (!hasReadme && !hasManifest && !hasCapabilities) {
    fail(`docs bundle does not look like Mango docs: ${sourceUrl}`);
  }
}

function readOptionalJson(path) {
  if (!existsSync(path)) {
    return null;
  }
  return JSON.parse(readFileSync(path, 'utf8'));
}

function readMangoVersionFromPom(projectDir) {
  for (const relativePath of ['backend/pom.xml', 'pom.xml']) {
    const pomPath = join(projectDir, relativePath);
    if (!existsSync(pomPath)) {
      continue;
    }
    const content = readFileSync(pomPath, 'utf8');
    const match = content.match(/<mango\.version>\s*([^<\s]+)\s*<\/mango\.version>/);
    if (match) {
      return match[1];
    }
  }
  return '';
}

function tailFile(path, lineCount) {
  if (!existsSync(path)) {
    return '';
  }
  const lines = readFileSync(path, 'utf8').split(/\r?\n/);
  return `${lines.slice(Math.max(0, lines.length - lineCount)).join('\n')}\n`;
}

const PMO_LOCK_RELATIVE_PATH = 'business-pmo/pmo-lock.json';
const PMO_BASELINE_RELATIVE_PATH = 'business-pmo/mango-baseline';
const PMO_SKILL_STATE_RELATIVE_PATH = '.agents/skills/.mango-pmo.json';
const PMO_RUNTIME_RELATIVE_PATH = '.mango/pmo';
const PMO_PROJECT_FILES_STATE_NAME = 'project-files.json';
const PROJECT_PR_TEMPLATE_RELATIVE_PATH = '.github/pull_request_template.md';
const PMO_GOVERNANCE_WORKFLOW_MARKER = '# Managed by Mango CLI. Use `mango pmo sync`; do not edit this file directly.';
const PMO_GOVERNANCE_WORKFLOW_PATHS = ['.github/workflows/pmo-doc-check.yml', '.gitea/workflows/pmo-doc-check.yml'];
const LEGACY_CANONICAL_GOVERNANCE_WORKFLOW_HASHES = new Map([
  [
    '.github/workflows/pmo-doc-check.yml',
    new Set(['8b2c7050883a124923d07a5562bbc004341346ab2aa8b2b9690b09ea3a0e64cb']),
  ],
  ['.gitea/workflows/pmo-doc-check.yml', new Set(['1e74d241b302b70a844f5365fecd2d9407c008740eb2e661f15ee1215a665c85'])],
]);
const DELIVERY_ASSURANCE_CONTRACT_ID = 'delivery-assurance';
const PROJECT_PR_TEMPLATE_MINIMUM_REVISION = 5;

function runPmoCommand(command, argv) {
  if (command === 'status') {
    const options = parsePmoArgs(argv);
    const targetDir = resolve(process.cwd(), options.projectDir);
    const status = getPmoStatus(targetDir, { locked: options.locked });
    printPmoStatus(status);
    return;
  }
  if (command === 'check') {
    const options = parsePmoArgs(argv);
    const targetDir = resolve(process.cwd(), options.projectDir);
    const status = getPmoStatus(targetDir, { locked: options.locked });
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
  if (command === 'rollback') {
    rollbackPmoBaseline(argv);
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
    mavenRepository: DEFAULT_MAVEN_REPOSITORY,
    modules: 'none',
  });
  const installedLock = readPmoLock(targetDir);
  const availableBaseline = loadPmoPackageBaseline();
  verifyPmoBundle(availableBaseline);
  const baseline =
    command === 'sync' && installedLock
      ? resolveLockedPmoBaseline(targetDir, installedLock, availableBaseline)
      : resolveUpgradePmoBaseline(availableBaseline, options.to);
  const historicalPmoVersionPlan =
    command === 'upgrade' ? planHistoricalPmoVersionCompatibility(targetDir, baseline) : [];
  const projectTemplatePlan = planPmoProjectTemplateSync(targetDir, baseline);
  const governanceWorkflowPlan = planPmoGovernanceWorkflowSync(targetDir, variables, options.adoptGovernance);
  const plan = [
    ...(command === 'upgrade' ? planReleaseTupleUpgrade(targetDir, baseline) : []),
    ...planPmoBaselineSync(targetDir, baseline),
    ...planPmoSkillSync(targetDir, baseline),
    ...historicalPmoVersionPlan,
    ...projectTemplatePlan,
    ...governanceWorkflowPlan,
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
  const managedProjectFileError = plan.find(
    (item) => ['project-pr-template', 'project-governance-workflow'].includes(item.scope) && item.action === 'warn',
  );
  if (managedProjectFileError) {
    if (managedProjectFileError.scope === 'project-pr-template') {
      fail(
        `${managedProjectFileError.reason}. Resolve ${managedProjectFileError.path} and rerun ` +
          `mango pmo ${command} --project-dir .`,
      );
    }
    fail(
      `${managedProjectFileError.reason}. Resolve ${managedProjectFileError.path} or explicitly accept Mango ownership with ` +
        `mango pmo ${command} --project-dir . --adopt-governance`,
    );
  }
  const snapshots = capturePmoUpgradeSnapshots(plan);
  let pmoInstallResult = null;
  let historicalPmoVersionResult;
  let releaseTupleWriteCount = 0;
  try {
    if (
      plan.some(
        (item) =>
          ['pmo-bundle', 'project-pr-template', 'project-governance-workflow'].includes(item.scope) &&
          !['skip', 'warn'].includes(item.action),
      )
    ) {
      pmoInstallResult = installPmoBundleAtomic(targetDir, baseline);
    }
    for (const item of plan) {
      if (
        item.scope === 'pmo-bundle' ||
        item.scope === 'historical-pmo-version' ||
        item.action === 'skip' ||
        item.action === 'warn'
      ) {
        continue;
      }
      if (item.action === 'delete') {
        rmSync(item.targetPath, { recursive: true, force: true });
      } else {
        writePlannedFile(item);
      }
      if (item.scope === 'release-tuple') {
        releaseTupleWriteCount += 1;
        injectReleaseTupleWriteFailure(releaseTupleWriteCount);
      }
    }
    historicalPmoVersionResult =
      command === 'upgrade' ? runHistoricalPmoVersionCompatibility(targetDir, baseline) : null;
    const status = getPmoStatus(targetDir, { locked: true });
    if (status.errors.length > 0 || status.warnings.length > 0) {
      throw new Error(`PMO baseline ${command} verification failed:\n${formatPmoStatusProblems(status)}`);
    }
    if (command === 'upgrade') {
      verifyReleaseTupleUpgrade(targetDir, baseline);
    }
  } catch (error) {
    const recoveryFailures = [];
    if (pmoInstallResult) {
      attemptPmoRecovery(
        () => restorePreviousPmoBundle(targetDir, pmoInstallResult),
        'restore previous PMO bundle',
        recoveryFailures,
      );
    }
    restorePmoUpgradeSnapshots(snapshots, recoveryFailures);
    const recovery =
      recoveryFailures.length === 0
        ? 'all managed project files restored byte-for-byte'
        : `recovery incomplete: ${recoveryFailures.join('; ')}`;
    throw new Error(`PMO baseline ${command} transaction failed (${recovery}): ${error.message}`, {
      cause: error,
    });
  }
  const synced = summary.add + summary.update;
  process.stdout.write(
    `PMO baseline ${command} complete: ${synced} files written, ${summary.delete || 0} removed, ${summary.skip} skipped.\n`,
  );
  process.stdout.write(
    `Project PMO skills are synchronized under .agents/skills; no user-level Codex plugin installation was performed.\n`,
  );
  if (historicalPmoVersionResult?.added.length > 0) {
    process.stdout.write(
      `Historical PMO version baseline locked ${historicalPmoVersionResult.added.length} lifecycle document(s).\n`,
    );
  }
}

function planReleaseTupleUpgrade(targetDir, baseline) {
  const tuple = resolveReleaseTuple(baseline);
  const configPath = join(targetDir, 'mango.config.json');
  if (!existsSync(configPath)) {
    process.stdout.write('Release tuple: skipped because this PMO consumer has no mango.config.json\n');
    return [];
  }
  const config = readJsonFile(configPath);
  const backendRoot = resolveManagedProjectPath(targetDir, config.paths?.backend || 'backend', 'paths.backend');
  const frontendRoot = resolveManagedProjectPath(targetDir, config.paths?.frontend || 'frontend', 'paths.frontend');
  const pomPath = join(backendRoot, 'pom.xml');
  const packagePath = join(frontendRoot, 'package.json');
  if (!existsSync(pomPath) || !existsSync(packagePath)) {
    fail(`release tuple requires ${relative(targetDir, pomPath)} and ${relative(targetDir, packagePath)}`);
  }

  const pom = readFileSync(pomPath, 'utf8');
  const mangoVersionMatches = [...pom.matchAll(/<mango\.version>\s*([^<\s]+)\s*<\/mango\.version>/g)];
  if (mangoVersionMatches.length !== 1) {
    fail(`release tuple requires exactly one <mango.version> in ${relative(targetDir, pomPath)}`);
  }
  const currentMavenVersion = mangoVersionMatches[0][1];
  if (config.mangoBackendVersion && config.mangoBackendVersion !== currentMavenVersion) {
    fail(
      `release tuple conflict: mango.config.json mangoBackendVersion=${config.mangoBackendVersion} ` +
        `but ${relative(targetDir, pomPath)} uses ${currentMavenVersion}`,
    );
  }

  const packageJson = readJsonFile(packagePath);
  const packageLocations = collectManagedMangoPackageLocations(packageJson);
  const cliLocation = packageLocations.find((entry) => entry.name === '@mango/cli');
  if (!cliLocation) {
    fail(`release tuple requires project-local @mango/cli in ${relative(targetDir, packagePath)}`);
  }
  const configuredFrontendVersions = config.mangoFrontendVersions || {};
  if (!isPlainObject(configuredFrontendVersions)) {
    fail('release tuple requires mango.config.json mangoFrontendVersions to be an object');
  }
  const managedPackageNames = new Set([
    ...Object.keys(configuredFrontendVersions),
    ...packageLocations.map((entry) => entry.name),
  ]);
  for (const name of managedPackageNames) {
    if (!name.startsWith('@mango/')) {
      fail(`release tuple found non-Mango package in mangoFrontendVersions: ${name}`);
    }
    if (!tuple.npm[name]) {
      fail(`release-versions.json is missing managed package ${name}`);
    }
  }
  for (const entry of packageLocations) {
    const configured = configuredFrontendVersions[entry.name];
    if (configured && configured !== entry.version) {
      fail(
        `release tuple conflict: mango.config.json ${entry.name}=${configured} ` +
          `but ${relative(targetDir, packagePath)} ${entry.section}.${entry.name}=${entry.version}`,
      );
    }
  }
  if (config.mangoCliVersion && config.mangoCliVersion !== cliLocation.version) {
    fail(
      `release tuple conflict: mango.config.json mangoCliVersion=${config.mangoCliVersion} ` +
        `but project-local @mango/cli=${cliLocation.version}`,
    );
  }
  const installedLock = readPmoLock(targetDir);
  const allowedConfiguredPmoVersions = new Set(
    [installedLock?.packageVersion, tuple.npm['@mango/pmo']].filter(Boolean),
  );
  if (
    config.mangoPmoVersion &&
    installedLock?.packageVersion &&
    !allowedConfiguredPmoVersions.has(config.mangoPmoVersion)
  ) {
    fail(
      `release tuple conflict: mango.config.json mangoPmoVersion=${config.mangoPmoVersion} ` +
        `matches neither installed pmo-lock.json packageVersion=${installedLock.packageVersion} ` +
        `nor upgrade target=${tuple.npm['@mango/pmo']}`,
    );
  }

  const nextConfig = structuredClone(config);
  nextConfig.mangoBackendVersion = tuple.maven;
  nextConfig.mangoCliVersion = tuple.npm['@mango/cli'];
  nextConfig.mangoPmoVersion = tuple.npm['@mango/pmo'];
  nextConfig.mangoFrontendVersions = { ...configuredFrontendVersions };
  for (const name of managedPackageNames) {
    if (name !== '@mango/cli' && name !== '@mango/pmo') {
      nextConfig.mangoFrontendVersions[name] = tuple.npm[name];
    }
  }

  const nextPackageJson = structuredClone(packageJson);
  for (const entry of packageLocations) {
    nextPackageJson[entry.section][entry.name] = tuple.npm[entry.name];
  }
  const nextPom = pom.replace(
    /<mango\.version>\s*[^<\s]+\s*<\/mango\.version>/,
    `<mango.version>${tuple.maven}</mango.version>`,
  );
  const plan = [
    createReleaseTupleFilePlan(
      targetDir,
      configPath,
      `${JSON.stringify(nextConfig, null, 2)}\n`,
      'Mango version tuple',
    ),
    createReleaseTupleFilePlan(targetDir, pomPath, nextPom, `Maven ${currentMavenVersion} -> ${tuple.maven}`),
    createReleaseTupleFilePlan(
      targetDir,
      packagePath,
      `${JSON.stringify(nextPackageJson, null, 2)}\n`,
      `${packageLocations.length} managed npm package(s)`,
    ),
  ];
  process.stdout.write(
    `Release tuple target: Maven ${tuple.maven}, CLI ${tuple.npm['@mango/cli']}, PMO ${tuple.npm['@mango/pmo']}, ` +
      `${managedPackageNames.size} managed npm package(s)\n`,
  );
  return plan;
}

function resolveReleaseTuple(baseline) {
  const mavenVersion = releaseVersions.maven?.mangoBackend;
  const cliVersion = releaseVersions.npm?.['@mango/cli'];
  const pmoVersion = releaseVersions.npm?.['@mango/pmo'];
  if (!mavenVersion || !cliVersion || !pmoVersion) {
    fail('release-versions.json must declare maven.mangoBackend, npm.@mango/cli, and npm.@mango/pmo');
  }
  if (baseline.manifest.packageVersion !== pmoVersion) {
    fail(
      `release tuple PMO ${pmoVersion} does not match selected @mango/pmo baseline ${baseline.manifest.packageVersion}`,
    );
  }
  return { maven: mavenVersion, npm: releaseVersions.npm };
}

function resolveManagedProjectPath(targetDir, configuredPath, field) {
  if (typeof configuredPath !== 'string' || !configuredPath.trim() || isAbsolute(configuredPath)) {
    fail(`release tuple requires mango.config.json ${field} to be a non-empty project-relative path`);
  }
  const resolvedPath = resolve(targetDir, configuredPath);
  const relativePath = relative(targetDir, resolvedPath);
  if (
    relativePath === '..' ||
    relativePath.startsWith('../') ||
    relativePath.startsWith('..\\') ||
    isAbsolute(relativePath)
  ) {
    fail(`release tuple requires mango.config.json ${field} to stay inside the project directory`);
  }
  return resolvedPath;
}

function collectManagedMangoPackageLocations(packageJson) {
  const result = [];
  for (const section of ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies']) {
    const dependencies = packageJson[section];
    if (dependencies === undefined) continue;
    if (!isPlainObject(dependencies)) {
      fail(`release tuple requires frontend/package.json ${section} to be an object`);
    }
    for (const [name, version] of Object.entries(dependencies)) {
      if (name.startsWith('@mango/')) {
        result.push({ section, name, version });
      }
    }
  }
  return result;
}

function createReleaseTupleFilePlan(targetDir, targetPath, content, reason) {
  const current = readFileSync(targetPath, 'utf8');
  return {
    action: current === content ? 'skip' : 'update',
    path: toPosix(relative(targetDir, targetPath)),
    targetPath,
    content,
    reason,
    scope: 'release-tuple',
  };
}

function verifyReleaseTupleUpgrade(targetDir, baseline) {
  if (!existsSync(join(targetDir, 'mango.config.json'))) return;
  const tuple = resolveReleaseTuple(baseline);
  const config = readJsonFile(join(targetDir, 'mango.config.json'));
  const backendRoot = resolveManagedProjectPath(targetDir, config.paths?.backend || 'backend', 'paths.backend');
  const frontendRoot = resolveManagedProjectPath(targetDir, config.paths?.frontend || 'frontend', 'paths.frontend');
  const pomVersion = readMangoVersionFromPom(backendRoot);
  const packageJson = readJsonFile(join(frontendRoot, 'package.json'));
  const failures = [];
  if (config.mangoBackendVersion !== tuple.maven || pomVersion !== tuple.maven) {
    failures.push(`Maven expected ${tuple.maven}, config=${config.mangoBackendVersion}, pom=${pomVersion}`);
  }
  if (config.mangoCliVersion !== tuple.npm['@mango/cli']) failures.push('mango.config.json CLI version mismatch');
  if (config.mangoPmoVersion !== tuple.npm['@mango/pmo']) failures.push('mango.config.json PMO version mismatch');
  for (const entry of collectManagedMangoPackageLocations(packageJson)) {
    if (entry.version !== tuple.npm[entry.name]) {
      failures.push(`${entry.section}.${entry.name} expected ${tuple.npm[entry.name]}, got ${entry.version}`);
    }
  }
  const lock = readPmoLock(targetDir);
  if (lock?.packageVersion !== tuple.npm['@mango/pmo']) {
    failures.push(`pmo-lock.json expected ${tuple.npm['@mango/pmo']}, got ${lock?.packageVersion || '<missing>'}`);
  }
  if (failures.length > 0) throw new Error(`release tuple verification failed:\n${failures.join('\n')}`);
}

function capturePmoUpgradeSnapshots(plan) {
  const snapshots = [];
  const seen = new Set();
  for (const item of plan) {
    if (!item.targetPath || item.scope === 'pmo-bundle' || item.scope === 'historical-pmo-version') continue;
    if (seen.has(item.targetPath)) continue;
    seen.add(item.targetPath);
    if (!existsSync(item.targetPath)) {
      snapshots.push({ path: item.targetPath, existed: false });
      continue;
    }
    if (!lstatSync(item.targetPath).isFile()) {
      throw new Error(`cannot snapshot non-file PMO upgrade target: ${item.targetPath}`);
    }
    snapshots.push({
      path: item.targetPath,
      existed: true,
      content: readFileSync(item.targetPath),
      mode: statSync(item.targetPath).mode & 0o777,
    });
  }
  return snapshots;
}

function restorePmoUpgradeSnapshots(snapshots, failures) {
  for (const snapshot of snapshots) {
    attemptPmoRecovery(
      () => {
        if (!snapshot.existed) {
          rmSync(snapshot.path, { recursive: true, force: true });
          return;
        }
        mkdirSync(dirname(snapshot.path), { recursive: true });
        writeFileSync(snapshot.path, snapshot.content);
        chmodSync(snapshot.path, snapshot.mode);
        if (Buffer.compare(readFileSync(snapshot.path), snapshot.content) !== 0) {
          throw new Error('byte verification failed');
        }
      },
      `restore ${snapshot.path}`,
      failures,
    );
  }
}

function injectReleaseTupleWriteFailure(writeCount) {
  const configured = process.env.MANGO_CLI_TEST_FAIL_RELEASE_TUPLE_AFTER_WRITES;
  if (!configured) return;
  const expected = Number.parseInt(configured, 10);
  if (!Number.isSafeInteger(expected) || expected < 1) {
    throw new Error('invalid MANGO_CLI_TEST_FAIL_RELEASE_TUPLE_AFTER_WRITES test hook');
  }
  if (writeCount === expected) throw new Error(`injected release tuple write failure after ${writeCount} write(s)`);
}

function planHistoricalPmoVersionCompatibility(targetDir, baseline) {
  const result = runHistoricalPmoVersionCompatibility(targetDir, baseline, { dryRun: true });
  if (result.added.length === 0) return [];
  return [
    {
      action: existsSync(result.baselinePath) ? 'update' : 'add',
      path: relative(targetDir, result.baselinePath).split('\\').join('/'),
      targetPath: result.baselinePath,
      reason: `lock ${result.added.length} historical lifecycle document(s) created with prior supported PMO versions`,
      scope: 'historical-pmo-version',
    },
  ];
}

function runHistoricalPmoVersionCompatibility(targetDir, baseline, { dryRun = false } = {}) {
  const toolPath = join(baseline.root, 'tools/pin-historical-pmo-version-documents.mjs');
  if (!existsSync(toolPath)) {
    fail(`PMO bundle is missing historical pmoVersion compatibility tool: ${toolPath}`);
  }
  const businessDocsRoot = resolveBusinessDocsRoot(targetDir);
  const args = [toolPath, '--root', businessDocsRoot, '--json'];
  if (dryRun) args.push('--dry-run');
  const result = spawnSync(process.execPath, args, { encoding: 'utf8' });
  if (result.status !== 0) {
    fail(
      `historical pmoVersion compatibility ${dryRun ? 'plan' : 'pin'} failed:\n` +
        `${result.stdout}\n${result.stderr}`.trim(),
    );
  }
  try {
    const parsed = JSON.parse(result.stdout);
    if (!Array.isArray(parsed.added) || typeof parsed.baselinePath !== 'string') {
      throw new Error('tool output is missing added or baselinePath');
    }
    return parsed;
  } catch (error) {
    fail(`historical pmoVersion compatibility returned invalid JSON: ${error.message}`);
  }
}

function resolveBusinessDocsRoot(targetDir) {
  const config = readOptionalJson(join(targetDir, 'mango.config.json'));
  const configuredPath = config?.paths?.businessDocs ?? 'business-docs';
  if (typeof configuredPath !== 'string' || !configuredPath.trim() || isAbsolute(configuredPath)) {
    fail('mango.config.json paths.businessDocs must be a non-empty project-relative path');
  }
  const resolvedPath = resolve(targetDir, configuredPath);
  const relativePath = relative(targetDir, resolvedPath);
  if (
    relativePath === '..' ||
    relativePath.startsWith('../') ||
    relativePath.startsWith('..\\') ||
    isAbsolute(relativePath)
  ) {
    fail('mango.config.json paths.businessDocs must stay inside the project directory');
  }
  return resolvedPath;
}

function parsePmoArgs(argv) {
  const result = {
    projectDir: '.',
    dryRun: false,
    writeAgents: false,
    syncShell: false,
    locked: false,
    to: '',
    adoptGovernance: false,
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
    if (arg === '--adopt-governance') {
      result.adoptGovernance = true;
      continue;
    }
    if (arg === '--locked') {
      result.locked = true;
      continue;
    }
    if (arg.startsWith('--to=')) {
      result.to = arg.slice('--to='.length);
      if (!result.to) {
        fail('missing value for --to');
      }
      continue;
    }
    if (arg === '--to') {
      const next = argv[index + 1];
      if (!next || next.startsWith('--')) {
        fail('missing value for --to');
      }
      result.to = next;
      index += 1;
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
  fail(
    '@mango/pmo baseline package is missing. Install @mango/pmo or rebuild the Mango CLI workspace before running mango pmo commands.',
  );
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

function planPmoBaselineSync(targetDir, baseline) {
  const plan = [];
  for (const file of baseline.manifest.files || []) {
    const targetRelative = `business-pmo/mango-baseline/${file.path}`;
    const sourceFile = join(baseline.root, file.path);
    plan.push({
      ...buildFilePlanItem(targetRelative, join(targetDir, targetRelative), readRenderedBaselineFile(sourceFile)),
      scope: 'pmo-bundle',
    });
  }
  plan.push({
    ...buildFilePlanItem(
      'business-pmo/mango-baseline/baseline.json',
      join(targetDir, 'business-pmo/mango-baseline/baseline.json'),
      `${JSON.stringify(baseline.manifest, null, 2)}\n`,
    ),
    scope: 'pmo-bundle',
  });
  plan.push({
    ...buildFilePlanItem(
      PMO_LOCK_RELATIVE_PATH,
      join(targetDir, PMO_LOCK_RELATIVE_PATH),
      `${JSON.stringify(createPmoLock(baseline.manifest), null, 2)}\n`,
    ),
    scope: 'pmo-bundle',
  });

  const expected = new Set([...(baseline.manifest.files || []).map((file) => file.path), 'baseline.json']);
  const installedRoot = join(targetDir, PMO_BASELINE_RELATIVE_PATH);
  if (existsSync(installedRoot)) {
    for (const file of walkPmoFiles(installedRoot)) {
      const relativePath = toPosix(relative(installedRoot, file));
      if (!expected.has(relativePath)) {
        plan.push({
          action: 'delete',
          path: `${PMO_BASELINE_RELATIVE_PATH}/${relativePath}`,
          targetPath: file,
          scope: 'pmo-bundle',
        });
      }
    }
  }
  return plan;
}

function resolvePmoProjectTemplate(baseline) {
  const descriptor = (baseline.manifest.contracts || []).find(
    (contract) => contract.contractId === DELIVERY_ASSURANCE_CONTRACT_ID,
  );
  if (!descriptor || descriptor.schemaRevision < PROJECT_PR_TEMPLATE_MINIMUM_REVISION) {
    return null;
  }
  const contractPath = descriptor.path;
  if (!isSafePmoPath(contractPath)) {
    throw new Error(`unsafe delivery-assurance contract path: ${contractPath}`);
  }
  const contractFile = join(baseline.root, contractPath);
  if (!existsSync(contractFile)) {
    throw new Error(`delivery-assurance contract file is missing: ${contractPath}`);
  }
  const contract = JSON.parse(readFileSync(contractFile, 'utf8'));
  if (
    contract.contractId !== DELIVERY_ASSURANCE_CONTRACT_ID ||
    contract.schemaRevision !== descriptor.schemaRevision ||
    !contract.pullRequestBody?.templatePath
  ) {
    throw new Error('delivery-assurance contract metadata differs from the PMO manifest');
  }
  const templatePath = contract.pullRequestBody.templatePath;
  if (!isSafePmoPath(templatePath) || !templatePath.startsWith('templates/')) {
    throw new Error(`unsafe delivery-assurance PR template path: ${templatePath}`);
  }
  const sourceFile = join(baseline.root, templatePath);
  if (!existsSync(sourceFile)) {
    throw new Error(`delivery-assurance PR template is missing: ${templatePath}`);
  }
  return {
    content: readFileSync(sourceFile, 'utf8'),
    schemaRevision: descriptor.schemaRevision,
    templatePath,
  };
}

function planPmoProjectTemplateSync(targetDir, baseline) {
  const canonical = resolvePmoProjectTemplate(baseline);
  if (!canonical) return [];
  const targetPath = join(targetDir, PROJECT_PR_TEMPLATE_RELATIVE_PATH);
  const targetExists = existsSync(targetPath);
  const currentContent = targetExists ? readFileSync(targetPath, 'utf8') : '';
  const result = synchronizeProjectPullRequestTemplate(currentContent, canonical.content, { targetExists });
  if (result.action === 'error') {
    return [
      {
        action: 'warn',
        reason: result.reason,
        path: PROJECT_PR_TEMPLATE_RELATIVE_PATH,
        targetPath,
        scope: 'project-pr-template',
      },
    ];
  }
  return [
    {
      action: result.action,
      reason:
        result.action === 'skip' ? `delivery-assurance schema revision ${canonical.schemaRevision} is current` : '',
      path: PROJECT_PR_TEMPLATE_RELATIVE_PATH,
      targetPath,
      content: result.content,
      scope: 'project-pr-template',
    },
  ];
}

function planPmoGovernanceWorkflowSync(targetDir, variables, adoptGovernance = false) {
  return PMO_GOVERNANCE_WORKFLOW_PATHS.map((workflowPath) => {
    const targetPath = join(targetDir, workflowPath);
    const canonical = readRenderedTemplateFile(join(templateRoot, workflowPath), variables);
    if (!existsSync(targetPath)) {
      return {
        ...buildFilePlanItem(workflowPath, targetPath, canonical),
        scope: 'project-governance-workflow',
      };
    }
    const current = readFileSync(targetPath);
    const next = Buffer.from(canonical, 'utf8');
    if (Buffer.compare(current, next) === 0) {
      return {
        action: 'skip',
        reason: 'managed governance workflow is current',
        path: workflowPath,
        targetPath,
        scope: 'project-governance-workflow',
      };
    }
    const currentText = current.toString('utf8');
    const currentHash = createHash('sha256').update(currentText.replaceAll('\r\n', '\n')).digest('hex');
    const legacyCanonical = LEGACY_CANONICAL_GOVERNANCE_WORKFLOW_HASHES.get(workflowPath)?.has(currentHash) === true;
    const hasManagedMarker =
      currentText.startsWith(`${PMO_GOVERNANCE_WORKFLOW_MARKER}\n`) ||
      currentText.startsWith(`${PMO_GOVERNANCE_WORKFLOW_MARKER}\r\n`);
    if (hasManagedMarker || legacyCanonical || adoptGovernance) {
      return {
        action: 'update',
        reason:
          adoptGovernance && !legacyCanonical && !hasManagedMarker
            ? 'explicitly adopting existing workflow under Mango ownership'
            : legacyCanonical
              ? 'upgrading a verified legacy Mango workflow'
              : 'upgrading Mango-managed governance workflow',
        path: workflowPath,
        targetPath,
        content: canonical,
        scope: 'project-governance-workflow',
      };
    }
    return {
      action: 'warn',
      reason: 'existing workflow is business-owned or has unrecognized edits and will not be overwritten',
      path: workflowPath,
      targetPath,
      scope: 'project-governance-workflow',
    };
  });
}

function readRenderedBaselineFile(sourceFile) {
  return readFileSync(sourceFile);
}

function installPmoBaseline(targetDir) {
  const baseline = loadPmoPackageBaseline();
  verifyPmoBundle(baseline);
  installPmoBundleAtomic(targetDir, baseline);
  const templatePlan = planPmoProjectTemplateSync(targetDir, baseline);
  const failure = templatePlan.find((item) => item.action === 'warn');
  if (failure) fail(failure.reason);
  for (const item of templatePlan.filter((entry) => ['add', 'update'].includes(entry.action))) {
    writePlannedFile(item);
  }
}

function getPmoStatus(targetDir, { locked = false, checkGovernanceWorkflows = true } = {}) {
  const baselineDir = join(targetDir, PMO_BASELINE_RELATIVE_PATH);
  const errors = [];
  const warnings = [];
  if (!existsSync(targetDir) || !statSync(targetDir).isDirectory()) {
    errors.push(`project directory not found: ${targetDir}`);
    return {
      targetDir,
      baseline: emptyPmoBaseline(baselineDir),
      baselineDir,
      errors,
      warnings,
      missing: [],
      changed: [],
      extra: [],
      skillMissing: [],
      skillChanged: [],
      skillExtra: [],
      locked,
    };
  }
  if (!existsSync(baselineDir)) {
    errors.push('business-pmo/mango-baseline is missing. Run mango pmo sync --project-dir .');
    return {
      targetDir,
      baseline: emptyPmoBaseline(baselineDir),
      baselineDir,
      errors,
      warnings,
      missing: [],
      changed: [],
      extra: [],
      skillMissing: [],
      skillChanged: [],
      skillExtra: [],
      locked,
    };
  }

  const lock = readPmoLock(targetDir, { strict: false });
  let installedBaseline;
  try {
    installedBaseline = loadInstalledPmoBaseline(targetDir);
  } catch (error) {
    errors.push(error.message);
  }
  if (!installedBaseline) {
    errors.push('baseline.json is missing. Run mango pmo sync --project-dir . to install a versioned manifest.');
  }

  let baseline = installedBaseline || emptyPmoBaseline(baselineDir);
  if (locked) {
    if (!lock) {
      errors.push(`${PMO_LOCK_RELATIVE_PATH} is missing; run mango pmo upgrade --project-dir .`);
    } else if (installedBaseline) {
      checkPmoLockMatchesManifest(lock, installedBaseline.manifest, errors);
    }
  } else {
    try {
      baseline = loadPmoPackageBaseline();
      verifyPmoBundle(baseline);
      if (installedBaseline && !samePmoBundle(installedBaseline.manifest, baseline.manifest)) {
        warnings.push(
          `installed PMO ${formatPmoIdentity(installedBaseline.manifest)} differs from available ${formatPmoIdentity(baseline.manifest)}`,
        );
      }
      if (lock && !samePmoBundle(lock, baseline.manifest)) {
        warnings.push(`project PMO lock differs from available ${formatPmoIdentity(baseline.manifest)}`);
      }
    } catch (error) {
      errors.push(error.message);
    }
  }

  const comparison = comparePmoBaselineFiles(baselineDir, baseline.manifest);
  if (comparison.missing.length > 0) {
    errors.push(`${comparison.missing.length} baseline files are missing`);
  }
  if (comparison.changed.length > 0) {
    warnings.push(
      `${comparison.changed.length} baseline files differ from ${baseline.manifest.packageName}@${baseline.manifest.packageVersion}`,
    );
  }
  if (comparison.extra.length > 0) {
    errors.push(`${comparison.extra.length} stale baseline files are not declared by the PMO manifest`);
  }
  const skillComparison = comparePmoSkillProjection(targetDir, baseline.manifest);
  if (skillComparison.missing.length > 0) {
    errors.push(`${skillComparison.missing.length} project PMO skill files are missing`);
  }
  if (skillComparison.changed.length > 0) {
    warnings.push(`${skillComparison.changed.length} project PMO skill files differ from the locked bundle`);
  }
  if (skillComparison.extra.length > 0) {
    errors.push(`${skillComparison.extra.length} stale project PMO skill files remain installed`);
  }
  let projectTemplateContract = null;
  try {
    projectTemplateContract = resolvePmoProjectTemplate(baseline);
    if (projectTemplateContract) {
      const targetPath = join(targetDir, PROJECT_PR_TEMPLATE_RELATIVE_PATH);
      const targetExists = existsSync(targetPath);
      const inspection = inspectProjectPullRequestTemplate(
        targetExists ? readFileSync(targetPath, 'utf8') : '',
        projectTemplateContract.content,
        { targetExists },
      );
      for (const problem of inspection.errors) {
        errors.push(
          `${PROJECT_PR_TEMPLATE_RELATIVE_PATH}: ${problem} for ${formatPmoIdentity(baseline.manifest)}. ` +
            'Run mango pmo sync --project-dir .',
        );
      }
    }
  } catch (error) {
    errors.push(error.message);
  }
  const governanceWorkflows = [];
  try {
    const variables = buildVariables({
      project: basename(targetDir),
      preset: 'custom',
      topology: 'monolith',
      packageName: 'com.example.mango',
      groupId: 'com.example.mango',
      version: '1.0.0-SNAPSHOT',
      mangoVersion: defaultVersions.mangoBackend,
      npmRegistry: 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/',
      mavenRepository: DEFAULT_MAVEN_REPOSITORY,
      modules: 'none',
    });
    for (const item of planPmoGovernanceWorkflowSync(targetDir, variables, false)) {
      governanceWorkflows.push(item);
      if (checkGovernanceWorkflows && item.action !== 'skip') {
        errors.push(
          `${item.path}: ${item.reason || 'managed governance workflow is missing or stale'}. ` +
            'Run mango pmo sync --project-dir .',
        );
      }
    }
  } catch (error) {
    errors.push(error.message);
  }
  return {
    targetDir,
    baseline,
    baselineDir,
    errors,
    warnings,
    ...comparison,
    skillMissing: skillComparison.missing,
    skillChanged: skillComparison.changed,
    skillExtra: skillComparison.extra,
    skillExpectedFiles: skillComparison.expectedFiles,
    skillExpectedRoots: skillComparison.expectedRoots,
    projectTemplateContract,
    governanceWorkflows,
    lock,
    locked,
  };
}

function comparePmoBaselineFiles(baselineDir, manifest) {
  const expected = new Map((manifest.files || []).map((file) => [file.path, file]));
  const missing = [];
  const changed = [];
  for (const [filePath, file] of expected.entries()) {
    const targetPath = join(baselineDir, filePath);
    if (!existsSync(targetPath)) {
      missing.push(filePath);
      continue;
    }
    const content = readFileSync(targetPath);
    const actualHash = createHash('sha256').update(content).digest('hex');
    const actualMode = process.platform === 'win32' ? file.mode : statSync(targetPath).mode & 0o111 ? '0755' : '0644';
    if (actualHash !== file.sha256 || content.length !== file.size || actualMode !== file.mode) {
      changed.push(filePath);
    }
  }
  const expectedPaths = new Set([...expected.keys(), 'baseline.json']);
  const extra = walkPmoFiles(baselineDir)
    .map((file) => toPosix(relative(baselineDir, file)))
    .filter((file) => !expectedPaths.has(file));
  return { missing, changed, extra };
}

function printPmoStatus(status) {
  process.stdout.write(`Project: ${status.targetDir}\n`);
  process.stdout.write(`Mode: ${status.locked ? 'locked project bundle' : 'available package'}\n`);
  process.stdout.write(`Baseline: ${formatPmoIdentity(status.baseline.manifest)}\n`);
  if (status.lock) {
    process.stdout.write(`Lock: ${formatPmoIdentity(status.lock)}\n`);
  }
  process.stdout.write(
    `Files: ${status.baseline.manifest.files?.length || 0} expected, ${status.missing.length} missing, ${status.changed.length} changed, ${status.extra.length} extra\n`,
  );
  process.stdout.write(
    `Governance workflows: ${status.governanceWorkflows?.filter((item) => item.action === 'skip').length || 0}/` +
      `${PMO_GOVERNANCE_WORKFLOW_PATHS.length} current\n`,
  );
  process.stdout.write(
    `Skills: ${status.skillExpectedRoots || 0} roots, ${status.skillExpectedFiles || 0} expected, ` +
      `${status.skillMissing.length} missing, ${status.skillChanged.length} changed, ${status.skillExtra.length} extra\n`,
  );
  process.stdout.write(
    'Codex plugin: project skills checked; user-level plugin installation is not managed by this command.\n',
  );
  process.stdout.write(
    status.projectTemplateContract
      ? `PR template: delivery-assurance schema revision ${status.projectTemplateContract.schemaRevision}\n`
      : 'PR template: not managed by this historical PMO contract\n',
  );
  for (const warning of status.warnings) {
    process.stdout.write(`warn    ${warning}\n`);
  }
  for (const error of status.errors) {
    process.stdout.write(`error   ${error}\n`);
  }
  if (status.missing.length > 0) {
    process.stdout.write(
      `Missing: ${status.missing.slice(0, 10).join(', ')}${status.missing.length > 10 ? ', ...' : ''}\n`,
    );
  }
  if (status.changed.length > 0) {
    process.stdout.write(
      `Changed: ${status.changed.slice(0, 10).join(', ')}${status.changed.length > 10 ? ', ...' : ''}\n`,
    );
  }
  if (status.errors.length === 0 && status.warnings.length === 0) {
    process.stdout.write('PMO baseline is current.\n');
  }
}

function resolveUpgradePmoBaseline(availableBaseline, requestedVersion) {
  if (requestedVersion && availableBaseline.manifest.packageVersion !== requestedVersion) {
    fail(
      `@mango/pmo@${requestedVersion} is not available to this CLI. ` +
        `The resolved package is ${formatPmoIdentity(availableBaseline.manifest)}; run the project-local CLI that locks the requested PMO package.`,
    );
  }
  return availableBaseline;
}

function resolveLockedPmoBaseline(targetDir, lock, availableBaseline) {
  if (samePmoBundle(lock, availableBaseline.manifest)) {
    return availableBaseline;
  }
  for (const backup of listPmoBackups(targetDir)) {
    if (samePmoBundle(lock, backup.manifest)) {
      verifyPmoBundle(backup);
      return backup;
    }
  }
  fail(
    `mango pmo sync repairs the locked ${formatPmoIdentity(lock)}, but this CLI resolved ` +
      `${formatPmoIdentity(availableBaseline.manifest)} and no matching local backup exists. ` +
      `Use a project-local CLI with the locked @mango/pmo dependency or run mango pmo upgrade --to ${availableBaseline.manifest.packageVersion}.`,
  );
}

function createPmoLock(manifest) {
  return {
    schemaVersion: 1,
    packageName: manifest.packageName,
    packageVersion: manifest.packageVersion,
    bundleSha256: manifest.bundleSha256,
    sourceCommit: manifest.sourceCommit,
    manifestSchemaVersion: manifest.schemaVersion,
    contracts: (manifest.contracts || []).map((contract) => ({
      contractId: contract.contractId,
      schemaRevision: contract.schemaRevision,
    })),
  };
}

function readPmoLock(targetDir, { strict = true } = {}) {
  const path = join(targetDir, PMO_LOCK_RELATIVE_PATH);
  if (!existsSync(path)) {
    return null;
  }
  try {
    const lock = JSON.parse(readFileSync(path, 'utf8'));
    if (
      lock.schemaVersion !== 1 ||
      lock.packageName !== '@mango/pmo' ||
      typeof lock.packageVersion !== 'string' ||
      !isSha256(lock.bundleSha256) ||
      !Array.isArray(lock.contracts)
    ) {
      throw new Error(`invalid PMO project lock: ${PMO_LOCK_RELATIVE_PATH}`);
    }
    return lock;
  } catch (error) {
    if (strict) {
      throw error;
    }
    return null;
  }
}

function loadInstalledPmoBaseline(targetDir) {
  const root = join(targetDir, PMO_BASELINE_RELATIVE_PATH);
  const manifestPath = join(root, 'baseline.json');
  if (!existsSync(manifestPath)) {
    return null;
  }
  let manifest;
  try {
    manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  } catch (error) {
    throw new Error(`cannot parse installed PMO manifest: ${error.message}`, { cause: error });
  }
  validatePmoManifest(manifest);
  return { root, manifest };
}

function verifyPmoBundle(baseline) {
  validatePmoManifest(baseline.manifest);
  const comparison = comparePmoBaselineFiles(baseline.root, baseline.manifest);
  if (comparison.missing.length > 0 || comparison.changed.length > 0 || comparison.extra.length > 0) {
    throw new Error(
      `invalid ${formatPmoIdentity(baseline.manifest)} package bundle; ` +
        `missing=${comparison.missing.join(',') || '-'} changed=${comparison.changed.join(',') || '-'} extra=${comparison.extra.join(',') || '-'}`,
    );
  }
}

function validatePmoManifest(manifest) {
  if (manifest.packageName !== '@mango/pmo' || typeof manifest.packageVersion !== 'string') {
    throw new Error('invalid @mango/pmo manifest package identity');
  }
  if (manifest.schemaVersion !== 2) {
    throw new Error(`unsupported @mango/pmo manifest schemaVersion: ${manifest.schemaVersion}`);
  }
  if (typeof manifest.sourceCommit !== 'string' || !/^[0-9a-f]{40}$/i.test(manifest.sourceCommit)) {
    throw new Error('invalid @mango/pmo manifest sourceCommit');
  }
  if (!isSha256(manifest.bundleSha256) || !Array.isArray(manifest.files) || !Array.isArray(manifest.contracts)) {
    throw new Error('invalid @mango/pmo bundle manifest');
  }
  if (manifest.plugin !== null && manifest.plugin !== undefined) {
    if (
      manifest.plugin.path !== 'package-root' ||
      !isSha256(manifest.plugin.sha256) ||
      !Array.isArray(manifest.plugin.files)
    ) {
      throw new Error('invalid @mango/pmo plugin projection descriptor');
    }
    const pluginPaths = new Set();
    for (const file of manifest.plugin.files) {
      if (
        !isSafePmoPath(file.path) ||
        pluginPaths.has(file.path) ||
        !isSha256(file.sha256) ||
        !Number.isInteger(file.size) ||
        !['0644', '0755'].includes(file.mode) ||
        file.kind !== 'plugin'
      ) {
        throw new Error(`invalid @mango/pmo plugin projection file: ${file.path}`);
      }
      pluginPaths.add(file.path);
    }
    const pluginSha = createHash('sha256').update(JSON.stringify(manifest.plugin.files)).digest('hex');
    if (pluginSha !== manifest.plugin.sha256) {
      throw new Error('@mango/pmo plugin projection hash mismatch');
    }
  }

  const paths = new Set();
  const filesByPath = new Map();
  for (const file of manifest.files) {
    if (!isSafePmoPath(file.path) || paths.has(file.path)) {
      throw new Error(`invalid or duplicate @mango/pmo manifest path: ${file.path}`);
    }
    paths.add(file.path);
    filesByPath.set(file.path, file);
    if (
      !isSha256(file.sha256) ||
      !Number.isInteger(file.size) ||
      file.size < 0 ||
      !['0644', '0755'].includes(file.mode) ||
      !['agent', 'rule', 'template', 'contract', 'tool', 'skill', 'documentation', 'asset'].includes(file.kind)
    ) {
      throw new Error(`invalid @mango/pmo manifest file descriptor: ${file.path}`);
    }
  }
  const contractIds = new Set();
  for (const contract of manifest.contracts) {
    if (
      !contract.contractId ||
      contractIds.has(contract.contractId) ||
      !Number.isInteger(contract.schemaRevision) ||
      contract.schemaRevision < 1 ||
      filesByPath.get(contract.path)?.kind !== 'contract'
    ) {
      throw new Error(`invalid @mango/pmo contract descriptor: ${contract.contractId || '<missing>'}`);
    }
    contractIds.add(contract.contractId);
  }
  const actualBundleSha = createHash('sha256')
    .update(JSON.stringify({ files: manifest.files, contracts: manifest.contracts, plugin: manifest.plugin ?? null }))
    .digest('hex');
  if (actualBundleSha !== manifest.bundleSha256) {
    throw new Error(`@mango/pmo bundle hash mismatch: ${manifest.bundleSha256}`);
  }
}

function checkPmoLockMatchesManifest(lock, manifest, failures) {
  if (!samePmoBundle(lock, manifest)) {
    failures.push(
      `project PMO lock ${formatPmoIdentity(lock)} does not match installed ${formatPmoIdentity(manifest)}`,
    );
  }
  const expectedContracts = JSON.stringify(createPmoLock(manifest).contracts);
  if (JSON.stringify(lock.contracts) !== expectedContracts) {
    failures.push('project PMO lock contract revisions do not match the installed manifest');
  }
  if (lock.sourceCommit !== manifest.sourceCommit || lock.manifestSchemaVersion !== manifest.schemaVersion) {
    failures.push('project PMO lock source or manifest schema does not match the installed manifest');
  }
}

function samePmoBundle(left, right) {
  return (
    left?.packageName === right?.packageName &&
    left?.packageVersion === right?.packageVersion &&
    left?.bundleSha256 === right?.bundleSha256
  );
}

function emptyPmoBaseline(root) {
  return {
    root,
    manifest: {
      packageName: '@mango/pmo',
      packageVersion: '<missing>',
      bundleSha256: '',
      files: [],
      contracts: [],
    },
  };
}

function formatPmoIdentity(value) {
  const hash = value?.bundleSha256 ? `#${value.bundleSha256.slice(0, 12)}` : '';
  return `${value?.packageName || '@mango/pmo'}@${value?.packageVersion || '<missing>'}${hash}`;
}

function formatPmoStatusProblems(status) {
  return [...status.errors.map((value) => `error ${value}`), ...status.warnings.map((value) => `warn ${value}`)].join(
    '\n',
  );
}

function isSha256(value) {
  return typeof value === 'string' && /^[0-9a-f]{64}$/i.test(value);
}

function isSafePmoPath(path) {
  return (
    typeof path === 'string' &&
    path.length > 0 &&
    !path.startsWith('/') &&
    !path.includes('\\') &&
    path.split('/').every((segment) => segment && segment !== '.' && segment !== '..')
  );
}

function planPmoSkillSync(targetDir, baseline) {
  const plan = [];
  const projection = createPmoSkillProjection(baseline.manifest);
  for (const file of projection.files) {
    const sourceFile = join(baseline.root, `skills/${file.path}`);
    const targetRelative = `.agents/skills/${file.path}`;
    plan.push({
      ...buildFilePlanItem(targetRelative, join(targetDir, targetRelative), readFileSync(sourceFile)),
      scope: 'pmo-bundle',
    });
  }
  plan.push({
    ...buildFilePlanItem(
      PMO_SKILL_STATE_RELATIVE_PATH,
      join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH),
      `${JSON.stringify(projection, null, 2)}\n`,
    ),
    scope: 'pmo-bundle',
  });

  const currentState = resolvePreviousPmoSkillOwnership(targetDir, { strict: false });
  const expectedFiles = new Set(projection.files.map((file) => file.path));
  for (const root of currentState?.roots || []) {
    const rootPath = join(targetDir, '.agents/skills', root);
    if (!existsSync(rootPath)) {
      continue;
    }
    for (const file of walkPmoFiles(rootPath)) {
      const relativePath = toPosix(relative(join(targetDir, '.agents/skills'), file));
      if (!expectedFiles.has(relativePath)) {
        plan.push({
          action: 'delete',
          path: `.agents/skills/${relativePath}`,
          targetPath: file,
          scope: 'pmo-bundle',
        });
      }
    }
  }
  return plan;
}

function createPmoSkillProjection(manifest) {
  const files = (manifest.files || [])
    .filter((file) => file.kind === 'skill' && file.path.startsWith('skills/'))
    .map((file) => ({
      path: file.path.slice('skills/'.length),
      sha256: file.sha256,
      size: file.size,
      mode: file.mode,
    }))
    .sort((left, right) => left.path.localeCompare(right.path));
  for (const file of files) {
    if (!isSafePmoPath(file.path) || !file.path.includes('/')) {
      throw new Error(`invalid project PMO skill projection path: ${file.path}`);
    }
  }
  const roots = [...new Set(files.map((file) => file.path.split('/')[0]))].sort();
  return {
    schemaVersion: 1,
    packageName: manifest.packageName,
    packageVersion: manifest.packageVersion,
    bundleSha256: manifest.bundleSha256,
    roots,
    files,
  };
}

function readPmoSkillState(targetDir, { strict = true } = {}) {
  const path = join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH);
  if (!existsSync(path)) {
    return null;
  }
  try {
    const state = JSON.parse(readFileSync(path, 'utf8'));
    if (
      state.schemaVersion !== 1 ||
      state.packageName !== '@mango/pmo' ||
      !Array.isArray(state.roots) ||
      !Array.isArray(state.files) ||
      !isSha256(state.bundleSha256)
    ) {
      throw new Error(`invalid project PMO skill state: ${PMO_SKILL_STATE_RELATIVE_PATH}`);
    }
    for (const root of state.roots) {
      if (!isSafePmoPath(root) || root.includes('/')) {
        throw new Error(`invalid managed PMO skill root: ${root}`);
      }
    }
    return state;
  } catch (error) {
    if (strict) {
      throw error;
    }
    return null;
  }
}

function resolvePreviousPmoSkillOwnership(targetDir, { strict = true, trustedManifest = null } = {}) {
  const lockExists = existsSync(join(targetDir, PMO_LOCK_RELATIVE_PATH));
  const stateExists = existsSync(join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH));
  if (!lockExists && !stateExists) {
    return null;
  }
  try {
    const installed = loadInstalledPmoBaseline(targetDir);
    const lock = readPmoLock(targetDir, { strict: false });
    if (!installed) {
      throw new Error('cannot establish PMO skill ownership because the installed manifest is missing');
    }
    const projection = createPmoSkillProjection(installed.manifest);
    if (lock && samePmoBundle(lock, installed.manifest)) {
      return projection;
    }
    if (trustedManifest && samePmoBundle(trustedManifest, installed.manifest)) {
      return projection;
    }
    throw new Error('cannot establish PMO skill ownership from the project lock or trusted target bundle');
  } catch (error) {
    if (strict) {
      throw error;
    }
    return null;
  }
}

function comparePmoSkillProjection(targetDir, manifest) {
  const projection = createPmoSkillProjection(manifest);
  const skillRoot = join(targetDir, '.agents/skills');
  const state = readPmoSkillState(targetDir, { strict: false });
  const missing = [];
  const changed = [];
  const extra = [];

  if (!state) {
    missing.push(PMO_SKILL_STATE_RELATIVE_PATH);
  } else if (
    !samePmoBundle(state, manifest) ||
    JSON.stringify(state.roots) !== JSON.stringify(projection.roots) ||
    JSON.stringify(state.files) !== JSON.stringify(projection.files)
  ) {
    changed.push(PMO_SKILL_STATE_RELATIVE_PATH);
  }

  const expected = new Map(projection.files.map((file) => [file.path, file]));
  for (const [relativePath, file] of expected) {
    const path = join(skillRoot, relativePath);
    if (!existsSync(path)) {
      missing.push(`.agents/skills/${relativePath}`);
      continue;
    }
    const content = readFileSync(path);
    const actualMode = process.platform === 'win32' ? file.mode : statSync(path).mode & 0o111 ? '0755' : '0644';
    if (
      content.length !== file.size ||
      createHash('sha256').update(content).digest('hex') !== file.sha256 ||
      actualMode !== file.mode
    ) {
      changed.push(`.agents/skills/${relativePath}`);
    }
  }

  for (const root of projection.roots) {
    const rootPath = join(skillRoot, root);
    for (const file of walkPmoFiles(rootPath)) {
      const relativePath = toPosix(relative(skillRoot, file));
      if (!expected.has(relativePath)) {
        extra.push(`.agents/skills/${relativePath}`);
      }
    }
  }
  return {
    missing,
    changed,
    extra,
    expectedFiles: projection.files.length,
    expectedRoots: projection.roots.length,
  };
}

function installPmoBundleAtomic(targetDir, baseline) {
  verifyPmoBundle(baseline);
  const projection = createPmoSkillProjection(baseline.manifest);
  const previousSkillState = resolvePreviousPmoSkillOwnership(targetDir, {
    strict: true,
    trustedManifest: baseline.manifest,
  });
  const liveBaseline = join(targetDir, PMO_BASELINE_RELATIVE_PATH);
  const liveLock = join(targetDir, PMO_LOCK_RELATIVE_PATH);
  const liveSkillRoot = join(targetDir, '.agents/skills');
  const liveSkillState = join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH);

  for (const root of projection.roots) {
    const rootPath = join(liveSkillRoot, root);
    if (existsSync(rootPath) && !previousSkillState?.roots?.includes(root)) {
      throw new Error(
        `cannot install PMO skill ${root}: .agents/skills/${root} exists but is not owned by the current PMO bundle`,
      );
    }
  }

  const transactionId = `${Date.now()}-${randomBytes(4).toString('hex')}`;
  const runtimeRoot = join(targetDir, PMO_RUNTIME_RELATIVE_PATH);
  const transactionRoot = join(runtimeRoot, 'transactions', transactionId);
  const stagedBaseline = join(transactionRoot, 'baseline');
  const stagedSkills = join(transactionRoot, 'skills');
  const stagedSkillState = join(transactionRoot, 'skill-state.json');
  const stagedLock = join(transactionRoot, 'pmo-lock.json');
  const backupRoot = join(runtimeRoot, 'backups', transactionId);
  const backupBaseline = join(backupRoot, 'baseline');
  const backupSkills = join(backupRoot, 'skills');
  const backupSkillState = join(backupRoot, 'skill-state.json');
  const backupLock = join(backupRoot, 'pmo-lock.json');
  const backupProjectFilesState = join(backupRoot, PMO_PROJECT_FILES_STATE_NAME);
  const managedProjectFilePaths = [PROJECT_PR_TEMPLATE_RELATIVE_PATH, ...PMO_GOVERNANCE_WORKFLOW_PATHS];

  preparePmoTransaction({
    baseline,
    projection,
    stagedBaseline,
    stagedSkills,
    stagedSkillState,
    stagedLock,
  });
  mkdirSync(backupRoot, { recursive: true });
  const projectFiles = managedProjectFilePaths.map((path) => ({
    path,
    existed: existsSync(join(targetDir, path)),
  }));
  writeFileSync(
    backupProjectFilesState,
    `${JSON.stringify(
      {
        schemaVersion: 2,
        files: projectFiles,
      },
      null,
      2,
    )}\n`,
  );
  for (const file of projectFiles.filter((entry) => entry.existed)) {
    const source = join(targetDir, file.path);
    const backup = join(backupRoot, 'project-files', file.path);
    mkdirSync(dirname(backup), { recursive: true });
    copyFileSync(source, backup);
  }

  let movedBaseline = false;
  let movedLock = false;
  let movedSkillState = false;
  const movedSkillRoots = [];
  let installedBaseline = false;
  let newLockInstalled = false;
  let installedSkillState = false;
  const installedSkillRoots = [];
  try {
    if (existsSync(liveBaseline)) {
      mkdirSync(dirname(backupBaseline), { recursive: true });
      renameSync(liveBaseline, backupBaseline);
      movedBaseline = true;
    }
    if (existsSync(liveLock)) {
      renameSync(liveLock, backupLock);
      movedLock = true;
    }
    if (existsSync(liveSkillState)) {
      renameSync(liveSkillState, backupSkillState);
      movedSkillState = true;
    }
    for (const root of previousSkillState?.roots || []) {
      const source = join(liveSkillRoot, root);
      if (!existsSync(source)) {
        continue;
      }
      const target = join(backupSkills, root);
      mkdirSync(dirname(target), { recursive: true });
      renameSync(source, target);
      movedSkillRoots.push(root);
    }

    mkdirSync(dirname(liveBaseline), { recursive: true });
    renameSync(stagedBaseline, liveBaseline);
    installedBaseline = true;
    mkdirSync(liveSkillRoot, { recursive: true });
    for (const root of projection.roots) {
      renameSync(join(stagedSkills, root), join(liveSkillRoot, root));
      installedSkillRoots.push(root);
    }
    renameSync(stagedSkillState, liveSkillState);
    installedSkillState = true;
    mkdirSync(dirname(liveLock), { recursive: true });
    renameSync(stagedLock, liveLock);
    newLockInstalled = true;

    const installed = loadInstalledPmoBaseline(targetDir);
    verifyPmoBundle(installed);
    const verifiedLock = readPmoLock(targetDir);
    const failures = [];
    checkPmoLockMatchesManifest(verifiedLock, installed.manifest, failures);
    const skills = comparePmoSkillProjection(targetDir, installed.manifest);
    if (skills.missing.length > 0 || skills.changed.length > 0 || skills.extra.length > 0) {
      failures.push(
        `project PMO skill projection mismatch; missing=${skills.missing.join(',') || '-'} changed=${skills.changed.join(',') || '-'} extra=${skills.extra.join(',') || '-'}`,
      );
    }
    if (failures.length > 0) {
      throw new Error(failures.join('\n'));
    }
  } catch (error) {
    const recoveryFailures = [];
    if (installedBaseline) {
      attemptPmoRecovery(
        () => rmSync(liveBaseline, { recursive: true, force: true }),
        'remove failed baseline',
        recoveryFailures,
      );
    }
    if (movedBaseline && existsSync(backupBaseline)) {
      attemptPmoRecovery(
        () => {
          mkdirSync(dirname(liveBaseline), { recursive: true });
          renameSync(backupBaseline, liveBaseline);
        },
        'restore previous baseline',
        recoveryFailures,
      );
    }
    if (newLockInstalled) {
      attemptPmoRecovery(() => rmSync(liveLock, { force: true }), 'remove failed PMO lock', recoveryFailures);
    }
    if (movedLock && existsSync(backupLock)) {
      attemptPmoRecovery(
        () => {
          mkdirSync(dirname(liveLock), { recursive: true });
          renameSync(backupLock, liveLock);
        },
        'restore previous PMO lock',
        recoveryFailures,
      );
    }
    for (const root of installedSkillRoots) {
      attemptPmoRecovery(
        () => rmSync(join(liveSkillRoot, root), { recursive: true, force: true }),
        `remove failed PMO skill ${root}`,
        recoveryFailures,
      );
    }
    for (const root of movedSkillRoots) {
      const source = join(backupSkills, root);
      if (existsSync(source)) {
        attemptPmoRecovery(
          () => {
            mkdirSync(liveSkillRoot, { recursive: true });
            renameSync(source, join(liveSkillRoot, root));
          },
          `restore previous PMO skill ${root}`,
          recoveryFailures,
        );
      }
    }
    if (installedSkillState) {
      attemptPmoRecovery(
        () => rmSync(liveSkillState, { force: true }),
        'remove failed PMO skill state',
        recoveryFailures,
      );
    }
    if (movedSkillState && existsSync(backupSkillState)) {
      attemptPmoRecovery(
        () => {
          mkdirSync(dirname(liveSkillState), { recursive: true });
          renameSync(backupSkillState, liveSkillState);
        },
        'restore previous PMO skill state',
        recoveryFailures,
      );
    }
    attemptPmoRecovery(
      () => rmSync(transactionRoot, { recursive: true, force: true }),
      'clean PMO transaction staging',
      recoveryFailures,
    );
    if (existsSync(backupRoot) && walkPmoFiles(backupRoot).length === 0) {
      attemptPmoRecovery(
        () => rmSync(backupRoot, { recursive: true, force: true }),
        'clean empty PMO backup',
        recoveryFailures,
      );
    }
    const recovery =
      recoveryFailures.length === 0
        ? 'previous project bundle restored'
        : `recovery incomplete: ${recoveryFailures.join('; ')}`;
    throw new Error(`PMO bundle transaction failed (${recovery}): ${error.message}`, { cause: error });
  }

  rmSync(transactionRoot, { recursive: true, force: true });
  if (
    !movedBaseline &&
    !movedLock &&
    movedSkillRoots.length === 0 &&
    !movedSkillState &&
    !projectFiles.some((file) => file.existed)
  ) {
    rmSync(backupRoot, { recursive: true, force: true });
  }
  prunePmoBackupDirectories(targetDir, 5);
  return {
    backupRoot,
    movedBaseline,
    movedLock,
    movedSkillState,
    movedSkillRoots,
    projectionRoots: projection.roots,
  };
}

function restorePreviousPmoBundle(targetDir, transaction) {
  const previousBaselineRoot = join(transaction.backupRoot, 'baseline');
  const previousManifestPath = join(previousBaselineRoot, 'baseline.json');
  if (transaction.movedBaseline) {
    if (!existsSync(previousManifestPath)) {
      throw new Error(`previous PMO baseline backup is missing: ${previousManifestPath}`);
    }
    const previous = {
      root: previousBaselineRoot,
      manifest: JSON.parse(readFileSync(previousManifestPath, 'utf8')),
    };
    verifyPmoBundle(previous);
    installPmoBundleAtomic(targetDir, previous);
    return;
  }

  const liveSkillRoot = join(targetDir, '.agents/skills');
  for (const root of transaction.projectionRoots || []) {
    rmSync(join(liveSkillRoot, root), { recursive: true, force: true });
  }
  rmSync(join(targetDir, PMO_BASELINE_RELATIVE_PATH), { recursive: true, force: true });
  rmSync(join(targetDir, PMO_LOCK_RELATIVE_PATH), { force: true });
  rmSync(join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH), { force: true });

  for (const root of transaction.movedSkillRoots || []) {
    const source = join(transaction.backupRoot, 'skills', root);
    if (!existsSync(source)) throw new Error(`previous PMO skill backup is missing: ${source}`);
    mkdirSync(liveSkillRoot, { recursive: true });
    renameSync(source, join(liveSkillRoot, root));
  }
  if (transaction.movedSkillState) {
    const source = join(transaction.backupRoot, 'skill-state.json');
    mkdirSync(dirname(join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH)), { recursive: true });
    renameSync(source, join(targetDir, PMO_SKILL_STATE_RELATIVE_PATH));
  }
  if (transaction.movedLock) {
    const source = join(transaction.backupRoot, 'pmo-lock.json');
    mkdirSync(dirname(join(targetDir, PMO_LOCK_RELATIVE_PATH)), { recursive: true });
    renameSync(source, join(targetDir, PMO_LOCK_RELATIVE_PATH));
  }
}

function attemptPmoRecovery(action, label, failures) {
  try {
    action();
  } catch (error) {
    failures.push(`${label}: ${error.message}`);
  }
}

function preparePmoTransaction({ baseline, projection, stagedBaseline, stagedSkills, stagedSkillState, stagedLock }) {
  mkdirSync(stagedBaseline, { recursive: true });
  for (const file of baseline.manifest.files) {
    copyPmoFile(join(baseline.root, file.path), join(stagedBaseline, file.path), file.mode);
  }
  writeFileSync(join(stagedBaseline, 'baseline.json'), `${JSON.stringify(baseline.manifest, null, 2)}\n`);
  verifyPmoBundle({ root: stagedBaseline, manifest: baseline.manifest });

  mkdirSync(stagedSkills, { recursive: true });
  for (const file of projection.files) {
    copyPmoFile(join(baseline.root, 'skills', file.path), join(stagedSkills, file.path), file.mode);
  }
  writeFileSync(stagedSkillState, `${JSON.stringify(projection, null, 2)}\n`);
  writeFileSync(stagedLock, `${JSON.stringify(createPmoLock(baseline.manifest), null, 2)}\n`);
}

function copyPmoFile(source, target, mode) {
  mkdirSync(dirname(target), { recursive: true });
  copyFileSync(source, target);
  chmodSync(target, mode === '0755' ? 0o755 : 0o644);
}

function rollbackPmoBaseline(argv) {
  const options = parsePmoArgs(argv);
  const targetDir = resolve(process.cwd(), options.projectDir);
  if (!existsSync(targetDir) || !statSync(targetDir).isDirectory()) {
    fail(`project directory not found: ${targetDir}`);
  }
  const backups = listPmoBackups(targetDir).filter(
    (backup) => !options.to || backup.manifest.packageVersion === options.to,
  );
  if (backups.length === 0) {
    fail(options.to ? `no PMO backup found for @mango/pmo@${options.to}` : 'no PMO backup is available for rollback');
  }
  const selected = backups[0];
  const projectFilesState = readPmoBackupProjectFilesState(selected.backupRoot);
  process.stdout.write(
    `PMO rollback ${options.dryRun ? 'dry-run ' : ''}target: ${formatPmoIdentity(selected.manifest)}\n`,
  );
  if (options.dryRun) {
    return;
  }
  installPmoBundleAtomic(targetDir, selected);
  if (projectFilesState) {
    for (const file of projectFilesState.files) {
      const targetPath = join(targetDir, file.path);
      if (file.existed) {
        const saved = join(selected.backupRoot, 'project-files', file.path);
        mkdirSync(dirname(targetPath), { recursive: true });
        copyFileSync(saved, targetPath);
        if (Buffer.compare(readFileSync(saved), readFileSync(targetPath)) !== 0) {
          throw new Error(`PMO rollback project file verification failed: ${file.path}`);
        }
      } else {
        rmSync(targetPath, { force: true });
        if (existsSync(targetPath)) {
          throw new Error(`PMO rollback could not remove project file: ${file.path}`);
        }
      }
    }
  } else {
    const templatePlan = planPmoProjectTemplateSync(targetDir, selected);
    const failure = templatePlan.find((item) => item.action === 'warn');
    if (failure) fail(`PMO rollback PR template synchronization failed: ${failure.reason}`);
    for (const item of templatePlan.filter((entry) => ['add', 'update'].includes(entry.action))) {
      writePlannedFile(item);
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
      mavenRepository: DEFAULT_MAVEN_REPOSITORY,
      modules: 'none',
    });
    for (const item of planPmoGovernanceWorkflowSync(targetDir, variables, false)) {
      if (['add', 'update'].includes(item.action)) writePlannedFile(item);
    }
  }
  const status = getPmoStatus(targetDir, {
    locked: true,
    checkGovernanceWorkflows: !projectFilesState,
  });
  if (status.errors.length > 0 || status.warnings.length > 0) {
    fail(`PMO rollback verification failed:\n${formatPmoStatusProblems(status)}`);
  }
  process.stdout.write(`PMO rollback complete: ${formatPmoIdentity(selected.manifest)}\n`);
}

function readPmoBackupProjectFilesState(backupRoot) {
  const statePath = join(backupRoot, PMO_PROJECT_FILES_STATE_NAME);
  if (!existsSync(statePath)) return null;
  const state = JSON.parse(readFileSync(statePath, 'utf8'));
  let files;
  if (
    state.schemaVersion === 1 &&
    state.pullRequestTemplate?.path === PROJECT_PR_TEMPLATE_RELATIVE_PATH &&
    typeof state.pullRequestTemplate.existed === 'boolean'
  ) {
    files = [state.pullRequestTemplate];
  } else if (
    state.schemaVersion === 2 &&
    Array.isArray(state.files) &&
    state.files.length > 0 &&
    state.files.every(
      (file) =>
        [PROJECT_PR_TEMPLATE_RELATIVE_PATH, ...PMO_GOVERNANCE_WORKFLOW_PATHS].includes(file.path) &&
        typeof file.existed === 'boolean',
    )
  ) {
    files = state.files;
  } else {
    throw new Error(`invalid PMO backup project file state: ${statePath}`);
  }
  if (new Set(files.map((file) => file.path)).size !== files.length) {
    throw new Error(`invalid PMO backup project file state: ${statePath}`);
  }
  for (const file of files.filter((entry) => entry.existed)) {
    const savedPath = join(backupRoot, 'project-files', file.path);
    if (!existsSync(savedPath)) {
      throw new Error(`PMO backup project file is missing: ${savedPath}`);
    }
  }
  return { schemaVersion: state.schemaVersion, files };
}

function listPmoBackups(targetDir) {
  const root = join(targetDir, PMO_RUNTIME_RELATIVE_PATH, 'backups');
  if (!existsSync(root)) {
    return [];
  }
  const backups = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (!entry.isDirectory()) {
      continue;
    }
    const backupRoot = join(root, entry.name);
    const baselineRoot = join(backupRoot, 'baseline');
    const manifestPath = join(baselineRoot, 'baseline.json');
    const lockPath = join(backupRoot, 'pmo-lock.json');
    if (!existsSync(manifestPath) || !existsSync(lockPath)) {
      continue;
    }
    try {
      const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
      const lock = JSON.parse(readFileSync(lockPath, 'utf8'));
      validatePmoManifest(manifest);
      if (!samePmoBundle(lock, manifest)) {
        continue;
      }
      const backup = { root: baselineRoot, manifest, backupRoot, name: entry.name };
      verifyPmoBundle(backup);
      backups.push(backup);
    } catch {
      // Incomplete transactions are ignored and never selected for repair or rollback.
    }
  }
  return backups.sort((left, right) => right.name.localeCompare(left.name));
}

function prunePmoBackupDirectories(targetDir, keep) {
  const root = join(targetDir, PMO_RUNTIME_RELATIVE_PATH, 'backups');
  if (!existsSync(root)) {
    return;
  }
  const directories = readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort((left, right) => right.localeCompare(left));
  for (const directory of directories.slice(keep)) {
    rmSync(join(root, directory), { recursive: true, force: true });
  }
}

function walkPmoFiles(root) {
  if (!existsSync(root)) {
    return [];
  }
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`symbolic links are not allowed in PMO managed directories: ${path}`);
    }
    if (entry.isDirectory()) {
      files.push(...walkPmoFiles(path));
    } else if (entry.isFile()) {
      files.push(path);
    } else {
      throw new Error(`unsupported entry in PMO managed directory: ${path}`);
    }
  }
  return files;
}

function planShellSync(targetDir, variables, syncShell) {
  const shellFiles = ['scripts/dev-workspace.sh', 'scripts/backend-dev.sh'];
  if (!syncShell) {
    return [...shellFiles, 'mango.dev.json'].map((path) => ({
      action: 'skip',
      reason: 'rerun with --sync-shell to update generated startup entries',
      path,
      targetPath: join(targetDir, path),
    }));
  }
  const plan = shellFiles.map((path) => {
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
    plan.push(buildFilePlanItem('mango.dev.json', manifestPath, `${JSON.stringify(manifest, null, 2)}\n`));
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
  return /\/Users\/[^\s`'"]*\/mango-pmo/.test(content) || /\b[A-Za-z]:\\Users\\[^\s`'"]*\\mango-pmo/.test(content);
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
  return plan.reduce(
    (summary, item) => {
      summary[item.action] = (summary[item.action] || 0) + 1;
      return summary;
    },
    { add: 0, update: 0, delete: 0, skip: 0, warn: 0 },
  );
}

function printPmoSyncPlan(targetDir, plan, dryRun, command = 'sync') {
  const summary = summarizeSyncPlan(plan);
  process.stdout.write(
    `${dryRun ? 'PMO baseline dry-run plan' : `PMO baseline ${command} plan`} for ${relativeOrAbsolute(process.cwd(), targetDir)}\n`,
  );
  process.stdout.write(
    `  add: ${summary.add}, update: ${summary.update}, delete: ${summary.delete}, skip: ${summary.skip}, warn: ${summary.warn}\n`,
  );
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
    paths: {
      backend: 'backend',
      frontend: 'frontend',
      businessDocs: 'business-docs',
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
    replaceManagedBlock(content, 'imports', ensureFrontendTypeImport(variables.frontendEntryImports)),
    'features',
    [
      `const mangoFeatures = ${variables.frontendFeaturesExpression};`,
      `const mangoFeatureRegistrars: MangoAdminFeatureRegistrar[] = ${variables.frontendFeatureRegistrarsExpression};`,
    ].join('\n'),
  );
  writeFileSync(entryPath, nextContent);
}

function ensureFrontendTypeImport(importsBlock) {
  const typeImport =
    "import type { MangoAdminBootstrapHooks, MangoAdminFeatureRegistrar, MangoAdminShellOptions } from '@mango/admin';";
  return importsBlock.includes(typeImport) ? importsBlock : `${importsBlock.trimEnd()}\n${typeImport}`;
}

function updateRuntimeConfigFiles(targetDir, variables) {
  const configFiles = [
    ['frontend/public/runtime-config.json', variables.runtimeModulesJson],
    ['frontend/runtime-config.monolith.example.json', variables.runtimeModulesJson],
    ['frontend/runtime-config.microservice.example.json', variables.runtimeModulesMicroserviceJson],
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
    '            <version>${revision}</version>',
    '        </dependency>',
  ].join('\n');
  writeFileSync(
    backendPomPath,
    appendManagedLine(readFileSync(backendPomPath, 'utf8'), 'business-modules', moduleLine),
  );
  writeFileSync(
    appPomPath,
    appendManagedLine(readFileSync(appPomPath, 'utf8'), 'business-dependencies', dependencyXml),
  );
  updateBackendBusinessFlywayConfig(targetDir, variables);
}

function updateBackendBusinessFlywayConfig(targetDir, variables) {
  const applicationPath = join(targetDir, 'backend/app/src/main/resources/application.yml');
  const flywayModuleBlock = [`        ${variables.moduleKebab}:`, '          enabled: true'].join('\n');
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
  packageJson.dependencies[`@${variables.projectKebab}/${variables.moduleKebab}`] =
    `workspace:${variables.projectVersion}`;
  packageJson.dependencies[`@${variables.projectKebab}/${variables.moduleKebab}-api`] =
    `workspace:${variables.projectVersion}`;
  writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`);

  const entryPath = join(targetDir, 'frontend/src/main.ts');
  const content = readFileSync(entryPath, 'utf8');
  const importLine = `import { register${variables.modulePascal}Pages } from '@${variables.projectKebab}/${variables.moduleKebab}';`;
  const styleImportLine = `import '@${variables.projectKebab}/${variables.moduleKebab}/style.css';`;
  const registrarLine = `  () => register${variables.modulePascal}Pages(),`;
  const preparedContent = ensureFrontendBusinessRegistrars(content);
  const withHttpClient = ensureFrontendBusinessHttpClient(preparedContent);
  const withImport = withHttpClient.includes(importLine)
    ? withHttpClient
    : withHttpClient.replace('// mango-cli:imports:end', `${importLine}\n// mango-cli:imports:end`);
  const withStyleImport = withImport.includes(styleImportLine)
    ? withImport
    : withImport.replace('// mango-cli:imports:end', `${styleImportLine}\n// mango-cli:imports:end`);
  const next = appendBusinessFeatureRegistrar(withStyleImport, registrarLine);
  writeFileSync(entryPath, next);
}

function ensureFrontendBusinessHttpClient(content) {
  const httpClientImport = "import { createMangoHttpClient } from '@mango/http-client';";
  const sessionImport = "import { Session } from '@mango/common';";
  const runtimeImport = "import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';";
  let next = content;
  for (const importLine of [httpClientImport, sessionImport, runtimeImport]) {
    if (!next.includes(importLine)) {
      next = next.replace('// mango-cli:imports:end', `${importLine}\n// mango-cli:imports:end`);
    }
  }
  if (!next.includes('const mangoBusinessHttpClient = createMangoHttpClient({')) {
    const clientBlock = [
      'const mangoBusinessHttpClient = createMangoHttpClient({',
      "  baseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',",
      '  getAccessToken: () => Session.getToken(),',
      "  getTenantId: () => Session.get('userInfo')?.tenantId ?? Session.get('tenantId'),",
      '  onUnauthorized: () => {',
      '    Session.clearSession();',
      "    window.location.hash = '/login';",
      '  },',
      '});',
      '',
    ].join('\n');
    next = next.replace(
      '// mango-cli:business-feature-registrars:start',
      `${clientBlock}// mango-cli:business-feature-registrars:start`,
    );
  }
  return ensureFrontendBusinessAppProvision(next);
}

function ensureFrontendBusinessAppProvision(content) {
  const provideLine = 'mangoAdminApp.app.provide(MANGO_HTTP_CLIENT_KEY, mangoBusinessHttpClient);';
  if (content.includes(provideLine)) {
    return content;
  }
  const hooksStart = '// mango-cli:bootstrap-hooks:start';
  const hooksEnd = '// mango-cli:bootstrap-hooks:end';
  if (content.includes(hooksStart) && content.includes(hooksEnd)) {
    return replaceManagedBlock(
      content,
      'bootstrap-hooks',
      [
        'const mangoAdminBootstrapHooks: MangoAdminBootstrapHooks = {',
        '  beforeMount(mangoAdminApp) {',
        `    ${provideLine}`,
        '  },',
        '};',
      ].join('\n'),
    );
  }
  const createCall = 'createMangoAdminApp({';
  const mountCall = '}).mount();';
  if (!content.includes(createCall) || !content.includes(mountCall)) {
    throw new Error('frontend/src/main.ts must create and mount Mango admin app before adding a business module');
  }
  return content
    .replace(createCall, `const mangoAdminApp = ${createCall}`)
    .replace(mountCall, `});\n\n${provideLine}\nmangoAdminApp.mount();`);
}

function ensureFrontendBusinessRegistrars(content) {
  const preparedContent = ensureFrontendMainRegistrarTypes(ensureFrontendMainTypeImport(content));
  if (
    content.includes('// mango-cli:business-feature-registrars:start') &&
    content.includes('// mango-cli:business-feature-registrars:end')
  ) {
    return ensureAllFeatureRegistrarsUsage(preparedContent);
  }

  const block = [
    '// mango-cli:business-feature-registrars:start',
    'const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [];',
    '// mango-cli:business-feature-registrars:end',
    '',
    'const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [',
    '  ...mangoFeatureRegistrars,',
    '  ...mangoBusinessFeatureRegistrars,',
    '];',
    '',
  ].join('\n');
  const featureEnd = '// mango-cli:features:end';
  const withBlock = preparedContent.includes(featureEnd)
    ? preparedContent.replace(featureEnd, `${featureEnd}\n\n${block.trimEnd()}`)
    : `${block}${preparedContent}`;
  return ensureAllFeatureRegistrarsUsage(withBlock);
}

function ensureFrontendMainTypeImport(content) {
  const typeImport =
    "import type { MangoAdminBootstrapHooks, MangoAdminFeatureRegistrar, MangoAdminShellOptions } from '@mango/admin';";
  const existingTypeImport = /import type \{[^}]*MangoAdminFeatureRegistrar[^}]*\} from '@mango\/admin';/u;
  if (existingTypeImport.test(content)) {
    return content.replace(existingTypeImport, typeImport);
  }
  const importsEnd = '// mango-cli:imports:end';
  if (content.includes(importsEnd)) {
    return content.replace(importsEnd, `${typeImport}\n${importsEnd}`);
  }
  return `${typeImport}\n${content}`;
}

function ensureFrontendMainRegistrarTypes(content) {
  return content
    .replace(/const mangoFeatureRegistrars = /, 'const mangoFeatureRegistrars: MangoAdminFeatureRegistrar[] = ')
    .replace(
      /const mangoBusinessFeatureRegistrars = \[/,
      'const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [',
    )
    .replace(
      /const mangoAllFeatureRegistrars = \[/,
      'const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [',
    );
}

function ensureAllFeatureRegistrarsUsage(content) {
  return content.replace('featureRegistrars: mangoFeatureRegistrars,', 'featureRegistrars: mangoAllFeatureRegistrars,');
}

function appendBusinessFeatureRegistrar(content, registrarLine) {
  const start = '// mango-cli:business-feature-registrars:start';
  const end = '// mango-cli:business-feature-registrars:end';
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    throw new Error('managed block not found in frontend/src/main.ts: business-feature-registrars');
  }
  const startLineEnd = content.indexOf('\n', startIndex);
  const endLineStart = content.lastIndexOf('\n', endIndex) + 1;
  const currentBlock = content.slice(startLineEnd + 1, endLineStart);
  if (currentBlock.includes(registrarLine.trim())) {
    return content;
  }
  const registrarExpression = registrarLine.trim().replace(/,$/u, '');
  let nextBlock;
  if (currentBlock.includes(' = [];')) {
    nextBlock = currentBlock.replace(' = [];', ` = [${registrarExpression}];`);
  } else {
    const oneLineMatch = currentBlock.match(/ = \[(.+)\];/u);
    nextBlock = oneLineMatch
      ? currentBlock.replace(oneLineMatch[0], ` = [\n  ${oneLineMatch[1]},\n  ${registrarExpression},\n];`)
      : currentBlock.replace('\n];', `\n  ${registrarExpression},\n];`);
  }
  return `${content.slice(0, startLineEnd + 1)}${nextBlock}${content.slice(endLineStart)}`;
}

function updateBusinessConfig(targetDir, config, variables) {
  const businessModules = Array.isArray(config.businessModules) ? config.businessModules : [];
  const nextModules = businessModules.filter((item) => item.module !== variables.moduleKebab);
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
    throw new Error(`managed block not found: ${name}`);
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
    throw new Error(`managed block not found: ${name}`);
  }
  const startLineEnd = content.indexOf('\n', startIndex);
  const endLineStart = content.lastIndexOf('\n', endIndex) + 1;
  if (startLineEnd < 0 || endLineStart <= startLineEnd) {
    throw new Error(`invalid managed block: ${name}`);
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
    throw new Error(`managed block not found in frontend/src/main.ts: ${name}`);
  }
  return [content.slice(0, startIndex + start.length), '\n', replacement, '\n', content.slice(endIndex)].join('');
}

function replaceXmlManagedBlock(content, name, replacement, fileLabel = 'backend/pom.xml') {
  const start = `<!-- mango-cli:${name}:start -->`;
  const end = `<!-- mango-cli:${name}:end -->`;
  const startIndex = content.indexOf(start);
  const endIndex = content.indexOf(end);
  if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
    throw new Error(`managed block not found in ${fileLabel}: ${name}`);
  }
  return [content.slice(0, startIndex + start.length), '\n', replacement, '\n', content.slice(endIndex)].join('');
}

function resolveSelectedModules(options) {
  const codes = options.preset === 'full' ? FULL_MODULE_CODES : resolveModuleCodes(options.modules || 'none');
  return codes.map((code) => MODULE_BY_CODE.get(code));
}

function resolveModuleCodes(value) {
  const rawCodes = Array.isArray(value)
    ? value.flatMap((item) => String(item).split(','))
    : String(value || '').split(',');
  const normalized = rawCodes
    .map((item) => item.trim())
    .filter(Boolean)
    .flatMap((item) => (item === 'all' ? FULL_MODULE_CODES : item === 'none' ? [] : [item]));
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
  return items.map((item) => ({
    ...item,
    packageName: item.packageName || item.name,
    registrars: item.registrars || [],
  }));
}

function toFrontendDependency(module) {
  if (!module.packageName || !module.cliVersionKey) {
    fail(
      `admin module ${module.code || module.packageName || '<unknown>'} must declare packageName and cliVersionKey for CLI`,
    );
  }
  return {
    name: module.packageName,
    versionKey: module.cliVersionKey,
  };
}

function buildOptionalModules(adminFullModules, overlays) {
  const overlayByCode = new Map(overlays.map((overlay) => [overlay.code, overlay]));
  return adminFullModules
    .filter((module) => module.cliOptional !== false)
    .map((module) => {
      const overlay = overlayByCode.get(module.code);
      if (!overlay) {
        fail(`mango-cli optional module overlay missing for ${module.code}`);
      }
      return {
        ...overlay,
        frontendPackage: module.packageName,
        versionKey: module.cliVersionKey,
        styleImport: module.style,
        registrarImport: module.registrars.map(
          (registrar) => `import { ${registrar.name} } from '${registrar.import}';`,
        ),
        registrar: module.registrars.map((registrar) => registrar.name),
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
      "import { bootstrapMangoAdminApp } from '@mango/admin';",
      "import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';",
      "import '@mango/admin/style-full.css';",
    ].join('\n');
  }
  const imports = ["import { bootstrapMangoAdminApp } from '@mango/admin';", "import '@mango/admin/style.css';"];
  for (const module of ADMIN_DEFAULT_MODULES) {
    imports.push(...module.registrars.map((registrar) => `import { ${registrar.name} } from '${registrar.import}';`));
  }
  for (const module of selectedModules) {
    imports.push(...toArray(module.registrarImport));
  }
  for (const module of selectedModules) {
    if (module.styleImport) {
      imports.push(`import '${module.styleImport}';`);
    }
  }
  return uniqueBy(imports, (item) => item).join('\n');
}

function renderFrontendFeaturesExpression(preset, selectedModules) {
  if (preset === 'full') {
    return "'full'";
  }
  const features = selectedModules.map((module) => module.feature).filter(Boolean);
  return `[${features.map((feature) => `'${feature}'`).join(', ')}] as const`;
}

function renderFrontendFeatureRegistrarsExpression(preset, selectedModules) {
  if (preset === 'full') {
    return 'mangoFullAdminFeatureRegistrars';
  }
  const registrars = [
    ...ADMIN_DEFAULT_MODULES.flatMap((module) => module.registrars.map((registrar) => registrar.name)),
    ...selectedModules.flatMap((module) => toArray(module.registrar)),
  ];
  if (registrars.length === 0) {
    return '[]';
  }
  return `[\n${registrars.map((registrar) => `  ${registrar},`).join('\n')}\n]`;
}

function renderBackendManagedDependencies(preset, selectedModules) {
  if (preset === 'full') {
    return renderDependencyXml(
      [
        { groupId: 'io.mango', artifactId: 'mango-admin-starter' },
        { groupId: 'io.mango.infra.bootstrap', artifactId: 'mango-infra-bootstrap-starter' },
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
      ...selectedModules.flatMap((module) => module.backend || []),
    ],
    true,
    12,
  );
}

function renderBackendDependencies(preset, selectedModules) {
  if (preset === 'full') {
    return renderDependencyXml(
      [
        { groupId: 'io.mango', artifactId: 'mango-admin-starter' },
        { groupId: 'io.mango.infra.bootstrap', artifactId: 'mango-infra-bootstrap-starter' },
      ],
      false,
      8,
    );
  }
  return renderDependencyXml(
    [...CORE_BACKEND_DEPENDENCIES, ...selectedModules.flatMap((module) => module.backend || [])],
    false,
    8,
  );
}

function renderDependencyXml(dependencies, includeVersion, indentSize) {
  const indent = ' '.repeat(indentSize);
  const childIndent = `${indent}    `;
  return uniqueBy(dependencies, (dependency) => `${dependency.groupId}:${dependency.artifactId}`)
    .map((dependency) =>
      [
        `${indent}<dependency>`,
        `${childIndent}<groupId>${dependency.groupId}</groupId>`,
        `${childIndent}<artifactId>${dependency.artifactId}</artifactId>`,
        ...(includeVersion ? [`${childIndent}<version>${dependency.version || '${mango.version}'}</version>`] : []),
        `${indent}</dependency>`,
      ].join('\n'),
    )
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
      modules[module.runtimeModule.moduleCode] =
        mode === 'micro' ? module.runtimeModule.micro : module.runtimeModule.local;
    }
  }
  return JSON.stringify(modules, null, 2).replace(/\n/g, '\n  ');
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
        throw new Error(`unrendered placeholder in path: ${rel}`);
      }
      if (!isTextFile(file)) {
        continue;
      }
      const content = readFileSync(file, 'utf8');
      const match = content.match(/\{\{[^}]+}}/);
      if (match) {
        throw new Error(`unrendered placeholder ${match[0]} in ${rel}`);
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
  return (
    /\.(md|json|xml|java|ts|vue|html|mjs|sh|yml|yaml|txt|gitignore|npmrc|properties|imports|sql)$/.test(name) ||
    name.endsWith('.template') ||
    name === 'CODEOWNERS' ||
    name === 'AGENTS.md'
  );
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
  return readGitRef(gitDir, ref) || readGitRef(commonGitDir, ref) || readPackedGitRef(commonGitDir, ref) || 'unknown';
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
  const packedLine = lines.find((line) => line.endsWith(` ${ref}`));
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

function requireChineseDisplayName(value, optionName) {
  const normalized = value.trim();
  if (!normalized || !/\p{Script=Han}/u.test(normalized)) {
    fail(`${optionName} must contain at least one Chinese character`);
  }
  return normalized;
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

main().catch((error) => {
  console.error(`Error: ${error.message}`);
  process.exit(1);
});
