# 模块重构分析验收与提交前复核记录

## 1. 2026-10-05 实施验收结论

- 分支：`chore/module-refactor-analysis`
- 基线：`8a8e80fd9`
- 工作区：复用既有非 `main` worktree `/Users/hardy/Work/mango-module-refactor-analysis`
- 自动化结论：Java 21、Redis、分页、Flyway/MySQL、API、Bootstrap、文件预览和全 Reactor 验证均完成；最终 `mvn clean verify` 为 `BUILD SUCCESS`。
- 当日验收状态：自动化验证完成，当日未执行提交、发布或人工审批动作。下文第 2–11 节保留该阶段证据；提交前新增事实与修正见第 12 节，最终 head 的检查结果以 PR 回读记录为准。

本记录只保存结果摘要和可追溯入口，不保存运行日志、数据库密码、令牌、SM4 密钥或其它凭据。原始 Maven 日志位于本机 `/tmp`，不作为仓库资产提交。

## 2. 范围与不处理范围

### 2.1 本次范围

1. 统一后端 Maven Java 21 编译基线，使用 `--release 21`，移除已识别的 Java 17 覆盖。
2. 修复 Redis 自动装配开关、KV Memory fallback 和相关测试资产路径。
3. 修复 `PageResult.of` 的零/负分页大小除零风险。
4. 修复 Workflow legacy migration 测试 fixture、执行数量断言和 API fingerprint 契约基线；不改生产历史 migration。
5. 修复 Bootstrap 公共 API 声明版本断言，使其跟随 `@ApiAccess.version()`。
6. 使独立文件预览在缺少可选 `FileSettingsApi`/`ConvertApi` 时保持直接预览可用，并为异步任务启用 KV locker；适配真实外部 `/api` 前缀和 302 语义。
7. 修正管理端登录 E2E 的可见语义选择器。
8. 更新受影响模块 README 和历史验收基线补充，记录配置、跳过测试和独立风险。

### 2.2 不处理范围

- 不修改生产 `workflow-1.0.20`、`workflow-1.0.21`、`workflow-1.0.22` 历史 migration。
- 不处理 `infra-fileproc` 历史 `DeadlockLoserDataAccessException`；该项单独归档，不混入本分支生产代码修改。
- 不在 `main` worktree 工作，不创建第二个 worktree。
- 不把环境条件导致的 skipped tests 计为通过。
- 不记录或提交任何密码、token、密钥、数据库连接凭据或构建产物。

## 3. 可观察要求与结果

| ID | 入口/参与者 | 可观察要求 | 失败语义 | 结果 |
|---|---|---|---|---|
| ACC-001 | Maven parent/reactor | 所有受 parent 管理的 Java 模块以 Java 21 编译，不能残留 Java 17 override | 编译或字节码版本不一致即失败 | PASS |
| ACC-002 | KV 自动配置 | `mango.redis.enabled=false` 不创建 `RedissonClient`；`type=auto` 回退 Memory | 禁止因显式关闭仍连接 Redis | PASS |
| ACC-003 | Common 分页 | `PageResult.of(..., size <= 0)` 不抛除零异常，页数为 0 | 分页计算异常即失败 | PASS |
| ACC-004 | Workflow migration/API | legacy fixture 可完成真实 V1/V2/V3 测试语义；HTTP fingerprint 只在既有声明变化时更新 | 修改生产历史 SQL 或改变 HTTP 契约即失败 | PASS |
| ACC-005 | Bootstrap declaration | 公共 API 资源版本与 `ApiAccess.version()` 一致 | 固定错误版本或资源声明漂移即失败 | PASS |
| ACC-006 | File Preview app | 缺少可选转换能力时直接预览仍可用；转换任务给出明确失败语义；KV locker 已配置 | 启动期误报可选 Bean 缺失或异步任务无协调能力即失败 | PASS |
| ACC-007 | MySQL/Flyway | 隔离数据库可从空库形成，Flyway 幂等，真实 MySQL 并发用例通过 | 迁移失败、重复执行非零或数据约束失败即失败 | PASS |
| ACC-008 | 真实 API/E2E | 登录、用户信息、菜单、授权 decision、文件预览和 Chromium 用户可见流程保持通过 | 不得用路由 mock 或只看状态码代替业务断言 | PASS |
| ACC-009 | 全量交付 | 全 Reactor `clean verify` 成功，失败/错误为 0；skipped 单独登记 | skipped 不得被误报为通过 | PASS（23 个环境 skipped 已登记） |

