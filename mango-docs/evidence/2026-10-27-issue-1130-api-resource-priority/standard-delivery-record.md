# 标准交付记录

## 先读

- **一句话目标**：允许多个来源声明 API 资源，并让数据库人工登记成为最高优先级，同时保证 Gateway 与单体对同一接口得到一致访问决策。
- **真实场景**：业务 Controller 仅使用 `@ApiAccess(PUBLIC)` 声明匿名接口，Gateway 路由没有重复配置时，匿名请求仍应进入 Controller；数据库人工登记存在时，应覆盖代码、固定声明和 Gateway 默认策略。
- **关键输入/输出**：输入为 HTTP 方法、规范化路径和多来源资源声明；输出为唯一的 `PUBLIC`、`LOGIN`、`PERMISSION` 或 `INTERNAL` 访问决策。
- **成功/失败边界**：成功时同一有效路由只有一个生效决策；失败时不允许 Gateway 默认 `LOGIN` 覆盖 Controller 精确声明，也不允许同步覆盖数据库人工登记。
- **明确不做**：不删除既有 `@ApiAccess`、`API_RESOURCE`、Gateway 或注册 API；不改变已有请求模型；不强制迁移历史资源数据。
- **阅读顺序**：需求与验收 → 技术决定 → 实施清单 → 验收结果。

## 1. 元数据

- 任务 ID：issue-1130-api-resource-priority
- 交付模式：STANDARD
- 需求影响：L2
- 方案风险：L2
- 最终风险：L2
- 工作区决策：CREATE
- 评审选择：AGENT_ONLY；未指定外部同行评审，保留后续复核入口

## 2. 目标与范围

