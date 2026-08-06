import { spawn, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import http from 'node:http';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import {
  assertPnpmLockfileFixtureInvocations,
  createPnpmLockfileFixture,
} from './support/pnpm-lockfile-fixture.mjs';

const cliPackageRoot = resolve(import.meta.dirname, '..');
const uiRoot = resolve(cliPackageRoot, '../..');
const pmoPackageRoot = join(uiRoot, 'packages/mango-pmo');
const temporaryRoot = mkdtempSync(join(tmpdir(), 'mango-cli-packed-doctor-'));
const packDirectory = join(temporaryRoot, 'pack');
const consumerDirectory = join(temporaryRoot, 'consumer');
const browserSentinel = join(temporaryRoot, 'playwright-browser-sentinel');
const token = 'packed-consumer-token';
mkdirSync(packDirectory, { recursive: true });
mkdirSync(consumerDirectory, { recursive: true });
pack(pmoPackageRoot);
pack(cliPackageRoot);
const pmoTarball = findTarball('mango-pmo-');
const cliTarball = findTarball('mango-cli-');
writeFileSync(
  join(consumerDirectory, 'package.json'),
  `${JSON.stringify(
    {
      name: 'mango-module-doctor-packed-consumer',
      private: true,
      type: 'module',
    },
    null,
    2,
  )}\n`,
);
writeFileSync(
  join(consumerDirectory, 'pnpm-workspace.yaml'),
  `packages:\n  - .\noverrides:\n  '@mango/pmo': file:${pmoTarball}\n`,
);
runChecked('pnpm', ['add', '--offline', '--ignore-scripts', pmoTarball, cliTarball], consumerDirectory);

const command = join(consumerDirectory, 'node_modules', '.bin', 'mango');
const moduleProjectName = 'packed-module-project';
const moduleProjectRoot = join(consumerDirectory, moduleProjectName);
runChecked(command, ['init', moduleProjectName, '--preset', 'custom', '--modules', 'none'], consumerDirectory);
const lockfileFixture = createPnpmLockfileFixture(join(temporaryRoot, 'module-add-lockfile'), [
  'packages/quality-center',
  'packages/quality-center-api',
]);
runChecked(
  command,
  [
    'module',
    'add',
    'quality-center',
    '--aggregate',
    'review-record',
    '--aggregate-name',
    '评审记录',
    '--module-name',
    '质量中心',
    '--project-dir',
    moduleProjectRoot,
  ],
  consumerDirectory,
  {
    env: {
      ...lockfileFixture.env,
      NPM_CONFIG_REGISTRY: 'http://127.0.0.1:9/unreachable-registry/',
      npm_config_registry: 'http://127.0.0.1:9/unreachable-registry/',
    },
  },
);
assertPnpmLockfileFixtureInvocations(lockfileFixture.logPath);
runChecked(command, ['pmo', 'check', '--project-dir', moduleProjectRoot, '--locked'], consumerDirectory);
const generatedModuleRoot = join(moduleProjectRoot, 'backend/modules/quality-center');
const code = readFileSync(
  join(
    generatedModuleRoot,
    'quality-center-api/src/main/java/com/example/mango/qualityCenter/api/enums/ReviewRecordCode.java',
  ),
  'utf8',
);
const service = readFileSync(
  join(
    generatedModuleRoot,
    'quality-center-core/src/main/java/com/example/mango/qualityCenter/core/service/impl/ReviewRecordService.java',
  ),
  'utf8',
);
if (!code.includes('implements BizCode') || !service.includes('Require.notNull')) {
  throw new Error('packed CLI module add did not render the published code-baseline conventions');
}
for (const root of [
  generatedModuleRoot,
  join(moduleProjectRoot, 'frontend/packages/quality-center-api'),
  join(moduleProjectRoot, 'frontend/packages/quality-center'),
]) {
  for (const file of walkFiles(root)) {
    if (/\{\{[^}]+\}\}/u.test(file) || /\{\{[^}]+\}\}/u.test(readFileSync(file, 'utf8'))) {
      throw new Error(`packed CLI module add left an unresolved placeholder: ${file}`);
    }
  }
}

