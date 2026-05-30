import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const repoRoot = new URL('../..', import.meta.url).pathname;

const requiredFiles = [
  'README.md',
  'AGENTS.md',
  '.github/CODEOWNERS',
  '.gitignore',
  '.npmrc',
  'package.json',
  'pnpm-workspace.yaml',
  'tsconfig.base.json',
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
  'backend/pom.xml',
  'backend/apps/{{projectKebab}}-monolith-app/pom.xml',
  'backend/apps/{{projectKebab}}-monolith-app/src/main/java/{{basePackagePath}}/app/monolith/{{projectPascal}}MonolithApplication.java',
  'backend/apps/{{projectKebab}}-monolith-app/src/main/resources/application.yml',
  'backend/modules/{{moduleKebab}}/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/pom.xml',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/{{modulePascal}}Api.java',
  'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/command/Create{{aggregatePascal}}Command.java',
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
  'frontend/packages/{{moduleKebab}}-api/tsconfig.json',
  'frontend/packages/{{moduleKebab}}-api/src/api.ts',
  'frontend/packages/{{moduleKebab}}-api/src/types.ts',
  'frontend/packages/{{moduleKebab}}-admin/package.json',
  'frontend/packages/{{moduleKebab}}-admin/tsconfig.json',
  'frontend/packages/{{moduleKebab}}-admin/src/index.ts',
  'frontend/packages/{{moduleKebab}}-admin/src/views/{{moduleKebab}}/{{aggregateKebab}}/index.vue',
  'frontend/apps/{{projectKebab}}-admin/.env.example',
  'frontend/apps/{{projectKebab}}-admin/package.json',
  'frontend/apps/{{projectKebab}}-admin/public/mango-runtime-config.json',
  'frontend/apps/{{projectKebab}}-admin/tsconfig.json',
  'frontend/apps/{{projectKebab}}-admin/src/main.ts',
  'frontend/apps/{{projectKebab}}-admin/src/runtimeConfig.ts',
  'frontend/apps/{{projectKebab}}-admin/src/starterMenus.ts',
  'frontend/apps/{{projectKebab}}-admin/src/vite-env.d.ts',
  'scripts/dev-start.sh',
  'scripts/dev-stop.sh',
  'topologies/monolith/README.md',
  'topologies/microservice/README.md',
];

