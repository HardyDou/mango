# Issue #184 Mango 数据治理第一版需求与设计

## 1. 背景与目标

Issue #184 原始问题是 `mango-system` 的 Flyway V1 同时包含 DDL 和 `sys_tenant`、`sys_area`、`sys_i18n` 初始化数据。后续又发现 `mango-notice` 的 `notice_recipient_account` 管理员邮箱、手机号 seed，以及 `mango-cms`、`mango-payment` 中大量 demo、sample、运行时修复类 SQL。

本设计收敛为第一版可落地方案，不设计完整 DataOps 平台，不设计在线升级，不设计任务编排器。第一版只解决当前最直接的问题：

- Flyway migration 中混入业务数据、demo 数据、运行时修复数据。
- 新库初始化需要执行大量历史 migration，且缺少当前完整 schema 资产。
- `META-INF/mango/resources` 默认扫描，无法区分正式资源和 demo 资源。
- 菜单、角色、字典、配置、工作流等资源初始化缺少明确覆盖策略，容易覆盖运行时修改。
- 大 SQL、磁盘文件、远程 URL 数据没有正式入口，只能混进 Flyway 或临时脚本。

## 2. 范围

### 2.1 本次覆盖

- Flyway DDL 边界和模块级 schema baseline 设计。
- Resource 正式资源与 demo 资源目录隔离设计。
- Resource 同步策略设计，用于保护运行时可修改数据。
- Data Script 设计，用于磁盘文件、远程 URL SQL、标准大数据集导入。
- 停机升级流程设计。
- Mango 当前模块和数据场景覆盖推演。

### 2.2 本次不覆盖

- 不设计在线升级、影子表、双写、CDC、灰度切流。
- 不设计管理页面。
- 不设计通用 Data Package / task 编排器。
- 不设计环境 profile。
- 不支持任意 bean 方法字符串执行。
- 不把用户运行时数据变成普通 Resource 自动同步。
- 不直接修改现有历史 migration。

## 3. 需求说明

| ID | 需求 | 验收口径 |
|---|---|---|
| AC-001 | Flyway 只承接 DDL | 新增 migration 中不再放 demo、业务 seed、测试数据、运行时修复数据。 |
| AC-002 | 新库可从模块 baseline 初始化 | 对 migration 较多模块，新库无需执行全部历史 migration 即可获得最新结构。 |
| AC-003 | 当前完整 schema 可查看 | 每个启用 baseline 的模块存在当前完整 schema 文件，可用于阅读和校验。 |
| AC-004 | 正式资源与 demo 资源隔离 | 默认只扫描正式资源；demo 资源必须显式启用。 |
| AC-005 | Resource 支持运行时保护策略 | 资源声明可以表达只创建、不覆盖用户字段、系统强管等策略。 |
| AC-006 | 大 SQL/URL SQL 不进入 Flyway | 大文件通过 Data Script 执行，支持磁盘、classpath、远程 URL 和 checksum。 |
| AC-007 | 停机升级流程明确 | 已有库升级时先执行 DDL，再执行资源同步和数据脚本，不考虑运行时在线迁移。 |
| AC-008 | 当前 Mango 典型数据都有归属 | `sys_i18n`、`sys_area`、`sys_tenant`、CMS demo、notice recipient、payment seed 等都有处理路线。 |

## 4. 设计总览

第一版拆成三个小能力：

```text
1. Schema Baseline
   解决新库跑几百个历史 SQL、当前完整结构不可见。

2. Resource 分层与策略
   解决正式资源、demo 资源、运行时可修改资源的边界。

3. Data Script
   解决大 SQL、URL SQL、磁盘数据文件和一次性标准数据导入。
```

模块关系：

```text
mango-infra-persistence-starter
  - 继续负责 Flyway DDL
  - 增加模块级 baseline 支持

mango-resource
  - 继续负责 Resource Registry
  - 增加正式/demo 目录扫描
  - 增加 Resource 同步策略
  - 增加 Data Script 执行能力

业务模块 core/starter
  - 正式资源放 META-INF/mango/resources
  - demo 资源放 META-INF/mango/demo
  - 大 SQL 或 URL SQL 声明放 META-INF/mango/data-scripts
```

## 5. 能力设计

### 5.1 Schema Baseline

