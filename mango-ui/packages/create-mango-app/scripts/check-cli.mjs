import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const template = resolve(packageRoot, '../../../mango-business-starter');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-app-'));
const projectName = 'guarantee-platform';

const explicitTemplateResult = spawnSync(process.execPath, [
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
  '--features',
  'base,system,workflow,file',
  '--frontend-mode',
  'mixed',
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
    'package.json',
    'pnpm-workspace.yaml',
    'tsconfig.base.json',
    '.npmrc',
    'backend/pom.xml',
    'backend/apps/guarantee-platform-monolith-app/pom.xml',
    'backend/apps/guarantee-platform-monolith-app/src/main/java/com/example/guarantee/app/monolith/GuaranteePlatformMonolithApplication.java',
    'backend/apps/guarantee-platform-monolith-app/src/main/resources/application.yml',
    'backend/modules/guarantee/guarantee-core/src/main/java/com/example/guarantee/guarantee/core/mapper/LetterMapper.java',
    'backend/modules/guarantee/guarantee-api/src/main/java/com/example/guarantee/guarantee/api/GuaranteeApi.java',
    'backend/modules/guarantee/guarantee-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    'frontend/packages/guarantee/src/index.ts',
    'frontend/apps/guarantee-platform-admin/.env.example',
    'frontend/apps/guarantee-platform-admin/public/mango-runtime-config.json',
    'frontend/apps/guarantee-platform-admin/src/main.ts',
    'frontend/apps/guarantee-platform-admin/src/runtimeConfig.ts',
    'frontend/apps/guarantee-platform-admin/src/starterMenus.ts',
    'frontend/apps/guarantee-platform-admin/src/vite-env.d.ts',
    'scripts/dev-start.sh',
    'scripts/dev-stop.sh',
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
  assertArrayEqual(config.features, ['base', 'system', 'workflow', 'file'], 'features');
  assertEqual(config.frontend.app, 'guarantee-platform-admin', 'frontend.app');
  assertEqual(config.frontend.defaultMode, 'mixed', 'frontend.defaultMode');
  if (!explicitTemplateResult.stdout.includes('pnpm typecheck') || !explicitTemplateResult.stdout.includes('Frontend mode: mixed; features: base,system,workflow,file')) {
    throw new Error(`CLI next steps did not include validation and selection summary:\n${explicitTemplateResult.stdout}`);
  }

  const backendPom = readFileSync(join(projectRoot, 'backend/pom.xml'), 'utf8');
  if (!backendPom.includes('<artifactId>guarantee-platform-backend</artifactId>') || !backendPom.includes('<module>modules/guarantee</module>') || !backendPom.includes('<module>apps/guarantee-platform-monolith-app</module>') || backendPom.includes('{{')) {
    throw new Error('backend root pom was not rendered correctly');
  }
  if (!backendPom.includes('<artifactId>mango-infra-persistence-starter</artifactId>') || backendPom.includes('mybatis-plus-spring-boot3-starter') || backendPom.includes('mybatis-plus-jsqlparser')) {
    throw new Error('backend root pom must use infra-persistence instead of direct MyBatis-Plus starters');
  }

  const modulePom = readFileSync(join(projectRoot, 'backend/modules/guarantee/pom.xml'), 'utf8');
  if (!modulePom.includes('<artifactId>guarantee-platform-backend</artifactId>') || !modulePom.includes('<relativePath>../../pom.xml</relativePath>') || modulePom.includes('{{')) {
    throw new Error('backend module pom parent was not rendered correctly');
  }

  const monolithPom = readFileSync(join(projectRoot, 'backend/apps/guarantee-platform-monolith-app/pom.xml'), 'utf8');
  if (!monolithPom.includes('<artifactId>guarantee-platform-monolith-app</artifactId>') || !monolithPom.includes('<artifactId>mango-admin-starter</artifactId>') || !monolithPom.includes('<artifactId>guarantee-starter</artifactId>') || !monolithPom.includes('<mainClass>com.example.guarantee.app.monolith.GuaranteePlatformMonolithApplication</mainClass>') || monolithPom.includes('{{')) {
    throw new Error('monolith app pom was not rendered correctly');
  }

  const monolithApplication = readFileSync(join(projectRoot, 'backend/apps/guarantee-platform-monolith-app/src/main/java/com/example/guarantee/app/monolith/GuaranteePlatformMonolithApplication.java'), 'utf8');
  if (!monolithApplication.includes('package com.example.guarantee.app.monolith;') || !monolithApplication.includes('@SpringBootApplication(scanBasePackages = "com.example.guarantee")') || monolithApplication.includes('io.mango') || monolithApplication.includes('{{')) {
    throw new Error('monolith application was not rendered correctly');
  }

  const monolithConfig = readFileSync(join(projectRoot, 'backend/apps/guarantee-platform-monolith-app/src/main/resources/application.yml'), 'utf8');
  if (!monolithConfig.includes('name: guarantee-platform-monolith-app') || !monolithConfig.includes('jdbc:mysql://127.0.0.1:3306/guarantee') || !monolithConfig.includes('packages-to-scan: com.example.guarantee,io.mango') || monolithConfig.includes('{{')) {
    throw new Error('monolith application config was not rendered correctly');
  }

  const letterMapper = readFileSync(join(projectRoot, 'backend/modules/guarantee/guarantee-core/src/main/java/com/example/guarantee/guarantee/core/mapper/LetterMapper.java'), 'utf8');
  if (!letterMapper.includes('extends BaseMapper<LetterEntity>') || letterMapper.includes('{{')) {
    throw new Error('business mapper template was not rendered correctly');
  }

  const letterService = readFileSync(join(projectRoot, 'backend/modules/guarantee/guarantee-core/src/main/java/com/example/guarantee/guarantee/core/service/impl/LetterService.java'), 'utf8');
  if (!letterService.includes('LetterMapper letterMapper') || !letterService.includes('letterMapper.insert(entity)') || !letterService.includes('letterMapper.selectPage') || !letterService.includes('LetterEntity::getCreatedAt') || letterService.includes('IdWorker.getIdStr()') || letterService.includes('replace-with-generated-id') || letterService.includes('{{')) {
    throw new Error('business service persistence template was not rendered correctly');
  }

  const letterEntity = readFileSync(join(projectRoot, 'backend/modules/guarantee/guarantee-core/src/main/java/com/example/guarantee/guarantee/core/entity/LetterEntity.java'), 'utf8');
  if (!letterEntity.includes('extends TenantEntity') || !letterEntity.includes('io.mango.infra.persistence.api.entity.TenantEntity') || letterEntity.includes('private String id') || letterEntity.includes('{{')) {
    throw new Error('business entity must use infra-persistence TenantEntity');
  }

  const letterMigration = readFileSync(join(projectRoot, 'backend/modules/guarantee/guarantee-core/src/main/resources/db/migration/guarantee/V1__init_guarantee.sql'), 'utf8');
  if (!letterMigration.includes('id BIGINT NOT NULL') || !letterMigration.includes('created_by BIGINT NULL') || !letterMigration.includes('created_at DATETIME NULL') || !letterMigration.includes('updated_by BIGINT NULL') || !letterMigration.includes('updated_at DATETIME NULL') || /^\s*id\s+VARCHAR/im.test(letterMigration)) {
    throw new Error('business migration must use Mango persistence baseline columns');
  }

  const autoConfiguration = readFileSync(join(projectRoot, 'backend/modules/guarantee/guarantee-starter/src/main/java/com/example/guarantee/guarantee/starter/GuaranteeAutoConfiguration.java'), 'utf8');
  if (!autoConfiguration.includes('@MapperScan("com.example.guarantee.guarantee.core.mapper")') || autoConfiguration.includes('{{')) {
    throw new Error('starter mapper scan template was not rendered correctly');
  }

  const devStart = readFileSync(join(projectRoot, 'scripts/dev-start.sh'), 'utf8');
  if (!devStart.includes('mvn -pl "apps/guarantee-platform-monolith-app" -am -DskipTests package') || !devStart.includes('java -jar "${backend_jar}"') || !devStart.includes('pnpm --filter "guarantee-platform-admin" dev') || !devStart.includes('MANGO_DB_AUTO_CREATE="${MANGO_DB_AUTO_CREATE:-true}"') || !devStart.includes('/actuator/health') || devStart.includes('{{')) {
    throw new Error('dev-start script was not rendered correctly');
  }

  const viteConfig = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/vite.config.ts'), 'utf8');
  if (!viteConfig.includes('loadEnv') || !viteConfig.includes("'/api'") || !viteConfig.includes('VITE_ADMIN_PROXY_PATH') || viteConfig.includes('{{')) {
    throw new Error('admin app vite proxy template was not rendered correctly');
  }

  const apiContent = readFileSync(
    join(projectRoot, 'backend/modules/guarantee/guarantee-api/src/main/java/com/example/guarantee/guarantee/api/GuaranteeApi.java'),
    'utf8'
  );
  if (!apiContent.includes('R<LetterVO>') || apiContent.includes('{{')) {
    throw new Error('API template was not rendered correctly');
  }

  const starterImports = readFileSync(
    join(projectRoot, 'backend/modules/guarantee/guarantee-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'),
    'utf8'
  );
  const remoteStarterImports = readFileSync(
    join(projectRoot, 'backend/modules/guarantee/guarantee-starter-remote/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'),
    'utf8'
  );
  if (!starterImports.includes('com.example.guarantee.guarantee.starter.GuaranteeAutoConfiguration') || starterImports.includes('{{')) {
    throw new Error('starter auto-configuration imports were not rendered correctly');
  }
  if (!remoteStarterImports.includes('com.example.guarantee.guarantee.starter.remote.GuaranteeRemoteAutoConfiguration') || remoteStarterImports.includes('{{')) {
    throw new Error('remote starter auto-configuration imports were not rendered correctly');
  }

  const pageRegistry = readFileSync(join(projectRoot, 'frontend/packages/guarantee/src/index.ts'), 'utf8');
  if (!pageRegistry.includes("'guarantee/letter/index'") || pageRegistry.includes('{{')) {
    throw new Error('page registry was not rendered correctly');
  }

  const businessPage = readFileSync(join(projectRoot, 'frontend/packages/guarantee/src/views/guarantee/letter/index.vue'), 'utf8');
  if (!businessPage.includes('data-mango-layout="search"') || !businessPage.includes('data-mango-layout="actions"') || !businessPage.includes('data-mango-layout="table"') || !businessPage.includes('data-mango-layout="pagination"') || !businessPage.includes('el-pagination') || businessPage.includes('{{')) {
    throw new Error('business page layout template was not rendered correctly');
  }

  const appBootstrap = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/src/main.ts'), 'utf8');
  if (!appBootstrap.includes('registerDefaultAdminPages') || !appBootstrap.includes('mangoWorkflowCapability') || !appBootstrap.includes('mangoFileCapability') || !appBootstrap.includes("allowHttpEntries: import.meta.env.DEV || import.meta.env.VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES === 'true'") || !appBootstrap.includes("allowedEntryOrigins: ['http://127.0.0.1:5190', 'http://localhost:5190']") || appBootstrap.includes('{{')) {
    throw new Error('admin app bootstrap was not rendered correctly');
  }

  const runtimeConfig = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/src/runtimeConfig.ts'), 'utf8');
  if (!runtimeConfig.includes("'mango-workflow'") || !runtimeConfig.includes("appType: 'MICRO_APP'") || !runtimeConfig.includes("runtimeCode: 'mango-workflow-micro'") || !runtimeConfig.includes("profile: (import.meta.env.VITE_MANGO_RUNTIME_PROFILE || 'hybrid'") || runtimeConfig.includes('{{')) {
    throw new Error('runtime config template was not rendered correctly');
  }

  const appPackage = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/package.json'), 'utf8');
  if (!appPackage.includes('"@mango/workflow"') || !appPackage.includes('"@mango/file"') || appPackage.includes('"@mango/notice"') || appPackage.includes('{{')) {
    throw new Error('feature dependencies were not rendered correctly');
  }

  const publicRuntimeConfig = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/public/mango-runtime-config.json'), 'utf8');
  if (!publicRuntimeConfig.includes('"profile": "hybrid"') || !publicRuntimeConfig.includes('"mango-workflow"') || !publicRuntimeConfig.includes('"entry": "http://127.0.0.1:5190/"') || !publicRuntimeConfig.includes('"runtimeCode": "mango-workflow-micro"') || publicRuntimeConfig.includes('{{')) {
    throw new Error('public runtime config was not rendered correctly');
  }

  const envExample = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/.env.example'), 'utf8');
  if (!envExample.includes('VITE_APP_TITLE=GuaranteePlatform Admin') || !envExample.includes('VITE_MANGO_RUNTIME_PROFILE=hybrid') || !envExample.includes('VITE_GUARANTEE_MODE=micro') || !envExample.includes('VITE_WORKFLOW_MODE=micro') || !envExample.includes('VITE_SYSTEM_MODE=local') || !envExample.includes('VITE_WORKFLOW_ENTRY=http://127.0.0.1:5190/') || envExample.includes('{{')) {
    throw new Error('env example template was not rendered correctly');
  }

  const starterMenus = readFileSync(join(projectRoot, 'frontend/apps/guarantee-platform-admin/src/starterMenus.ts'), 'utf8');
  if (!starterMenus.includes("component: 'guarantee/letter/index'") || starterMenus.includes('{{')) {
    throw new Error('starter menu template was not rendered correctly');
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
    'backend/modules/guarantee,frontend/packages/guarantee',
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

  const generatedPersistenceRule = readFileSync(join(projectRoot, 'business-pmo/mango-baseline/rules/backend/07-persistence.md'), 'utf8');
  if (!generatedPersistenceRule.includes('mango-infra-persistence-starter') || !generatedPersistenceRule.includes('禁止直接依赖 `JdbcTemplate`') || !generatedPersistenceRule.includes('禁止直接声明 MyBatis-Plus starter 依赖')) {
    throw new Error('generated baseline persistence rule was not rendered correctly');
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

function assertArrayEqual(actual, expected, field) {
  if (!Array.isArray(actual) || actual.length !== expected.length || actual.some((item, index) => item !== expected[index])) {
    throw new Error(`${field} expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}
