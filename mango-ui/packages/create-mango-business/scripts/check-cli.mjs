import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const template = resolve(packageRoot, '../../../mango-business-template');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-business-'));
const projectName = 'guarantee-platform';

const result = spawnSync(process.execPath, [
  cli,
  'init',
  projectName,
  '--module',
  'guarantee',
  '--aggregate',
  'letter',
  '--package',
  'com.example.guarantee',
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
  if (result.status !== 0) {
    throw new Error(`CLI failed:\n${result.stdout}\n${result.stderr}`);
  }

  const projectRoot = join(tempRoot, projectName);
  const requiredFiles = [
    'mango.config.json',
    'AGENTS.md',
    'backend/modules/guarantee/guarantee-api/src/main/java/com/example/guarantee/guarantee/api/GuaranteeApi.java',
    'backend/modules/guarantee/guarantee-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    'frontend/packages/guarantee/src/index.ts',
    'frontend/apps/guarantee-platform-admin/src/main.ts',
    'topologies/microservice/README.md',
  ];

  for (const file of requiredFiles) {
    if (!existsSync(join(projectRoot, file))) {
      throw new Error(`missing generated file: ${file}`);
    }
  }

  const config = JSON.parse(readFileSync(join(projectRoot, 'mango.config.json'), 'utf8'));
  assertEqual(config.topology, 'microservice', 'topology');
  assertEqual(config.module, 'guarantee', 'module');
  assertEqual(config.aggregate, 'letter', 'aggregate');

  const apiContent = readFileSync(
    join(projectRoot, 'backend/modules/guarantee/guarantee-api/src/main/java/com/example/guarantee/guarantee/api/GuaranteeApi.java'),
    'utf8'
  );
  if (!apiContent.includes('R<LetterVO>') || apiContent.includes('{{')) {
    throw new Error('API template was not rendered correctly');
  }

  const pageRegistry = readFileSync(join(projectRoot, 'frontend/packages/guarantee/src/index.ts'), 'utf8');
  if (!pageRegistry.includes("'guarantee/letter/index'") || pageRegistry.includes('{{')) {
    throw new Error('page registry was not rendered correctly');
  }

  const templateCheck = spawnSync(process.execPath, ['scripts/check-template.mjs'], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (templateCheck.status !== 0) {
    throw new Error(`generated project template check failed:\n${templateCheck.stdout}\n${templateCheck.stderr}`);
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
