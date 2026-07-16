# Mango Resource 历史债务治理交付记录

## 1. 元数据

- 任务 ID：RESOURCE-DEBT-20260716
- 交付模式：FULL（跨模块公共契约治理；复用既有 Resource Registry 设计和交付契约，不伪造新的产品文档链）
- 需求影响：L3 - 资源注册、同步、管理接口和持久化属于平台公共能力，协议模型被多个平台模块直接消费。
- 方案风险：L3 - 调整 API/Core/Starter/Remote 分层并迁移协议模型，需要同步全部源码消费者并验证数据库、HTTP 契约和消费兼容性。
- 最终风险：L3
- 工作区决策：REUSE（`/Users/hardy/Work/mango-resource-debt`，分支 `refactor/resource-debt`）

## 2. 目标与范围

- 目标：清理 `mango-platform/mango-resource` 的架构和代码质量历史债务，建立可重复的单元、API、数据库集成和浏览器 E2E 基线。
- 成功条件：Resource 定向架构问题为 0；改前已有行为和 HTTP 路径保持兼容；有效自动化测试、全新 MySQL 启动和 Resource 页面 E2E 全部通过。
- 处理范围：Resource 的 `api/support/core/starter/sync-starter/starter-remote/target-core/target-starter` 八个子模块、Resource 正式测试、Resource 验收证据和历史债务经验文档；对 19 个直接消费者执行本地 SPI import/POM 的机械兼容迁移。
- 不处理范围：其它模块历史债务、全仓质量扫描、Resource Registry 新业务能力、线上数据迁移和发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| RR-001 | 资源声明提供方 | 合法正式或 demo 声明 | 解析、校验、注册和重复同步语义与改前一致 | 非法声明返回明确业务错误 | 现有单元/集成测试与新增边界测试通过 |
| RR-002 | Resource 管理 API | 有权限用户访问 `/resource/**` | 分页、强制同步、删除、日志和处理器契约响应字段与路径保持一致 | 参数、业务状态和权限错误不产生 500 | API 测试与浏览器 E2E 通过 |
| RR-003 | 本地/远程目标模块 | 目标模块存在对应 handler | upsert/disable/delete 正确调度到目标 handler | handler 缺失或远端失败返回统一业务错误 | 本地服务与远程适配测试通过 |
| RR-004 | 全新部署 | 空 MySQL 数据库 | Flyway 仅建立 Resource DDL，启动同步写入注册与日志数据 | DDL、字段或租户不一致时启动失败并暴露根因 | 全新库启动健康，表结构与数据断言通过 |
| RR-005 | 平台管理员浏览器入口 | 登录并具备 Resource 菜单权限 | 页面可查询注册资源、同步日志、变更日志和处理器契约，可执行受控同步/删除 | 非预期 4xx/5xx、console 错误或页面不可用即失败 | Resource Playwright 用例通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | RR-002 | HTTP 契约拆为 `ResourceAdminApi`、`ResourceRegistryApi`、`ResourceTargetApi`，Query/Command/VO 进入 API；Controller 实现 API 并只依赖对应 `I*Service` | api/core/starter/target-starter | 回退本次提交；HTTP 路径和 JSON 字段不变 |
| TD-002 | RR-001/RR-003 | 动态本地 SPI（Provider、Handler、Dispatcher、声明模型和 Builder）从公开 HTTP API 移入 `mango-resource-support`；业务失败统一 `Require + ResourceCode`，不保留第二套协议 | api/core/support/sync-starter 和直接消费者 | 回退本次提交；声明序列化字段名保持不变 |
| TD-003 | RR-003 | 目标服务端适配器独立进入 `target-core/target-starter`；`starter-remote` 只保留动态 URI 客户端和注册远程调用，不再同时承载服务端 Controller | starter-remote/target-core/target-starter 和能力应用 | 回退本次提交；远程 HTTP 路径保持 `/resource/targets/**` |
| TD-004 | RR-004 | Resource 实体使用统一租户实体，V1 保持空库最终结构，测试断言审计/租户字段和同步结果 | core migration/test | 删除一次性测试库并回退提交 |
| TD-005 | RR-001/RR-002 | 复杂同步流程按校验、分组、执行、记录拆分，保持 hash、INIT_ONLY、AUTO/MANUAL/LOCKED 和删除语义不变 | core | 回退本次提交 |
| TD-006 | RR-001/RR-003 | HTTP 协议使用 command/query/vo/enums，本地协作类型使用 support SPI；19 个源码消费者同步依赖/import，不借机修改消费者业务实现 | api/support 与直接消费者 | 单提交整体回退，不保留双套废弃模型 |
| TD-007 | RR-001/RR-005 | 强制同步仍可重放 `AUTO`，但目标 Handler 必须在声明租户上下文内幂等执行；调用后恢复原上下文，禁止通过忽略租户拦截器注解规避 | notice-core 直接消费者与 Resource E2E | 回退 Notice Handler 上下文切换；保留强制同步回归用例 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | 1 | `mango-resource-api`、`mango-resource-core` | API、模型、Service 和错误码分层符合门禁 |
| IMPL-002 | TD-001/TD-003 | 2 | `mango-resource-starter*`、`mango-resource-target-*` | Controller/Feign/Service 契约一致且测试覆盖成功与失败 |
| IMPL-003 | TD-004/TD-005 | 3 | core migration、同步服务和数据库测试 | 全新库与同步语义测试通过 |
| IMPL-004 | 全部 | 4 | Resource 模块测试、E2E、证据、经验文档 | 改前/改后结果可对比，所有验收项有证据 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| RR-001 | 单元与 H2 集成 | Resource 聚合 `clean test` | 改前 48/48 PASS；改后 50/50 PASS | `.runtime/resource-debt/before/mvn-test.log`、`mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-002 | API 测试 | 真实管理员 token 调用参数错误与强制同步 | 缺 `resourceId` 返回 400；强制同步返回 200/true | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-003 | 本地/远程适配与消费者兼容 | Resource 聚合测试、19 个直接消费者编译、Authorization 真实注册集成测试 | 全部 PASS | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-004 | 全新 MySQL 集成 | 重建 `mango_dev_mango_resource_debt_001` 后启动后端 | V1 唯一成功迁移；三表租户/审计字段完整；1782 条声明完成 | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| RR-005 | UI/E2E | Resource Playwright spec，Chromium 单 worker | 1/1 PASS（48.8s），含真实登录、页面、接口、强制同步和零运行时错误 | `mango-docs/evidence/baselines/resource/latest/acceptance.md` |
| 全部 | 定向静态与架构门禁 | Resource 八子模块的 partial Reactor 报告 | 改前 149 条架构、58 条静态；改后 dependency/ArchUnit/PMD/blocking 均为 0，静态问题和工具失败为 0 | `.runtime/resource-debt/before/architecture-detail.tsv`、`mango-docs/evidence/baselines/resource/latest/acceptance.md` |

## 7. 例外与剩余风险

- 不执行全仓扫描；本次结论仅覆盖 Resource 八个子模块、必要直接消费者和真实单体入口。
- `full` 模式要求完整 Reactor，不能用于定向模块验收；本次保留已审计的八模块 partial 报告，不用全仓检查替代模块结论。
