# Issue #184 S5 Mango 数据物料清理清单与适配推演

状态：已完成。

当前总入口见 [Issue #184 总设计](../designs/2026-07-01-issue-184-data-governance-design.md)。本清单只做当前 Mango 物料盘点和后续清理优先级，不定义新的平台能力。

## 目标

基于 S1-S4 已落地能力，梳理 Mango 现有初始化、demo、运行时、升级数据物料的归属，给后续模块开发和存量清理提供执行边界。

S5 不新增平台能力，重点回答三个问题：

- 现有数据物料现在在哪里。
- 以后同类数据应该放在哪里。
- 哪些存量问题需要清理，哪些不能在本轮自动迁移。

## 验收计划

- 列出现有 Resource 声明、demo 声明、Flyway migration 的主要分布。
- 按模块给出风险数据物料分类和处理建议。
- 明确新能力能覆盖的场景和不能覆盖的场景。
- 明确后续清理优先级。
- 文档检查通过，无代码行为变更。

## 总体规则

| 数据类别 | 以后归属 | 处理规则 |
| --- | --- | --- |
| 表结构 DDL | Flyway 默认 classpath migration 或 baseline pack | 继续只由 Flyway 管理。 |
| 模块固定初始化数据，后续不由用户改 | Resource `AUTO` 或模块 Owner 初始化 | 声明变化后允许同步覆盖。 |
| 模块初始化数据，运行时会被用户改且升级要保留 | Resource `INIT_ONLY` | 首次创建，后续升级只更新 registry 元数据，不覆盖业务表。 |
| demo 数据 | `META-INF/mango/demo/`，显式 `demo-enabled=true` | 默认不进正式启动路径。 |
| 大 SQL、大字典、外部升级包 | Flyway `filesystem:` / `http(s):` locations | 停机升级时由运维配置，仍写 Flyway history。 |
| 新数据库完整结构 | schema baseline pack | 新库可用全量结构 SQL，旧库继续走历史 migration。 |
| 运行时生成数据 | 业务 Owner Service / 同步任务 | 不进入 Resource 和 Flyway 初始化声明。 |
| 历史运行时数据修复 | Flyway 升级脚本或模块维护脚本 | 只做确定性、可回滚评审的停机升级。 |

## 当前扫描结果

### Resource 声明

当前正式 Resource 声明位于 `META-INF/mango/resources/`，覆盖 35 个文件：

- auth：dict、domain。
- authorization：dict、menu。
- calendar：domain、menu。
- cms：menu。
- domain：domain、menu。
- file：dict、domain、menu、storage。
- job：definition、domain、menu。
- notice：domain、menu、message。
- numgen：domain、menu。
- org：dict。
- payment：domain、job、menu、numgen。
- system：config、dict、menu。
- template：dict、domain、menu。
- workflow：definition、domain、menu。

结论：菜单、字典、领域、系统配置、文件存储、任务定义、消息模板、号段、工作流定义已经具备 Resource 化基础。后续主要补齐 `sync-mode` 语义，不需要新增 `Data Package` 或任务历史表。

### Demo 声明

当前 `META-INF/mango/demo/` 没有正式物料。

结论：S1 已经提供 demo 隔离加载能力，但 CMS、payment 等历史 demo/模拟数据还在 Flyway migration 中，后续新增 demo 数据必须进入 demo 目录，存量迁移需要单独排期。

### Flyway migration 分布

按模块统计：

| 模块 | SQL 数量 | 主要风险 |
| --- | ---: | --- |
| payment | 99 | DDL、种子数据、模拟运行时数据、清理脚本混杂。 |
| authorization | 61 | 大量菜单、按钮、权限、角色相关数据通过 SQL 演进。 |
| notice | 16 | 内置站内信渠道、收件人、邮件模板、任务消息种子数据。 |
| mango-cms | 9 | 多个 demo 站点、demo 新闻、demo 广告种子 SQL。 |
| mango-job | 8 | sample job 和 payment job 种子 SQL。 |
| identity | 5 | 管理员联系方式、外部身份组织变更等运行时修复。 |
| system | 4 | 个人配置、配置面板、选项源等平台配置数据。 |
| workflow | 3 | schema、领域、发起入口显示字段。 |
| 其他模块 | 1-2 | 主要是初始化 schema。 |

## 模块适配推演

### authorization

现状：`V27` 之后存在大量 menu、permission、role、data scope 相关 SQL。

目标：

- 新增菜单、按钮、权限声明进入 `authorization-common-menu.json` 或对应模块自己的 menu resource。
- 运行时用户改过的角色授权、数据范围用 `INIT_ONLY` 或业务 Owner 维护，不能被升级覆盖。
- 历史 migration 保留，已上线库继续依赖 Flyway history。

后续清理：

- P0：停止新增 menu/permission 类 Flyway migration。
- P1：将仍在 SQL 中演进的菜单权限整理为 Resource 声明。

### cms

现状：`V5`、`V6`、`V8`、`V9`、`V10` 是 demo 站点、demo 新闻、demo 封面、demo 广告、demo 帮助站点数据。

目标：

- 新增 CMS demo 数据进入 `mango-cms-starter/src/main/resources/META-INF/mango/demo/`。
- 只有最终应用显式配置 `mango.resource.registry.demo-enabled=true` 时加载。
- 正式环境默认不安装 demo 站点、demo 新闻、demo 广告。

后续清理：

- P1：把 CMS demo 数据从默认 Flyway 路径迁到 demo Resource。
- P1：历史 SQL 不直接删除，避免破坏已有库的 Flyway history。

### payment

现状：payment 有 99 个 SQL，包含 schema 演进、Mango Pay 种子、合约模板、通道配置、模拟运行时数据、demo 清理、运行时残留清理等。

目标：

- DDL 和历史修复继续走 Flyway。
- 支付菜单、号段、任务定义已具备 Resource 声明基础。
- 支付通道、合约模板、公共演示密钥、模拟运行时数据不能再混入默认初始化路径。
- 大的支付升级修复包使用 S3 的 `filesystem:` / `http(s):` locations，由运维在停机升级时显式配置。

后续清理：

- P0：停止新增 simulated/runtime/demo seed 进入默认 Flyway。
- P1：区分通道主数据、demo 通道数据、运行时订单/对账/流水数据。
- P2：payment SQL 数量已经偏高，适合优先制作 schema baseline pack。

### notice

现状：notice 同时存在站内信渠道、默认租户渠道、管理员收件人、邮件模板、任务消息种子数据；starter 已有 `notice-common-message.yml`。

目标：

- 消息模板、通知定义等可声明数据进入 Resource，运行时可改的使用 `INIT_ONLY`。
- 发送记录、任务收件人快照、用户接收偏好属于运行时数据，不进入 Resource。
- 历史修复继续保留在 Flyway。

后续清理：

- P1：把可声明的 notice template/channel/job message 收敛到 Resource。
- P2：运行时偏好和发送记录只由 notice Owner Service 管理。

### job

现状：有 sample jobs 和 payment channel bill fetch job 种子 SQL，starter 已有 `job-common-definition.yml`，payment starter 也有 `payment-common-job.yml`。

目标：

- 系统必需任务进入 Resource，运行时可调参数使用 `INIT_ONLY`。
- sample/demo job 进入 demo 目录。
- job worker snapshot、实例、日志等运行时数据继续由 job 服务维护。

后续清理：

- P1：判断 `seed_default_sample_jobs` 是否为 demo；若是则迁到 demo Resource。
- P1：支付账单拉取任务优先迁到 payment 模块 Resource。

### system

现状：系统配置、个人配置、面板元数据、选项源混在 Flyway migration 和 `system-common-config.yml` 中。

目标：

- 平台默认配置、配置元数据进入 Resource，运行时可改配置使用 `INIT_ONLY`。
- 用户个人配置属于运行时数据，不应由升级脚本覆盖。

后续清理：

- P1：梳理 system config 哪些是平台默认值，哪些是用户配置。
- P2：个人配置类升级只允许做补字段、补默认值，不允许覆盖用户值。

### file

现状：starter 已有 `file-common-storage.yml`，core 仍有默认 local storage active SQL。

目标：

- 默认存储配置用 Resource `INIT_ONLY` 表达。
- 用户修改的存储启用状态、密钥、桶配置作为运行时配置保留。

后续清理：

- P1：默认本地存储从 Flyway seed 收敛到 Resource。

### workflow

现状：starter 已有 `workflow-common-definition.yml`，core migration 主要是 schema 和字段演进。

目标：

- 流程定义初始化进入 Resource `INIT_ONLY`。
- 流程定义的部署、发布、版本切换仍由 workflow Owner Service 完成，Resource 只承载声明入口。
- 流程实例、任务、审批记录是运行时数据，不进入 Resource。
- 涉及旧流程定义字段迁移的复杂升级继续走 Flyway 或模块维护脚本。

后续清理：

- P1：明确 workflow definition handler 的发布语义，避免只导入草稿但未部署。
- P2：复杂定义迁移单独写升级脚本，不做通用任务编排。

### calendar

现状：calendar 目前只有 schema、domain、menu。用户提到每年 300 多条日历/节假日数据。

目标：

- 小规模、稳定的日历类型、菜单、领域信息可用 Resource。
- 全国行政区划、节假日、每年日历这类大数据不建议放 YAML。
- 数据量达到几十 MB 以上时，使用外部 SQL 或模块 Owner 批量导入；500MB/1GB 级别必须走外部文件、对象存储或运维分发目录。

后续清理：

- P1：如果日历基础数据必须内置，提供 `filesystem:` / `http(s):` Flyway 包或模块批量导入服务。
- P2：年度日历数据按年份独立发布，不打进默认 Resource 包。

## 已覆盖的问题

- demo 数据和正式数据隔离：S1 已覆盖。
- 初始化一次、升级保留运行时改动：S2 `INIT_ONLY` 已覆盖。
- 停机升级时执行磁盘 SQL、远程 URL SQL：S3 已覆盖。
- 新数据库不想从 V1 跑几百个 SQL：S4 baseline pack 方案已覆盖。
- 大 SQL 不进 YAML、不进 jar 默认 classpath：S3/S4 已覆盖。
- 模块向 authorization、job、notice 等公共模块注入声明：用模块自己的 Resource 声明文件，handler 按 resource type 写入目标模块表。

## 未覆盖的问题

- 不自动迁移历史 Flyway seed/demo SQL。历史 SQL 需要保留，避免破坏已上线数据库的 Flyway history。
- 不做在线影子表升级。第一版本只支持停机升级。
- 不做管理页面。当前问题是工程治理和启动迁移，不需要 UI。
- 不做通用 bean 方法任务编排。复杂升级先用模块维护脚本或 Flyway SQL，避免把 SQL 治理问题扩大成任务治理平台。
- 不把 demo 数据做成单独 starter 或部署时替换依赖。demo 跟模块走，但默认不加载。

## 后续小任务优先级

| 优先级 | 任务 | 验收标准 |
| --- | --- | --- |
| P0 | 新增规范：禁止新增 menu/permission/demo/simulated runtime seed 到默认 Flyway | PR review 能按规则拦截。 |
| P1 | CMS demo 数据迁入 `META-INF/mango/demo/` | 默认启动不安装 CMS demo；显式 demo 开关才安装。 |
| P1 | authorization 菜单权限 SQL 收敛到 Resource | 新增菜单权限不再增加 authorization migration。 |
| P1 | job sample/payment job 种子归类 | demo job 走 demo，必需 job 走 Resource `INIT_ONLY`。 |
| P1 | notice template/channel/job message 收敛 | 可声明通知数据进入 Resource，运行时发送数据不受影响。 |
| P1 | payment simulated/demo seed 分类 | 默认 Flyway 不再产生演示或模拟运行时数据。 |
| P2 | payment、authorization baseline pack | 新库可用 baseline 初始化，不必从 V1 跑完整历史。 |
| P2 | calendar/行政区划大数据导入方案 | 大数据通过外部 SQL 或模块批量导入，不进入 YAML。 |

## 结论

Issue #184 第一阶段不需要把 Resource 升级成完整 DataOps 平台。当前最小闭环是：

1. 正式声明继续走 Resource。
2. demo 声明进入 `META-INF/mango/demo/` 且默认关闭。
3. 运行时可改的初始化声明使用 `INIT_ONLY`。
4. DDL、复杂升级、大 SQL 继续走模块化 Flyway，必要时使用外部 locations 和 baseline pack。
5. 存量历史 SQL 不强行删除，后续按模块逐步清理新增路径。