const requests = [];
const backend = await startBackendServer(requests);
try {
  const result = await run(command, [
    'module',
    'doctor',
    'mango-link',
    '--app',
    'internal-admin',
    '--backend-url',
    backend.origin,
    '--frontend-url',
    'http://127.0.0.1:30001',
    '--project-dir',
    consumerDirectory,
    '--json',
  ]);
  if (result.code !== 3) {
    throw new Error(`packed CLI exit code was ${result.code}:\n${result.stdout}\n${result.stderr}`);
  }
  if (result.stderr !== '') {
    throw new Error(`packed CLI wrote stderr in JSON mode: ${result.stderr}`);
  }
  const lines = result.stdout.trim().split('\n');
  if (lines.length !== 1) {
    throw new Error(`packed CLI must write exactly one JSON line, received ${lines.length}`);
  }
  const output = JSON.parse(lines[0]);
  const frontend = output.report?.conditions?.find((condition) => condition.id === 'frontend.pageRuntime');
  if (
    output.status !== 'UNKNOWN' ||
    output.exitCode !== 3 ||
    frontend?.reasonCode !== 'PLAYWRIGHT_UNAVAILABLE' ||
    typeof frontend?.evidence?.hint !== 'string'
  ) {
    throw new Error(`unexpected packed consumer result: ${result.stdout}`);
  }
  if (existsSync(browserSentinel)) {
    throw new Error('packed CLI created or wrote the Playwright browser sentinel directory');
  }
  if (requests.length !== 1) {
    throw new Error(`packed CLI made ${requests.length} backend requests; expected exactly one`);
  }
  const request = requests[0];
  if (
    request.authorization !== `Bearer ${token}` ||
    request.module !== 'mango-link' ||
    request.app !== 'internal-admin' ||
    request.profile !== 'ADMIN_MODULE_RUNTIME_V1'
  ) {
    throw new Error(`packed CLI backend request contract mismatch: ${JSON.stringify(request)}`);
  }
} finally {
  await backend.close();
}

process.stdout.write(
  'Packed @mango/cli consumer generated a baseline module and returned UNKNOWN (exit 3) without Playwright, browser downloads, token leakage, or protocol drift.\n',
);

function pack(packageRoot) {
  runChecked('pnpm', ['pack', '--pack-destination', packDirectory], packageRoot);
}

function findTarball(prefix) {
  const name = readdirSync(packDirectory).find((file) => file.startsWith(prefix) && file.endsWith('.tgz'));
  if (!name) {
    throw new Error(`pnpm pack did not create ${prefix}*.tgz`);
  }
  return join(packDirectory, name);
}

function runChecked(command, args, cwd, options = {}) {
  const result = spawnSync(command, args, { cwd, encoding: 'utf8', env: options.env ?? process.env });
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed:\n${result.stdout}\n${result.stderr}`);
  }
}

function walkFiles(root) {
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) files.push(...walkFiles(path));
    else if (entry.isFile()) files.push(path);
  }
  return files;
}

function run(command, args) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, args, {
      cwd: consumerDirectory,
      env: {
        ...process.env,
        MANGO_DIAGNOSTIC_TOKEN: token,
        PLAYWRIGHT_BROWSERS_PATH: browserSentinel,
      },
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let stdout = '';
    let stderr = '';
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    child.stdout.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk;
    });
    child.once('error', rejectRun);
    child.once('close', (code) => resolveRun({ code, stdout, stderr }));
  });
}

function startBackendServer(requests) {
  const server = http.createServer((request, response) => {
    const url = new URL(request.url, 'http://127.0.0.1');
    requests.push({
      authorization: request.headers.authorization,
      module: url.searchParams.get('module'),
      app: url.searchParams.get('app'),
      profile: url.searchParams.get('profile'),
    });
    const pass = (id, reasonCode, evidence = {}) => ({
      id,
      status: 'PASS',
      required: true,
      reasonCode,
      evidence,
      observedAt: new Date().toISOString(),
      durationMs: 1,
      stale: false,
    });
    const body = {
      schemaVersion: 1,
      profile: 'ADMIN_MODULE_RUNTIME_V1',
      reportScope: 'INSTANCE_OBSERVATION',
      service: 'packed-consumer',
      instanceId: 'fixture',
      observedAt: new Date().toISOString(),
      modules: [
        {
          moduleCode: 'mango-link',
          status: 'UNKNOWN',
          incompleteOptional: false,
          backendVersion: {
            value: '1.0.0',
            source: 'JAR_POM_PROPERTIES',
            status: 'PASS',
            reasonCode: 'VERSION_OBSERVED',
          },
          frontendVersion: {
            value: null,
            source: 'BROWSER_REPORT',
            status: 'UNKNOWN',
            reasonCode: 'FRONTEND_REPORT_PENDING',
          },
          expectedVersion: {
            value: null,
            source: 'NONE',
            status: 'UNKNOWN',
            reasonCode: 'NO_EXPECTATION_PROVIDER',
          },
          conditions: [
            pass('installation', 'MODULE_INSTALLED'),
            pass('persistence.flyway', 'FLYWAY_APPLIED'),
            pass('resource.materialization', 'CURRENT_DECLARATIONS_APPLIED'),
            pass('authorization.menuApi', 'AUTHORIZATION_MATERIALIZED', {
              pageRequirements: ['link/items/index'],
            }),
            {
              id: 'frontend.pageRuntime',
              status: 'UNKNOWN',
              required: true,
              reasonCode: 'MISSING_CONTRIBUTOR',
              evidence: {},
              observedAt: new Date().toISOString(),
              durationMs: 0,
              stale: false,
            },
          ],
        },
      ],
    };
    response.writeHead(200, { 'content-type': 'application/json', 'cache-control': 'no-store' });
    response.end(JSON.stringify(body));
  });
  return new Promise((resolveListen, rejectListen) => {
    server.once('error', rejectListen);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      resolveListen({
        origin: `http://127.0.0.1:${address.port}`,
        close: () => new Promise((resolveClose) => server.close(resolveClose)),
      });
    });
  });
}