## 4. 技术决定、兼容性与回退

| ID | 决定 | 影响 | 回退方式 |
|---|---|---|---|
| DEC-001 | parent compiler 保留 source/target 21，并增加 `release=21` | 全仓编译与发布字节码统一到 Java 21；因此必须做完整 Reactor 验证 | 回退 parent/POM 改动并恢复原 override；不得只回退单个模块 |
| DEC-002 | Redis auto-configuration 增加 `mango.redis.enabled` 条件 | 显式关闭 Redis 时 Redisson 不装配，KV auto 使用 Memory | 回退条件和新增测试；不改变 `IKvStore` API |
| DEC-003 | PageResult 零 size 返回 0 pages | 防止除零，保留 page/size/total 的原值语义 | 回退实现并保留新增测试作为缺陷证据 |
| DEC-004 | 可选协作能力使用 Spring `@Nullable` 构造器参数（提交前修正，见第 12 节） | FileSettings/Convert starter 缺失时直接预览不被阻断；转换不可用时任务明确 `FAILED` | 回退到强依赖会恢复独立预览启动阻断，不作为本次推荐方案 |
| DEC-005 | File Preview 宿主显式启用 `mango.kv.capability.locker=true` | 异步预览任务具备跨实例 lease/worker slot 协调能力 | 回退配置会使应用缺少 `ILeaseLocker`，启动应失败而不是静默降级 |
| DEC-006 | Workflow 只修测试 fixture/断言和 fingerprint | 不改变生产 migration、HTTP path/verb/binding/permission/response | 反向移动测试或恢复断言；不新增 fallback migration |
| DEC-007 | API declaration version 跟随注解 | 保留既有 Branding API version 2，不把产品 API 强行改成 version 1 | 只回退测试断言会重新制造错误契约，不推荐 |
| DEC-008 | 测试文件只移动路径，不改变 package 语义 | 统一测试目录边界，不改变运行时类名或产品代码 | 反向移动文件即可 |

## 5. 实施路径

- `mango/mango-parent/pom.xml` 及受影响 infra/context/crypto/org/system POM：Java 21/release 清理；根 `mango/pom.xml` 未修改。
- `mango/mango-infra/mango-infra-kv/**`：Redis 条件、测试和 README。
- `mango/mango-common/**`：分页实现与单测。
- `mango/mango-platform/mango-workflow/**`：legacy test fixture、迁移断言和 API surface test。
- `mango/mango-app/monolith/mango-monolith-app/**`：Bootstrap declaration test。
- `mango/mango-platform/mango-file-preview/**` 与 `mango/mango-app/microservice/mango-file-preview-app/**`：可选依赖、locker 配置、Flow 测试和 README。
- `mango-ui/apps/mango-admin/e2e/specs/auth-info.spec.ts`：用户可见登录/租户选择器。
- `mango/mango-infra/mango-infra-test/**`、`mango/mango-platform/mango-resource/mango-resource-core/src/test/**`：测试资产路径规范化。
- `mango-docs/evidence/**`：最终结果、模块补充和例外记录；不放运行日志副本。

## 6. 验证证据

### 6.1 完整 Reactor

验证命令使用 Java 21、隔离 MySQL、显式 `MANGO_DB_USERNAME=root`、本机数据库地址和空密码环境；密码值未写入本记录，且验证前执行 `unset MANGO_CRYPTO_SM4_SECRET_KEY`：

```bash
cd /Users/hardy/Work/mango-module-refactor-analysis/mango
source ../.mango/dev-workspace.env
# 注入本地隔离数据库名、root 用户、127.0.0.1:3306 和空密码；不将凭据写入仓库
unset MANGO_CRYPTO_SM4_SECRET_KEY
mvn clean verify
```

