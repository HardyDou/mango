# Issue 106 Job 菜单初始化与授权模块归口边界回归报告

## 结论

- 状态：PASS
- Issue：#106 治理：Job 菜单初始化与授权模块归口边界
- 分支：feature/issue-106-job-menu-boundary
- 验证日期：2026-06-08

## 改动范围

- 新增 `mango-pmo/rules/backend/11-module-menu.md`，明确功能模块菜单初始化归口。
- `mango-authorization` 资源清单命令新增 `packageCodes`、`roleCodes`。
- `AppModuleServiceImpl` 注册资源清单时，幂等关联已存在菜单包和角色。
- `mango-job-starter` 新增 `META-INF/mango/resource-manifest.json`，声明 Job 默认菜单、页面路由和按钮权限。
- `mango-authorization` README 更新资源清单使用说明，并链接 PMO 规则源。

## 业务断言

- Job 默认菜单由 Job starter 自身资源清单提供，不再要求新增 authorization migration。
- Authorization 只消费清单并关联已存在的菜单包/角色，不创建角色、主体或菜单包。
- 资源清单包含任务定义、执行实例、Worker 节点、告警规则、运行状态 5 个页面菜单。
- 资源清单权限覆盖前端 `v-auth` 中的 Job 权限点，包括 `job:log:list`。
- Job starter 打包产物包含 `META-INF/mango/resource-manifest.json`。

## 执行命令

```bash
node mango-pmo/tools/pmo-preflight.mjs --role tech-lead --phase design --task "Issue 106: define Job menu initialization ownership boundary and installer/seed mechanism" --paths "mango-pmo,mango-docs,mango/mango-platform/mango-job,mango/mango-platform/mango-authorization"
node mango-pmo/tools/pmo-preflight.mjs --role pmo --phase governance --task "Issue 106: add module menu initialization ownership rule and evidence" --paths "mango-pmo/rules,mango-docs/evidence,mango/mango-platform/mango-authorization,mango/mango-platform/mango-job"
node mango-pmo/tools/pmo-preflight.mjs --role tech-lead --phase design --task "Issue 106 resource-manifest 菜单初始化 默认菜单 packageCodes roleCodes 授权模块归口" --paths "mango/mango-platform/mango-job/mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json,mango/mango-platform/mango-authorization,mango-pmo/rules/backend/11-module-menu.md"
mvn -q -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-core -am test -Dcheckstyle.skip=true -Dtest=AppModuleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter -am test -Dcheckstyle.skip=true -Dtest=AppModuleResourceManifestSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false
node -e "JSON.parse(require('fs').readFileSync('mango-pmo/rules/index.json','utf8')); JSON.parse(require('fs').readFileSync('mango/mango-platform/mango-job/mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json','utf8')); console.log('json ok')"
mvn -q -f mango/pom.xml -pl mango-platform/mango-job/mango-job-starter -am package -DskipTests -Dcheckstyle.skip=true
jar tf mango/mango-platform/mango-job/mango-job-starter/target/mango-job-starter-1.0.0-SNAPSHOT.jar | rg 'META-INF/mango/resource-manifest.json'
rg "v-auth|job:" mango-ui/packages/job/src -n
git diff --check
```

## 验证结果

- `AppModuleServiceImplTest`：PASS
- `AppModuleResourceManifestSyncRunnerTest`：PASS
- PMO index 与 Job manifest JSON 解析：PASS
- Job starter package：PASS
- jar 资源清单存在性检查：PASS
- Job 前端页面注册键与 manifest component 对照：PASS
- Job 前端 `v-auth` 权限点覆盖检查：PASS
- `git diff --check`：PASS

## UI / E2E 说明

- 本 Issue 属于菜单初始化归口治理与后端资源清单链路改造，未新增或修改前端 UI。
- 本次 UI 识别以 `mango-ui/packages/job/src/admin-pages.ts` 与各 Job 页面 `v-auth` 权限点为准，确认 manifest 中的页面 component 和权限点能覆盖现有页面入口。
- 截图文件：`regression-report.png`，截图源：`regression-report.html`。

## 未验证项和风险

- 未启动完整本地后端和管理台做真实菜单入库后的浏览器联调；本次以单测、资源加载、打包产物和前端静态权限点对照验证。
- 历史 authorization migration 中已有 Job 菜单数据未在本 Issue 中删除或重写，避免修改已执行历史 migration。
- `packageCodes` / `roleCodes` 只关联已存在菜单包和角色；目标环境缺少这些基础资产时会跳过关联，需要由 authorization 基础初始化保证。