const contentChecks = [
  {
    file: 'package.json',
    patterns: ['"backend:validate": "mvn -f backend/pom.xml validate"', '"dev:start": "./scripts/dev-start.sh"', '"dev": "pnpm --filter {{projectKebab}}-admin dev"', '"typecheck": "pnpm -r typecheck"', '"build": "pnpm -r build"', '"@types/node"'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/vite-env.d.ts',
    patterns: ['vite/client'],
  },
  {
    file: 'pnpm-workspace.yaml',
    patterns: ['frontend/apps/*', 'frontend/packages/*'],
  },
  {
    file: 'backend/pom.xml',
    patterns: ['<artifactId>{{projectKebab}}-backend</artifactId>', '<module>modules/{{moduleKebab}}</module>', '<module>apps/{{projectKebab}}-monolith-app</module>', '<artifactId>mango-admin-starter</artifactId>', '<artifactId>mango-common</artifactId>', '<artifactId>mango-infra-persistence-starter</artifactId>'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/pom.xml',
    patterns: ['<artifactId>{{projectKebab}}-backend</artifactId>', '<relativePath>../../pom.xml</relativePath>', '<module>{{moduleKebab}}-api</module>', '<module>{{moduleKebab}}-starter</module>'],
  },
  {
    file: 'backend/apps/{{projectKebab}}-monolith-app/pom.xml',
    patterns: ['<artifactId>{{projectKebab}}-monolith-app</artifactId>', '<artifactId>mango-admin-starter</artifactId>', '<artifactId>{{moduleKebab}}-starter</artifactId>', '<mainClass>{{basePackage}}.app.monolith.{{projectPascal}}MonolithApplication</mainClass>'],
  },
  {
    file: 'backend/apps/{{projectKebab}}-monolith-app/src/main/java/{{basePackagePath}}/app/monolith/{{projectPascal}}MonolithApplication.java',
    patterns: ['@SpringBootApplication(scanBasePackages = "{{basePackage}}")'],
  },
  {
    file: 'backend/apps/{{projectKebab}}-monolith-app/src/main/resources/application.yml',
    patterns: ['port: ${MANGO_BACKEND_PORT:5555}', 'url: ${MANGO_DB_URL:jdbc:mysql://127.0.0.1:3306/{{moduleKebabSnake}}', 'required-paths: []', 'system:', 'authorization:', 'identity:', 'org:', 'captcha:', 'file:', 'template:', 'workflow:', 'kv:', 'calendar:', 'numgen:', 'notice:', '{{moduleKebab}}:', 'packages-to-scan: {{basePackage}},io.mango', 'database-schema-update: false', 'async-executor-activate: false', 'eventregistry:', 'enabled: false'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/src/main/java/{{basePackagePath}}/{{modulePackage}}/api/{{modulePascal}}Api.java',
    patterns: ['R<{{aggregatePascal}}VO>', 'R<PageResult<{{aggregatePascal}}VO>>', '@ParameterObject', '@RequestParam("id")', '@Validated'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-api/pom.xml',
    patterns: ['<groupId>io.mango.common</groupId>', '<artifactId>mango-common</artifactId>'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/controller/{{modulePascal}}Controller.java',
    patterns: ['implements {{modulePascal}}Api', 'I{{aggregatePascal}}Service', '@RequestMapping("/{{moduleKebab}}")'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/mapper/{{aggregatePascal}}Mapper.java',
    patterns: ['extends BaseMapper<{{aggregatePascal}}Entity>', '@Mapper'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/service/impl/{{aggregatePascal}}Service.java',
    patterns: ['{{aggregatePascal}}Mapper {{aggregateCamel}}Mapper', '{{aggregateCamel}}Mapper.insert(entity)', '{{aggregateCamel}}Mapper.selectPage', '{{aggregateCamel}}Mapper.selectById(id)', '{{aggregatePascal}}Entity::getCreatedAt', 'Require.notNull(entity, 404'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/java/{{basePackagePath}}/{{modulePackage}}/core/entity/{{aggregatePascal}}Entity.java',
    patterns: ['extends TenantEntity', 'io.mango.infra.persistence.api.entity.TenantEntity'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-core/src/main/resources/db/migration/{{moduleKebab}}/V1__init_{{moduleKebab}}.sql',
    patterns: ['id BIGINT NOT NULL', 'created_by BIGINT NULL', 'created_at DATETIME NULL', 'updated_by BIGINT NULL', 'updated_at DATETIME NULL'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/src/main/java/{{basePackagePath}}/{{modulePackage}}/starter/{{modulePascal}}AutoConfiguration.java',
    patterns: ['@MapperScan("{{basePackage}}.{{modulePackage}}.core.mapper")'],
  },
  {
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter/pom.xml',
    patterns: ['<groupId>io.mango.infra.web</groupId>', '<artifactId>mango-infra-web-starter</artifactId>'],
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
    file: 'backend/modules/{{moduleKebab}}/{{moduleKebab}}-starter-remote/pom.xml',
    patterns: ['<groupId>io.mango.infra.feign</groupId>', '<artifactId>mango-infra-feign-starter</artifactId>'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-api/src/api.ts',
    patterns: ["from '@mango/common/utils/request'", 'post<{{aggregatePascal}}VO>', 'get<PageResult<{{aggregatePascal}}VO>>'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-api/package.json',
    patterns: ['"typecheck": "vue-tsc --noEmit -p tsconfig.json"', '"build": "pnpm typecheck"', '"exports"'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-admin/src/index.ts',
    patterns: ["from '@mango/admin-pages/core'", 'registerModulePages', "'{{moduleKebab}}/{{aggregateKebab}}/index'"],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-admin/src/views/{{moduleKebab}}/{{aggregateKebab}}/index.vue',
    patterns: ['page{{aggregatePascal}}', 'v-loading="loading"', '{{aggregatePascal}}名称', 'data-mango-layout="search"', 'data-mango-layout="actions"', 'data-mango-layout="table"', 'data-mango-layout="pagination"', 'el-pagination', 'empty-text="暂无{{aggregatePascal}}数据"'],
  },
  {
    file: 'frontend/packages/{{moduleKebab}}-admin/package.json',
    patterns: ['"typecheck": "vue-tsc --noEmit -p tsconfig.json"', '"build": "pnpm typecheck"', '"exports"'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/main.ts',
    patterns: ["from '@mango/admin'", "import '@mango/admin/style.css'", 'createMangoAdmin', "preset: '{{adminPreset}}'", "from '@{{projectKebab}}/{{moduleKebab}}-admin'", 'register{{modulePascal}}Pages', 'starterRuntimeConfig', 'starterMenus', 'enabledMangoCapabilities'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/runtimeConfig.ts',
    patterns: ["from '@mango/app-runtime'", 'starterRuntimeConfig', 'modules'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/src/starterMenus.ts',
    patterns: ["from '@mango/admin/menu'", "component: '{{moduleKebab}}/{{aggregateKebab}}/index'", "moduleCode: '{{moduleKebab}}'"],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/public/mango-runtime-config.json',
    patterns: ['"profile"', '"modules"'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/package.json',
    patterns: ['"typecheck": "vue-tsc --noEmit -p tsconfig.json"', '"build": "pnpm typecheck && vite build"', '"@mango/admin"', '"@mango/admin-pages"', '"@mango/app-runtime"'],
  },
  {
    file: 'frontend/apps/{{projectKebab}}-admin/vite.config.ts',
    patterns: ['loadEnv', "proxy: {", "'/api'", 'VITE_ADMIN_PROXY_PATH'],
  },
  {
    file: 'scripts/dev-start.sh',
    patterns: ['MANGO_BACKEND_PORT="${MANGO_BACKEND_PORT:-5555}"', 'MANGO_FRONTEND_PORT="${MANGO_FRONTEND_PORT:-5173}"', 'MANGO_DB_AUTO_CREATE="${MANGO_DB_AUTO_CREATE:-true}"', 'CREATE DATABASE IF NOT EXISTS', '/actuator/health', 'mvn -pl "apps/{{projectKebab}}-monolith-app" -am -DskipTests package', 'java -jar "${backend_jar}"', 'pnpm --filter "{{projectKebab}}-admin" dev'],
  },
  {
    file: 'scripts/dev-stop.sh',
    patterns: ['stop_pid_file "${PID_DIR}/frontend.pid"', 'stop_pid_file "${PID_DIR}/backend.pid"'],
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
    patterns: ['"rules/03-ai-coding-redlines.md"', '"frontend.elementPlusUi"', '"frontend.componentDevelopment"', '"rules/frontend/02-element-plus-ui.md"', '"rules/frontend/03-component-development.md"', '"backend/**"', '"frontend/**"', '"business-docs/**"', '"business-pmo/**"'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/backend/10-dev-flow.md',
    patterns: ['必须先拆解需求并提取', '字典和枚举', '数据模型', '设计模式'],
  },
  {
    file: 'business-pmo/mango-baseline/rules/backend/07-persistence.md',
    patterns: ['mango-infra-persistence-starter', 'BaseEntity', 'MangoCrudServiceImpl', '禁止直接依赖 `JdbcTemplate`', '禁止直接声明 MyBatis-Plus starter 依赖'],
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

const adminAppFiles = allTextFiles.filter((file) => file.includes('/frontend/apps/') && file.endsWith('.ts'));
for (const file of adminAppFiles) {
  const content = readFileSync(file, 'utf8');
  const relativePath = relative(repoRoot, file);
  check(!content.includes("from '@mango/admin-shell'"), `${relativePath} directly imports @mango/admin-shell`);
  check(!content.includes("from '@mango/admin-shell/"), `${relativePath} directly imports @mango/admin-shell subpath`);
  check(!content.includes("import '@mango/admin-shell/style.css'"), `${relativePath} directly imports @mango/admin-shell style`);
  check(!content.includes('createMangoAdminApp'), `${relativePath} directly uses createMangoAdminApp`);
}

for (const file of packageFiles) {
  const content = readFileSync(file, 'utf8');
  const relativePath = relative(repoRoot, file);
  if (relativePath.includes('frontend/apps/') && relativePath.endsWith('package.json')) {
    check(!content.includes('"@mango/admin-shell"'), `${relativePath} directly depends on @mango/admin-shell`);
  }
  if (relativePath.includes('frontend/apps/') && relativePath.endsWith('package.json')) {
    check(content.includes('"@{{projectKebab}}/{{moduleKebab}}-admin"'), `${relativePath} must depend on business admin package`);
    check(!content.includes('"@{{projectKebab}}/{{moduleKebab}}"'), `${relativePath} must not depend on unqualified business package`);
  }
  if (relativePath.includes('frontend/packages/{{moduleKebab}}-admin/package.json')) {
    check(content.includes('"name": "@{{projectKebab}}/{{moduleKebab}}-admin"'), `${relativePath} must use -admin package name`);
    check(content.includes('"@mango/admin-pages"'), `${relativePath} must depend on Admin page registry`);
    check(content.includes('"element-plus"'), `${relativePath} must declare Admin UI dependency`);
  }
  if (relativePath.includes('frontend/packages/{{moduleKebab}}-api/package.json')) {
    check(!content.includes('"@mango/admin"'), `${relativePath} must not depend on @mango/admin`);
    check(!content.includes('"@mango/admin-shell"'), `${relativePath} must not depend on @mango/admin-shell`);
    check(!content.includes('"@mango/admin-pages"'), `${relativePath} must not depend on @mango/admin-pages`);
    check(!content.includes('"element-plus"'), `${relativePath} must not depend on Element Plus`);
  }
}

const starterMenuFiles = allTextFiles.filter((file) => file.endsWith('/starterMenus.ts'));
const forbiddenBuiltInMenuMarkers = [
  'mango-system',
  'mango-authorization',
  'mango-file',
  'mango-notice',
  'mango-template',
  'mango-workflow',
  'mango-calendar',
  'mango-numgen',
  "menuCode: 'system:",
  'menuCode: "system:',
  "menuCode: 'file:",
  'menuCode: "file:',
  "menuCode: 'notice:",
  'menuCode: "notice:',
  "menuCode: 'template:",
  'menuCode: "template:',
  "menuCode: 'workflow:",
  'menuCode: "workflow:',
  "menuCode: 'data:calendar'",
  'menuCode: "data:calendar"',
  "menuCode: 'data:numgen'",
  'menuCode: "data:numgen"',
];
for (const file of starterMenuFiles) {
  const content = readFileSync(file, 'utf8');
  const relativePath = relative(repoRoot, file);
  for (const marker of forbiddenBuiltInMenuMarkers) {
    check(!content.includes(marker), `${relativePath} must not declare built-in Mango menu marker: ${marker}`);
  }
}

const backendTemplateFiles = allTextFiles.filter((file) => file.startsWith(join(root, 'backend') + '/'));
for (const file of backendTemplateFiles) {
  const content = readFileSync(file, 'utf8');
  const relativePath = relative(repoRoot, file);
  check(!content.includes('JdbcTemplate'), `${relativePath} directly references JdbcTemplate`);
  check(!content.includes('java.sql.Connection'), `${relativePath} directly references JDBC Connection`);
  check(!content.includes('java.sql.Statement'), `${relativePath} directly references JDBC Statement`);
  check(!content.includes('mybatis-plus-spring-boot3-starter'), `${relativePath} directly declares MyBatis-Plus starter`);
  check(!content.includes('mybatis-plus-jsqlparser'), `${relativePath} directly declares MyBatis-Plus jsqlparser`);
}

if (errors.length > 0) {
  console.error('Template check failed:');
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`Template check passed: ${requiredFiles.length} required files, ${contentChecks.length} contract checks.`);
