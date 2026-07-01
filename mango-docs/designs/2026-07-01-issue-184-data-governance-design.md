# Issue #184 Mango 数据治理第一版需求与设计

## 1. 结论

Issue #184 第一版不建设 DataOps 平台，不新增任务包、任务历史表、管理页面、在线影子表升级或任意 bean 方法执行器。第一版只把 Mango 现在最容易混乱的数据初始化和停机升级问题收敛到两个已有平台能力：

- `mango-resource`：管理正式资源、demo 资源、初始化一次且运行时可改的数据。
- `mango-infra-persistence`：继续用模块化 Flyway 管理 DDL、停机升级 SQL、外部 SQL 包和新库 baseline pack。

一句话规则：

```text
结构和版本化 SQL 归 Flyway；小而结构化的模块资源归 Resource；demo 默认隔离；运行时可改的初始化数据用 INIT_ONLY；大 SQL 和停机升级包走 Flyway 外部 locations。
```

## 2. 目标

本次要解决的问题：

- Flyway migration 中混入 demo、sample、业务 seed、运行时修复数据，导致新库初始化和升级边界不清。
- 模块历史 migration 越来越多，新库从 V1 跑完整历史，启动慢，也不容易看到当前完整结构。
- `META-INF/mango/resources` 默认扫描，无法区分正式资源和 demo 资源。
- 菜单、角色、字典、系统配置、任务、工作流等初始化数据有些后续会被用户或业务运行时修改，升级时不能覆盖。
- 大 SQL、磁盘 SQL、远程 URL SQL 没有正式入口，容易被临时脚本或默认 Flyway classpath 混用。

## 3. 不做

- 不做完整 DataOps 平台。
- 不做 `Data Package` / task 编排器。
- 不新增 `resource_data_task_history` 或类似任务历史表。
- 不做管理页面。
- 不做在线升级、影子表、双写、CDC、灰度切流。
- 不支持任意 bean 方法字符串执行。
- 不自动迁移历史 Flyway seed/demo SQL。
- 不把 demo 数据做成单独 starter，也不要求部署时更换依赖包。

历史 Flyway migration 只说明“这个库曾经怎样升级过”。它不能作为新增字典、菜单、角色、工作流、demo 或业务 seed 的当前模板。Agent 在历史 SQL 中看到旧字典、旧菜单或旧 demo 数据时，必须回到当前 Resource README、Persistence README 和本设计判断当前归属。

## 4. 能力总览

| 能力 | 归属模块 | 已落地入口 | 解决的问题 |
| --- | --- | --- | --- |
| 正式 Resource 默认扫描 | `mango-resource` | `META-INF/mango/resources/*.{json,yml,yaml}` | 菜单、字典、配置、消息、任务、号段、文件配置等结构化资源声明。 |
| demo Resource 隔离 | `mango-resource` | `mango.resource.registry.demo-enabled=false`，目录 `META-INF/mango/demo/` | demo 默认不进入正式启动路径。 |
| `INIT_ONLY` 同步模式 | `mango-resource` | Resource 声明 `sync-mode: INIT_ONLY` | 首次创建，后续升级只更新 registry 元数据，不覆盖业务表运行时修改。 |
| 外部 Flyway locations | `mango-infra-persistence` | `mango.persistence.flyway.modules.<module>.locations` | 停机升级时执行磁盘目录或远程 URL SQL，仍写模块 Flyway history。 |
| Schema baseline pack | `mango-infra-persistence` | 新库显式配置 `filesystem:/opt/mango/baseline/<module>` | 新库可用当前完整结构包，不必从 V1 跑全部历史 SQL。 |
| 当前物料清理清单 | `mango-docs/plans` | `2026-07-01-issue-184-s5-data-material-audit.md` | 给存量 Flyway seed/demo/runtime SQL 后续清理排优先级。 |

## 5. 业务 Agent 快速判断

业务 Agent 在业务模块里新增数据物料时，按下表选择入口：

