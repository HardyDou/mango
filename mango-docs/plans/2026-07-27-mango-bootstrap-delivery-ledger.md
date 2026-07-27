# Mango Bootstrap 一次性交付台账

## 1. 模式基线

- 任务：一次性交付 Bootstrap/Runtime 分离、Resource 执行分类与回执、generation fencing、expand/contract 和 finalize 前 abort。
- 需求影响：L3。改变所有最终应用启动入口、数据库初始化、资源初始化、租户对账和发布主流程。
- 方案风险：L3。跨 Infra/Resource/System/App/BOM，涉及并发互斥、持久化、滚动升级和 finalize 后不可逆边界。
- 最终风险：L3，交付模式 FULL。
- 工作区：`/Users/hardy/Work/mango-bootstrap`，分支 `feat/mango-bootstrap`，M01=REUSE。
- 设计：[Mango Bootstrap 生命周期治理设计](../designs/2026-07-27-mango-bootstrap-lifecycle-design.md)，用户已确认进入实施。
- 启用措施：M08 能力说明、M09 静态验证、M10 状态机/指纹/门禁单测、M11 MySQL/Spring/跨模块集成、M12 命令与 Resource API 契约验证、M14 高风险架构复核、M16 空库/升级/滚动场景验收。
- 不启用：M13（无页面或浏览器入口）、M15（当前交付不写外部系统状态）。M02 性能测试只创建带 `_bootstrap_sql_perf` / `_bootstrap_resource_perf` 后缀的专用空库，并在结束后自动删除；不清空共享或既有业务库。
- 旧业务升级口径：保留业务源码与模块能力，允许丢弃旧数据库、历史业务数据和旧 Flyway 执行历史，以空库 cold Bootstrap 生成 generation 1；新生命周期启用后的后续升级继续执行 rolling 三阶段。

## 2. 原子交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | 说明或文档 | 验证与证据 | 状态 |
|---|---|---|---|---|---|---|---|
| MB-001 | DEC-001/002/003 | 提供同制品 Bootstrap/Runtime 入口，Runtime 不执行阻断初始化 | `MangoApplication` 路由进程模式，Bootstrap 非 Web | `mango-infra-bootstrap-api/core/starter` | Bootstrap README、应用接入指南 | 所有官方应用已切换入口，164 模块 test-compile 通过 | DONE |
| MB-002 | DEC-002/007 | 提供 plan/apply/verify/finalize 编排与有向步骤图 | SPI contributor + 拓扑排序 + 幂等步骤执行 | Bootstrap API/core | 设计第 6 节 | `BootstrapPlanBuilderTest` 与编排测试 | DONE |
| MB-003 | DEC-005 | 持久化 execution、step、control、runtime lease | Bootstrap 自有 Flyway migration 与 JDBC repository | Bootstrap core/starter migration | 数据表说明 | `JdbcBootstrapRepositoryIntegrationTest` | DONE |
| MB-004 | DEC-005 | release/generation/manifest fingerprint fail closed | 规范化 SHA-256、同代防漂移、stale 拒绝 | Bootstrap manifest/gate | Bootstrap README | fingerprint、同代冲突、generation 边界测试 | DONE |
| MB-005 | DEC-002/006 | Flyway 从 Runtime 自动执行迁入 Bootstrap expand/contract | 显式 migration executor，保留模块 history table | persistence starter contributor | persistence README | Persistence Flyway 96 项测试基线 | DONE |
| MB-006 | DEC-004 | Resource 新增 executionPhase，与 syncMode 正交 | 默认 required；eventual/manual 显式声明 | resource API/support/core | resource README/schema 示例 | 声明过滤、兼容默认与 eventual worker 测试 | DONE |
| MB-007 | DEC-005/006 | Resource 命令携带 generation/fingerprint/fencing，服务端拒绝旧代 | control fence 校验；写操作只允许权威代 | resource API/core/remote | Resource 生命周期说明 | stale、同代漂移和失锁测试 | DONE |
| MB-008 | DEC-006 | expand 不 disable missing，finalize 才执行 destructive resource | apply/finalize 分离 | resource core/bootstrap contributor | Resource 生命周期说明 | expand/finalize 模式测试 | DONE |
| MB-009 | DEC-002 | 租户前置与最终对账迁入 Bootstrap | prerequisites -> resource -> reconciliation | system core contributor | 设计第 10 节 | contributor 顺序与重入测试 | DONE |
| MB-010 | DEC-003/005 | Runtime receipt gate、lease、旧代 drain/finalize 门禁与候选撤回 | stable/candidate 双代窗口；abort 恢复 stable 权威代并刷新 fencing token | Bootstrap starter/core | Bootstrap README | repository lease、finalize 与 abort 门禁测试 | DONE |
| MB-011 | DEC-001/007 | 最终应用模板、BOM 和 starter 使用新入口 | 删除旧隐式初始化契约 | app/admin-starter/BOM | Bootstrap README、能力地图 | 官方应用 Reactor test-compile 通过 | DONE |
| MB-012 | AC-001..015 | 一次性交付验证 | 定向单元、真实 MySQL、入口流程和消费验证 | 正式测试目录与最小证据 | 本台账结果区 | 定向模块套件、真实 MySQL 性能与回滚、静态审计通过；Baohan 仓按用户要求只读 | DONE |
| MB-013 | 用户补充约束 | 已有 Mango 业务保留源码、允许清库接入 | 入口迁移 + 空库 cold Bootstrap，不提供旧库原地兼容 | app starter、迁移检查 | Bootstrap README | 官方应用代码保留并切换统一入口 | DONE |
| MB-014 | 用户性能基线 | 同库手工初始化 SQL 约 1 分钟，避免逐模块历史重放 | 每模块唯一空库 baseline + 模块 history + 后续增量 | persistence starter | persistence README | 5 模块、375 表、37,500 行、16.37 MB，MySQL 8.4 为 3.443s | DONE |
| MB-015 | 用户 Resource 规模补充 | 真实测试 Workflow 发布与文件存储写入，各关键类型规模均至少为保函 5 倍 | 以保函 232 个声明、4 个启动发布流程、15 个启动物化文件为只读基准；运行时生成资产，真实 Registry/Handler/Flowable/LOCAL，分阶段计时 | admin-starter 性能集成测试、基准脚本 | Resource/File/Workflow README | 1,255 声明、75 MiB 文件、20 个八级流程，冷 13.447s、热 84ms | DONE |

