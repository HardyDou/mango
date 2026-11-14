# Issue #1248 真实场景验收报告

## 1. 结论

- 真实场景 18 项断言全部通过：启动后端、Flyway `V5` 升级真实数据库、创建申请写属性、任务分页按属性过滤、提交后覆盖/清空属性、默认通知模板展开属性变量。
- `V5__workflow_business_apply_attribute.sql` 在 MariaDB 11.8.6 上从「基线 + V1~V4」的存量库升级成功，表、字段和索引与设计一致。
- 事件载荷 `applyAttributes` 经真实领域事件链路进入默认通知，`{{applyAttribute_<key>}}` 模板变量可按业务配置渲染，多值按英文逗号拼接。
- 未覆盖：多租户隔离（本次只使用平台默认租户）、生产 MySQL 8 的 `EXPLAIN`、EMAIL/WECOM/SMS 渠道渲染、浏览器 UI（该能力目前只有 API 入口）。

## 2. 环境与数据

| 项 | 值 |
|---|---|
| 时间 | 2026-11-14 16:05（Asia/Shanghai） |
| 代码 | `origin/main` `55b351394`（PR #1259 合并后） |
| 工作区 | `/data/Mango/mango-issue-1248-verify`，分支 `test/issue-1248-real-scenario-verify` |
| 应用 | `mango-monolith-app`，`java -version` 21.0.13，端口 `18003`，`/actuator/health` 为 `UP`（db `UP`） |
| 数据库 | MariaDB 11.8.6，库 `mango_dev_mango_issue_1248_verify_003`（从本机开发库克隆：已有基线 + `V1~V4`，无属性表） |
| 缓存 | 本机 Redis 127.0.0.1:6379（领域事件 outbox/通知使用） |
| 数据 | 业务类型 `BAOHAN_RC_PROJECT_REVIEW`（保函风控审批）；3 条申请 `BIZ-E2E-1248-1/2/3`；流程定义 `BAOHAN_RC_REVIEW_E2E_1248`（设计器创建 → 发布） |
| 账号 | `admin`，租户 `1`（`default`），登录域 `INTERNAL` |

## 3. 执行步骤（可复核）

```bash
# 1. 初始化工作区（生成 .mango/workspace.json 与 dev-workspace.env，端口 18003 / 库名见上表）
node mango-ui/packages/mango-cli/src/index.mjs workspace init

# 2. 用开发库克隆作为真实存量库起点（保留 V1~V4 历史与基础数据）
mysqldump -h127.0.0.1 -uroot --single-transaction --routines --triggers --events mango_dev_mango_001 \
  | mysql -h127.0.0.1 -uroot mango_dev_mango_issue_1248_verify_003

# 3. 启动真实后端（应用启动时执行 Flyway V5）
node mango-ui/packages/mango-cli/src/index.mjs dev start backend
curl -s http://127.0.0.1:18003/actuator/health

# 4. 执行真实场景脚本（HTTP + 真实数据库断言）
node mango-docs/evidence/issue-1248/real-scenario/run-real-scenario.mjs

# 5. 收尾
node mango-ui/packages/mango-cli/src/index.mjs dev stop
```

脚本内步骤：登录取令牌 → 创建并发布含人工审批节点的流程定义 → 按业务配置把默认 SITE 通知模板改为引用 `{{applyAttribute_<key>}}` → 创建 3 条带属性申请（间隔 1.1s）→ 逐条发起流程 → 调 `POST /workflow/tasks/page-by-business-key` 验证过滤/排序/分页 → `PUT /workflow/business-applies/filter-attributes` 覆盖与清空 → 查库校验属性行与通知内容。

## 4. 覆盖与结果

| 验收项 | 场景 | 结果 |
|---|---|---|
| 基线 | 不传属性过滤 | PASS：返回 3 条任务 |
| AC-1 | `guaranteeTypeCode=PERF` 单条件单值 | PASS：2 条（履约保函） |
| AC-3 | 同一条件多值 `[PERF,BID]` | PASS：3 条（值之间为或） |
| AC-2 | `guaranteeTypeCode=PERF` 且 `versionStatus=PENDING` | PASS：1 条 |
| AC-5 | 多值字段 `beneficiary` 命中 `中铁四局` | PASS：1 条 |
| AC-4 | 未登记的属性值 `UNKNOWN` | PASS：0 条 |
| AC-9 | `PERF` + `size=1&page=2` | PASS：`total=2`，第 2 页 1 条，翻页与 `total` 同源 |
| AC-8 | `sortBy=applyTime&sortOrder=desc` | PASS：按申请创建时间倒序 |
| AC-7 | `applyTimeBegin/End` 仅覆盖第 2 条申请时间 | PASS：只返回第 2 条 |
| AC-1 | 非法属性名 `1invalid` | PASS：请求失败，错误信息为可筛属性名规则 |
| AC-12 | 提交后把第 3 条申请 `versionStatus` 改为 `PENDING` | PASS：`versionStatus=PENDING` 命中 3 条 |
| AC-12 | 空数组清空第 2 条申请属性 | PASS：`guaranteeTypeCode=BID` 命中 0 条 |
| AC-11 | 多值字段一行一个值、重复值去重 | PASS：`beneficiary` 输入 3 个值（含重复）落库 2 行 |
| AC-13/14 | 事件载荷进入默认通知并展开 `applyAttribute_<key>` | PASS：通知正文渲染出 `保函种类：PERF`、`受益人：中铁三局,中铁四局`、`版本状态：PENDING` |
| 迁移 | 存量库启动时应用 `V5` | PASS：`flyway_schema_history_workflow` 出现 `5|workflow business apply attribute|1` |

