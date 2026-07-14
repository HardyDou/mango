# Notice 历史债务治理交付契约

## 1. 目标

一次性治理 `mango-platform/mango-notice` 的 API、Core、Starter、Remote、Support、六类渠道、DDL 与初始化资源债务；保持业务方法、字段、权限租户、发送/重试状态、渠道副作用、站内信动作和公告结果。仓内调用方同批升级到固定路径，不保留双协议。

## 2. 范围

设计输入为 `mango-docs/designs/notice-architecture-debt/` 下已批准的 BRD、SRS、TDD 与实施计划。代码范围为 Notice 十一个 Maven 子模块、实际仓内 HTTP 调用点、Payment 的 Workflow 直接兼容点、模块 README、能力地图、测试和本证据。

## 3. 不做什么

不新增通知渠道或业务特性；不改变发送策略和页面产品交互；不支持旧 Flyway history 原地升级；不初始化用户联系方式、任务、发送记录、站内消息或公告；不执行全仓检查。

## 4. 设计输入

- `BRD-NOTICE-DEBT`、`SRS-NOTICE-DEBT`、`TDD-NOTICE-DEBT`、`PLAN-NOTICE-DEBT` 均为 `APPROVED/NEXT`。
- 用户已批准所有后续模块沿用 Payment 政策，一步到位、只支持新数据库并在确认无问题后直接合并。
- before 生产基线为 main `3264cfaa6`；实际架构扫描 663 条，历史预算 697 条。

## 5. 设计说明

### 5.1 影响模块

Notice API、Support、Core、Starter、Starter Remote、Site/Email/SMS/WeCom/DingTalk/WeChat Official 六类渠道、Notice 前端实际调用点，以及完整启动所需的 Payment 直接消费者。

### 5.2 接口变化

Java 业务方法、字段、返回和错误语义保持；历史路径变量改为固定 path 与显式 query/body；Controller、Feign 和仓内前端同批切换并由指纹测试保护。

### 5.3 数据变化

V1-V17 折叠为单一纯 DDL V1，等价建立 20 张最终表。管理员邮箱/手机号不进入任何默认初始化；正式资源、Demo 与运行态数据分层。

### 5.4 菜单/页面/权限变化

页面、菜单、permission code 和可见性不变，只同步请求 URL 与 binding；个人消息仍按当前 permission、用户和租户归属保护。

### 5.5 测试范围

保留 71 条既有用例；新增 7 条高价值契约用例，覆盖两个公共 API、全部 HTTP 路由与权限、Feign、20 表最终 schema 和正式资源边界。改后复用同一 78 条入口，并增加纯 DDL、新库启动和仓内请求目录验证。

### 5.6 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango/mango-platform/mango-notice`、实际仓内调用点、Payment 直接兼容点 |
| README/使用说明 | 是 | Notice README；前端无独立说明时在后端 README 记录唯一请求目录 |
| 需求文档 | 是 | `mango-docs/designs/notice-architecture-debt` |
| 详细设计文档 | 是 | 同上 TDD/Plan |
| E2E 脚本 | 否 | EXCEPTION：无新增浏览器交互；HTTP/Feign/前端请求目录、真实持久化和完整服务启动提供更直接证据 |
| 测试结果基线 | 是 | `mango-docs/evidence/baselines/notice-architecture/latest/report.md` |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001,SAC-002 | 配置、发送、记录、Outbox 和渠道副作用 | P0 | 单元/API | AUTO | H2 fixture 与渠道替身 | 返回、状态、版本、幂等和发送请求 | Notice 十一模块 Maven test | `report.md` | AUTOMATED |
| TC-002 | SAC-003 | 本人消息动作、未读数、公告发布/确认 | P0 | API | AUTO | 多用户、多租户消息与公告 | 归属、动作、确认和状态 | 同一 Maven test | `report.md` | AUTOMATED |
| TC-003 | SAC-004 | 两个 Java API、全部 HTTP/Feign、权限和仓内 URL | P0 | API | AUTO | 反射、路由与请求 fixture | 方法、字段、verb/path/binding/permission | Maven test+前端定向检查 | `report.md` | AUTOMATED |
| TC-004 | SAC-005 | 单一纯 DDL V1、20 表 schema 和新库启动 | P0 | API | AUTO | 独立 workspace MySQL | schema hash、零 Flyway DML、health | Maven test+CLI backend start | `report.md` | CANDIDATE |
| TC-005 | SAC-005 | 正式/Demo/运行态资源边界 | P1 | API | AUTO | 正式与显式 Demo 声明 | 默认无个人联系和运行态数据 | Maven resource test+新库查询 | `report.md` | CANDIDATE |
| TC-006 | SAC-005 | Workflow→Payment 消费者兼容和完整启动 | P0 | API | AUTO | 退款审批 JSON、独立新库 | DTO JSON 等价、Payment test、health | Payment 定向 test+monolith start | `report.md` | CANDIDATE |

