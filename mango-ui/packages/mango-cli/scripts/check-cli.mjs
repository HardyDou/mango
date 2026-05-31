import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-cli-'));
const projectName = 'mango-full-acceptance';

try {
  const result = spawnSync(process.execPath, [
    cli,
    'init',
    projectName,
    '--preset',
    'full',
    '--topology',
    'monolith',
    '--package',
    'com.example.acceptance',
    '--group-id',
    'com.example',
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });

  if (result.status !== 0) {
    throw new Error(`CLI failed:\n${result.stdout}\n${result.stderr}`);
  }

  const projectRoot = join(tempRoot, projectName);
  const requiredFiles = [
    'mango.config.json',
    'AGENTS.md',
    '.gitignore',
    'frontend/package.json',
    'frontend/src/main.ts',
    'frontend/public/runtime-config.json',
    'backend/pom.xml',
    'backend/src/main/java/com/example/acceptance/MangoFullAcceptanceApplication.java',
    'backend/src/main/resources/application.yml',
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    'topologies/monolith/README.md',
    'topologies/microservice/README.md',
  ];

  for (const file of requiredFiles) {
    if (!existsSync(join(projectRoot, file))) {
      throw new Error(`missing generated file: ${file}`);
    }
  }

  const config = JSON.parse(readFileSync(join(projectRoot, 'mango.config.json'), 'utf8'));
  assertEqual(config.preset, 'full', 'preset');
  assertEqual(config.topology, 'monolith', 'topology');
  assertEqual(config.basePackage, 'com.example.acceptance', 'basePackage');

  const mainTs = readFileSync(join(projectRoot, 'frontend/src/main.ts'), 'utf8');
  if (!mainTs.includes("from '@mango/admin/full'") || !mainTs.includes("import '@mango/admin/style-full.css'")) {
    throw new Error('frontend entry does not consume @mango/admin/full');
  }
  if (mainTs.includes('{{')) {
    throw new Error('frontend entry contains unrendered placeholders');
  }

  const frontendPackage = JSON.parse(readFileSync(join(projectRoot, 'frontend/package.json'), 'utf8'));
  for (const dependency of ['@mango/admin', '@mango/file', '@mango/workflow', '@mango/template', '@mango/notice']) {
    if (!frontendPackage.dependencies[dependency]) {
      throw new Error(`frontend package missing dependency: ${dependency}`);
    }
  }
  const expectedVersions = {
    '@mango/admin': readWorkspacePackageVersion('admin'),
    '@mango/calendar': readWorkspacePackageVersion('calendar'),
    '@mango/file': readWorkspacePackageVersion('file'),
    '@mango/notice': readWorkspacePackageVersion('notice'),
    '@mango/numgen': readWorkspacePackageVersion('numgen'),
    '@mango/template': readWorkspacePackageVersion('template'),
    '@mango/workflow': readWorkspacePackageVersion('workflow'),
    '@mango/workflow-business-example': readWorkspacePackageVersion('workflow-business-example'),
  };
  for (const [dependency, expectedVersion] of Object.entries(expectedVersions)) {
    assertEqual(frontendPackage.dependencies[dependency], expectedVersion, dependency);
  }

  const pom = readFileSync(join(projectRoot, 'backend/pom.xml'), 'utf8');
  if (!pom.includes('<artifactId>mango-admin-starter</artifactId>') || pom.includes('{{')) {
    throw new Error('backend pom was not rendered as Mango full backend');
  }
  if (pom.includes('<password>') || pom.includes('_authToken')) {
    throw new Error('generated backend contains repository credentials');
  }

  const npmrc = readFileSync(join(projectRoot, 'frontend/.npmrc'), 'utf8');
  if (!npmrc.includes('registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/')) {
    throw new Error('generated .npmrc does not contain the configured Mango npm registry');
  }
  if (npmrc.includes('_authToken') || npmrc.includes('password') || npmrc.includes('username')) {
    throw new Error('generated .npmrc contains credentials');
  }

  const baselinePreflight = spawnSync(process.execPath, [
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    '--role',
    'dev',
    '--phase',
    'develop',
    '--task',
    '基于 Mango full preset 开发业务项目',
    '--paths',
    'frontend,backend',
  ], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (baselinePreflight.status !== 0) {
    throw new Error(`generated PMO preflight failed:\n${baselinePreflight.stdout}\n${baselinePreflight.stderr}`);
  }
  if (!baselinePreflight.stdout.includes('rules/frontend/01-vue-code.md')) {
    throw new Error(`generated PMO preflight did not include frontend rules:\n${baselinePreflight.stdout}`);
  }

  console.log('mango-cli full preset check passed.');
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function assertEqual(actual, expected, field) {
  if (actual !== expected) {
    throw new Error(`${field} expected ${expected}, got ${actual}`);
  }
}

function readWorkspacePackageVersion(packageName) {
  const packageJson = JSON.parse(readFileSync(resolve(packageRoot, `../${packageName}/package.json`), 'utf8'));
  return packageJson.version;
}
