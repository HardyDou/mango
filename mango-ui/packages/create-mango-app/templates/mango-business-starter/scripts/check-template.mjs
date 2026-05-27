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
  'business-pmo/mango-baseline/rules/backend/03-api.md',
  'business-pmo/mango-baseline/rules/backend/04-db.md',
  'business-pmo/mango-baseline/rules/backend/05-module.md',
  'business-pmo/mango-baseline/rules/backend/08-test.md',
  'business-pmo/mango-baseline/rules/frontend/01-vue-code.md',
  'business-pmo/mango-baseline/rules/frontend/04-test.md',
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
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/query/{{aggregatePascal}}PageQuery.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/vo/{{aggregatePascal}}VO.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/entity/{{aggregatePascal}}Entity.java',
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
    patterns: ['R<{{aggregatePascal}}VO>', 'R<PageResult<{{aggregatePascal}}VO>>', '@ParameterObject', '@Validated'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/controller/{{modulePascal}}Controller.java',
    patterns: ['implements {{modulePascal}}Api', 'I{{aggregatePascal}}Service', '@RequestMapping("/{{moduleKebab}}")'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/module.properties',
    patterns: ['module-name={{moduleKebab}}', 'module-path={{moduleKebab}}'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    patterns: ['"moduleCode": "{{moduleKebab}}"', '"component": "{{moduleKebab}}/{{aggregateKebab}}/index"', '"permissionItems"'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/remote/{{modulePascal}}FeignClient.java',
    patterns: ['extends {{modulePascal}}Api', '@FeignClient(name = "{{moduleKebab}}"', 'path = "/{{moduleKebab}}"'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-api/src/api.ts',
    patterns: ["from '@mango/common/utils/request'", 'post<{{aggregatePascal}}VO>', 'get<PageResult<{{aggregatePascal}}VO>>'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}/src/index.ts',
    patterns: ["from '@mango/admin-pages/core'", 'registerModulePages', "'{{moduleKebab}}/{{aggregateKebab}}/index'"],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/main.ts',
    patterns: ["from '@mango/admin-shell'", 'createMangoAdminApp', 'register{{modulePascal}}Pages'],
  },
  {
    file: 'topologies/monolith/README.md',
    patterns: ['{{moduleKebab}}-starter', '单体 app 不依赖 `{{moduleKebab}}-starter-remote`'],
  },
  {
    file: 'topologies/microservice/README.md',
    patterns: ['{{moduleKebab}}-starter-remote', '调用方不得依赖 `{{moduleKebab}}-core`'],
  },
  {
    file: 'AGENTS.md',
    patterns: ['business-pmo/mango-baseline/tools/pmo-preflight.mjs', '实际加载的 Mango baseline 文件'],
  },
  {
    file: 'business-pmo/README.md',
    patterns: ['business-pmo/mango-baseline/tools/pmo-preflight.mjs', '业务团队不得在普通业务需求中直接修改 `mango-baseline`'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/index.json',
    patterns: ['"backend/**"', '"frontend/**"', '"business-docs/**"', '"business-pmo/**"'],
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