- 目标：统一 API 资源多来源冲突处理，并支持数据库人工登记绝对控制。
- 成功条件：`MANUAL > FIXED_DECLARATION/Gateway > CONTROLLER_SCAN`；同一路由只保留一个有效决策；Gateway 外部路径优先回落到单体应用路径；旧调用方无需新增字段即可继续注册资源。
- 处理范围：授权资源来源优先级、固定声明覆盖范围、Gateway/应用路径决策顺序、运行时缓存、回归测试和能力说明。
- 不处理范围：新增后台人工编辑页面、修改既有 API 注册协议、改变租户和业务数据权限。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1130-01 | 单体 Controller | `@ApiAccess(PUBLIC)`，无重复固定声明 | 匿名请求获得 `PUBLIC` 决策 | 不得因 Gateway 默认策略返回 401 | 资源同步 + 访问决策测试通过 |
| AC-1130-02 | Gateway + 单体 | 外部路径带 `/api` 前缀，内部 Controller 路径无前缀 | 优先使用应用路径决策；无应用资源时回退 Gateway 路由决策 | 不得因路径先匹配 Gateway `LOGIN` 而阻断 Controller `PUBLIC` | Access evaluator / authorization manager 测试通过 |
| AC-1130-03 | 数据库人工接管 | 同一路由已有启用的人工接管记录 | 人工接管覆盖固定声明、Gateway 和 Controller 扫描；非人工同步被跳过 | 同步不得覆盖人工记录，运行时不回退低优先级声明 | Service 集成测试通过 |
| AC-1130-04 | 固定声明与 Controller | 固定通配路径和 Controller 精确路径同时存在 | 精确路径优先，通配声明不提前删除精确扫描结果 | 不得用来源优先级破坏路径匹配优先级 | Service / Discoverer 测试通过 |
| AC-1130-05 | 兼容调用方 | 旧注册请求不携带新增字段 | 按原有默认来源处理，注册接口保持兼容 | 不得拒绝历史请求，不新增表字段 | API 编译、全量模块测试通过 |
| AC-1130-06 | 管理端人工接管 | 调用 `/authorization/api-resources/manual` 与 `manual/release` | 接管后立即生效并清缓存；释放后重新同步可恢复代码声明 | 未找到记录、PERMISSION 缺权限码必须报业务错误 | Service 集成测试、控制器契约测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-1130-01 | AC-1130-03 | 复用现有 `ApiResourceSource.MANUAL` 表示数据库人工接管，提升其排序，不新增数据库字段 | `ApiResourceSource`、`ApiResourceService` | 恢复来源排序常量 |
| TD-1130-02 | AC-1130-03 | 已启用的 `MANUAL` 记录禁止被非人工来源同键更新；同路由跨模块冲突时人工记录获胜 | `ApiResourceService#registerApiResources` | 恢复原覆盖逻辑 |
| TD-1130-03 | AC-1130-04 | 固定声明只在同方法同精确路径时覆盖扫描；通配与精确路径同时保留，由运行时匹配优先级决定 | `ApiAccessResourceDiscoverer`、`ApiResourceService#routeMatches` | 恢复双向路径匹配 |
| TD-1130-04 | AC-1130-02 | 访问决策先查应用路径：人工接管优先，其次已命中的声明；Gateway 外部路径用于兵底或发现更高优先级的人工接管 | `ApiResourceAuthorizationManager`、`AccessEvaluator` | 恢复原 Gateway-first 查找顺序 |
| TD-1130-05 | AC-1130-06 | 新增 `ApiResourceManualCommand`、`ApiResourceManualQuery` 和 `system:api-resource:manual` 权限；不新增表字段、不改已有注册接口 | `mango-authorization-api`、`mango-authorization-core`、`mango-authorization-starter` | 回退控制器和接口，无数据清理 |
| TD-1130-06 | AC-1130-05 | `ApiResourceAccessDecisionVO` 新增 `source` 并在原三参构造器基础上保留，旧调用方源码与 JSON 均兼容 | `mango-authorization-api`、`mango-access-core` | 回退 `source` 字段和构造器 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-1130-01 | TD-1130-01/02 | 1 | `mango-authorization-core/.../ApiResourceService.java` | 人工来源最高，非人工同步不覆盖已启用人工记录 |
| TASK-1130-02 | TD-1130-03 | 2 | `ApiAccessResourceDiscoverer.java`、`ApiResourceService.java` | 固定通配路径不再屏蔽精确 Controller |
| TASK-1130-03 | TD-1130-04 | 3 | `ApiResourceAuthorizationManager.java`、`AccessEvaluator.java` | Gateway 与单体路径决策一致，人工接管跨路径生效 |
| TASK-1130-04 | TD-1130-05 | 4 | `ApiResourceManualCommand`、`ApiResourceManualQuery`、`IApiResourceService`、`ApiResourceService`、`ApiResourceAdminApi`、`ApiResourceAdminController`、`authorization-common-menu.json` | 人工接管与释放可用，并有独立权限码 |
| TASK-1130-05 | TD-1130-06 | 5 | `ApiResourceAccessDecisionVO` | `source` 可读且兼容旧三参构造 |
| TASK-1130-06 | 全部 | 6 | 授权/访问模块测试 | 覆盖来源、路径、人工接管、释放和兼容行为 |
| TASK-1130-07 | 全部 | 7 | `mango-platform/mango-authorization/README.md`、`mango-docs/capabilities/README.md` | 记录唯一控制、优先级、接口和升级兼容 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1130-01、AC-1130-03 至 AC-1130-06 | M10/M11 集成测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core verify -Dtest=ApiResourceServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs` | PASS（20/20） | `mango-authorization-core/target/surefire-reports/io.mango.authorization.core.service.impl.ApiResourceServiceImplIntegrationTest.txt` |
| AC-1130-02 | M10 单元测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-access/mango-access-core verify -Dtest=AccessEvaluatorTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs` | PASS（9/9） | `mango-access-core/target/surefire-reports/io.mango.access.core.auth.AccessEvaluatorTest.txt` |
| AC-1130-02 | M10 单元测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-resource-access-starter verify -Dtest=ApiResourceAuthorizationManagerTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs` | PASS（8/8） | `mango-authorization-resource-access-starter/target/surefire-reports` |
| AC-1130-04 | M10 单元测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter verify -Dtest=ApiAccessResourceDiscovererTest,GatewayRouteResourceSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs` | PASS（14/14） | `mango-authorization-resource-sync-starter/target/surefire-reports` |
| 全部改动模块 | M09 全量模块验证 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-authorization/mango-authorization-resource-sync-starter,mango-platform/mango-authorization/mango-authorization-resource-access-starter,mango-platform/mango-access/mango-access-core,mango-platform/mango-access/mango-access-web-starter,mango-platform/mango-access/mango-access-gateway-starter verify -DskipITs` | PASS（8 个模块全部 SUCCESS） | Maven Reactor 输出 |
| 全部改动模块 | M09 架构与静态质量门禁 | `mvn -f mango/pom.xml -pl ...,:mango-architecture-verification -Dmango.architecture.skip=false -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false -Dmango.architecture.base=main -Dmango.check.gate=no-new-violations verify` | PASS（dependency=0, archunit=0, pmd=0, blocking=0） | `mango/target/mango-static-report.json` |
| 文档 | STANDARD 记录结构 | `node mango-pmo/tools/check-standard-delivery-record.mjs mango-docs/evidence/2026-10-27-issue-1130-api-resource-priority/standard-delivery-record.md` | PASS | 命令输出 |
| 文档 | 能力说明审计 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS | 命令输出 |

## 6.0 真实运行态验证（M12，单体）

环境：本机 MySQL 8.4.8（`127.0.0.1:3306`，数据库 `mango_dev_mango_issue_1130_024`，workspace slot 24），后端 `mango dev start backend`，端口 `18024`，真实 Spring Security 过滤链，请求不带 `Authorization`。

| 步骤 | 命令 | 实际结果 | 证明的要求 |
|---|---|---|---|
| 1. 注解 Controller 匿名访问 | `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:18024/captcha/types` | `200`，响应体返回真实 `types` 列表 | AC-1130-01：`@ApiAccess(PUBLIC)` 的 Controller 扫描记录（`source=CONTROLLER_SCAN`）真实生效，无需重复 `API_RESOURCE` |
| 2. 对照组（权限接口） | `curl ... http://127.0.0.1:18024/system/tenants` | `401` | 证明安全链真实生效，步骤 1 的 200 不是全局放行 |
| 3. 人工接管 | 插入 `manual-control / GET /captcha/arithmetic / MANUAL / LOGIN`，重启后端（清运行时缓存） | `HTTP 401`，且 `mango-captcha` 的 `CONTROLLER_SCAN/PUBLIC` 记录仍为 `status=1` | AC-1130-03：人工接管在运行态覆盖同路由的 Controller 扫描，即使低优先级记录仍 active |
| 4. 作用范围 | `curl ... /captcha/types` | `200` | 人工接管只影响目标路由，没有全局副作用 |
| 5. 释放后恢复 | 将人工记录置 `status=0`，重启后端 | `HTTP 200` | AC-1130-06：停用人工接管后代码声明重新生效 |
| 6. 收尾 | 删除验证记录、`mango dev stop` | 残留人工记录 `0`，后端已停止 | 环境无脏数据 |

