import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const repoRoot = new URL('../..', import.meta.url).pathname;

const requiredFiles = [
  'README.md',
  'AGENTS.md',
  '.github/CODEOWNERS',
  'business-pmo/README.md',
  'business-pmo/mango-baseline/README.md',
  'business-pmo/mango-baseline/agents/01-pm-agent.md',
  'business-pmo/mango-baseline/agents/02-tech-lead-agent.md',
  'business-pmo/mango-baseline/agents/03-dev-agent.md',
  'business-pmo/mango-baseline/agents/04-qa-agent.md',
  'business-pmo/mango-baseline/agents/05-pmo-agent.md',
  'business-pmo/mango-baseline/rules/index.json',
  'business-pmo/mango-baseline/rules/00-dev-flow.md',
  'business-pmo/mango-baseline/rules/01-delivery-contract.md',
  'business-pmo/mango-baseline/rules/02-dev-environment.md',
  'business-pmo/mango-baseline/rules/03-ai-coding-redlines.md',
  'business-pmo/mango-baseline/rules/backend/01-code.md',
  'business-pmo/mango-baseline/rules/backend/02-naming.md',
  'business-pmo/mango-baseline/rules/backend/03-api.md',
  'business-pmo/mango-baseline/rules/backend/04-db.md',
  'business-pmo/mango-baseline/rules/backend/05-module.md',
  'business-pmo/mango-baseline/rules/backend/06-security.md',
  'business-pmo/mango-baseline/rules/backend/07-persistence.md',
  'business-pmo/mango-baseline/rules/backend/08-test.md',
  'business-pmo/mango-baseline/rules/backend/09-versioning.md',
  'business-pmo/mango-baseline/rules/backend/10-dev-flow.md',
  'business-pmo/mango-baseline/rules/frontend/01-vue-code.md',
  'business-pmo/mango-baseline/rules/frontend/02-element-plus-ui.md',
  'business-pmo/mango-baseline/rules/frontend/03-component-development.md',
  'business-pmo/mango-baseline/rules/frontend/04-test.md',
  'business-pmo/mango-baseline/rules/frontend/05-dev-flow.md',
  'business-pmo/mango-baseline/rules/frontend/06-monorepo-architecture.md',
  'business-pmo/mango-baseline/rules/product/01-prd-template.md',
  'business-pmo/mango-baseline/rules/product/02-sprint.md',
  'business-pmo/mango-baseline/templates/delivery-contract.md',
  'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
  'business-pmo/mango-baseline/tools/delivery-contract-check.mjs',
  'business-docs/plans/example-contract.md',
  'business-docs/plans/example-ledger.md',
  'backend/modules/{{moduleKebab}}/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/{{modulePascal}}Api.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/command/Create{{aggregatePascal}}Command.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/command/Update{{aggregatePascal}}Command.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/query/{{aggregatePascal}}PageQuery.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/vo/{{aggregatePascal}}VO.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/entity/{{aggregatePascal}}Entity.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/mapper/{{aggregatePascal}}Mapper.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/service/I{{aggregatePascal}}Service.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/service/impl/{{aggregatePascal}}Service.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/resources/db/migration/{{moduleKebab}}/V1__init_{{moduleKebab}}.sql',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/controller/{{modulePascal}}Controller.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/module.properties',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/resource-manifest.json',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/remote/{{modulePascal}}FeignClient.java',
  'frontend/packages/{{moduleKebab}}-api/package.json',
  'frontend/packages/{{moduleKebab}}-api/src/api.ts',
  'frontend/packages/{{moduleKebab}}-api/src/types.ts',
  'frontend/packages/{{moduleKebab}}/package.json',
  'frontend/packages/{{moduleKebab}}/src/index.ts',
  'frontend/packages/{{moduleKebab}}/src/views/{{moduleKebab}}/{{aggregateKebab}}/index.vue',
  'frontend/apps/{{projectKebab}}-admin/package.json',
  'frontend/apps/{{projectKebab}}-admin/src/main.ts',
  'topologies/monolith/README.md',
  'topologies/microservice/README.md',
];

