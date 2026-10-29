# Issue #1133 真实 MySQL 验收证据

## 1. 验收范围

验证已有数据库在 Resource 声明变化后执行 `rolling APPLY -> rolling FINALIZE`，不重建数据库且保留已有文件与工作流数据。

## 2. 执行环境

- 数据库：本机 MySQL 8.4，`127.0.0.1:3306`
- 验收库：`mango_dev_issue1133_1793240405_bootstrap_resource_perf`
- 测试：`BootstrapResourcePerformanceIntegrationTest`
- 证据日志：`.runtime/issue-1133-real-mysql/`

## 3. 功能验收记录

| 台账 ID | 用例 ID | 功能点 | 关键断言 | 结果 |
|---|---|---|---|---|
| ISSUE-1133 | TC-MYSQL-ROLLING-001 | Resource 声明变化 | workflow 模块 generation 1 -> 3；`APPLY(ROLLING)` 返回 `EXPANDED`，`FINALIZE(ROLLING)` 返回 `FINALIZED` | 通过 |
| ISSUE-1133 | TC-MYSQL-DATA-001 | 已有数据保留 | rolling 变更前后 `fileState` 相等；文件记录 15 条、工作流定义 4 个、资源登记 1291 条 | 通过 |
| ISSUE-1133 | TC-MYSQL-CONTROL-001 | 最终控制面状态 | `mango_bootstrap_control.state=FINALIZED`，`candidate_generation=NULL` | 通过 |

执行命令：

```bash
MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL="jdbc:mysql://127.0.0.1:3306/<dedicated-db>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
MANGO_BOOTSTRAP_RESOURCE_PERF_DB_USERNAME=root \
MANGO_BOOTSTRAP_RESOURCE_PERF_DB_PASSWORD='' \
mvn -f mango/mango-admin-starter/pom.xml \
  -Dtest=BootstrapResourcePerformanceIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：`Tests run: 1, Failures: 0, Errors: 0`，`BUILD SUCCESS`。测试随后还执行了文件变更恢复和并发场景，因此最终数据库中的 workflow receipt 为 generation 5；rolling Resource 声明变化场景本身为 generation 3，断言已在测试中完成。

## 4. 未验证项和风险

- 本次验证覆盖真实 MySQL 和真实 Resource/Workflow/File handler；未覆盖真实 IDEA 启动进程的端到端人工启动。
- 旧 Runtime lease 阻断 finalize 的路径由定向单元测试覆盖，未在本次性能库中额外制造存活 Runtime。

## 5. 证据索引

- Maven Surefire：`.runtime/issue-1133-real-mysql/BootstrapResourcePerformanceIntegrationTest.txt`
- JUnit XML：`.runtime/issue-1133-real-mysql/TEST-BootstrapResourcePerformanceIntegrationTest.xml`
- 最终数据库查询：`.runtime/issue-1133-real-mysql/final-database-state.txt`