现状：Mango 已经是模块级 Flyway，history 表默认为 `flyway_schema_history_{module}`。这允许各模块独立版本，但新库仍要执行模块下所有历史 migration。

设计：

```text
db/schema/<module>/baseline.sql
db/migration/<module>/Vxxx__xxx.sql
db/migration-archive/<module>/Vold__xxx.sql
```

第一版行为：

- 新库安装：执行 `db/schema/<module>/baseline.sql`，再将模块 Flyway 标记到 baseline 版本，随后只执行 baseline 之后的 DDL migration。
- 老库升级：继续根据 `flyway_schema_history_{module}` 执行增量 migration。
- 历史 migration 第一阶段不删除，可移动到 archive 或保留原目录，具体迁移节奏按模块拆 Sprint。

不在 baseline 中放业务数据。baseline 只表达当前完整表结构、索引、约束。

### 5.2 Resource 正式/demo 目录

当前默认扫描：

```text
META-INF/mango/resources/*.json
META-INF/mango/resources/*.yml
META-INF/mango/resources/*.yaml
```

第一版保留正式资源目录，并新增 demo 目录：

```text
META-INF/mango/resources/   # 正式内置资源，默认扫描
META-INF/mango/demo/        # demo 资源，默认不扫描
```

配置：

```yaml
mango:
  resource:
    registry:
      demo-enabled: false
```

规则：

- `resources/` 表示产品正式内置资源。
- `demo/` 表示演示资源，必须显式启用。
- 不支持环境 profile。
- 现有正式 resource 文件第一版不要求迁移目录。
- 现有 demo seed 逐步从 Flyway 迁到 `META-INF/mango/demo/` 或 Data Script。

### 5.3 Resource 同步策略

现有 Resource Declaration 已有 `syncMode`、`status`、`targetModule` 等字段，但缺少对运行时可修改数据的覆盖策略。

第一版新增策略口径：

| 策略 | 用途 | 示例 |
|---|---|---|
| `create_if_absent` | 不存在才创建，存在则不覆盖 | 默认角色、默认日历分类、可由用户调整的配置 |
| `upsert_managed_fields` | 只更新系统托管字段，不覆盖用户字段 | 菜单路由、权限码、系统字典展示字段 |
| `force_system` | 系统强管，声明变化可覆盖 | 平台固定字典、内部系统配置 |

策略可以先落在 Resource Declaration 的扩展字段或新增字段。每个 ResourceHandler 负责解释策略，不能由 Registry 直接更新目标业务表。

### 5.4 Data Script

Data Script 不是 task 编排器。一个声明文件对应一个可执行数据脚本。

目录：

```text
META-INF/mango/data-scripts/*.yml
```

示例：

```yaml
id: system.admin-region.2026
module: system
phase: init
type: sql
source:
  type: url
  uri: https://artifact.example.com/mango/system/admin-region-2026.sql
  checksum: sha256:xxxx
runOnce: true
```

支持 source：

| source | 第一版是否支持 | 用途 |
|---|---:|---|
| `classpath` | 是 | 小 SQL 或随模块发布的脚本 |
| `file` | 是 | `/opt/mango/datasets/*.sql` 等部署文件 |
| `url` | 是 | 制品库、对象存储、内网 artifact URL |

执行记录：

```text
resource_data_script_history
```

建议字段：

```text
id
script_id
module
phase
type
source_type
source_uri
checksum
status
started_at
finished_at
error_message
```

第一版只记录脚本级历史，不记录 task 级历史。

URL SQL 限制：

- URL 必须 checksum。
- URL host 应走配置白名单。
- 生产环境默认手动执行，不随应用启动自动跑。
- DDL 不走 Data Script，仍归 Flyway。

### 5.5 停机升级流程

新库安装：

```text
1. 创建空数据库
2. 执行模块 schema baseline
3. 执行 baseline 后 Flyway DDL
4. 同步正式 resources
5. 按需执行 init Data Script
6. 按需启用 demo
7. 启动应用
```

老库停机升级：

```text
1. 停应用
2. 备份数据库
3. 执行 Flyway DDL 增量
4. 同步正式 resources
5. 执行 upgrade/init Data Script
6. 校验关键数据
7. 启动应用
```

## 6. Mango 当前情况推演

### 6.1 模块 migration 数量与风险

当前真实扫描结果：

