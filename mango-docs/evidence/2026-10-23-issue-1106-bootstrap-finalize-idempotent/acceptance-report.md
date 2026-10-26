# Issue #1106 真实进程级验证报告

## 结论

在真实 `mango-monolith-app` 进程中按 README 的命令路径执行 bootstrap，同一 generation 重复 `finalize` 从修复前的失败变为幂等成功。

- 修复前（未打补丁的 `BootstrapOrchestrator`，真实 app 装配）：`BOOTSTRAP_CANDIDATE_MISSING: generation=2`，`mvn` 测试失败。
- 修复后：重复 `finalize` 返回 `FINALIZED`、`executionId=null`、`executedSteps=0`，进程退出码 0，控制表与审计表不变。

## 环境

| 项 | 值 |
|---|---|
| 应用 | `mango/mango-app/monolith/mango-monolith-app`，`mango-monolith-app-1.0.0-SNAPSHOT.jar` |
| Java / Maven | 21.0.12 / 3.9.13 |
| 数据库 | 隔离库 `mango_it_app_1106`（MySQL 8.4.8，`127.0.0.1:3306`） |
| environment key | `it-1106` |
| 时区 | JVM `CST+0800`，MySQL `SYSTEM`（+0800），JDBC `serverTimezone=Asia/Shanghai` |
| 实例 | worktree `/Users/hardy/Work/mango-issue-1106`，分支 `fix/issue-1106-finalize-idempotent` |

## 执行矩阵

| 用例 | 命令动作 | release / revision / generation | 期望 | 实际 |
|---|---|---|---|---|
| 1 冷启 | `bootstrap apply --strategy=cold` | `it-release` / `it-revision` / 1 | `FINALIZED` | `FINALIZED`，executedSteps=6，executionId 非空 |
| 2 重复 finalize #1 | `bootstrap finalize` | `it-release` / `it-revision` / 1 | 幂等 `FINALIZED` | `FINALIZED`，executedSteps=0，executionId=null，退出码 0 |
| 3 重复 finalize #2 | `bootstrap finalize` | 同上 | 幂等 `FINALIZED` | 同用例 2 |
| 4 内容不一致 | `bootstrap finalize` | `it-release-CHANGED` / `it-revision` / 1 | 显式拒绝 | `BOOTSTRAP_GENERATION_ALREADY_RELEASED: generation=1`，退出码 1，控制表不变 |
| 5 正常滚动 | `bootstrap apply --strategy=rolling` | `it-release` / `it-revision-g2` / 2 | `EXPANDED` | `EXPANDED`，executedSteps=4 |
| 6 正常 finalize | `bootstrap finalize` | 同上 | `FINALIZED` | `FINALIZED`，executedSteps=2 |
| 7 升级后重复 finalize | `bootstrap finalize` | 同上 | 幂等 `FINALIZED` | `FINALIZED`，executedSteps=0，executionId=null |
| 8 runtime 门禁 | `bootstrap verify` | 同上 | `VERIFIED` | `VERIFIED` |
| 9 abort 已 finalize 的 generation | `bootstrap abort` | 同上 | 既有缺口 | `BOOTSTRAP_CANDIDATE_MISSING: generation=2`，未修改 |

用例 5、6 证明正常升级路径行为未变；用例 7 证明幂等分支在 stable generation 推进后仍成立；用例 9 证明 ABORT 缺口与本次改动无关，保持原状。

## 不变性证据

用例 2 与 3 前后控制表逐字段相同：

```text
stable_generation=1, stable_fingerprint=29fa8d83…350dd, candidate_generation=NULL,
authoritative_generation=1, state=FINALIZED, fencing_token=1,
release_id=it-release, build_revision=it-revision
```

- 证据：`bootstrap-control-before-repeat-finalize.txt`、`bootstrap-control-after-repeat-finalize.txt`
- 审计表未增长：`mango_bootstrap_execution` 保持 1 行（仅冷启 apply），`mango_bootstrap_step_execution` 保持 6 行。
- 完成矩阵 9 个用例后总计 `executions=3`（cold apply + rolling apply + finalize）、`steps=12`（6+4+2），重复 finalize 未新增任何行。
- 回执正常写出，`assertStableReleaseIdentity` 通过：`stable-receipt-after-repeat-finalize.json`

## 指纹稳定性

同一 generation 的两次 `finalize` 指纹一致：`29fa8d8350c3149fc5c60edf0f148121fa1171a4c47d2a27226a5253c78350dd`（generation 1）、`a970f9623148cdeaae85b1f9ea00da6381da120697b022203c5f794832d1ef9b`（generation 2）。两次为独立 JVM 进程，证明真实 contributor（Persistence Flyway + Resource + 租户步骤）生成的指纹在进程间稳定，幂等分支在生产路径可命中。

## 反向对照

| 层级 | 未打补丁时的结果 |
|---|---|
| 单元测试 | 新增 2 条用例失败，报 `BOOTSTRAP_CANDIDATE_MISSING: generation=2` |
| 真 MySQL 集成测试 | 新增 2 条用例失败，报 `BOOTSTRAP_CANDIDATE_MISSING: generation=1` |
| 真实 app 装配（`BootstrapResourcePerformanceIntegrationTest`，1291 条 Resource 声明 + 真实 File/Workflow handler） | 失败，报 `BOOTSTRAP_CANDIDATE_MISSING: generation=2` |

## 复现步骤

```bash
mysql -h127.0.0.1 -uroot -e "CREATE DATABASE mango_it_app_1106 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
export MANGO_DB_URL="jdbc:mysql://127.0.0.1:3306/mango_it_app_1106?...&serverTimezone=Asia/Shanghai"
export MANGO_DB_USERNAME=root MANGO_DB_PASSWORD= MANGO_CRYPTO_SM4_SECRET_KEY=<workspace key>
java -jar mango-monolith-app-1.0.0-SNAPSHOT.jar bootstrap apply --mango.bootstrap.strategy=cold \
  --mango.bootstrap.environment-key=it-1106 --mango.release.id=it-release \
  --mango.release.revision=it-revision --mango.release.generation=1 --spring.main.web-application-type=none
java -jar mango-monolith-app-1.0.0-SNAPSHOT.jar bootstrap finalize \
  --mango.bootstrap.environment-key=it-1106 --mango.release.id=it-release \
  --mango.release.revision=it-revision --mango.release.generation=1 --spring.main.web-application-type=none
```

## 边界

- 验证使用隔离库 `mango_it_app_1106`，结束后未删除，供复核；不属于共享业务库。
- 真实进程验证使用 `--spring.main.web-application-type=none` 关闭 Web 端口，不占用工作区端口。
- 未验证受管环境（5.114 / 5.118）的 `mango-deploy-shell` 引擎侧行为；该组件不在本仓库。