| 场景 | 放哪里 | 怎么写 | 注意 |
| --- | --- | --- | --- |
| 新表、改表、索引、约束 | `core/src/main/resources/db/migration/<module>/V*.sql` | Flyway SQL | DDL 只走 Flyway。 |
| 菜单、按钮、字典、系统配置、任务、号段、消息模板等正式资源 | `starter/src/main/resources/META-INF/mango/resources/` | Resource JSON/YAML | 默认启动会扫描。 |
| 初始化后运行时会被用户改的数据 | Resource 声明 | `sync-mode: INIT_ONLY` | 首次创建；声明升级不覆盖目标业务表。 |
| demo 站点、demo 角色、demo 流程、demo 日历、sample job | `starter/src/main/resources/META-INF/mango/demo/` | Resource JSON/YAML | 默认不加载；演示场景显式打开 `demo-enabled`。 |
| 用户创建的业务单据、流程实例、任务实例、日历事件、用户改的角色授权 | 业务 Owner Service | 普通业务写入 | 不进 Resource，不进初始化 SQL。 |
| 停机升级修复历史数据 | 运维显式配置的 Flyway 外部 locations | `filesystem:` 目录或 `http(s)` 单个 SQL 文件 | 必须是版本化 `V*.sql`，仍进模块 history table。 |
| 全国行政区划、年度日历、500MB/1GB 大数据 | Flyway 外部 SQL 包或模块批量导入服务 | 磁盘文件或远程制品 URL | 不放 YAML，不打进默认 jar classpath。 |
| 新库不想跑几百个历史 SQL | baseline pack | `filesystem:/opt/mango/baseline/<module>` | 只给新库用，不和历史 V1...Vn 混用。 |

业务 Agent 不要做的事：

- 不要把 demo 数据放进 `META-INF/mango/resources/`。
- 不要为了初始化菜单/角色/工作流再新增默认 Flyway DML。
- 不要把运行时用户数据写成 Resource 声明。
- 不要把大 SQL 内容写进 YAML。
- 不要新增一个 `Data Package` 包来包任务。

## 6. Mango 框架开发 Agent 快速判断

框架开发 Agent 维护能力时，按模块边界处理：

| 模块 | 负责 | 不负责 |
| --- | --- | --- |
| `mango-resource-api` | Resource 声明模型、资源类型、`ResourceSyncMode`。 | 目标模块业务表结构。 |
| `mango-resource-support` | 扫描 `resources/` 和可选 `demo/` 声明。 | 执行 SQL 文件。 |
| `mango-resource-core` | registry、hash、sync log、change log、`INIT_ONLY` 跳过目标 handler 的同步语义。 | 解释每种资源字段的业务含义。 |
| 目标模块 handler | 把 Resource 字段写入本模块或目标模块业务表。 | 决定全局扫描路径和 registry 表结构。 |
| `mango-infra-persistence-starter` | 模块 Flyway、独立 history table、classpath/filesystem/http(s) locations、baseline pack 目录执行。 | Resource 同步和 demo 开关。 |
| 业务模块 core/starter | DDL 放 core migration；Resource/demo 声明放 starter resources。 | 新增平台级任务编排。 |

框架开发 Agent 修改这些能力时必须同时更新：

- 模块 README：`mango/mango-platform/mango-resource/README.md` 或 `mango/mango-infra/mango-infra-persistence/README.md`。
- 本设计文档。
- S1-S5 阶段计划或后续清理清单。
- 对应测试：Resource 同步测试或 Persistence Flyway 测试。

## 7. Resource 设计

### 7.1 正式资源

正式资源默认扫描：

```text
classpath*:META-INF/mango/resources/*.json
classpath*:META-INF/mango/resources/*.yml
classpath*:META-INF/mango/resources/*.yaml
```

推荐命名：

```text
{module}-common-{resource}.{json,yml,yaml}
```

示例：

```text
payment-common-menu.json
payment-common-job.yml
system-common-config.yml
workflow-common-definition.yml
```

### 7.2 demo 资源

demo 资源默认扫描路径：

```text
classpath*:META-INF/mango/demo/*.json
classpath*:META-INF/mango/demo/*.yml
classpath*:META-INF/mango/demo/*.yaml
```

配置：

```yaml
mango:
  resource:
    registry:
      demo-enabled: false
```

默认 `false`。只有显式设置 `true` 时才会把 demo 目录追加到声明扫描列表。

### 7.3 `sync-mode`

| 模式 | 行为 | 适用场景 |
| --- | --- | --- |
| `AUTO` | 声明版本或 hash 变化时调用目标 handler 覆盖目标数据。 | 模块强维护的固定资源。 |
| `INIT_ONLY` | 首次同步创建目标数据；目标已存在后只更新 registry 声明元数据，不调用目标 handler 覆盖业务表。 | 初始化后允许用户或业务运行时修改的数据。 |
| `MANUAL` | registry 记录被人工接管后跳过声明同步。 | 运维临时接管。 |
| `LOCKED` | registry 锁定后跳过声明同步。 | 禁止声明继续改动。 |