| 模块 | migration 数 | 观察 |
|---|---:|---|
| `payment` | 99 | 高风险，含大量 INSERT/UPDATE/DELETE，优先做 baseline 和数据分类。 |
| `authorization` | 61 | 高风险，菜单/权限类 migration 多，应逐步转 Resource。 |
| `notice` | 16 | 中风险，含管理员接收账号 seed，应迁出 Flyway。 |
| `mango-cms` | 9 | demo seed 明显，应迁到 `META-INF/mango/demo/` 或 Data Script。 |
| `mango-job` | 8 | 含 sample job seed，应区分正式 job 与 demo/sample job。 |
| `identity` | 5 | 默认账号和联系方式要区分 bootstrap、demo、运行时账号。 |
| `system` | 4 | V1 混合 sys_tenant/sys_area/sys_i18n，属于本 issue 核心案例。 |
| 其它模块 | 1-3 | 大多可先保持现状，后续按新增规则收敛。 |

结论：第一版能力能覆盖当前高风险模块，但迁移应按模块分 Sprint，不应一次性重写所有历史 migration。

### 6.2 典型数据归属推演

| 当前数据 | 现状 | 目标归属 | 第一版是否覆盖 | 说明 |
|---|---|---|---:|---|
| 表结构、索引、约束 | Flyway | Flyway / baseline | 是 | baseline 解决新库全量结构，migration 继续解决老库增量。 |
| `sys_i18n` | system V1 seed | Resource `I18N_MESSAGE` | 是 | 已有 handler，适合正式 resource。 |
| `sys_area` 行政区划 | system V1 seed | Data Script / 标准数据 SQL | 是 | 不适合 yml；大数据可 file/url + checksum。 |
| `sys_tenant` 默认租户 | system V1 seed | 租户开通流程或 bootstrap handler | 部分覆盖 | 不应普通 Resource 化；第一版可从 Flyway 剥离，但具体开通流程需单独设计。 |
| authorization 菜单/权限 | 多个 Flyway SQL + Resource | Resource 正式资源 | 是 | 适合 `upsert_managed_fields`，避免覆盖运行时授权。 |
| 默认角色/角色绑定 | Flyway/Provisioner/Resource 混合 | Resource 或 TenantProvisioner | 是 | 可用 `create_if_absent` 保护用户调整。 |
| 工作流定义 | Resource 文件 + handler | Resource handler | 是 | 不直接 SQL；handler 负责部署、版本、校验。 |
| 日历基础分类/节假日 | 当前不统一 | Resource 或 Data Script | 是 | 小分类走 Resource；年度节假日/大数据走 Data Script 或 handler 生成。 |
| CMS demo 站点/新闻/广告 | cms Flyway seed | `META-INF/mango/demo/` 或 Data Script | 是 | demo 默认不进生产。 |
| notice admin 邮箱/手机号 | notice Flyway seed | demo 或用户运行时服务 | 是 | 不做普通 Resource。生产绑定由用户或同步任务维护。 |
| payment demo/runtime seed | payment Flyway seed | demo / Data Script / 业务 handler | 是 | demo 数据迁出；运行时修复类 SQL 后续归 Data Script 或专用 handler。 |
| 全国行政区划 500M/1G | 不适合 yml/Flyway | file/url Data Script | 是 | 只在 yml 登记 source/checksum，不把大文件放 jar。 |
| 测试数据 | 测试代码散落 | Test fixture | 不纳入本设计实现 | 第一版只明确不进入 Flyway/resources 默认扫描。 |

### 6.3 与现有 Resource 的兼容

现有 Resource 文件在 starter 下：

```text
META-INF/mango/resources/*-common-*.yml
META-INF/mango/resources/*-common-*.json
```

第一版不要求全部加外层 package，也不要求全部改格式。兼容策略：

- 正式资源继续默认扫描。
- 新增 demo 资源放 `META-INF/mango/demo/`。
- 需要保护运行时修改的 resource，逐步补同步策略。
- 需要大 SQL、远程 URL 或磁盘文件的场景，新增 Data Script。

## 7. 影响模块与边界