补充说明：`mango dev start backend` 使用 `verify bootstrap generation` 模式启动，未重放资源同步，因此步骤 3 中两条记录同时 active，正好证明“运行时按来源优先级选择”，而不是依赖同步阶段删行。

## 6.0.1 权限标识页面编辑功能（真实验证）

新增“权限标识”页面编辑动作：编辑访问模式/权限码后保存即人工接管，行上提供“解除接管”。

| 步骤 | 操作 | 实际结果 |
|---|---|---|
| 1 | 匿名 `GET /captcha/arithmetic` | `HTTP 200`（编辑前基线） |
| 2 | 真实 HTTP `POST /authorization/api-resources/manual`，`accessMode=LOGIN` | `{"code":200,"success":true}` |
| 3 | 立刻匿名 `GET /captcha/arithmetic` | `HTTP 401`，未重启服务 |
| 4 | 真实 HTTP `PUT /authorization/api-resources/manual/release?...` | `{"code":200,"success":true}` |
| 5 | 立刻匿名 `GET /captcha/arithmetic` | `HTTP 200`，未重启服务 |
| 6 | 数据库复核 | `manual-control/MANUAL/LOGIN/status=0`；`mango-captcha/CONTROLLER_SCAN/PUBLIC/status=1` |

结论：编辑和解除接管都在下一次请求立即生效；人工接管使用独立 `manual-control` 模块行，不覆盖 Controller 扫描行，所以释放后无需等待重新同步。

前置条件说明：`/authorization/api-resources/manual*` 需要 `system:api-resource:manual` 权限。本次没有可用登录凭据，验证时用两条临时公开记录打开这两个端点，验证完成后已删除（残留人工记录 `0`），没有改动生产配置。

直接改库的结论（回答“直接改这里的权限是否生效”）：**不生效且不安全**。运行时按 `activeResourceCache` 和按 method+path 的决策缓存提供结果，改库不会让缓存失效；下一次资源同步还会用声明把该行覆盖回声明值。人工控制必须走接管接口。

## 6.0.2 浏览器端走查（M13，admin 登录）

环境：`mango dev start backend`（18024）+ `mango dev start frontend`（30024），浏览器 Chrome，账号 `admin`。脚本：`api-resource-edit.e2e.mjs`，截图目录 `screenshots/`。