## 3. 测试用例候选

| 用例 ID | 来源 | 场景 | 优先级 | 层级 | 自动化 | 数据与清理 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-MB-001 | AC-001/002/011 | 空库 Bootstrap 成功；未 Bootstrap 的 Runtime 失败 | P0 | 入口流程/集成 | AUTO | worktree 专属 MySQL；测试 schema 销毁 | receipt 状态与 Runtime reason code | Bootstrap flow test | `MangoApplicationTest`、cold orchestrator、JDBC receipt gate 通过 | PASS |
| TC-MB-002 | AC-003/004/012 | 同 fingerprint 重入、同代漂移和 fencing 失效 | P0 | 单元/集成 | AUTO | `IT_MB_` 记录并清理 | generation+fingerprint+token | Bootstrap core tests | plan/orchestrator/JDBC tests 通过 | PASS |
| TC-MB-003 | AC-005/006/007/008 | N/N+1 expand、lease drain、finalize、旧代拒绝 | P0 | 入口流程/集成 | AUTO | 独立库与两个 instance ID | control 状态机 | Bootstrap rolling flow test | rolling orchestrator 与 MySQL lease/finalize 测试通过 | PASS |
| TC-MB-004 | AC-009/010/011 | required/eventual/manual 与 INIT_ONLY 组合 | P1 | Resource 集成 | AUTO | `IT_MB_RESOURCE_` 数据并清理 | registry/sync/change log | Resource module tests | Resource core、sync starter、content hash 测试通过 | PASS |
| TC-MB-005 | AC-013 | 多步骤部分失败后同 fingerprint 续跑 | P1 | 组件/集成 | AUTO | 隔离 execution rows | step 幂等键 | Orchestrator tests | 失败回执与成功步骤复用测试通过 | PASS |
| TC-MB-006 | AC-014 | finalize 前停止候选并恢复稳定代 | P1 | 入口流程 | AUTO | 独立库 | stable/candidate/fingerprint | Bootstrap abort flow test | 活跃候选拒绝、清除 candidate、stable 恢复、旧 token 失效的 MySQL 测试通过 | PASS |
| TC-MB-007 | AC-015 | Baohan 类空库消费制品启动 | P0 | 人工/消费验证 | MANUAL | 一次性测试环境 reset-demo | Bootstrap Job 0 + Runtime ready | 业务测试流水线 | 用户明确 Baohan 仓只读参考，本次不修改或发布业务制品 | EXCLUDED |
| TC-MB-008 | 用户业务复测 | 与旧启动模式 649 秒 API readiness、732 秒发布耗时对比 | P0 | 消费/性能验收 | MANUAL | 同等空库与业务制品 | Bootstrap 可长时独立执行；其后 Runtime readiness 秒级 | 业务测试流水线 | 旧模式基线：649s/732s，前端均 200；新制品消费留给业务发布验证 | BASELINE_ONLY |
| TC-MB-009 | 用户 Resource 规模补充 | 声明、Workflow、File 三个维度分别达到 5 倍参考量，执行真实冷注入与热重入 | P0 | MySQL/存储/Flowable 集成 | AUTO | 两个后缀专用库和临时文件目录自动清理 | 1,255 registry、20 个 Flowable deployment、75 组 file 记录与对象大小/SHA-256、无新增日志/部署 | `scripts/tests/bootstrap-performance.sh` | SQL 3.443s；Resource 冷 13.447s、热 84ms | PASS |

## 4. 执行顺序

1. Bootstrap 模块、数据模型、状态机和 Runtime gate。
2. Persistence 显式 migration contributor。
3. Resource execution phase、fencing、apply/finalize。
4. System tenant contributor。
5. App/BOM/README/能力说明。
6. 定向质量、MySQL 空库、升级、rolling 和消费验证。

## 5. 结果与证据

框架交付与自动化门禁已完成：真实 MySQL 8.4 SQL baseline、Workflow/File Resource 注入、热重入、rolling/finalize/abort、测试质量和文档静态审计均通过。Baohan 仓按用户要求保持只读；业务制品升级后的流水线耗时对比属于消费方发布验证，不在本工作区伪造结论。