| 模块 | 改动类型 | 职责 |
|---|---|---|
| `mango-infra-persistence-starter` | 修改 | 增加模块 schema baseline 支持，仍只处理 DDL。 |
| `mango-resource-api` | 修改 | 增加 Resource 同步策略枚举或字段。 |
| `mango-resource-support` | 修改 | 扫描 `resources/` 与可选 `demo/`，解析 Data Script。 |
| `mango-resource-core` | 修改 | 执行 Data Script，记录脚本历史，调度 Resource sync。 |
| `mango-resource-starter` / `mango-resource-sync-starter` | 修改 | 增加 demo 开关、Data Script 执行入口。 |
| 业务模块 starter/core | 渐进迁移 | demo 资源迁目录，大 SQL 增加 data-script 声明。 |

依赖方向保持：

```text
业务模块 -> mango-resource API/SPI
mango-resource -> mango-infra-persistence
mango-infra-persistence 不依赖 mango-resource
```

## 8. 关键取舍

### 8.1 不做 Data Package

前期讨论的 Data Package + tasks 容易演化成第二套 Flyway。第一版只保留 Data Script，一个 yml 只描述一个脚本，不支持 task 数组。

### 8.2 不支持环境 profile

环境 profile 容易把业务差异藏进 dev/test/prod 目录。当前核心问题是正式/demo隔离，所以第一版只支持：

```text
resources/
demo/
```

### 8.3 Data Script 不承接 DDL

DDL 继续归 Flyway。Data Script 只处理数据导入、标准数据初始化、历史数据修复。

### 8.4 复杂升级用专用 handler，不做通用编排

如果升级涉及多个步骤、复杂业务校验或跨模块服务调用，第一版不靠通用 task 编排表达，而是写专用 Java handler 或拆成后续 Sprint。

## 9. 实施拆分建议

| Sprint | 目标 | 范围 | 准入结论 |
|---|---|---|---|
| S1 | Resource demo 隔离 | 增加 `demo-enabled`，支持扫描 `META-INF/mango/demo/`，迁移 CMS demo 作为试点。 | 可进入开发。 |
| S2 | Resource 同步策略 | 增加独立 `syncPolicy` 字段，先在菜单、字典、配置、工作流声明中落地。 | 需补 API 兼容设计后进入开发。 |
| S3 | Data Script | 支持 classpath/file/url SQL、checksum、执行历史、手动执行入口。 | 阻断，需补生产运维契约、DBA 执行边界和失败状态机。 |
| S4 | Schema Baseline 试点 | 先选择 `system` 或 `notice` 验证 baseline 机制，再考虑 `payment`、`authorization`。 | 阻断，需补 baselineVersion、新老库分流和等价校验。 |
| S5 | 高风险 migration 清理 | 分模块迁出 demo、seed、运行时修复 SQL。 | 依赖 S1-S4 的已验收能力。 |

当前评审结论：本文不能作为整体开发输入。只允许把 S1 拆成独立小任务推进；S2/S3/S4 必须先补设计并重新评审。

## 10. 验收与测试候选

| 用例 ID | 来源 | 场景 | 优先级 | 层级 | 自动化判断 |
|---|---|---|---|---|---|
| TC-001 | AC-001 | 新增 migration 扫描检查不包含明显 demo/seed/runtime data 关键词 | P1 | 静态检查 | AUTO |
| TC-002 | AC-002 | 空库使用模块 baseline 初始化后 Flyway history 正确 | P0 | 集成测试 | AUTO |
| TC-003 | AC-004 | `demo-enabled=false` 时不加载 `META-INF/mango/demo` | P0 | 单元/集成 | AUTO |
| TC-004 | AC-004 | `demo-enabled=true` 时加载 demo resource | P1 | 集成测试 | AUTO |
| TC-005 | AC-005 | `create_if_absent` 不覆盖已有运行时修改 | P0 | Handler 集成测试 | AUTO |
| TC-006 | AC-006 | Data Script 执行 file source 并记录 history | P0 | 集成测试 | AUTO |
| TC-007 | AC-006 | Data Script 执行 URL source 前校验 checksum | P0 | 单元/集成 | AUTO |
| TC-008 | AC-007 | 停机升级命令顺序可复核：Flyway -> Resource -> Data Script | P1 | 手工/集成 | MANUAL |

## 11. 风险与待评审问题

