# Mango Resource 历史债务治理交付记录

## 1. 元数据

- 任务 ID：RESOURCE-DEBT-20260716
- 交付模式：FULL（跨模块公共契约治理；复用既有 Resource Registry 设计和交付契约，不伪造新的产品文档链）
- 需求影响：L3 - 资源注册、同步、管理接口和持久化属于平台公共能力，协议模型被多个平台模块直接消费。
- 方案风险：L3 - 调整 API/Core/Starter/Remote 分层并迁移协议模型，需要同步全部源码消费者并验证数据库、HTTP 契约和消费兼容性。
- 最终风险：L3
- 工作区决策：REUSE（最终收口工作区 `/Users/hardy/Work/mango-resource-target-removal`，分支 `refactor/resource-target-removal`）

## 2. 目标与范围

- 目标：清理 `mango-platform/mango-resource` 的架构和代码质量历史债务，删除空壳发布物，建立可重复的单元、API、数据库集成及单体/微服务拓扑 E2E 基线。
- 成功条件：Resource 定向架构问题为 0；改前已有行为和 HTTP 路径保持兼容；有效自动化测试、全新 MySQL、单体/微服务单节点和多节点验证全部通过。
- 处理范围：Resource 最终 `api/support/core/starter/sync-starter/starter-remote` 六个子模块、必要基础设施、Resource 正式测试、验收证据和历史债务经验文档；删除没有有效实现的 `target-core/target-starter`。
- 不处理范围：其它模块历史债务、全仓质量扫描、Resource Registry 新业务能力、线上数据迁移和发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| RR-001 | 资源声明提供方 | 合法正式或 demo 声明 | 解析、校验、注册和重复同步语义与改前一致 | 非法声明返回明确业务错误 | 现有单元/集成测试与新增边界测试通过 |
| RR-002 | Resource 管理 API | 有权限用户访问 `/resource/**` | 分页、强制同步、删除、日志和处理器契约响应字段与路径保持一致 | 参数、业务状态和权限错误不产生 500 | API 测试与浏览器 E2E 通过 |
| RR-003 | 本地/远程目标模块 | 目标模块存在对应 handler | upsert/disable/delete 正确调度到目标 handler | handler 缺失或远端失败返回统一业务错误 | 本地服务与远程适配测试通过 |
| RR-004 | 全新部署 | 空 MySQL 数据库 | Flyway 仅建立 Resource DDL，启动同步写入注册与日志数据 | DDL、字段或租户不一致时启动失败并暴露根因 | 全新库启动健康，表结构与数据断言通过 |
| RR-005 | 真实部署拓扑 | 全新库、真实 Nacos/KV/锁 | 单体和微服务在单节点、多节点、乱序启动及注册中心实例失效后均能最终收敛 | 锁竞争、父资源未就绪或节点失效不得静默丢声明 | 数据计数、重复数、同步日志、Nacos 健康实例和故障切换均符合预期 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | RR-002 | HTTP 契约拆为 `ResourceAdminApi`、`ResourceDeclarationApi`、`ResourceTargetApi`，Query/Command/VO 进入 API；Controller 实现 API 并只依赖对应服务端口 | api/core/starter/sync-starter | 回退本次提交；HTTP 路径和 JSON 字段不变 |
| TD-002 | RR-001/RR-003 | 动态本地 SPI（Provider、Handler、Dispatcher、声明模型和 Builder）从公开 HTTP API 移入 `mango-resource-support`；业务失败统一 `Require + ResourceCode`，不保留第二套协议 | api/core/support/sync-starter 和直接消费者 | 回退本次提交；声明序列化字段名保持不变 |
| TD-003 | RR-003 | 删除没有有效源码的 `target-core/target-starter`；纯执行器进入 support，目标 Controller 由 sync-starter 装配，starter-remote 只保留客户端 | support/sync-starter/starter-remote 和能力应用 | 回退本次提交；远程 HTTP 路径保持 `/resource/targets/**` |
| TD-004 | RR-004 | Resource 实体使用统一租户实体，V1 保持空库最终结构，测试断言审计/租户字段和同步结果 | core migration/test | 删除一次性测试库并回退提交 |
| TD-005 | RR-001/RR-002 | 复杂同步流程按校验、分组、执行、记录拆分，保持 hash、INIT_ONLY、AUTO/MANUAL/LOCKED 和删除语义不变 | core | 回退本次提交 |
| TD-006 | RR-001/RR-003 | HTTP 协议使用 command/query/vo/enums，本地协作类型使用 support SPI；19 个源码消费者同步依赖/import，不借机修改消费者业务实现 | api/support 与直接消费者 | 单提交整体回退，不保留双套废弃模型 |
| TD-007 | RR-001/RR-005 | 强制同步仍可重放 `AUTO`，但目标 Handler 必须在声明租户上下文内幂等执行；调用后恢复原上下文，禁止通过忽略租户拦截器注解规避 | notice-core 直接消费者与 Resource E2E | 回退 Notice Handler 上下文切换；保留强制同步回归用例 |
| TD-008 | RR-005 | 远程同步失败或注册中心返回未完成时周期重试；注册中心未获得分布式锁必须返回 `false`，不能把跳过伪装成成功 | core/sync-starter | 回退本次提交；默认重试间隔 10 秒，可配置 |
| TD-009 | RR-003/RR-005 | 动态服务地址必须通过 LoadBalancer 解析并保留 base path；内部调用只信任 HMAC Filter 写入的服务端属性 | infra-feign/infra-web/auth/authorization/resource-remote | 回退本次提交；公开 API 与 Header 名保持不变 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | 1 | `mango-resource-api`、`mango-resource-core` | API、模型、Service 和错误码分层符合门禁 |
| IMPL-002 | TD-001/TD-003 | 2 | `mango-resource-support`、`mango-resource-sync-starter`、`mango-resource-starter-remote` | Controller/Feign/Executor 契约一致且测试覆盖成功与失败；空 target 模块不再发布 |
| IMPL-003 | TD-004/TD-005 | 3 | core migration、同步服务和数据库测试 | 全新库与同步语义测试通过 |
| IMPL-004 | 全部 | 4 | Resource 模块测试、E2E、证据、经验文档 | 改前/改后结果可对比，所有验收项有证据 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| RR-001 | 单元与 H2 集成 | Resource 六模块定向 `clean verify` | 改前 48/48 PASS；改后 60/60 PASS | `.runtime/resource-debt/before/mvn-test.log`、`mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-002 | API 测试 | 真实管理员 token 调用参数错误与强制同步 | 缺 `resourceId` 返回 400；强制同步返回 200/true | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-003 | 本地/远程适配与消费者兼容 | Resource 聚合测试、19 个直接消费者编译、Authorization 真实注册集成测试 | 全部 PASS | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-004 | 全新 MySQL 集成 | 分别重建单体和微服务验收库后启动真实应用 | Flyway、Resource 表结构、声明注册和派生关系完成 | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-005 | 拓扑 E2E | 单体/微服务单节点、多节点、乱序启动和 Resource 节点失效 | registry 数量稳定、重复为 0、锁竞争不丢声明、Nacos 实例健康、故障切换成功 | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| 全部 | 定向静态与架构门禁 | Resource 六模块 `clean verify` | 32 个 Reactor 模块成功；架构规则 154/154，Resource 测试 60/60，Checkstyle/PMD/SpotBugs 通过 | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |

## 7. 例外与剩余风险

- 不执行全仓扫描；本次结论覆盖 Resource 六个最终子模块、必要基础设施以及真实单体/微服务入口。
- Resource 当前没有独立产品菜单或页面，Resource 专属浏览器 UI 验收为不适用；通用 Chromium shell/API 用例不能写成“Resource 页面通过”。
