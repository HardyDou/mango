# Issue #1084 老库增量升级真实验收证据

> 状态：**#1084 代码语义 PASS；预发布验收仍被两个既有缺口阻断**。记录日期：2026-10-22。
> 覆盖 MySQL `8.4.8` + 本机保函测试库 `baohan_test` 真实数据副本的完整 Bootstrap 纵向链路，不代表生产实例。

## 1. 验收范围

- 目标：证明 `V3__normalize_resource_registry_unique_keys.sql` 与 `ResourceRegistryService` 的 `NULL tenant_id` 兼容逻辑在**真实老库**上成立。
- 使用真实业务数据：本机保函测试环境库 `baohan_test`（2652 行 `resource_registry`，其中 2652 行 `tenant_id IS NULL`，2634 行 `ACTIVE`，`flyway_schema_history_resource` 只有冷基线 `2`，唯一键仍为 `(tenant_id, resource_id)` / `(tenant_id, resource_type, biz_key)`）。
- 不做破坏性操作：原库只读；全部升级动作在 `mysqldump` 副本上执行。
- 不在范围内：数据库基线生成契约（`mango:baseline-generate`）、CI 门禁、生产切流与回滚。

## 2. 执行环境

| 项 | 取值 |
|---|---|
| MySQL | `8.4.8`（本机 127.0.0.1:3306，`MODE` 默认严格） |
| Java / Maven | OpenJDK `21.0.12.1` / Maven `3.9.13` |
| 制品 | worktree `mango-issue-1084`，分支 `fix/issue-1084-resource-incremental-upgrade`（`b2ccf06e9`，内容与 `origin/main` 的 `fa30badd0` 一致），`mvn -f mango/pom.xml -pl :mango-monolith-app -am -DskipTests package` |
| 入口 | `java -jar mango-monolith-app.jar bootstrap plan|apply|finalize`（显式生命周期，非本地托管入口） |
| 老库快照 | `mysqldump baohan_test` → `/tmp/baohan_test_1084.sql`（345 MB，`--single-transaction --quick`） |
| 环境标识 | `--mango.bootstrap.environment-key=baohan-test`（沿用老库 `mango_bootstrap_control` 的 `FINALIZED / 2610200830`） |

## 3. 升级前只读预检（真实数据，脚本可复用）

```sql
-- 1) 全局身份重复：V3 的 ADD UNIQUE KEY 会直接失败
SELECT resource_id, COUNT(*) c FROM resource_registry GROUP BY resource_id HAVING c > 1;
SELECT resource_type, biz_key, COUNT(*) c FROM resource_registry
 GROUP BY resource_type, biz_key HAVING c > 1;
-- 2) 规模快照
SELECT COUNT(*) total, SUM(tenant_id IS NULL) null_tenant,
       SUM(status='ACTIVE') active, SUM(status='REMOVED') removed FROM resource_registry;
SHOW INDEX FROM resource_registry;
```

实测结果：`resource_id` 重复 0 组，`resource_type + biz_key` 重复 0 组；2652 / 2652 / 2634 / 18。

另做一次离线身份审计（本仓 64 个声明文件 vs 老库 2652 行）：162 行 `id` 完全一致，**1 行同 `type + bizKey` 但 `id` 不同**（见 5.1），2489 行属于业务模块、当前平台制品不声明。

