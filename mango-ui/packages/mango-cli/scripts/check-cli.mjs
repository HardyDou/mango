import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-cli-'));
const fullProjectName = 'mango-full-acceptance';
const customProjectName = 'mango-custom-acceptance';

try {
  const result = spawnSync(process.execPath, [
    cli,
    'init',
    fullProjectName,
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

  const projectRoot = join(tempRoot, fullProjectName);
  const requiredFiles = [
    'mango.config.json',
    'AGENTS.md',
    '.gitignore',
    'frontend/package.json',
    'frontend/src/main.ts',
    'frontend/src/mango-admin-modular.d.ts',
    'frontend/tsconfig.app.json',
    'frontend/public/runtime-config.json',
    'backend/pom.xml',
    'backend/app/pom.xml',
    'backend/app/src/main/java/com/example/acceptance/MangoFullAcceptanceApplication.java',
    'backend/app/src/main/resources/application.yml',
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    'business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs',
    'business-pmo/mango-baseline/templates/acceptance-evidence.md',
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
  assertIncludes(config.modules.optional, 'workflow', 'full optional modules');
  assertIncludes(config.modules.optional, 'template', 'full optional modules');
  assertIncludes(config.modules.optional, 'notice', 'full optional modules');

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
  const appPom = readFileSync(join(projectRoot, 'backend/app/pom.xml'), 'utf8');
  if (!appPom.includes('<artifactId>mango-admin-starter</artifactId>') || pom.includes('{{') || appPom.includes('{{')) {
    throw new Error('backend poms were not rendered as Mango full backend');
  }
  if (pom.includes('<password>') || pom.includes('_authToken') || appPom.includes('<password>') || appPom.includes('_authToken')) {
    throw new Error('generated backend contains repository credentials');
  }

  const npmrc = readFileSync(join(projectRoot, 'frontend/.npmrc'), 'utf8');
  if (!npmrc.includes('registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/')) {
    throw new Error('generated .npmrc does not contain the configured Mango npm registry');
  }
  if (npmrc.includes('_authToken') || npmrc.includes('password') || npmrc.includes('username')) {
    throw new Error('generated .npmrc contains credentials');
  }
  assertNoUnrenderedPlaceholders(projectRoot);
  const businessAgents = readFileSync(join(projectRoot, 'AGENTS.md'), 'utf8');
  if (!businessAgents.includes('mango-cli init --preset full')) {
    throw new Error('generated full AGENTS.md should record full preset');
  }
  if (!businessAgents.includes('acceptance-evidence-check.mjs')) {
    throw new Error('generated AGENTS.md should mention acceptance evidence check');
  }
  const baselineReadme = readFileSync(join(projectRoot, 'business-pmo/mango-baseline/README.md'), 'utf8');
  if (baselineReadme.includes('7bca6b8f') || baselineReadme.includes('{{mangoBaselineCommit}}')) {
    throw new Error('generated baseline README contains stale or unrendered commit source');
  }

  const fullAddResult = spawnSync(process.execPath, [
    cli,
    'add',
    'notice',
    '--project-dir',
    projectRoot,
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (fullAddResult.status === 0 || !fullAddResult.stderr.includes('full preset already includes all optional modules')) {
    throw new Error(`add command should reject full preset:\n${fullAddResult.stdout}\n${fullAddResult.stderr}`);
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
  if (!baselinePreflight.stdout.includes('rules/frontend/04-test.md')) {
    throw new Error(`generated PMO preflight did not include frontend test rules:\n${baselinePreflight.stdout}`);
  }
  assertBusinessAcceptanceBaseline(projectRoot);

  const customResult = spawnSync(process.execPath, [
    cli,
    'init',
    customProjectName,
    '--preset',
    'custom',
    '--modules',
    'workflow-example,template',
    '--topology',
    'monolith',
    '--package',
    'com.example.custom',
    '--group-id',
    'com.example',
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (customResult.status !== 0) {
    throw new Error(`custom CLI failed:\n${customResult.stdout}\n${customResult.stderr}`);
  }

  const customRoot = join(tempRoot, customProjectName);
  const customConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  assertEqual(customConfig.preset, 'custom', 'custom preset');
  assertEqual(customConfig.modules.optional.join(','), 'workflow,workflow-example,template', 'custom modules');
  const customAgents = readFileSync(join(customRoot, 'AGENTS.md'), 'utf8');
  if (!customAgents.includes('mango-cli init --preset custom') || customAgents.includes('mango-cli init --preset full')) {
    throw new Error('generated custom AGENTS.md should record custom preset');
  }

  const customPackage = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/workflow', 'custom dependencies');
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/workflow-business-example', 'custom dependencies');
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/template', 'custom dependencies');
  for (const dependency of ['@mango/calendar', '@mango/file', '@mango/notice', '@mango/numgen']) {
    assertIncludes(Object.keys(customPackage.dependencies), dependency, 'admin optional peer dependencies');
    assertEqual(customPackage.dependencies[dependency], readWorkspacePackageVersion(dependency.replace('@mango/', '')), dependency);
  }

  const customMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (!customMain.includes("from '@mango/admin'") || customMain.includes("from '@mango/admin/full'")) {
    throw new Error('custom frontend entry should consume modular @mango/admin entry');
  }
  if (!customMain.includes('const mangoFeatures = ["workflow","template"] as const;')) {
    throw new Error('custom frontend entry should preserve literal feature types');
  }
  for (const expected of [
    "registerMangoWorkflowAdminPages",
    "registerMangoWorkflowBusinessExampleAdminPages",
    "registerMangoTemplateAdminPages",
  ]) {
    if (!customMain.includes(expected)) {
      throw new Error(`custom frontend entry missing registrar: ${expected}`);
    }
  }
  if (customMain.includes('registerMangoNoticeAdminPages')) {
    throw new Error('custom frontend entry registered unselected notice module');
  }

  const customPom = readFileSync(join(customRoot, 'backend/pom.xml'), 'utf8');
  const customAppPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  if (customAppPom.includes('<artifactId>mango-admin-starter</artifactId>')) {
    throw new Error('custom backend should not depend on full mango-admin-starter');
  }
  for (const expected of ['mango-system-starter', 'mango-workflow-starter', 'mango-template-starter']) {
    if (!customAppPom.includes(`<artifactId>${expected}</artifactId>`)) {
      throw new Error(`custom backend missing dependency: ${expected}`);
    }
  }
  if (customAppPom.includes('<artifactId>mango-notice-starter</artifactId>')) {
    throw new Error('custom backend added unselected notice dependency');
  }
  const businessReadmePath = join(customRoot, 'README.md');
  const businessReadmeBeforeAdd = '# business-owned readme\n';
  writeFileSync(businessReadmePath, businessReadmeBeforeAdd);
  const customPackageBeforeAdd = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  customPackageBeforeAdd.dependencies['business-owned-package'] = '1.2.3';
  writeFileSync(join(customRoot, 'frontend/package.json'), `${JSON.stringify(customPackageBeforeAdd, null, 2)}\n`);
  writeFileSync(
    join(customRoot, 'frontend/src/main.ts'),
    `${customMain}\nconsole.info('business-owned bootstrap hook');\n`,
  );
  writeFileSync(
    join(customRoot, 'backend/app/pom.xml'),
    customAppPom.replace(
      '        <dependency>\n            <groupId>org.springframework.boot</groupId>\n            <artifactId>spring-boot-starter-actuator</artifactId>\n        </dependency>',
      [
        '        <dependency>',
        '            <groupId>com.example</groupId>',
        '            <artifactId>business-owned-starter</artifactId>',
        '            <version>1.2.3</version>',
        '        </dependency>',
        '        <dependency>',
        '            <groupId>org.springframework.boot</groupId>',
        '            <artifactId>spring-boot-starter-actuator</artifactId>',
        '        </dependency>',
      ].join('\n'),
    ),
  );

  const addResult = spawnSync(process.execPath, [
    cli,
    'add',
    'notice',
    '--project-dir',
    customRoot,
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (addResult.status !== 0) {
    throw new Error(`add command failed:\n${addResult.stdout}\n${addResult.stderr}`);
  }
  const addedConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  assertEqual(addedConfig.modules.optional.join(','), 'workflow,workflow-example,template,notice', 'modules after add');
  const addedPackage = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  assertIncludes(Object.keys(addedPackage.dependencies), '@mango/notice', 'dependencies after add');
  assertEqual(addedPackage.dependencies['@mango/file'], readWorkspacePackageVersion('file'), 'file peer dependency after add');
  assertEqual(addedPackage.dependencies['business-owned-package'], '1.2.3', 'business dependency after add');
  const addedMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (!addedMain.includes('registerMangoNoticeAdminPages') || !addedMain.includes('registerMangoNoticeAdminShell')) {
    throw new Error('add command did not update notice frontend registrars');
  }
  if (!addedMain.includes("console.info('business-owned bootstrap hook');")) {
    throw new Error('add command overwrote business-owned frontend entry content');
  }
  const addedPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  if (!addedPom.includes('<artifactId>mango-notice-starter</artifactId>')) {
    throw new Error('add command did not update notice backend dependency');
  }
  if (!addedPom.includes('<artifactId>business-owned-starter</artifactId>')) {
    throw new Error('add command overwrote business-owned backend dependency');
  }
  assertEqual(readFileSync(businessReadmePath, 'utf8'), businessReadmeBeforeAdd, 'business-owned file after add');
  assertNoUnrenderedPlaceholders(customRoot);

  const moduleAddResult = spawnSync(process.execPath, [
    cli,
    'module',
    'add',
    'contract',
    '--aggregate',
    'seal',
    '--module-name',
    '合同管理',
    '--project-dir',
    customRoot,
  ], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (moduleAddResult.status !== 0) {
    throw new Error(`module add command failed:\n${moduleAddResult.stdout}\n${moduleAddResult.stderr}`);
  }
  for (const file of [
    'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/command/UpdateSealCommand.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/entity/SealEntity.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/mapper/SealMapper.java',
    'backend/modules/contract/contract-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    'frontend/packages/contract-api/src/api.ts',
    'frontend/packages/contract/src/index.ts',
    'frontend/packages/contract/src/views/contract/seal/index.vue',
  ]) {
    if (!existsSync(join(customRoot, file))) {
      throw new Error(`module add missing generated file: ${file}`);
    }
  }
  const modulePom = readFileSync(join(customRoot, 'backend/pom.xml'), 'utf8');
  const moduleAppPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  const moduleApplicationYml = readFileSync(join(customRoot, 'backend/app/src/main/resources/application.yml'), 'utf8');
  if (!modulePom.includes('<module>modules/contract</module>')) {
    throw new Error('module add did not register backend module');
  }
  if (!modulePom.includes('<artifactId>mango-infra-persistence-starter</artifactId>')
    || !modulePom.includes('<artifactId>mango-infra-feign-starter</artifactId>')
    || !modulePom.includes('<artifactId>swagger-annotations</artifactId>')) {
    throw new Error('module add did not provide business backend dependency management');
  }
  if (!moduleAppPom.includes('<artifactId>contract-starter</artifactId>')) {
    throw new Error('module add did not register app dependency');
  }
  if (!moduleApplicationYml.includes('        contract:\n          enabled: true')) {
    throw new Error('module add did not enable business Flyway migration');
  }
  const moduleController = readFileSync(
    join(customRoot, 'backend/modules/contract/contract-starter/src/main/java/com/example/custom/contract/starter/controller/ContractController.java'),
    'utf8',
  );
  if (!moduleController.includes('extends BaseCrudController') || !moduleController.includes('@RequestMapping("/contract/seals")')) {
    throw new Error('module add did not generate standard CRUD controller');
  }
  const moduleApi = readFileSync(join(customRoot, 'frontend/packages/contract-api/src/api.ts'), 'utf8');
  if (!moduleApi.includes('`${basePath}/create`') || !moduleApi.includes('`${basePath}/page`')) {
    throw new Error('module add did not generate standard CRUD frontend API');
  }
  const moduleMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (!moduleMain.includes("from '@mango-custom-acceptance/contract'") || !moduleMain.includes('registerContractPages();')) {
    throw new Error('module add did not register frontend pages');
  }
  const moduleConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  if (!Array.isArray(moduleConfig.businessModules) || moduleConfig.businessModules[0]?.module !== 'contract') {
    throw new Error('module add did not update mango.config.json businessModules');
  }
  const modulePackageJson = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  if (!Array.isArray(modulePackageJson.workspaces) || !modulePackageJson.workspaces.includes('packages/*')) {
    throw new Error('module add did not configure frontend workspaces');
  }
  assertNoUnrenderedPlaceholders(customRoot);

  console.log('mango-cli full/custom/add/module checks passed.');
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function assertEqual(actual, expected, field) {
  if (actual !== expected) {
    throw new Error(`${field} expected ${expected}, got ${actual}`);
  }
}

function assertIncludes(values, expected, field) {
  if (!values.includes(expected)) {
    throw new Error(`${field} expected to include ${expected}`);
  }
}

function assertNotIncludes(values, unexpected, field) {
  if (values.includes(unexpected)) {
    throw new Error(`${field} should not include ${unexpected}`);
  }
}

function readWorkspacePackageVersion(packageName) {
  const packageJson = JSON.parse(readFileSync(resolve(packageRoot, `../${packageName}/package.json`), 'utf8'));
  return packageJson.version;
}

function assertBusinessAcceptanceBaseline(projectRoot) {
  const baselineRoot = join(projectRoot, 'business-pmo/mango-baseline');
  const frontendTestRule = readFileSync(join(baselineRoot, 'rules/frontend/04-test.md'), 'utf8');
  for (const expected of [
    '不能只验证接口 200',
    '业务结果断言',
    'UI 细节断言',
    'console error',
    '截图或 trace/video 路径',
    'acceptance-evidence-check.mjs',
  ]) {
    if (!frontendTestRule.includes(expected)) {
      throw new Error(`generated frontend test rule missing acceptance requirement: ${expected}`);
    }
  }

  const qaAgent = readFileSync(join(baselineRoot, 'agents/04-qa-agent.md'), 'utf8');
  for (const expected of [
    'frontend/04-test.md',
    'frontend/02-element-plus-ui.md',
    '不能只写接口 200',
  ]) {
    if (!qaAgent.includes(expected)) {
      throw new Error(`generated QA agent missing acceptance requirement: ${expected}`);
    }
  }

  const deliveryTemplate = readFileSync(join(baselineRoot, 'templates/delivery-contract.md'), 'utf8');
  const evidenceTemplate = readFileSync(join(baselineRoot, 'templates/acceptance-evidence.md'), 'utf8');
  for (const expected of [
    '功能点',
    '关键断言',
    'UI/交互检查',
    'console/network 结果',
    '截图/trace/日志',
  ]) {
    if (!deliveryTemplate.includes(expected) || !evidenceTemplate.includes(expected)) {
      throw new Error(`generated acceptance templates missing evidence column: ${expected}`);
    }
  }

  const validEvidencePath = join(projectRoot, 'acceptance-valid.md');
  writeFileSync(validEvidencePath, [
    '# acceptance',
    '',
    '| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |',
    '|---|---|---|---|---|---|---|---|---|',
    '| TASK-001 | /demo | 新增业务记录 | name=acceptance | 列表新增一行且详情字段回显正确 | 主按钮位置正确，表单分组和必填校验可见 | 无 console error，无资源 404，无接口 4xx/5xx | artifacts/demo.png | DONE |',
    '',
  ].join('\n'));
  const evidenceCheck = spawnSync(process.execPath, [
    'business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs',
    '--evidence',
    validEvidencePath,
  ], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (evidenceCheck.status !== 0) {
    throw new Error(`generated acceptance evidence check failed:\n${evidenceCheck.stdout}\n${evidenceCheck.stderr}`);
  }

  const weakEvidencePath = join(projectRoot, 'acceptance-weak.md');
  writeFileSync(weakEvidencePath, [
    '# weak acceptance',
    '',
    '| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |',
    '|---|---|---|---|---|---|---|---|---|',
    '| TASK-001 | /demo | 新增 | - | 接口 200 | 页面无异常 | 无报错 | 截图正常 | DONE |',
    '',
  ].join('\n'));
  const weakEvidenceCheck = spawnSync(process.execPath, [
    'business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs',
    '--evidence',
    weakEvidencePath,
  ], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (weakEvidenceCheck.status === 0 || !weakEvidenceCheck.stdout.includes('weak acceptance wording')) {
    throw new Error(`generated acceptance evidence check should reject weak evidence:\n${weakEvidenceCheck.stdout}\n${weakEvidenceCheck.stderr}`);
  }
}

function assertNoUnrenderedPlaceholders(projectRoot) {
  const result = spawnSync('rg', ['-n', '\\{\\{', projectRoot], {
    encoding: 'utf8',
  });
  if (result.status === 0) {
    throw new Error(`generated project contains unrendered placeholders:\n${result.stdout}`);
  }
  if (result.status !== 1) {
    throw new Error(`placeholder scan failed:\n${result.stdout}\n${result.stderr}`);
  }
}
