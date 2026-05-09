# 保函跨机构协同模型 E2E 任务计划

## 目标

为保函业务建立“默认机构隔离 + 单笔业务显式协同”的基础模型。

本计划只处理保函业务接入前必须具备的协同底座，不提前实现完整保函申请、出函、支付、签章等业务流程。

## 边界

- 机构仍使用现有 `sys_tenant` 表表达企业空间，用户可见名称统一为“机构”。
- 账号、成员、角色、组织、菜单继续复用当前平台基础能力。
- 跨机构协同不绕开 `tenant_id` 隔离；共享必须通过业务参与方和授权记录表达。
- 暂不引入独立微服务，先按模块化单体方式落在业务模块中，后续可拆部署。

## 核心概念

| 概念 | 说明 |
|---|---|
| 保函业务单 | 一笔保函申请或处理主记录，拥有来源机构 |
| 业务参与方 | 一笔业务中参与协同的机构，例如接单方、担保方、银行 |
| 资料共享 | 某份业务资料对某个参与机构开放的授权边界 |
| 协同任务 | 分配给某个参与机构或机构成员的处理任务 |

## 推荐模块

首阶段新建 `mango-guarantee` 模块：

```text
mango-platform/mango-guarantee/
├── mango-guarantee-api
├── mango-guarantee-core
└── mango-guarantee-starter
```

暂不新建 `guarantee-app`。当前单体服务先集成 `mango-guarantee-starter`，保证前端可直接联调。

## 数据模型

首阶段只建协同底座表：

| 表 | 说明 | 是否机构隔离 |
|---|---|---|
| `guarantee_case` | 保函业务单主表 | 是，`tenant_id` 为来源机构 |
| `guarantee_case_participant` | 业务参与方 | 是，记录创建方机构，同时保留 `participant_tenant_id` |
| `guarantee_case_document` | 业务资料元数据 | 是，`tenant_id` 为资料拥有机构 |
| `guarantee_case_document_share` | 资料共享授权 | 是，记录授权发起机构，保留目标机构 |
| `guarantee_case_task` | 协同任务 | 是，`tenant_id` 为任务归属机构 |

注意：需要跨机构查询的场景不能简单依赖 MyBatis-Plus 自动租户过滤，应通过专用 Mapper 明确约束：

```text
当前机构可见业务 = source_tenant_id = currentTenant
             OR exists case_participant where participant_tenant_id = currentTenant
```

这类查询必须写清楚授权条件，不能为了查到数据而加 `@IgnoreTenant` 后裸查。

## 任务拆分

| 任务 | 状态 | 范围 | 验证 |
|---|---|---|---|
| G1 模块骨架与迁移 | 已完成 | 新建 `mango-guarantee` 三层模块、Flyway 分组、单体集成 | 后端构建、Flyway 执行、Swagger 分组可见 |
| G2 保函业务单最小 CRUD | 已完成 | 来源机构创建、列表、详情、修改、删除；只返回当前机构来源或参与的数据 | API E2E |
| G3 业务参与方管理 | 待处理 | 为业务单添加/移除参与机构，参与方类型用字典维护 | API E2E：A 创建、B 参与后可见 |
| G4 资料元数据与共享 | 待处理 | 上传前元数据登记、授权给参与机构、撤销授权 | API E2E：未授权不可见，授权后可见 |
| G5 协同任务 | 待处理 | 给参与机构创建任务、状态流转、处理人绑定成员 | API E2E |
| G6 前端最小管理页 | 待处理 | 业务单列表、详情、参与方、资料、任务基础页面 | Playwright E2E |

## 首个可执行闭环

先做 G1，不写业务接口：

1. 新建模块结构和 POM。
2. 添加 Flyway 迁移位置 `db/migration/guarantee`。
3. 单体服务引入 `mango-guarantee-starter`。
4. 暴露一个只读健康/元信息接口 `GET /guarantee/meta`，用于确认模块装配和 Swagger 分组。
5. E2E 验证：
   - 服务启动时 Flyway 执行 guarantee 分组。
   - `/v3/api-docs/mango-guarantee` 可访问。
   - `/guarantee/meta` 登录后返回模块信息。

### G1 验证记录

- 2026-05-09：`mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- 2026-05-09：单体服务 `5555` 端口启动成功，KV 仍使用内存库，Flyway 正常执行。
- 2026-05-09：`flyway_schema_history_guarantee` 已执行 `V1__init_guarantee_collaboration.sql`，`guarantee_module_marker` 存在 `mango-guarantee / collaboration-foundation` 记录。
- 2026-05-09：`GET /v3/api-docs/mango-guarantee` 返回 200，包含 `/guarantee/meta`。
- 2026-05-09：`GET /guarantee/meta` 未登录返回 401，登录后返回模块元信息。

## G2 接口范围

`/guarantee/cases` 提供保函业务单最小 CRUD：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/guarantee/cases` | 分页查询当前机构来源或参与可见的业务单，查询参数使用 `GuaranteeCaseQuery` |
| GET | `/guarantee/cases/detail` | 查询当前机构可见的业务单详情 |
| POST | `/guarantee/cases` | 当前机构作为来源机构创建业务单 |
| PUT | `/guarantee/cases` | 仅来源机构可修改业务单 |
| DELETE | `/guarantee/cases` | 仅来源机构可删除业务单 |

G2 已建立 `guarantee_case` 与 `guarantee_case_participant`。列表和详情查询使用明确授权条件：

```text
source_tenant_id = 当前机构
OR exists guarantee_case_participant where participant_tenant_id = 当前机构 and status = 1
```

该查询局部忽略 MyBatis-Plus 自动租户插件，但没有裸查，跨机构可见性由业务参与关系显式约束。

### G2 验证记录

- 2026-05-09：`V2__init_guarantee_case.sql` 已执行，`guarantee_case`、`guarantee_case_participant` 表已创建。
- 2026-05-09：`GET /v3/api-docs/mango-guarantee` 返回 200，包含 `/guarantee/cases` 五个接口，列表查询参数和请求体字段均有中文说明。
- 2026-05-09：`pnpm --filter mango-admin exec playwright test e2e/specs/guarantee-case-api.spec.ts --project=chromium --workers=1` 通过。
- 2026-05-09：`mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。

### G2 留给 G3 的边界

- G2 只验证“未成为参与方的机构不可见”。
- G3 才正式提供参与方新增、移除接口，并验证 A 创建后把 B 加为参与方，B 可见该业务单。

## ADR

### 决策：跨机构协同使用显式参与关系，不关闭机构隔离

状态：已采用。

原因：

- 保函业务存在元丰行、担保公司、银行多机构协作。
- 各机构默认不能互看全量业务和资料。
- 单纯依赖 `tenant_id = 当前机构` 无法表达“某笔业务授权可见”。

方案：

- 业务主记录保留来源机构。
- 参与方表表达哪些机构可参与某笔业务。
- 资料共享表表达资料级授权。
- 跨机构查询必须显式 join 参与方/共享表。

取舍：

- 查询会比单租户 CRUD 复杂。
- 但授权边界清晰，可审计，可扩展到资料、任务、流程。