const contentChecks = [
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/{{modulePascal}}Api.java',
    patterns: ['R<Object>', 'R<PersistencePageResult<?>>', '@ParameterObject', '@Validated', '/create', '/update', '/delete', '/page', '/detail', '{{aggregateName}}'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/controller/{{modulePascal}}Controller.java',
    patterns: ['extends BaseCrudController', 'I{{aggregatePascal}}Service', '@RequestMapping("/{{moduleKebab}}/{{aggregateKebab}}s")'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/pom.xml',
    patterns: ['io.mango.infra.web', 'mango-infra-persistence-web-starter'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/pom.xml',
    patterns: ['io.mango.infra.feign', 'mango-infra-feign-starter'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/query/{{aggregatePascal}}PageQuery.java',
    patterns: ['extends PageQuery', '@QueryField(type = QueryType.LIKE)'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/module.properties',
    patterns: ['module-name={{moduleKebab}}', 'module-path={{moduleKebab}}'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    patterns: [
      '"moduleCode": "{{moduleKebab}}"',
      '"menuName": "{{aggregateName}}管理"',
      '"component": "{{moduleKebab}}/{{aggregateKebab}}/index"',
      '"{{moduleKebab}}:{{aggregateKebab}}:create"',
      '"{{moduleKebab}}:{{aggregateKebab}}:view"',
      '"{{moduleKebab}}:{{aggregateKebab}}:update"',
      '"{{moduleKebab}}:{{aggregateKebab}}:delete"',
      '"permissionItems"',
    ],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/remote/{{modulePascal}}FeignClient.java',
    patterns: ['extends {{modulePascal}}Api', '@FeignClient(name = "{{moduleKebab}}"', 'path = "/{{moduleKebab}}"'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-api/src/api.ts',
    patterns: ["from '@mango/common'", 'post<string>', 'get<PageResult<{{aggregatePascal}}VO>>', '`${basePath}/create`', '`${basePath}/update`', '`${basePath}/delete`', '`${basePath}/page`', '`${basePath}/detail`'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-api/src/types.ts',
    patterns: ['Update{{aggregatePascal}}Command', 'DeleteCommand', 'page: number', 'size: number', 'records: T[]'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}/src/views/{{moduleKebab}}/{{aggregateKebab}}/index.vue',
    patterns: ['create{{aggregatePascal}}', 'update{{aggregatePascal}}', 'delete{{aggregatePascal}}', 'get{{aggregatePascal}}Detail', 'openCreateDialog', 'openEditDialog', 'openDetail', 'handleDelete', 'el-pagination', 'el-dialog', 'el-drawer', 'records.value = result.records', 'page: 1', 'size: 20'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}/src/index.ts',
    patterns: ["from '@mango/admin-pages/core'", 'registerModulePages', "'{{moduleKebab}}/{{aggregateKebab}}/index'"],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/main.ts',
    patterns: ["from '@mango/admin'", "import '@mango/admin/style.css'", 'createMangoAdminApp', 'register{{modulePascal}}Pages'],
  },
  {
    file: 'topologies/monolith/README.md',
    patterns: ['<module>-starter', '不应依赖业务模块的 remote starter', '<module>/<aggregate>/index'],
  },
  {
    file: 'topologies/microservice/README.md',
    patterns: ['<module>-starter-remote', '不允许调用方直接依赖业务模块 `core`'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/pom.xml',
    patterns: ['<relativePath>../../pom.xml</relativePath>'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/pom.xml',
    patterns: ['<relativePath>../pom.xml</relativePath>', 'mango-infra-persistence-starter'],
  },
  {
    file: 'AGENTS.md',
    patterns: ['business-pmo/mango-baseline/tools/pmo-preflight.mjs', '实际加载的 Mango baseline 文件'],
  },
  {
    file: 'business-pmo/README.md',
    patterns: ['business-pmo/mango-baseline/tools/pmo-preflight.mjs', '不随普通业务需求直接修改 `mango-baseline/**`'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/index.json',
    patterns: ['"rules/03-ai-coding-redlines.md"', '"frontend.elementPlusUi"', '"frontend.componentDevelopment"', '"rules/frontend/02-element-plus-ui.md"', '"rules/frontend/03-component-development.md"', '"backend/**"', '"frontend/**"', '"business-docs/**"', '"business-pmo/**"'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/backend/10-dev-flow.md',
    patterns: ['必须先拆解需求并提取', '字典和枚举', '数据模型', '设计模式'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/frontend/02-element-plus-ui.md',
    patterns: ['Element Plus UI 规范', '选择组件必须优先支持单选', '必须支持输入检索', '业务状态统一使用 `ElTag`'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/frontend/03-component-development.md',
    patterns: ['前端组件开发规范', '单体部署', '微前端部署', 'npm 独立消费', '独立消费要求', '组件包禁止依赖 `apps/*`'],
  },
];

const errors = [];

function check(condition, message) {
  if (!condition) {
    errors.push(message);
  }
}

function readTemplateFile(path) {
  return readFileSync(join(root, path), 'utf8');
}

function walk(dir) {
  const result = [];
  for (const entry of readdirSync(dir)) {
    const fullPath = join(dir, entry);
    if (statSync(fullPath).isDirectory()) {
      result.push(...walk(fullPath));
    } else {
      result.push(fullPath);
    }
  }
  return result;
}

for (const file of requiredFiles) {
  check(existsSync(join(root, file)), `missing required file: ${file}`);
}

for (const item of contentChecks) {
  if (!existsSync(join(root, item.file))) {
    continue;
  }
  const content = readTemplateFile(item.file);
  for (const pattern of item.patterns) {
    check(content.includes(pattern), `${item.file} missing pattern: ${pattern}`);
  }
}

const packageFiles = walk(root).filter((file) => file.endsWith('package.json'));
for (const file of packageFiles) {
  const content = readFileSync(file, 'utf8');
  check(!content.includes('workspace:*'), `${relative(repoRoot, file)} contains workspace:*`);
}

const allTextFiles = walk(root).filter((file) => {
  return !file.includes('node_modules') && !file.endsWith('.png') && !file.endsWith('.jpg');
});
for (const file of allTextFiles) {
  const content = readFileSync(file, 'utf8');
  const forbiddenMangoAdminSourcePath = '../../../apps/' + 'mango-admin';
  check(!content.includes(forbiddenMangoAdminSourcePath), `${relative(repoRoot, file)} references mango-admin source path`);
}

if (errors.length > 0) {
  console.error('Template check failed:');
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`Template check passed: ${requiredFiles.length} required files, ${contentChecks.length} contract checks.`);