- 日志：`/tmp/mango-clean-verify-final-java21-redis-20261005-8.log`
- 结果：`BUILD SUCCESS`，`MAVEN_EXIT_CODE=0`。
- 去重口径：Maven 同时输出测试类结果和模块汇总结果；按测试类报告去重为 3,116 tests，Failures 0，Errors 0，Skipped 23。简单把两类输出相加会错误得到 6,232/46，不作为实际用例数。
- JAR 复核：构建产物 Manifest 为 Java 21；构建前使用 clean，未把旧删除 migration/class 带入产物。

### 6.2 skipped 测试登记

以下 23 个 skipped 只表示本地环境没有提供专用外部数据库/系统属性，不表示用例通过：

| 模块/用例 | 数量 | 缺失条件 | 处理 |
|---|---:|---|---|
| `mango-maven-plugin` `BaselineGeneratorIntegrationTest` | 11 | `MANGO_BASELINE_TEST_DB_URL` | 保留 skipped；需要专用隔离基线数据库时补跑 |
| `mango-maven-plugin` `BaselineGeneratorPerformanceIntegrationTest` | 1 | `MANGO_BASELINE_PERF_DB_URL` | 保留 skipped；性能验收另行安排 |
| `mango-infra-bootstrap-core` `JdbcBootstrapRepositoryIntegrationTest` | 4 | `mango.bootstrap.test.jdbc-url` | 保留 skipped；需要专用 JDBC 验收参数时补跑 |
| `mango-infra-persistence-starter` `PersistenceColdBaselinePerformanceIntegrationTest` | 1 | `MANGO_BOOTSTRAP_PERF_DB_URL` | 保留 skipped；性能验收另行安排 |
| `mango-org-starter` `OrgPostResourceHandlerMySqlIntegrationTest` | 5 | `MANGO_MYSQL_IT_URL` | 本分支已有独立真实 MySQL 证据，但该通用变量未注入；需要统一环境时补跑 |
| `mango-admin-starter` `BootstrapResourcePerformanceIntegrationTest` | 1 | `MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL` | 保留 skipped；性能验收另行安排 |

### 6.3 定向与真实入口

| 验证 | 结果 | 证据 |
|---|---|---|
| Common/Infra/Platform/App 按顺序定向测试 | PASS | 最终 Reactor 日志及各模块 Surefire reports |
| `FileServiceConcurrentSaveIntegrationTest` | 3/3 PASS | `mango/mango-platform/mango-file/mango-file-core/target/surefire-reports/` |
| `OrgPostResourceHandlerMySqlIntegrationTest` | 5/5 PASS（独立真实 MySQL 环境） | 同名 Surefire report；全量 run 中因另一专用变量未注入而 skipped 的记录不覆盖该证据 |
| Authorization/Identity/Resource Flyway 幂等 | PASS，重复执行 `migrationsExecuted=0` | 任务会话数据库回读记录 |
| Bootstrap plan/apply/runtime | PASS，状态 `FINALIZED`，`executedSteps=5`、`reusedSteps=1` | 任务会话日志和运行回读 |
| 真实登录、用户信息、菜单、Authorization access decision | PASS | `auth-info.spec.ts`、Authorization API/Chromium E2E 结果 |
| `MangoFilePreviewAppFlowTest` | 5/5 PASS | `/tmp/mango-file-preview-app-flow-20261005-4.log` |
| Workflow migration/API 定向回归 | PASS | `WorkflowMigrationUpgradeIntegrationTest`、`WorkflowApiSurfaceContractTest` Surefire reports |
| Redis disabled/Memory fallback | PASS | `KvRedisAutoConfigurationTest`、`KvStoreAutoConfigurationTest` |
| Test quality | PASS | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`，14 file(s) |
| Diff whitespace | PASS | `git diff --check` |
| README module/source facts | PASS | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs` |