通知实际渲染样例：

```text
待审核：保函风控审核（1248 真实场景验收）（PERF）
业务：履约保函风控审批-1；保函种类：PERF；受益人：中铁三局,中铁四局；版本状态：PENDING
```

## 5. 证据文件

- `run-real-scenario.mjs`：本次真实场景脚本（HTTP 入口 + 真实库断言）。
- `run-output.json`：逐项断言、申请 ID、流程定义信息、通知正文。
- `db-schema-check.txt`：Flyway 历史、属性表字段与索引。
- `db-data-check.txt`：申请、属性行、通知消息的库内事实。

## 6. 结论边界

- 真实库是 MariaDB 11.8.6（MySQL 兼容模式），不是生产 MySQL 8；本次未采集 `EXPLAIN`，优化器行为仍需在生产环境复核。
- 只验证平台默认租户；跨租户隔离由 `WorkflowTaskAttributeFilterIntegrationTest` 的 H2 集成测试覆盖，本次未造第二个租户。
- 默认通知只验证 SITE 渠道；EMAIL/WECOM/SMS 模板与路由未验证。
- 通知模板改动是本次场景的业务配置前置（脚本内执行 SQL），不是产品默认文案。
- 脚本依赖本地 `.mango/dev-workspace.env`、本机 MariaDB/Redis 和已存在的 admin 账号，不能直接作为 CI 入口；长期回归仍以仓库自动化测试为准。

## 7. 验收台账

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| EV-1248-01 | `POST /workflow/definitions`、`POST /workflow/definitions/deploy` | 创建并发布含人工审批节点的流程定义 | 定义编码 `BAOHAN_RC_REVIEW_E2E_1248`，审批节点指派用户 1 | 返回 `definitionId`，发布返回 `processDefinitionId` 且版本号 1 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 `definition` | PASS |
| EV-1248-02 | `POST /workflow/business-applies` | 创建申请写入可筛属性，多值重复去重 | `beneficiary` 输入 `中铁三局/中铁四局/中铁四局` | 属性表该键落 2 行，重复值只保留 1 行 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `db-data-check.txt` 属性行清单 | PASS |
| EV-1248-03 | `POST /workflow/tasks/page-by-business-key` | 单条件单值、同一条件多值（或） | `guaranteeTypeCode=PERF`；`[PERF,BID]` | `total` 分别为 2、3，业务键集合与预期一致 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-1/AC-3 断言 | PASS |
| EV-1248-04 | `POST /workflow/tasks/page-by-business-key` | 条件间为且、多值字段命中 | `PERF` + `versionStatus=PENDING`；`PERF` + `beneficiary=中铁四局` | 两次均只返回 `BIZ-E2E-1248-1` | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-2/AC-5 断言 | PASS |
| EV-1248-05 | `POST /workflow/tasks/page-by-business-key` | 未登记属性值不匹配 | `guaranteeTypeCode=UNKNOWN` | `total=0` | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-4 断言 | PASS |
| EV-1248-06 | `POST /workflow/tasks/page-by-business-key` | 分页与 `total` 同源 | `PERF`，`page=2&size=1` | `total=2`，第 2 页只返回 `BIZ-E2E-1248-3` | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-9 断言 | PASS |
| EV-1248-07 | `POST /workflow/tasks/page-by-business-key` | 按申请创建时间排序与区间过滤 | `sortBy=applyTime&sortOrder=desc`；`applyTimeBegin=applyTimeEnd=第2条申请时间` | 倒序为 `3,1`；区间只返回 `BIZ-E2E-1248-2` | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-7/AC-8 断言 | PASS |
| EV-1248-08 | `POST /workflow/tasks/page-by-business-key` | 非法属性名被拒绝 | `key=1invalid` | `success=false`，错误信息包含「可筛属性名只支持字母开头」 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的非法属性名断言 | PASS |
| EV-1248-09 | `PUT /workflow/business-applies/filter-attributes` | 提交后覆盖与清空属性 | 第 3 条 `REVIEWED` 改 `PENDING`；第 2 条传空数组 | 改后 `PENDING` 命中 3 条；清空后 `BID` 命中 0 条 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `run-output.json` 的 AC-12 断言 | PASS |
| EV-1248-10 | `workflow.task.assigned` 事件 → 默认 SITE 通知 | 事件载荷属性进入模板变量 | 模板引用 `{{applyAttribute_guaranteeTypeCode}}`、`{{applyAttribute_beneficiary}}`、`{{applyAttribute_versionStatus}}` | 通知正文渲染出 `保函种类：PERF`、`受益人：中铁三局,中铁四局`、`版本状态：PENDING` | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `db-data-check.txt` 通知正文 | PASS |
| EV-1248-11 | Flyway `V5` 迁移 | 存量库从基线 + `V1~V4` 升级 | 克隆开发库后启动应用 | `flyway_schema_history_workflow` 版本 5 `success=1`，属性表字段与两条索引齐全 | 不适用：无 UI 入口，交互结果见 HTTP 响应 | 不适用：无浏览器；网络断言见 run-output.json | `db-schema-check.txt` | PASS |
