# 标准交付记录

## 先读

- **一句话目标**：把人工接管从"复制一条 manual-control 记录"改成"同一行上的覆盖标记 + 原值快照"，接管不改模块归属、不丢权限标识、解除即还原且不留残留行。
- **真实场景**：运维在「权限标识」页面把某个接口临时改成匿名访问，事后点「解除接管」，应回到代码里的原始声明，界面只保留一行。
- **关键输入/输出**：输入为 route（module + method + path）、访问模式、权限码；输出为一行 `authorization_api_resource`，`manual_override` 表示是否接管。
- **成功/失败边界**：成功时同路由只有一行，模块名、资源编码、处理入口保持不变，权限标识不被清空；失败时返回业务错误（例如 PERMISSION 无权限码）。
- **明确不做**：不引入独立覆盖表；不做多实例缓存广播；不做接管审批流与有效期。
- **阅读顺序**：要求 → 技术决定 → 实施清单 → 验收结果。

## 1. 元数据

- 任务 ID：issue-1130-manual-override
- 交付模式：STANDARD
- 需求影响：L2 - 改变人工接管的数据模型与释放语义，影响所有 API 的运行时访问决策
- 方案风险：L2 - 涉及授权表结构迁移、同步跳过规则、页面交互；失败可由代码回滚与 V5 迁移前快照恢复
- 最终风险：L2
- 工作区决策：CREATE
- 评审选择：AGENT_ONLY；未指定外部同行评审

## 2. 目标与范围

- 目标：人工接管改为单表覆盖模型，解决模块归属被改、权限标识丢失、解除后残留禁用行三个缺陷。
- 成功条件：接管后同路由仅一行且 `module_name` 不变；权限标识保留并可切回；解除后字段还原、行仍为一条、缓存立即生效。
- 处理范围：授权表迁移 V5、实体字段、接管/释放服务、同步跳过规则、决策返回、权限标识页面、README。
- 不处理范围：独立覆盖表、多实例缓存一致、接管审批与过期策略、批量接管。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-01 | 运维接管 Controller 接口 | 存在扫描行，调用 `POST /authorization/api-resources/manual` | 该行被标记 `manual_override=1`，`module_name`、`resource_code`、`handler_*` 不变，`origin_*` 记录原值 | 路由无声明时返回业务错误 | 集成测试 + 真实 HTTP |
| AC-02 | 运维在模式之间切换 | 从 PERMISSION 切到 PUBLIC 再切回 PERMISSION | `permission_code` 始终保留，不因切换被清空 | PERMISSION 且无任何权限码时报错 | 集成测试 + 真实 HTTP |
| AC-03 | 运维解除接管 | 调用 `PUT .../manual/release` | 快照还原 `source/access_mode/permission_code/description`，清除标记，行数仍为 1 | 未找到接管时报错 | 集成测试 + 真实 HTTP |
| AC-04 | 资源同步 | 接管期间执行扫描同步 | 同步跳过该行，不覆盖、不停用 | 无 | 集成测试 |
| AC-05 | 历史数据升级 | 数据库存在旧 `manual-control` 行 | V5 迁移把覆盖合并回同路由声明行并删除重复行 | 无 | 迁移脚本 + 冷启动验证 |
| AC-06 | 权限标识页面 | 接管前后查看列表 | 仍是一行，显示「人工接管」标记；解除后标记消失 | 接口错误时提示 | 浏览器走查 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-01 | AC-01/03/05 | 单表方案：新增 `manual_override` 与 `origin_*` 快照列，同路由只保留一行 | `V5__api_resource_manual_override.sql`、`ApiResourceEntity` | 回滚代码并恢复迁移前快照 |
| TD-02 | AC-01 | 接管只覆盖 `access_mode`、`permission_code`、`description`，不改模块、资源编码、处理入口 | `ApiResourceService#saveManualApiResource` | 回滚服务实现 |
| TD-03 | AC-02 | 权限码只在请求显式提供时更新，不再按模式置空；PERMISSION 校验接受已有值 | `saveManualApiResource`、`toManualCommand` | 回滚校验与序列化 |
| TD-04 | AC-03 | 释放用显式 `LambdaUpdateWrapper` 写回快照并清空 `origin_*`（`updateById` 会忽略 null） | `releaseManualApiResource` | 回滚实现 |
| TD-05 | AC-03/05 | 无 `origin_source` 的历史独立接管行，释放直接物理删除 | `releaseManualApiResource` | 回滚实现 |
| TD-06 | AC-04 | 同步三处守卫：合并跳过、stale 停用跳过、同路由冲突不停用接管行 | `registerApiResources`、`loadStaleAutoScannedResources`、`resolveRouteOwnershipConflicts` | 回滚守卫 |
| TD-07 | AC-06 | 页面按 `manualOverride` 显示标记与解除按钮，不再依赖 `source=MANUAL` | `rbac` 页面与 API 类型 | 回滚页面 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-01 | TD-01/05 | 1 | `db/migration/authorization/V5__api_resource_manual_override.sql` | 迁移可在空库与旧库执行 |
| TASK-02 | TD-01 | 2 | `ApiResourceEntity`、`ApiResourceVO`、`ApiResourceAccessDecisionVO` | 字段可读写并对外暴露 |
| TASK-03 | TD-02/03/04/05/06 | 3 | `ApiResourceService` | 接管/释放/同步守卫全部生效 |
| TASK-04 | TD-07 | 4 | `mango-ui/packages/rbac`（页面、API、单测） | 页面一行、标记正确、解除可用 |
| TASK-05 | 全部 | 5 | 集成测试与 schema | 覆盖 AC-01 至 AC-05 |
| TASK-06 | 全部 | 6 | `mango-authorization/README.md` | 说明覆盖模型与升级步骤 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-01、AC-02、AC-03、AC-04 | M11 集成测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core -Dtest=ApiResourceServiceImplIntegrationTest -DskipITs test` | PASS（21/21） | surefire 报告 |
| AC-01 至 AC-05 | M09 模块验证 | 8 个改动模块 `mvn verify -DskipITs` | PASS | Maven Reactor 输出 |
| 全部 | M09 架构与静态门禁 | changed 模式 `mango:architecture` + 静态门禁 | PASS（new=0） | `mango/target/mango-static-report.json` |
| AC-01/02/03 | M12 真实 HTTP | 接管→匿名 200→401；切 PERMISSION 不传码仍保留权限码；释放→匿名 200；路由行数始终为 1 | PASS | 数据库查询输出 |
| AC-06 | M13 浏览器走查 | admin 登录，编辑→保存→解除，行数始终为 1，出现/消失「人工接管」标记，console error 0 | PASS | 走查输出 |
| 前端 | M10 | `pnpm --filter @mango/rbac test`、`eslint`、`build`、`admin:styles:check`、`admin:module-styles:check` | PASS（13 tests） | 命令输出 |

## 7. 例外与剩余风险

- 多实例部署时保存/释放只清理处理请求的实例缓存，其它实例仍为旧值；本次未实现版本号或广播。
- V5 迁移对历史 `manual-control` 行只按 `(method, path)` 归并；若历史上同一路由存在多条旧接管行，只保留合并结果的一条，需在升级前人工核对。
- 接管暂无审批、有效期与操作人审计字段；变更历史仍依赖资源同步日志。