## 7. 功能验收台账

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| MRA-001 | TC-001 | Maven parent/reactor | Java 21 编译与打包 | Java 21、clean workspace、隔离 MySQL | `--release 21` 生效；最终 Reactor `BUILD SUCCESS`；失败和错误均为 0 | 无独立 UI；产物 Manifest 为 Java 21 | Maven 构建无未解释编译或测试错误 | `/tmp/mango-clean-verify-final-java21-redis-20261005-8.log` | PASS |
| MRA-002 | TC-002 | KV auto-configuration | Redis disabled 与 Memory fallback | `mango.redis.enabled=false`、`type=auto/redis` | 不创建 `RedissonClient`；auto provider 为 `MemoryKvStore` | 无独立 UI；配置语义由上下文测试断言 | ApplicationContextRunner 无启动错误 | `KvRedisAutoConfigurationTest`、`KvStoreAutoConfigurationTest` Surefire reports | PASS |
| MRA-003 | TC-003 | File Preview app Flow | 直接预览、302 和可选转换能力 | 独立 app、随机引擎文件名、外部 `/api` 前缀 | Flow 5/5；直接预览返回预期内容；转换依赖缺失给出明确失败语义 | 预览入口和重定向链路可观察；不依赖路由 mock | HTTP 302/200 链路无未解释失败；测试仅剥离直连前缀 | `/tmp/mango-file-preview-app-flow-20261005-4.log` | PASS |
| MRA-004 | TC-004 | Workflow migration/API | legacy fixture、Flyway 数量和 HTTP fingerprint | V1/V2/V3 test migrations、现有 API declarations | migration assertions 与当前资源表结构一致；fingerprint 仅反映既有版本声明 | 无独立 UI；HTTP 契约由 Controller surface test 断言 | 定向 Maven 测试无失败/错误；生产历史 SQL 未改 | `WorkflowMigrationUpgradeIntegrationTest`、`WorkflowApiSurfaceContractTest` reports | PASS |
| MRA-005 | TC-005 | Bootstrap declaration/API | 公共 API 资源版本契约 | `ApiAccess.version()` 现有声明 | 资源 version 与注解版本一致；path、verb、权限和包装保持 | 真实 API/Chromium 用户流程已验证登录、用户信息和菜单 | API 请求与浏览器流程无未解释失败 | `BootstrapPublicApiDeclarationContractTest`、`auth-info.spec.ts` | PASS |
| MRA-006 | TC-006 | MySQL/Flyway/业务入口 | 并发、租户、迁移幂等和 Bootstrap runtime | 隔离 MySQL 数据库、root 用户、tenant fixtures | 文件并发 3/3、组织 MySQL 5/5；Flyway 重跑 0；Bootstrap `FINALIZED` | 无独立 UI；真实 API/E2E 作为入口补充 | 数据库健康和 API 结果无未解释 4xx/5xx | MySQL Surefire reports、Bootstrap 回读记录 | PASS |
| MRA-007 | TC-007 | 全 Reactor 与交付审计 | 全量测试、测试质量、README 和 diff | Java 21、Redis、隔离 MySQL；环境 skipped 单独登记 | 去重后 3,116 tests，failures/errors 0；23 skipped 已列原因 | 无独立 UI；文档审计和工作区检查可复核 | `git diff --check`、test-quality、README audits 均通过 | 最终 Maven log、`acceptance-evidence.md`、`log-index.md` | PASS |

## 8. 文档与能力说明

- 能力地图已有 File Preview 入口，不新增能力分类；提交前补充 Java 21 和 Common 分页边界，三个业务指南同步使用或无运行时影响说明。
- `mango/mango-platform/mango-file-preview/README.md` 已补充 `ILeaseLocker`、`mango.kv.capability.locker=true` 和可选转换能力的当前运行边界。
- `mango-docs/evidence/baselines/infra-kv/latest/acceptance.md` 已补充 Redis disabled/Memory fallback 证据。
- `mango-docs/evidence/baselines/file-preview/latest/README.md` 已补充可选依赖、locker 和独立 app Flow 证据。
- `mango-docs/evidence/baselines/workflow-architecture/latest/report.md` 已补充 legacy fixture/API fingerprint 的当前分支说明。
- 未修改长期 PMO 规则；未新增规则入口，因此不改 `mango-pmo/rules/index.json`。

## 9. 独立问题与剩余风险

