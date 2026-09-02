# 标准交付记录

任务：Issue #938 Identity Resource 生命周期审计确定性修复

## 1. 元数据

- 任务 ID：GitHub Issue #938
- 交付模式：STANDARD
- 需求影响：L2 - 已发布 Identity 默认 Resource 在两个独立空库写入不同的生命周期事件，阻断 portable BSQL 构建。
- 方案风险：L2 - 修改初始化持久化结果和 Resource 字段契约，但限定在 Identity starter 的 Resource 创建路径，不改变正常 API 生命周期语义。
- 最终风险：L2
- 工作区决策：CREATE - `issue-938-identity-resource-determinism`

## 2. 目标与范围

- 目标：同一 `IDENTITY_USER` 声明在独立空库生成完全一致的用户、成员和成员创建事件数据。
- 成功条件：事件主键和初始化时间稳定；两次重建空库后的三张表完整快照一致；正常 API 创建、移出、恢复路径不变。
- 处理范围：Identity Resource handler、默认 bootstrap 声明、真实 Mapper 集成回归和 Identity 能力说明。
- 不处理范围：不修改生命周期表结构、正常业务 API、雪花 ID 全局策略、baseline 比较器或 `MANGO-BASELINE-040`。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | `IDENTITY_USER` Resource handler | 声明包含固定 `initializedAt`、`tenantId` 和 `memberId` | 创建事件 ID 由稳定业务身份派生，时间使用声明值 | 缺少固定时间时拒绝声明；稳定 ID 被其它事件占用时明确失败 | 两个独立空库的三张目标表完整快照一致 |
| R-02 | 正常成员生命周期 API | 通过现有业务 service 创建、移出或恢复成员 | 继续使用动态事件 ID 和真实发生时间 | Resource 修复不得进入正常业务路径 | 本次 diff 不修改 core service/provider 生命周期实现 |
| R-03 | BSQL 确定性门禁 | 同一 revision 重放 Flyway 和 portable Resource | 保留双空库完整数据比较 | 任一未声明的动态数据继续触发 `MANGO-BASELINE-040` | 不修改 baseline 生成器和比较规则 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01 | 使用 `PortableResourceIds.stable(table, tenantId, memberId, CREATED)` 生成正数 BIGINT，并在写入前检查主键碰撞 | Identity Resource handler | 恢复 MyBatis-Plus 自动 ID |
| D-02 | R-01 | `initializedAt` 为必填 DATETIME，同一时间覆盖用户初始化、成员加入和 CREATED 事件 | handler、默认声明、README | 删除字段并恢复运行时时钟 |
| D-03 | R-02、R-03 | 不修改 core 业务生命周期服务和 baseline 比较器 | 改动范围约束 | 直接撤销 starter 局部修复 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-01 | D-01、D-02 | 1 | `IdentityUserResourceHandler`、`ResourceFieldReader` | 稳定 ID、固定时间和碰撞检查生效 |
| I-02 | D-02 | 2 | `identity-common-bootstrap.yml`、Identity README | 默认声明和消费者说明包含 `initializedAt` |
| I-03 | D-01 至 D-03 | 3 | `IdentityUserResourceHandlerIntegrationTest` | 真实 Mapper 双空库完整快照回归通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-01 至 R-03 | M11 定向集成测试 | `MAVEN_OPTS='-Dmaven.repo.local=.mango/m2/repository' mvn -B -ntp -f mango/pom.xml -pl :mango-identity-starter -Dtest=IdentityUserResourceHandlerIntegrationTest test` | PASS | 11 tests；覆盖双空库三表完整快照、固定 ID/时间、缺字段和碰撞零写入拒绝 |
| R-01 至 R-03 | M09 模块质量检查 | `MAVEN_OPTS='-Dmaven.repo.local=.mango/m2/repository' mvn -B -ntp -f mango/pom.xml -pl :mango-identity-starter verify` | PASS | 19 tests，直接修改 Maven 模块 verify 成功 |
| R-01 | M09 测试质量检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | test quality 1 file；mock audit block=0、warn=0 |
| R-01 至 R-03 | M08 能力说明审计 | `node mango-pmo/tools/check-standard-delivery-record.mjs <record>`、`node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS | STANDARD 记录、模块 README 和源码事实审计通过 |

## 7. 例外与剩余风险

- Maven `1.0.50` 不可变版本发布已由用户授权，按独立 `mango-release` 流程执行；Baohan 真实 MySQL BSQL/Jenkins 回归仍属于业务消费验收边界。
