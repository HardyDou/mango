import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const template = resolve(packageRoot, '../../../mango-business-starter');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-app-'));
const projectName = 'contract-platform';

const explicitTemplateResult = spawnSync(process.execPath, [
  cli,
  'init',
  projectName,
  '--module',
  'contract',
  '--aggregate',
  'archive',
  '--package',
  'com.example.contract',
  '--group-id',
  'com.example',
  '--topology',
  'microservice',
  '--template',
  template,
], {
  cwd: tempRoot,
  encoding: 'utf8',
});

try {
  if (explicitTemplateResult.status !== 0) {
    throw new Error(`CLI failed:\n${explicitTemplateResult.stdout}\n${explicitTemplateResult.stderr}`);
  }

  const projectRoot = join(tempRoot, projectName);
  const requiredFiles = [
    'mango.config.json',
    'AGENTS.md',
    'backend/modules/contract/contract-api/src/main/java/com/example/contract/contract/api/ContractApi.java',
    'backend/modules/contract/contract-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    'frontend/packages/contract/src/index.ts',
    'frontend/apps/contract-platform-admin/src/main.ts',
    'topologies/microservice/README.md',
  ];

  for (const file of requiredFiles) {
    if (!existsSync(join(projectRoot, file))) {
      throw new Error(`missing generated file: ${file}`);
    }
  }

  const config = JSON.parse(readFileSync(join(projectRoot, 'mango.config.json'), 'utf8'));
  assertEqual(config.topology, 'microservice', 'topology');
  assertEqual(config.module, 'contract', 'module');
  assertEqual(config.aggregate, 'archive', 'aggregate');

  const apiContent = readFileSync(
    join(projectRoot, 'backend/modules/contract/contract-api/src/main/java/com/example/contract/contract/api/ContractApi.java'),
    'utf8'
  );
  if (
    !apiContent.includes('CreateArchiveCommand')
    || !apiContent.includes('@PostMapping("/archives/create")')
    || apiContent.includes('{{')
  ) {
    throw new Error('API template was not rendered correctly');
  }

  const pageRegistry = readFileSync(join(projectRoot, 'frontend/packages/contract/src/index.ts'), 'utf8');
  if (!pageRegistry.includes("'contract/archive/index'") || pageRegistry.includes('{{')) {
    throw new Error('page registry was not rendered correctly');
  }

  const templateCheck = spawnSync(process.execPath, ['scripts/check-template.mjs'], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (templateCheck.status !== 0) {
    throw new Error(`generated project template check failed:\n${templateCheck.stdout}\n${templateCheck.stderr}`);
  }

  const baselinePreflight = spawnSync(process.execPath, [
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    '--role',
    'dev',
    '--phase',
    'develop',
    '--task',
    '新增业务模块',
    '--paths',
    'backend/modules/contract,frontend/packages/contract',
  ], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (baselinePreflight.status !== 0) {
    throw new Error(`generated project baseline preflight failed:\n${baselinePreflight.stdout}\n${baselinePreflight.stderr}`);
  }
  if (!baselinePreflight.stdout.includes('rules/backend/03-api.md') || !baselinePreflight.stdout.includes('rules/frontend/01-vue-code.md')) {
    throw new Error(`generated project baseline preflight did not include backend and frontend rules:\n${baselinePreflight.stdout}`);
  }

  const packagedProjectName = 'packaged-platform';
  const packagedTemplateResult = spawnSync(process.execPath, [
    cli,
    'init',
    packagedProjectName,
    '--module',
    'packaged',
    '--aggregate',
    'order',
    '--package',
    'com.example.packaged',
    '--force',
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (packagedTemplateResult.status !== 0) {
    throw new Error(`CLI packaged template failed:\n${packagedTemplateResult.stdout}\n${packagedTemplateResult.stderr}`);
  }
  const packagedProjectRoot = join(tempRoot, packagedProjectName);
  if (!existsSync(join(packagedProjectRoot, 'business-pmo/mango-baseline/tools/pmo-preflight.mjs'))) {
    throw new Error('packaged template did not generate Mango baseline preflight');
  }

  console.log('Initializr CLI check passed.');
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function assertEqual(actual, expected, field) {
  if (actual !== expected) {
    throw new Error(`${field} expected ${expected}, got ${actual}`);
  }
}