## 6. 风险与限制

主要风险是拆分大服务改变事务/副作用顺序、类型迁移遗漏、固定路径遗漏调用方、V1 折叠遗漏结构和 Payment 兼容修复扩大。每项都以同组测试、全调用目录、schema hash、定向消费者测试和完整启动作为停止条件。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求、SAC-001至SAC-004 | 先建立有价值基线 | 保留71条并新增7条高风险契约保护 | Notice tests、remote test dependency | Notice README待最终同步 | Notice 四阶段规格 | EXCEPTION：后端领域与协议由自动测试直接观察 | `report.md` | before 78/78、test quality PASS | DONE | `report.md` |
| TASK-002 | 用户要求、SAC-001至SAC-004 | 十一模块一次到最终边界 | 窄服务、选择性 CRUD、规范实体/Mapper/SPI、固定路径 | `mango-platform/mango-notice` | Notice README | TDD/Plan | EXCEPTION：页面交互不变 | `report.md` | 78条同组 after、663→0 | TODO | `report.md` |
| TASK-003 | 用户要求、SAC-005 | Flyway 只负责最终 DDL | V1-V17 折叠为纯 DDL V1 | core migration | Notice README | TDD/Plan | EXCEPTION：数据库结构无浏览器结果 | `report.md` | schema hash 等价、零 DML、新库启动 | TODO | `report.md` |
| TASK-004 | 用户要求、SAC-005 | 正式/Demo/运行态分层 | 用户联系方式和运行态数据不初始化 | starter resources | Notice README | TDD/Plan | EXCEPTION：资源同步无新增 UI | `report.md` | 默认数据集合和声明检查 | TODO | `report.md` |
| TASK-005 | 用户要求、SAC-005 | 完整应用兼容、PR 和合并 | 等价修复 Payment 直接消费者，最新 main 同步一次 | Payment direct consumer、docs | README/capability map | Plan | EXCEPTION：完整后端启动提供系统证据 | `report.md` | Payment test、health、required check | TODO | `report.md` |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001至TC-003 | Notice 十一模块测试入口 | 配置、发送、消息、公告、API/HTTP/Feign、schema 和资源 | 71条既有+7条契约 | 78/78；API/HTTP/Feign hash、20表集合和正式资源边界被冻结 | EXCEPTION：无新增 UI | EXCEPTION：before 自动测试不经浏览器 | `report.md` 与 surefire reports | PASS |
| TASK-003 | TC-004 | Notice migration/启动 | 最终结构和正式新库 | 独立 MySQL | before schema hash 已记录；after 待执行 | EXCEPTION：数据库结构无 UI | after 待执行 | `report.md` | TODO |
| TASK-005 | TC-006 | monolith | Payment 兼容和完整启动 | 退款审批 JSON、独立新库 | Payment test 和 health | EXCEPTION：后端启动 | after 待执行 | `report.md` | TODO |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001 | TC-001至TC-003 | EXCEPTION：后端改前基线 | Notice 十一 artifact `test` | Java 21.0.10、Maven 3.9.13 | H2 fixture、渠道替身 | tenant 1/2、user A/B | channel 29、core 38、starter 9、remote 2，共78/78 | 无；Mockito/JCL为既有警告 | `report.md` 与 surefire reports | 生产代码尚未修改 |
| BASELINE-002 | TASK-001至TASK-005 | TC-001至TC-006 | EXCEPTION：无新增浏览器行为 | 同一78条、定向 architecture/static、前端请求目录、Payment test、单体新库启动 | Java 21/MySQL 8.4 | H2 fixture与独立新库 | tenant 1/2 | after 待补 | 待补 | `report.md` | 内部架构、固定路径和初始化政策有意调整 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango Notice 业务开发者 | 使用唯一固定路径、选择性 CRUD 服务和单一新库 V1；正式资源默认，Demo 显式；复用同组测试 | Notice README 与本交付契约 | Notice 十一模块定向 test | 每个 workspace 独立新库；运行态由用户操作形成 | 测试查 surefire；启动/资源失败查 workspace 日志 | IN_PROGRESS |
