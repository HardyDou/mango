# Mango Resource 历史债务治理验收证据

## 1. 结论

- 验收日期：2026-07-16
- 工作区：`/Users/hardy/Work/mango-resource-debt`
- 分支：`refactor/resource-debt`
- 结论：Resource 八子模块的单元/集成、API、全新 MySQL、真实浏览器 E2E、直接消费者编译和定向质量检查均满足交付要求。
- 范围：不执行全仓检查；覆盖 Resource 八子模块、19 个直接 Java 消费者、真实单体组装入口和本次 E2E 暴露的 Notice 渠道幂等修复。

## 2. 改前与改后基线

| 层级 | 改前 | 改后 |
|---|---:|---:|
| Resource 单元/H2 集成 | 48/48 PASS | 50/50 PASS |
| Resource 远程适配（包含在聚合统计） | 9/9 PASS | 6/6 PASS；原服务端测试迁入 target-core 后 3/3 PASS |
| Notice 渠道真实租户拦截器集成 | 未覆盖跨租户重复重放 | 6/6 PASS |
| 架构问题 | 149 | 0 |
| 静态问题 | 58 | 0 |
| 浏览器 E2E | 无可用基线 | 1/1 PASS |

Resource 新增的两条测试固定以下风险：V1 必须直接包含三张表的租户/审计最终结构；源码和 clean 后的构件中都不得残留 V2 审计补丁。

## 3. 自动化验证

### 3.1 Resource 聚合测试

```bash
mvn -q -f mango/mango-platform/mango-resource/pom.xml clean test -DskipTests=false
```

结果：`tests=50, failures=0, errors=0, skipped=0`。clean 后 `target/classes/db/migration/resource` 仅有 `V1__init_resource_registry.sql`。

### 3.2 直接消费者兼容

对 Auth、Authorization、Calendar、CMS、Domain、File、Identity、Job、Notice、Numgen、Org、Payment、System、Template、Workflow 的 19 个直接消费者执行同一 Reactor 编译，结果 PASS。Authorization 的 `FrontendRuntimeResourceSyncIntegrationTest` 2/2 PASS，证明消费者通过公开 `IResourceRegistryService` 注册声明，且测试 schema 已跟随租户/审计契约。

### 3.3 Notice 强制重放回归

```bash
mvn -q -f mango/mango-platform/mango-notice/mango-notice-core/pom.xml \
  -Dtest=NoticeChannelResourceHandlerIntegrationTest -DskipTests=false test
```

结果：6/6 PASS。测试启用真实 MyBatis 租户拦截器，以 `caller-tenant` 调用上下文连续同步 `default`、`1` 两个声明租户；目标表始终只有两行，且调用后恢复原租户上下文。生产代码未使用忽略租户检查的注解。

### 3.4 定向质量检查

八模块 partial Reactor 报告：`dependency=0`、`archunit=0`、`pmd=0`、`blocking=0`；聚合静态检查 `totalIssueCount=0`、`toolFailureCount=0`，BUILD SUCCESS。检查范围包含新增 `target-core/target-starter`，不包含全仓。

## 4. 全新 MySQL 验收

数据库：`mango_dev_mango_resource_debt_001`，验收前执行 drop/create 后由应用启动。

- `flyway_schema_history_resource`：baseline + `V1 init resource registry`，均 `success=1`，无 V2。
- `resource_registry`、`resource_sync_log`、`resource_change_log` 均含 `tenant_id/org_id/created_by/created_at/updated_by/updated_at`。
- 显式启用 demo 后完成 1782 条 active 声明注册；管理员角色 4 条、角色菜单关系 293 条。
- Notice 渠道保持两条：`270501/default/SITE/INTERNAL`、`270502/1/SITE/INTERNAL`。
- 真实 token 调用 `DELETE /resource/registries?physical=false`（缺少 `resourceId`）返回 HTTP 400、业务 code 400，不再因 API/Controller 参数校验冲突产生 500。
- 真实 token 调用 `POST /resource/sync/force` 返回 HTTP 200、`data=true`；修复前该调用因错误租户上下文重复插入 Notice 固定主键而返回 500。

## 5. 浏览器端到端验收

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui --filter mango-admin exec \
  playwright test e2e/specs/infra-kv-resource-registry.spec.ts \
  --project=chromium --workers=1 --reporter=line
```

结果：1/1 PASS，48.8 秒。覆盖：

- 真实 `admin/admin123` 登录与首页跳转；
- 菜单管理页面可见且菜单接口 200；
- Resource Registry 类型过滤、同步日志、Handler Spec 接口；
- 缺参删除稳定返回 400；
- 强制同步稳定返回 200/true；
- console error、Vue warning、page error、非预期页面 4xx/5xx、request failure 均为 0。

## 6. 验收边界

- Flyway 只负责 Resource DDL；正式资源和 demo 资源仍由 Resource Registry 分目录、分开关登记。
- HTTP API 与本地动态 SPI 已分离；跨模块本地 Provider/Handler 不再污染公开 API。
- CLI 端口就绪不等于资源派生完成；本次等待 `ResourceRegistryService` 输出 1782 条声明同步完成后才执行 API/E2E。
- `mango.dev.json` 的定向安装改动已恢复；demo 启动参数仅存在于忽略的工作区环境文件，交付前恢复默认关闭。