1. **`infra-fileproc` deadlock**：历史定向运行曾出现 `DeadlockLoserDataAccessException`，属于独立问题；最终 clean verify 未再出现该失败。本分支未修改 `infra-fileproc` 生产代码，后续应在其 own acceptance ledger 中单独复现、定位和验收。
2. **23 个 skipped**：缺失专用环境条件，不能作为完整全绿的替代证据；如发布门禁要求无 skipped，必须提供对应隔离数据库/系统属性并重跑。
3. **本地环境密码**：`.mango/dev-workspace.env` 没有可用密码配置；本次使用隔离本机数据库的空密码条件完成验证，未把该值写入仓库或证据。
4. **人工责任**：本记录的三轮意见和同行检查是 Agent 角色视角分析，不是人工审批；若后续进入提交、合并或发布流程，再由对应责任人按项目流程处理。

## 10. 三轮角色视角分析

> 以下为按 PMO 规则组织的隔离角色视角分析，不冒充真实专家、人工理解或审批。

### 第一轮：事实与用户视角

- 参与视角：Dev/QA Agent 的事实核对视角。
- 来源：当前分支代码、模块 README、能力地图、测试源码、Surefire XML、最终 Maven 日志和历史验收基线。
- 结论：用户要求的 Java 21、Redis、分页、Flyway/MySQL、API、E2E 与 Reactor 入口均有对应实现或证据；现有 HTTP、JSON、权限、租户和业务语义保持为兼容目标。
- 不确定项：外部生产部署、真实发布包消费和人工现场验收不在本地证据内。
- 输入下一轮：重点检查全局 parent 影响、可选 Bean、数据库 migration 和权限/租户边界。

### 第二轮：技术与风险视角

- 参与视角：Tech Lead/安全与数据边界的 Agent 角色视角。
- 来源：parent/root POM、Workflow migration 目录、File Preview gateway/task、KV auto-configuration、API fingerprint、真实 MySQL/Flyway 结果。
- 结论：parent 变化已触发完整 Reactor；可选能力在当日使用 `ObjectProvider`（提交前改为 `@Nullable`，见第 12 节），核心 locker/token 能力仍显式要求；Workflow 只改测试 fixture/断言；权限、租户、Flyway 和历史 migration 未被绕过。
- 反对意见/冲突：把 V2/V3 缺失列直接补进生产历史 SQL，或为独立预览恢复进程内 fallback，会掩盖真实升级/多实例风险；均拒绝。
- 输入下一轮：确认每项风险有可观察验证、skipped 未被误报、文档和回滚路径可追溯。

### 第三轮：验证与交付视角

- 参与视角：QA/交付审计的 Agent 角色视角。
- 来源：最终 `clean verify` 日志、定向测试报告、真实 MySQL 结果、Bootstrap 回读、Chromium E2E、test-quality、README 审计和 git 状态。
- 结论：自动化阻断项为 0；最终日志去重后 3,116 个测试类用例中失败/错误为 0，23 个 skipped 已按环境条件登记；文档已同步受影响公开配置和例外。
- 未解决事项：23 个专用环境用例尚未补跑；提交、合并或发布时的责任人审批不属于本次自动化验证。
- 输入最终方案：本记录只标记“自动化验证完成”，不能写成“所有测试无 skipped”或“已发布”。

## 11. 最终方案与同行检查

### 11.1 采用与拒绝

- 采用：Java 21 `--release`、Redis 开关条件、分页零 size 保护、可选文件预览协作能力、显式 locker、测试资产规范化、测试 fixture 修复和注解版本跟随。
- 拒绝：修改生产历史 migration、恢复进程内 token/任务协调 fallback、固定公共 API version=1、修改与本任务无关的 `infra-fileproc` 生产实现、将环境 skipped 删除或强行改成通过。

### 11.2 影响、恢复和验证

- 影响范围：Maven parent/infra/platform/app、KV/File Preview/Workflow/Resource 测试资产、管理端登录 E2E、模块 README 和验收证据。
- 运行时回退：回退对应 Java/POM、KV 条件和 File Preview 可选依赖提交；数据回退不涉及新增生产 migration。
- 验证标准：完整 Reactor 成功；受影响定向测试通过；真实 MySQL/Flyway、Bootstrap、API、Chromium 入口有独立结果；skipped 和独立 deadlock 明确登记。

### 11.3 同行检查记录