## 4. 用例与断言

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| ISSUE-1084 | TC-001 | `bootstrap plan/apply/finalize`（`mango-monolith-app`） | 真实老库滚动升级：`V3` 唯一键迁移与 Bootstrap 生命周期 | 副本 `mango_issue1084_live2`（`baohan_test` 快照，2652 行、`tenant_id` 全 NULL、基线版本 2） | `flyway_schema_history_resource` 新增 `3 / success=1`；`uk_*` 只剩 `(resource_id)`、`(resource_type, biz_key)`；`mango_bootstrap_control` 由 `FINALIZED/2610200830` 推到 `FINALIZED/2610200831` | 不涉及页面交互，后端进程生命周期 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-live2b.log`、`/tmp/1084-finalize-live2.log` | PASS |
| ISSUE-1084 | TC-002 | `ResourceRegistryService.disableMissing` | `NULL tenant_id` 历史缺失资源不得阻断 Bootstrap | 预置行 `3001084000000000001`（`IDENTITY_USER`，`tenant_id IS NULL`，未被声明） | 日志 `Mango resource missing disable skipped: tenant scope is unavailable, resourceType=IDENTITY_USER, resourceId=3001084000000000001`；该行仍 `ACTIVE`；目标用户 `business-manager` 仍 `status=1`；无 `resource_sync_log` | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-finalize-live2.log` 第 15202 行 | PASS |
| ISSUE-1084 | TC-003 | `ResourceHandlerInvoker.disable` | 有租户的缺失资源仍按 registry 行租户上下文禁用 | 预置行 `3001084000000000002`（`tenant_id='1'`，`target_id=2026062919100001301`） | 该行 `REMOVED`；`resource_sync_log = DISABLE/SUCCESS "Identity user disabled: changed=true"`；`identity_user.sales-demo-01.status` 由 `1` 变 `0` | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-finalize-live2.log`；库查询输出 | PASS |
| ISSUE-1084 | TC-004 | `bootstrap apply --strategy=cold` | 空库冷启动回归：`V1→V2→V3` 组合 | 空库 `mango_issue1084_cold` | `V1`、`V2`、`V3` 均 `success=1`；`uk_*` 规范化；`state=FINALIZED` | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-cold.log` | PASS |
| ISSUE-1084 | TC-005 | `V3__normalize_resource_registry_unique_keys.sql` | `NULL tenant_id` 物理重复身份下迁移失败必须原子回滚 | 副本 `mango_issue1084_dup` 注入两条同 `SYSTEM_DICT + system.dict.user-sex`、`tenant_id IS NULL` 的行 | 原因：物理重复身份使唯一键添加失败；`V3` 报 `Duplicate entry 'SYSTEM_DICT-system.dict.user-sex' for key 'resource_registry.uk_resource_registry_type_biz_key'`；索引仍是旧定义；`flyway_schema_history_resource` 留 `version=3, success=0`；`control.state=FAILED`；进程 exit 1 | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-dup.log` | EXCEPTION |
| ISSUE-1084 | TC-006 | Flyway 模块迁移 | 失败历史行存在时直接重试必须明确报错 | 删除重复行后重跑 `apply` | 原因：失败历史行未清理；报 `FlywayValidateException: Detected failed migration to version 3 ... run repair to fix the schema history` | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-dup-retry.log` | EXCEPTION |
| ISSUE-1084 | TC-007 | Flyway 模块迁移 | 清理重复行 + 删除失败历史行后可恢复 | `mango_issue1084_dup`：删重复行与 `success=0` 历史行 → `apply` | `V3` `success=1`，迁移阶段通过 | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-dup-recover2.log` | PASS |
| ISSUE-1084 | TC-008 | `mango.resource.registry.fail-on-conflict` | 该开关不能绕过身份冲突 | 老库副本保留冲突行，`--mango.resource.registry.fail-on-conflict=false` | 原因：`V3` 后唯一键已生效，冲突降级为 WARN 后 upsert 撞新唯一键：`Duplicate entry 'API_RESOURCE-api.mango-link.GET.link.open.public-links.list'`，Bootstrap 仍失败 | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-flagoff.log` | EXCEPTION |
| ISSUE-1084 | TC-009 | `bootstrap apply/finalize` + `resource_registry` | 按 14.1 清理冲突行后完整升级且清理无损 | 副本 `mango_issue1084_linkfix`：备份冲突行 → 删除日志与登记行 → `apply` → `finalize` | `apply` 直接 `EXPANDED`；`finalize` `FINALIZED`；新登记行 `resource_id=807245613893456101`、`module_code=link`；新行 `target_id=6673508641616300253` 与清理前一致；`authorization_api_resource` 中该 `resource_code` 仍 1 行、id 与 `access_mode=PUBLIC` 未变 | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-linkfix.log`、`/tmp/1084-finalize-linkfix.log` | PASS |
| ISSUE-1084 | TC-010 | `bootstrap apply`（EXPANDED） | 升级的迁移与扩展阶段对真实历史行零副作用 | 副本 `mango_issue1084_applyonly`：快照 2651 行后执行 `apply --strategy=rolling` | 快照对比：`status` 变化 0、`sync_mode` 变化 0、`target_id` 变化 0；`state=EXPANDED` | 不涉及页面交互，后端进程生命周期 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-only.log` | PASS |
| ISSUE-1084 | TC-011 | `bootstrap finalize` + 真实应用权威 | 真实权威下 `NULL tenant_id` 缺失行被跳过并保持可用 | 副本 `mango_issue1084_faithful2`，`--mango.resource.registry.remote.app-code=baohan-system-backend`，注入控制行 `3001084000000000011`（`IDENTITY_USER`、`tenant_id IS NULL`、未被声明） | 日志 `Mango resource missing disable skipped: tenant scope is unavailable, resourceType=IDENTITY_USER, resourceId=3001084000000000011`；该行仍 `ACTIVE`；22 个模块 `FINALIZE` 全部执行后 `state=FINALIZED` | 不涉及页面交互，后端 handler 行为 | 未启用 Web，无浏览器请求 | `/tmp/1084-apply-control.log`、`/tmp/1084-finalize-control.log` | PASS |
| ISSUE-1084 | TC-012 | `bootstrap finalize`（异应用冒用业务权威） | 反例：不得用异应用制品代替业务应用做升级验证 | 副本 `mango_issue1084_faithful2`，monolith 制品以 `baohan-system-backend` 权威执行 `FINALIZE` | 原因：`API_RESOURCE` handler 无执行租户字段，#1084 跳过分支不适用，`disableMissing` 按语义停用该制品未声明的 1214 条历史行（`API_RESOURCE` 1212 + `JOB_DEFINITION` 2）；该配置不能作为业务升级验收证据 | 不涉及页面交互，反例说明 | 未启用 Web，无浏览器请求 | `/tmp/1084-finalize-faithful2.log` | EXCEPTION |