`INIT_ONLY` 支持 `INIT_ONLY`、`init-only`、`init_only` 写法。

## 8. Flyway 设计

### 8.1 默认模块 migration

默认路径：

```text
classpath:db/migration/<module>
```

每个模块使用独立 history table：

```text
flyway_schema_history_<module>
```

### 8.2 外部升级 SQL

停机升级需要执行不随应用 jar 发布的 SQL 时，配置模块级 `locations`：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          locations:
            - classpath:db/migration/payment
            - filesystem:/opt/mango/upgrade/payment
            - https://artifact.example.com/mango/payment/V2026070101__fix_channel_data.sql
```

规则：

- `classpath:` 和 `filesystem:` 可以是 Flyway migration 目录。
- `http://` / `https://` 只支持单个 `.sql` 文件。
- 外部 SQL 文件名仍必须符合 Flyway 版本命名，例如 `V2026070101__fix_channel_data.sql`。
- URL SQL 会先下载到临时目录，再交给 Flyway 执行。
- 执行结果仍写入当前模块的 Flyway history table。
- 不提供绕过 Flyway history 的裸 SQL 执行器。

### 8.3 Schema baseline pack

模块历史 migration 很多时，新数据库可以显式使用 baseline pack：

```text
/opt/mango/baseline/payment/
  V2026070100__baseline_payment_schema.sql
  V2026070101__add_payment_channel_index.sql
```

新数据库配置：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          locations:
            - filesystem:/opt/mango/baseline/payment
```

旧数据库升级继续使用历史 migration 或升级包：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          locations:
            - classpath:db/migration/payment
            - filesystem:/opt/mango/upgrade/payment
```

边界：

- baseline pack 不是默认自动替换历史 migration。
- 新库不要同时配置 baseline pack 和同模块历史 V1...Vn 目录。
- 旧库切换 baseline pack 前必须单独评审。
- baseline SQL 只表达当前完整结构，不放 demo 或运行时数据。

## 9. 停机升级流程

新库安装：

```text
1. 创建空数据库。
2. 选择默认 classpath migration 或模块 baseline pack。
3. Flyway 按模块执行 migration 并写 history table。
4. Resource 同步正式 resources。
5. 如需演示数据，显式打开 demo-enabled 后同步 demo resources。
6. 启动应用并验收关键资源。
```

旧库停机升级：

```text
1. 停应用。
2. 备份数据库。
3. 配置默认 classpath migration 和必要的 filesystem/http(s) 外部升级 SQL。
4. Flyway 按模块执行增量 migration。
5. Resource 同步正式 resources；INIT_ONLY 保留目标业务表运行时修改。
6. 校验关键数据。
7. 启动应用。
```

第一版不支持在线升级，升级窗口内执行。

## 10. Mango 当前物料推演

| 模块或数据 | 当前现状 | 第一版处理路线 |
| --- | --- | --- |
| authorization 菜单/权限 SQL | 历史 Flyway 中很多 menu/permission/role SQL | 历史保留；新增菜单/权限进入 Resource；运行时角色授权用 `INIT_ONLY` 或 Owner Service 保护。 |
| CMS demo 站点/新闻/广告 | Flyway seed 中有 demo 数据 | 历史保留；新增 demo 进入 `META-INF/mango/demo/`。 |
| payment seed/demo/runtime cleanup | Flyway 数量最多，混有种子、模拟运行时、清理脚本 | DDL/历史修复继续 Flyway；demo/模拟数据不再进默认 migration；大升级包走外部 locations。 |
| notice template/channel/admin recipient | Flyway 和 Resource 混合 | 可声明模板/渠道进入 Resource + `INIT_ONLY`；发送记录、偏好和快照是运行时数据。 |
| job sample/payment job | Flyway seed 与 Resource 并存 | 必需 job 走 Resource + `INIT_ONLY`；sample/demo job 走 demo。 |
| system config/personal config | Flyway 和 `system-common-config.yml` 并存 | 平台默认配置走 Resource；用户个人配置是运行时数据。 |
| file storage default | 已有 `file-common-storage.yml`，历史 Flyway 仍有默认存储 | 后续默认存储收敛到 Resource + `INIT_ONLY`。 |
| workflow definition | Resource 文件存在，部署/发布语义由 workflow 模块负责 | 声明入口归 Resource；部署、发布、版本切换由 workflow Owner Service/handler 完成。 |
| calendar/行政区划/年度节假日 | 小资源和大数据边界不清 | 小分类走 Resource；年度节假日和行政区划大数据走外部 SQL 或 Owner 批量导入。 |