- 检查人：Agent 角色视角模拟同行检查；不是独立真人 PR Review。
- 事实准确性：通过；已用最终日志/Surefire XML 去重并更正 6,232/46 的重复统计。
- 范围越界：未发现；生产 Workflow 历史 migration 和 `infra-fileproc` 生产代码均未改。
- 风险遗漏：提出并保留 23 skipped、空密码本地条件、独立 deadlock、人工审批四项剩余风险。
- 验证充分性：通过自动化目标检查；性能类专用外部环境和人工发布审批不由本地证据替代。
- 文本清晰性：通过；模块 README、能力地图入口和最终验收记录可相互追溯。
- 阻断问题：无自动化阻断问题。
- 非阻断建议：合并前由真实 Dev/QA/Tech Lead 完成 PR Review；若门禁禁止 skipped，补齐六组专用环境后重跑。
- 当日检查结论：自动化验证完成；不代签人工批准或发布。

## 12. 提交前复核与修正

- 用户授权：提交 PR；只包含本地 commit、任务分支 push、PR 创建和回读，不包含合并、审批、发布或部署。
- 已形成任务提交 `8d004bb5a`，并以非破坏性 merge 合入 `origin/main` 的 `06aa9c1ee`，产生 `e581240de`；主 worktree 未修改。
- 首次提交前 `clean verify` 在 KV 测试阶段被 2,400 秒超时中断，不能计为通过。重新核验 Locker/Counter 为 21/21，后续完整执行采用 `caffeinate` 避免休眠干扰。
- 2026-10-07 完整复验的用例无失败/错误，但静态门禁发现两个新增 `CT_CONSTRUCTOR_THROW`：`FilePreviewFileGateway` 和 `FilePreviewTaskServiceImpl` 在构造器中解析 `ObjectProvider`。该次结果是 `BUILD FAILURE`，不以 10 月 5 日结果替代。
- 修正为 Spring `@Nullable` 参数注入，移除本次新增的 provider 重载，保留原有构造器签名、可选能力语义和强制 locker/token 依赖。未增加 SpotBugs 抑制或修改债务基线。定向 core/Flow 复验 24/24，含新增 Office 缺少转换能力时的明确失败回归。
- 带真实 PR body 的能力文档检查发现缺失覆盖，已补充能力地图及文件上传、业务审批、租户基础数据三个指南；未带 PR body 的检查结果不能替代此门禁。
- 额外 `pnpm check:affected` 的改动文件检查通过，共享 `check:static` 因 23 条存量 ESLint diagnostic identity 失败。相关 9 个文件及 lockfile、checker/config 与 base 逐字节相同，独立登记 [Issue #1010](https://github.com/HardyDou/mango/issues/1010)，不混入本次修复。required `frontend-pr-quality` 的 PR job 已由主分支暂停为 notice，本任务没有修改或停用该检查，也不把 notice 算作前端全局质量通过。
- 最终提交验证使用工作区 `.runtime/maven-repository` 隔离本项目可变制品。第三方缓存复制时排除 `io/mango`，当前源码重新安装 gate 与生成项目验收前置制品；不复用共享仓库的 Mango SNAPSHOT 作为提交证据。

### 三视角与方案决定

| 视角/参与者 | 来源、结论与不确定项 | 反对意见与下一步 |
|---|---|---|
| 事实与用户 / Agent | 用户要求提交既有任务；diff 和本地 gate 明确定位到新增可选依赖构造器及缺失能力说明。旧绿灯不能覆盖新失败。 | 不扩大至无关前端诊断；记录 Issue 后回到提交目标。 |
| 技术与风险 / Agent | Spring 可空参数注入保持启动时依赖解析和原签名，不在对象构造内执行 provider 查找。无新迁移或 HTTP/权限/租户变化。 | 拒绝全局抑制、放松检查和更新债务预算；以应用 Flow 与静态检查确认。 |
| 验证与交付 / Agent | 使用当前源码与隔离制品仓执行 required workflow 同源入口，最终命令、环境、SHA 和结果记录到 PR。 | 任一适用 required 本地门禁失败都停止 push；23 环境 skipped 继续单列，外部 CI 待回读。 |

评审选择：已在会话询问是否需要外部同行评审；未指定，按 `AGENT_ONLY` 继续。上述分析不称为独立专家或人工审批。回退为反向撤销任务提交；本任务不执行数据回滚、发布或合并。最终 head 尚需通过同源全量门禁后才可提交 PR。