| 问题 | 风险 | 当前建议 |
|---|---|---|
| baseline 如何标记 Flyway 版本 | 标记错误会导致老 migration 重跑或漏跑 | 阻断。必须定义每模块 `coveredVersion`、history 记录方式、baseline 后首个 migration 版本。 |
| 新库/老库如何分流 | 老库误走 baseline 会漏执行历史迁移 | 阻断。必须定义空库、已有表无 history、已有模块 history、部分模块 history 四类判定。 |
| baseline 等价性如何验证 | 新库 baseline 结构和历史 migration 回放结构可能漂移 | 阻断。必须设计 schema diff 或结构校验。 |
| 历史 migration 是否移动 archive | 移动会影响已部署环境 validate | 阻断。第一版默认不移动已发布 migration；若移动必须另给兼容方案。 |
| Resource 同步策略字段放哪里 | API 兼容和 handler 解释成本 | 阻断。不能复用 `syncMode`，应新增独立 `syncPolicy` 并定义 hash、持久化、默认值和 handler 契约。 |
| Data Script 事务边界 | 大 SQL 可能半执行、锁表或不可重跑 | 阻断。必须定义整脚本/分语句/分批事务、超时、重试、幂等要求。 |
| URL/file SQL 安全 | 任意 URL 或路径执行风险高 | 阻断。必须定义 host 白名单、路径白名单、checksum、大小限制、SSRF/重定向/TLS 策略。 |
| Data Script 审计字段 | 失败后无法判断影响范围和执行人 | 阻断。必须记录执行人、环境、数据库、应用版本、影响表、影响行数、失败阶段、日志引用。 |
| Data Script 权限最小化 | 使用应用账号执行大 SQL 会放大事故面 | 阻断。必须区分 Flyway DDL、Resource、Data Script 所需数据库权限。 |
| demo 生产防护 | 生产误启 demo 会污染数据 | 阻断。必须定义生产强拦截或二次确认机制。 |
| `sys_tenant` 归属 | 涉及租户开通，不是普通静态数据 | 明确例外。单独设计 bootstrap/tenant provisioner，不作为本设计已覆盖能力。 |
| payment 历史 seed 清理 | payment 已有大量运行时和 demo 数据修复 | 按模块专项迁移，不在第一版统一重写。 |

## 12. 专家评审记录

已按以下视角完成初评：

| 视角 | 结论 | 阻断点摘要 | 可先推进范围 |
|---|---|---|---|
| 技术专家 | 不同意整体进入开发 | baseline 标记机制不成立；Resource 策略与 `syncMode` 维度冲突；Data Script 边界过宽；自检不能用未确认 `EXCEPTION` 放行。 | S1 Resource demo 隔离。 |
| 运维专家 | 不同意进入开发 | 停机升级 runbook 不完整；执行入口不明确；备份/PITR 前置不足；URL/file SQL 安全不足；日志、失败重入、demo 生产防护不足。 | S1 Resource demo 隔离。 |
| 数据库专家 | 不同意数据库相关开发 | baselineVersion、新库/老库分流、baseline 等价校验、Data Script 事务/checksum/幂等缺失。 | S1/S2 可拆开，S3/S4 需补设计。 |
| DBA | 不同意整体进入开发 | 大 SQL 缺少分批/限流/超时；锁表风险、执行窗口、回滚策略、审计字段、失败重试、权限最小化不足。 | 最多 S1。 |

评审结论：方案方向成立，但生产执行能力未闭环。本文不能直接进入整体开发；只允许拆出 S1 作为独立需求开发。S2/S3/S4 必须补充设计并重新组织评审。

## 13. 设计自检

| 检查项 | 结果 | 说明 |
|---|---|---|
| 是否明确目标、范围和不处理范围 | PASS | 已收敛为第一版小方案。 |
| 是否明确影响模块 | PASS | 已列出 persistence/resource/业务模块边界。 |
| 是否明确接口和数据变化 | BLOCKED | Resource 策略字段、Data Script history、baseline 标记仍需补完整契约。 |
| 是否覆盖当前 Mango 典型数据 | BLOCKED | `sys_tenant` 只能列为例外；Data Script 和 baseline 覆盖需要补设计。 |
| 是否避免长期规则写入 mango-docs | PASS | 本文是设计资产，后续规则需单独进入 mango-pmo。 |
| 是否存在阻断待确认项 | BLOCKED | 四个专家视角均不同意整体进入开发；仅 S1 可拆小推进。 |
