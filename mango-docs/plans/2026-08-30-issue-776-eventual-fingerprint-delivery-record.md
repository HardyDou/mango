# Issue #776 Resource Eventual Fingerprint 标准交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #776
- 交付模式：STANDARD
- 需求影响：L2 - 共享 Resource starter 在所有业务应用中周期性执行远程全量注册，运行期开销和日志可观察行为发生变化。
- 方案风险：L2 - 成功状态判断错误可能漏报声明变化或阻断失败后的收敛重试。
- 最终风险：L2
- 工作区决策：CREATE - `fix/issue-776-eventual-fingerprint`

## 2. 目标与范围

- 目标：未变化的 `RUNTIME_EVENTUAL` 声明在首次成功后不再每 30 秒重复远程注册。
- 成功条件：首次提交；未变化跳过；声明、受管模块或 Bootstrap authority 变化时重新提交；失败不缓存并在下一轮重试。
- 处理范围：`mango-resource-sync-starter` worker、Spring 装配、定向测试、Resource 能力说明。
- 不处理范围：不改变 `RUNTIME_EVENTUAL` 分类、调度周期、注册中心 API、持久化模型、Bootstrap required 流程或发布物料。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | Runtime eventual worker | 当前 authority 可写且存在 eventual 声明 | 本进程首次轮次提交完整声明 | 远程拒绝时记录 WARN | 远程入口收到一次注册 |
| R-02 | Runtime eventual worker | 与最近成功轮次的 authority、来源、模块和声明均相同 | 跳过远程注册，仅记录 DEBUG | 不制造成功 INFO 或数据库扫描 | 连续两轮只提交一次 |
| R-03 | Runtime eventual worker | canonical 声明、模块范围或 authority 变化 | 重新提交完整声明 | 变化不能被旧 fingerprint 隐藏 | 变化后调用次数增加 |
| R-04 | Runtime eventual worker | 上一轮远程返回失败 | 不推进成功 fingerprint，下一轮继续提交 | WARN 保留失败上下文 | 失败后成功收敛，随后未变化轮次跳过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01, R-02, R-03 | 复用 `ResourceDeclarationCanonicalizer`，用 SHA-256 汇总 authority、来源、模块和按 ID 排序的 canonical bytes；不改变远程 command | worker 与自动配置 | 回退 worker 和构造器注入 |
| D-02 | R-04 | fingerprint 仅在 `R<Boolean>` 明确成功后写入进程内 volatile 状态；不持久化 | worker | 删除成功状态和跳过分支 |
| D-03 | R-01 至 R-04 | 保留固定延迟调度和 `RUNTIME_EVENTUAL` 语义；无变化日志为 DEBUG | worker 与 README | 恢复每轮提交行为 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-01 | D-01, D-02 | 1 | `ResourceEventualReconciliationWorker.java`、`ResourceSyncAutoConfiguration.java` | 成功 fingerprint 可跳过且变化/失败可重试 |
| I-02 | D-01, D-02, D-03 | 2 | `ResourceEventualReconciliationWorkerTest.java` | 覆盖首次、未变化、声明变化、authority 变化、失败重试 |
| I-03 | D-03 | 3 | Resource README、能力地图 | 消费者可判断运行期行为和失败语义 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-01 至 R-04 | M10 定向单元测试 | `mvn -B -ntp -f mango/pom.xml -pl :mango-resource-sync-starter -Dtest=ResourceEventualReconciliationWorkerTest test` | PASS，7/7 | 覆盖首次提交、未变化跳过、声明变化、authority 变化、失败重试后跳过 |
| R-01 至 R-04 | M09 模块编译与质量检查 | `mvn -B -ntp -f mango/pom.xml -pl :mango-resource-sync-starter verify` | PASS，31/31 | 直接修改模块完整 verify |
| R-01 至 R-04 | M09 测试质量检查 | `test-quality-check.mjs` 与 `audit-backend-test-mocks.mjs --report-only --changed-only` | PASS | block=0，warn=0 |
| R-01 至 R-04 | M08 能力说明审计 | `audit-module-readmes.mjs` 与 `audit-readme-source-facts.mjs` | PASS | Resource README 与能力地图事实一致 |
| R-02 | M11 真实调度协作验证 | 项目内 CLI 启动源码后端，观察 `18003` 与 `mango_dev_mango_issue_776_003` | PASS | health=UP；20:13:17 首次上报 800 条；20:13:47、20:14:18、20:14:48、20:15:18 均为 DEBUG skip；跨周期 `resource_sync_log=1693`、`resource_change_log=1693` 均未增长 |

## 7. 例外与剩余风险

- fingerprint 按设计只保存在当前进程；应用重启后的首次 eventual 轮次会重新提交一次，以重新建立当前进程和 authority 的成功事实。
- 全局旧 CLI 未识别 `processMode`，真实验证改用当前仓库 Node 22.23.1 下的 `mango-ui/packages/mango-cli/src/index.mjs` 项目内入口；未修改 CLI，源码入口完成标准 cold bootstrap、runtime 和 stop 生命周期。