| 步骤 | 操作 | 页面结果 | 匿名 HTTP 结果 |
|---|---|---|---|
| 1 | 打开 `#/system/api-resource`，按关键词 `arithmetic` 查询 | 列表出现目标行，访问模式“公开”，来源“Controller 扫描” | `200` |
| 2 | 行内点“编辑”，选“登录”，填接管说明，点“保存并接管” | 行变为 来源“人工登记”、访问模式“登录”，出现“解除接管”动作；弹框关闭并提示成功 | **`401`** |
| 3 | 行内点“解除接管”并确认 | 扫描行恢复“Controller 扫描 / 公开”；原人工行变为“禁用”；列表正常刷新 | **`200`** |

- console error：`0` 条，无 Vue error、无 4xx/5xx 资源加载失败。
- 匿名状态用独立的无 Cookie 请求上下文测量，避免浏览器登录态干扰。

截图：`screenshots/01-list-before.png`、`02-dialog.png`、`03-dialog-filled.png`、`04-after-save.png`、`05-release-confirm.png`、`06-after-release.png`、`07-final-state.png`。

## 6.1 测试最小化与证明力

规则：一个测试必须证明多个需求要点，且修复回退后必须失败；证明不了本次行为的测试不保留。

| 保留的测试 | 测试数量 | 一次覆盖的需求要点 | 回退修复后是否失败 |
|---|---:|---|---|
| `ApiResourceServiceImplIntegrationTest#manualTakeoverShouldDominateAllOtherSourcesAndStayReleasable` | 1 | R1 多来源共存、R2 同路由唯一有效、R3 人工接管最高、R4 人工记录不被同步覆盖、R9 释放后代码声明恢复 | 是（已用回退排序、移除人工写保护、移除释放停用三次变异验证） |
| `ApiAccessResourceDiscovererTest#fixedWildcardShouldNotSuppressExactControllerScan` | 1 | R8 固定通配不再提前屏蔽 Controller 精确路径（扫描阶段） | 是（恢复双向匹配后失败） |
| `ApiResourceAuthorizationManagerTest#externalPathDecisionMatrixCoversAllResolutionRules` | 1 | R5 人工接管跨路径生效、R6 应用路径优先于 Gateway 默认、R7 Gateway 路径兜底 | 是（恢复 Gateway 优先后失败） |
| `AccessEvaluatorTest#check_shouldResolveExternalPathDecisionMatrix` | 1 | 同上三点，覆盖 access 过滤器实际使用的决策实现 | 是（恢复 Gateway 优先后失败） |

| 删除的测试 | 删除原因 |
|---|---|
| `manualResourceShouldOverrideFixedResourceOnTheSameRoute` | 与人工接管主测试重复，只验证同一断言的子集 |
| `manualResourceShouldNotBeOverwrittenByControllerScan` | 同上，且未覆盖释放与固定声明冲突 |
| 提前合并的 `manualTakeoverShouldWin...` 拆分版本 | 与 R1-R4/R9 单测重复 |
| `applicationPathShouldTakePrecedenceOverGatewayPath`、`manualGatewayDecisionShouldOverrideApplicationDecision`、`gatewayPathShouldRemainFallbackWhenApplicationPathIsUnmatched` | 三个用例合并为一个决策矩阵测试，一次覆盖三点 |
| `fixedWildcardShouldNotSuppressExactControllerRoute`（Service 层） | 变异证明其在旧实现下也通过，无法证明本次修复 |
| 同名的两个 `AccessEvaluator` 新增用例 | 合并为一个决策矩阵测试 |

删除依据：变异测试结果显示 Service 层 `routeMatches` 双向改精确在现有 SQL 路径下不可达，该处改动已一并撤销，避免保留无测试可证明的代码。

## 7. 例外与剩余风险

- 未执行真实 Gateway + 单体的跨进程联调：未启动 `mango-gateway-app` 微服务拓扑（缺服务发现和下行服务实例）。`/api` 前缀回落与人工接管跨路径只在决策层单测覆盖，跨进程实体结果保留为 `未验证`。
- 人工接管与释放接口（`POST /authorization/api-resources/manual`、`PUT .../manual/release`）需要 `system:api-resource:manual` 权限；本次没有可用登录凭据，真实运行态用等价的数据库操作验证决策效果，HTTP 接口本身由 Service 集成测试覆盖。
- 升级后历史 `MANUAL` 记录获得最高运行时优先级，这是目标语义。若某环境依赖旧的 `FIXED_DECLARATION > MANUAL` 排序，升级前需核查同路由的人工记录。
- 释放人工接管只停用记录；Controller 声明需要下一次资源同步重建，以避免在没有代码上下文时伪造访问模式。
- 权限标识页面已提供编辑（人工接管）和解除接管入口；更细的运营功能（批量接管、接管审计视图）未包含在本次范围。