更完整清单见 [S5 数据物料清理清单](../plans/2026-07-01-issue-184-s5-data-material-audit.md)。

## 11. 已实施拆分

| 阶段 | 状态 | 交付 |
| --- | --- | --- |
| S1 Resource demo 隔离 | 已完成 | `demo-enabled`、`demo-locations`、loader 测试。 |
| S2 Resource `INIT_ONLY` | 已完成 | `ResourceSyncMode.INIT_ONLY`、registry 跳过目标 handler 的集成测试。 |
| S3 Flyway 外部 locations | 已完成 | 模块级 `locations`，支持 `classpath:`、`filesystem:`、`http(s)` 单个 SQL 文件。 |
| S4 Schema baseline pack | 已完成设计说明 | 基于 S3 外部目录能力给新库提供 baseline pack 使用方式。 |
| S5 当前物料清理清单 | 已完成 | 扫描 Resource 和 Flyway 物料，给 authorization/cms/payment/notice/job/system/file/workflow/calendar 分配后续路线。 |
| 严格对比测试 | 已完成 | Resource 新旧模式 5 组入库对比；Flyway classpath/filesystem/url 5 组数据入库对比。 |

## 12. 验收用例

| 用例 ID | 场景 | 层级 | 状态 |
| --- | --- | --- | --- |
| TC-184-001 | `demo-enabled=false` 不加载 `META-INF/mango/demo` | Resource loader 单元测试 | 已自动化 |
| TC-184-002 | `demo-enabled=true` 加载 demo 资源 | Resource loader 单元测试 | 已自动化 |
| TC-184-003 | `INIT_ONLY` 首次同步创建目标数据 | Resource 集成测试 | 已自动化 |
| TC-184-004 | `INIT_ONLY` 升级不覆盖运行时修改 | Resource 集成测试 | 已自动化 |
| TC-184-005 | `AUTO` 与 `INIT_ONLY` 首次同步 5 组目标表数据一致 | Resource 集成测试 | 已自动化 |
| TC-184-006 | `INIT_ONLY` 5 组数据升级保留运行时修改 | Resource 集成测试 | 已自动化 |
| TC-184-007 | `filesystem:` 外部目录执行 5 组数据入库对比 | Persistence Flyway 测试 | 已自动化 |
| TC-184-008 | `http(s)` URL SQL 执行 5 组数据入库对比 | Persistence Flyway 测试 | 已自动化 |
| TC-184-009 | classpath 旧模式与 filesystem 新模式 5 组数据严格一致 | Persistence Flyway 测试 | 已自动化 |

## 13. 后续清理优先级

| 优先级 | 任务 | 验收口径 |
| --- | --- | --- |
| P0 | 停止新增 menu/permission/demo/simulated runtime seed 到默认 Flyway | PR review 能拦截新增错误路径。 |
| P1 | CMS demo 迁入 `META-INF/mango/demo/` | 默认启动不安装 CMS demo；显式 demo 开关才安装。 |
| P1 | authorization 菜单权限 SQL 收敛到 Resource | 新增菜单权限不再增加 authorization migration。 |
| P1 | job sample/payment job 种子归类 | demo job 走 demo；必需 job 走 Resource `INIT_ONLY`。 |
| P1 | notice template/channel/job message 收敛 | 可声明通知数据进入 Resource；运行时发送数据不受影响。 |
| P1 | payment simulated/demo seed 分类 | 默认 Flyway 不再产生演示或模拟运行时数据。 |
| P2 | payment、authorization baseline pack | 新库可用 baseline 初始化，不必从 V1 跑完整历史。 |
| P2 | calendar/行政区划大数据导入方案 | 大数据通过外部 SQL 或模块批量导入，不进入 YAML。 |

## 14. 文档入口

- Resource 使用说明：[mango-resource README](../../mango/mango-platform/mango-resource/README.md)
- Flyway 外部 locations 和 baseline pack：[mango-infra-persistence README](../../mango/mango-infra/mango-infra-persistence/README.md)
- 当前物料清理清单：[S5 数据物料清理清单](../plans/2026-07-01-issue-184-s5-data-material-audit.md)
- 能力地图：[Mango 能力地图](../capabilities/README.md)