证据日志：`/tmp/1084-apply-live2b.log`、`/tmp/1084-finalize-live2.log`、`/tmp/1084-apply-dup.log`、`/tmp/1084-apply-dup-retry.log`、`/tmp/1084-apply-dup-recover2.log`、`/tmp/1084-apply-cold.log`、`/tmp/1084-apply-flagoff.log`、`/tmp/1084-apply-linkfix.log`、`/tmp/1084-finalize-linkfix.log`、`/tmp/1084-plan-live.log`。

## 5. 发现

### 5.1 老库存在 1 处全局身份冲突，会阻断升级（已由 #1087 结案，非 #1084 引入）

老库 `resource_registry` 中 `API_RESOURCE + api.mango-link.GET.link.open.public-links.list` 的登记行 `resource_id = 838970718355162904`、`module_code = authorization`；当前制品声明 `resource_id = 807245613893456101`、`module_code = link`。`V3` 把 `(resource_type, biz_key)` 变成全局唯一后，两种身份不能共存，`apply` 在资源同步阶段失败：

```text
Resource bizKey already registered by another id: API_RESOURCE:api.mango-link.GET.link.open.public-links.list
```

该现象已由 Issue #1087 登记并结案：根因是同一资源被两个声明来源声明（业务 `mango.authorization.resource-sync.resources` 与平台 `META-INF/mango/resources/`），决策是**固定 fail-closed + 一次性清理登记行**，不提供自动改身份/别名。本次验收按备份 + 删除旧登记行 + 由新声明重建的方式通过（用例 J），并确认清理无损：新登记行复用同一目标行 `6673508641616300253`。

现场补充：`baohan_test` 截至 2026-10-22 仍保留这条 link 冲突行（`created_at=2026-09-11`，来源 `baohan-system-backend`），即保函环境**尚未执行这条 link 行的清理**；下次包含 `5900fec2f`（平台新增 `link-common-public-api.yml`）的增量发布仍会撞到。本报告未修改该库。

### 5.2 `V3` 失败后缺少 repair 说明（本次已补文档）

