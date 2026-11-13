# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1231
- 交付模式：STANDARD
- 需求影响：L2 - 给已发布的 `POST /workflow/tasks/page-by-business-key` 增加可选查询参数，改变业务列表可观察的筛选能力，影响面限定在 Workflow 任务查询
- 方案风险：L1 - 单模块内新增一个查询条件和一段 LIKE 过滤，不改表结构、不迁移数据、回退只需删除字段和 SQL 片段
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-1231`，分支 `feat/issue-1231-apply-keyword-search`）

## 2. 目标与范围

- 目标：业务审批列表可以用一个关键字搜索申请编号、申请标题、申请摘要，不再为关键词筛选先查业务表取 `businessKeys`。
- 成功条件：`applyKeyword` 命中三列任意一列；`count` 与分页行一致；`keyword`（任务名/节点键）行为不变；租户隔离和分页口径不变。
- 处理范围：`mango-workflow-api`、`mango-workflow-core`（service/mapper/测试）、workflow README、业务接入指南、能力地图、业务接入历史变更、本记录。
- 不处理范围：不新增数据库列或索引、不做变量白名单过滤、不新增申请时间过滤、不改 `keyword`、`businessKeys`、`scope`、权限码语义、不改分页上限。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 业务列表 / `POST /workflow/tasks/page-by-business-key` | `applyKeyword=GR20260102000002` | 只返回申请编号命中的任务 | 无命中返回空列表，不报错 | 结果只含该申请的任务 |
| AC-2 | 同上 | `applyKeyword=中铁` | 命中标题或摘要包含该词的任务；同一申请的多条任务全部返回 | 同上 | 命中的申请下所有任务都出现，`total` 为任务数 |
| AC-3 | 同上 | `applyKeyword=审核_中铁` | `_` 和 `%` 按字面匹配，不当作通配符 | 同上 | 只有标题含字面 `审核_中铁` 的申请命中 |
| AC-4 | 同上 | 同时传 `keyword` 和 `applyKeyword` | 两个条件为 AND，各自保持原有匹配范围 | 同上 | 任务名条件与申请文本条件同时生效 |
| AC-5 | 同上 | 不传 `applyKeyword` 或传空白 | 行为与本变更前完全一致 | — | 既有 18 个服务单测、4 个租户集成测试保持不变通过 |
| AC-6 | 同上 | 跨租户数据 | 只返回当前租户申请下的任务 | 缺租户上下文报错 | 其他租户任务不出现 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1/AC-2 | 新增可选字段 `applyKeyword`（≤128），不改变 `keyword` 语义；两者可同时传 | `WorkflowTaskBusinessPageQuery` | 删除字段 |
| D-2 | AC-2 | 匹配 `a.apply_code`、`a.apply_title`、`a.apply_summary` 三列 OR，SQL 层过滤，`countTasks` 与 `selectTasks` 共用同一 `taskWhere` | `WorkflowTaskHistoryMapper.xml`、`WorkflowTaskHistoryCriteria` | 删除 XML 片段 |
| D-3 | AC-3 | 服务层把 `\`、`%`、`_` 转义后再拼 `%词%`，依赖 MySQL 默认反斜杠转义，不拼 SQL 字符串 | `WorkflowTaskBusinessQueryService` | 回退转义方法 |
| D-4 | AC-5 | 既有 `keyword` 的 `%词%` 拼接保持不变，本次不做行为变更，避免影响既有调用方 | `WorkflowTaskBusinessQueryService` | — |
| D-5 | AC-1 | 不新增索引：真实库 `EXPLAIN` 显示驱动表仍是 `workflow_business_apply` 的 `idx_workflow_business_apply_biz_latest`(tenant_id, business_type)，候选行 1060，LIKE 只作用于已按租户和业务类型收窄的行 | `resources/db/migration/workflow`（无改动） | 删除索引（本变更无索引） |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1 | 1 | `mango-workflow-api/.../query/WorkflowTaskBusinessPageQuery.java` | 新增字段带 `@Size` 和 `@Schema`，模块编译通过 |
| I-2 | D-2/D-3 | 2 | `mango-workflow-core/.../model/WorkflowTaskHistoryCriteria.java`、`.../service/impl/WorkflowTaskBusinessQueryService.java`、`mapper/workflow/WorkflowTaskHistoryMapper.xml` | 条件进入 criteria 与 SQL，全部使用 `#{}` |
| I-3 | AC-1~AC-3 | 3 | `WorkflowTaskBusinessQueryServiceTest`、`WorkflowTaskHistoryMapperIntegrationTest` | 新用例通过，既有用例不改语义 |
| I-4 | AC-1~AC-6 | 4 | workflow README §6.1、业务接入指南 §7.1、`mango-docs/changelog/business-integration/workflow-business-approval.md`、`mango-docs/capabilities/README.md` | 文档门禁通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-3/AC-5/AC-6 | 单元测试 + H2 集成测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core -Dtest='WorkflowTaskBusinessQueryServiceTest,WorkflowTaskHistoryMapperIntegrationTest,WorkflowTaskHistoryTenantIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test` | PASS：37 用例（18 + 15 + 4），0 失败 | 构建输出 |
| AC-1/AC-2/AC-3 | 真实 MySQL 只读验证 | `baohan_prod` 执行等价 SQL：`applyKeyword=%中铁%`、`%履约保函%`，以及旧 `keyword` 口径对照 | PASS：新条件命中 92 / 4035 条任务；旧 `keyword` 同词命中 0 条 | 见下方 SQL 输出 |
| AC-1~AC-5 | HTTP 端到端（完整应用 + 本地 MySQL） | `mango dev start backend`（工作区 slot 46，库 `mango_dev_mango_issue_1231_046`），`POST /auth/login` 取 token 后调 `POST /workflow/tasks/page-by-business-key` | PASS：13 个场景全部符合预期（见下方 HTTP 输出） | 下方 HTTP 结果与请求体 |
| AC-2 | 执行计划 | `EXPLAIN` 等价 SQL | PASS：驱动表 `a` 走 `idx_workflow_business_apply_biz_latest`，`ref=const,const`，rows≈1060；任务表走 `ACT_IDX_HI_TASK_INST_PROCINST` | 见下方 EXPLAIN 输出 |
| AC-1~AC-6 | 模块验证 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter verify` | PASS（见第 7 节未验证项说明） | 构建输出 |
| 文档 | 能力文档门禁 | `node mango-pmo/tools/audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`、`check-capability-docs.mjs` | PASS | 脚本输出 |

真实库核对（`baohan_prod`，2316 条申请，最大业务类型 2010 条）：

```text
%中铁%      -> 92 条任务
%履约保函%  -> 4035 条任务
旧 keyword（t.NAME_ / t.TASK_DEF_KEY_）同词 -> 0 条任务
```

执行计划（等价 SQL）：

```text
table a: type=ref key=idx_workflow_business_apply_biz_latest key_len=772 ref=const,const rows=1060
table t: type=ref key=ACT_IDX_HI_TASK_INST_PROCINST ref=baohan_prod.a.process_instance_id rows=2
Extra: Using where; Using temporary; Using filesort
```

HTTP 端到端（完整应用 + 本地 MySQL，种子数据 4 条申请 / 4 条任务，验证后已清理）：

```text
不传 applyKeyword                         total=2  [T2, T1]      向后兼容
applyKeyword=中铁                         total=2  [T2, T1]      标题命中
applyKeyword=审核_中铁                    total=1  [T1]          下划线字面匹配
applyKeyword=履约保函                     total=1  [T1]          摘要命中
applyKeyword=GR20260101                   total=1  [T1]          编号命中
keyword=复审                              total=1  [T2]          keyword 语义不变
keyword=复审 + applyKeyword=中铁           total=1  [T2]          AND 语义
applyKeyword=中铁 + businessKeys=[K2]      total=1  [T2]          与业务键共存
page=1 size=1 / page=2 size=1             total=2  [T2] / [T1]    分页不重不漏
applyKeyword=其他项目（任务归 bob）       total=0                SELF 范围生效
applyKeyword=中铁五局（tenant_id=2）      total=0                租户隔离生效
applyKeyword 129 字符                     code=400              参数校验生效
scope=ALL（未授予 workflow:task:all）     code=3660             权限语义不变
```

## 7. 例外与剩余风险

- 不传 `businessKeys` 时仍不做逐申请 `WorkflowBusinessApplyDataPermissionProvider` 收窄，沿用 Issue #1223 已发布契约；本变更未改变该边界。
- 变量白名单过滤（Issue #1231 的 B 部分）不做：`variables_json` 是提交时快照，且 H2 2.3.232（`MODE=MySQL`）不支持 `JSON_EXTRACT`/`JSON_UNQUOTE`/`->>`，函数索引方案无法进入现有测试通道。业务字段的多选精确筛选继续使用 `businessKeys`。
- `apply_summary` 只在创建申请单时写入，平台没有更新接口；摘要承载的检索文本不会随业务字段变更更新。
- 既有 `keyword` 未做通配符转义，本次不改变其行为；`applyKeyword` 单独转义。
- HTTP 端到端已在本地完整应用（工作区 slot 46 空库 + 种子数据）验证，但未在业务仓 `mango-baohan-risk` 联调新参数。
- workflow README 未新增「升级与兼容」版本行：目标发布版本号在发布批次确定，按能力说明规范在发布时补充。
- 已提交 Commit、Push 并创建 PR #1237；合并按用户授权和 review 结论执行。

## 8. 决定复核

| 视角 | 结论 | 依据与不确定项 |
|---|---|---|
| 事实与用户视角 | 业务列表的关键词筛选需要下推到平台，避免先查业务表取 `businessKeys` 后再受数量上限影响 | Issue #1231 场景与 `baohan_prod` 真实数据；不确定项：申请人、编号前缀筛选是否有真实入口，本次不纳入 |
| 技术与风险视角 | 只新增一个可选参数和一段三列 LIKE，不动表结构；不采纳变量白名单与函数索引 | 真实库 `EXPLAIN`、H2 2.3.232 对 JSON 函数不支持、`variables_json` 是提交时快照 |
| 验证与交付视角 | H2 集成测试 + 真实库等价 SQL 双通道可证明结果；回退只需删除字段和 SQL 片段 | 37 个用例通过；真实库命中 92 / 4035 条任务，旧 `keyword` 同词 0 条 |

- 被拒方案：扩展 `keyword` 语义（改变已发布字段命中集合）、`apply_title` 前缀索引（`%词%` 用不上）、变量函数索引（每业务字段一次平台 DDL，且无法进入现有测试通道）、业务字段投影表（当前 2 千级数据量下收益不足，`versionStatus` 仍需业务表）。
- 评审选择：`AGENT_ONLY`。本次由用户直接要求实现，未指定外部同行评审；Agent 分析结论不称为人工评审或审批。
- 需要人工确认：摘要 `名称：值，名称：值` 写法由业务侧遵守，报告协助确认；Commit / Push / PR 已按用户要求完成。