MySQL 8 的多子句 `ALTER` 是原子的（索引没被改坏），但 Flyway 会留下 `success=0` 的历史行；直接重试会停在 `Detected failed migration to version 3`。恢复动作是“清理重复数据 + 删除失败历史行（等价 `flyway repair`）”，已补入 `mango-resource/README.md` 14.2。

### 5.3 `mango.resource.registry.fail-on-conflict=false` 在 `V3` 之后不可用作升级绕过

冲突降级为 WARN 后，后续 upsert 直接撞新唯一键并抛原始 SQL 异常，错误信息不再包含冲突双方来源。升级必须先清理身份冲突；该边界已补入 `mango-resource/README.md` 14.1。

### 5.4 不能用“异应用 + 同权威”代替业务应用做升级验证（本次反例）

把 monolith 制品的资源权威改成业务应用（`baohan-system-backend`）后执行 `FINALIZE`，`disableMissing` 会按语义停用该制品未声明的 1214 条历史行（`API_RESOURCE` 1212 + `JOB_DEFINITION` 2）。原因不是 #1084 的缺陷：`API_RESOURCE` 处理器没有 `executionTenantField()`，#1084 的 `NULL tenant_id` 跳过分支对它不适用，而“缺失即停用”本就是 `disableMissing` 的定义行为；monolith 只声明自己的接口，业务库中其余接口自然被判为缺失。

结论：业务库升级必须用业务应用自身制品验证。`apply`（EXPANDED）阶段已验证零副作用（TC-010），停用判定只发生在 `finalize`，且必须与真实声明集合一起看。

### 5.5 `FINALIZE` 会重复处理已 `REMOVED` 的行

`disableMissing` 的行查询不按 `status` 过滤，重复执行 `FINALIZE` 会对已 `REMOVED` 的行再次写入 `DISABLE/SUCCESS` 日志（实测两次 `FINALIZE` 产生约 2 倍日志量）。不影响数据正确性和启动，但会放大 `resource_sync_log` 与 `resource_change_log`。

## 6. 复现步骤（本机）

```bash
# 0) 只读预检（第 3 节 SQL）
# 1) 复制老库
mysqldump -h127.0.0.1 -uroot --single-transaction --quick baohan_test > /tmp/baohan_test_1084.sql
mysql -h127.0.0.1 -uroot -e "create database mango_issue1084_live2 default charset utf8mb4"
mysql -h127.0.0.1 -uroot mango_issue1084_live2 < /tmp/baohan_test_1084.sql
# 2) 构建制品（worktree 内，不 install）
mvn -f mango/pom.xml -pl :mango-monolith-app -am -DskipTests package
# 3) 显式生命周期
export MANGO_DB_URL='jdbc:mysql://127.0.0.1:3306/mango_issue1084_live2?...'
java -jar mango/mango-app/monolith/mango-monolith-app/target/mango-monolith-app-1.0.0-SNAPSHOT.jar \
  bootstrap apply --mango.bootstrap.strategy=rolling --mango.bootstrap.environment-key=baohan-test \
  --mango.release.id=1.0.0.2610200831 --mango.release.revision=b2ccf06e9 --mango.release.generation=2610200831
java -jar ... bootstrap finalize  # 同上参数
```

## 7. 结论

- #1084 的目标行为在真实老库上成立：`V3` 可在 2652 行 `NULL tenant_id` 的真实数据上成功执行；`NULL tenant_id` 的历史缺失资源只告警并保留，不再阻断 Bootstrap；有租户的缺失资源仍按 registry 行的租户上下文完成禁用。
- #1084 的“预发布验收项”可以勾掉“真实老库增量升级”本身，但不能勾掉“能顺利升上去”：真实老库还卡在 5.1 的身份冲突与 5.2 的 repair 步骤上；前者是 #1087 的既定决策（fail-closed + 一次性清理），后者本次已补文档。
- 待业务侧执行的动作：保函环境仍需清理 5.1 记录的 link 冲突行（见 `mango-resource/README.md` 14.1），否则下一次包含 `5900fec2f` 的增量发布会重复同一失败。
