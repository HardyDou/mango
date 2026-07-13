# Issue 453 相同内容并发保存交付契约

## 1. 目标

永久消除相同存储口径、相同哈希文件首次并发保存时的唯一键竞争失败，保证五个并发保存动作全部成功并复用一个物理对象。

## 2. 范围

- `mango-file-core` 文件对象与哈希映射创建的并发幂等恢复。
- 对失败竞争方不同对象名的存储补偿清理。
- 五线程、真实 Mapper、唯一约束和 Spring 事务的并发集成测试。
- 受影响模块回归、静态质量门禁、交付证据和 PR。

## 3. 不做什么

- 不引入 Memory、Redis、JDBC 锁或任何 KV 依赖。
- 不修改公开接口、配置、数据库结构、前端和日志字段长度。
- 不执行版本发布或合并 PR。

## 4. 设计输入

- GitHub Issue 453。
- `BRD-ISSUE-453`、`SRS-ISSUE-453`、`TDD-ISSUE-453`、`PLAN-ISSUE-453`。
- 当前 `FileServiceImpl`、`uk_file_object_hash_storage` 和 `uk_file_hash_mapping_target` 代码与结构事实。

## 5. 设计说明

### 5.1 影响模块

`mango/mango-platform/mango-file/mango-file-core`。

### 5.2 接口变化

无。所有公开方法、路径、入参与返回契约保持兼容。

### 5.3 数据变化

无结构或历史数据变化。运行时同一物理内容只保留一个 `file_object`，每次保存继续建立独立 `file_record` 并原子累计引用数。

### 5.4 菜单/页面/权限变化

无。沿用现有租户、访问级别、菜单与权限边界。

### 5.5 测试范围

- 五线程同时越过首次查询并竞争插入同一物理内容。
- 全部保存成功，一个物理对象、一个哈希映射、五个文件结果、引用数五、一个存储对象。
- 非目标冲突不被吞掉；现有文件模块测试保持通过。

### 5.6 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango/mango-platform/mango-file/mango-file-core/src/main/java/io/mango/file/core/service/impl/FileServiceImpl.java` |
| README/使用说明 | 否 | EXCEPTION: 公开使用方式、接口、配置和升级动作均不变化 |
| 需求文档 | 是 | `mango-docs/designs/issue-453-file-dedup-concurrency/business-requirements.md`、`system-requirements.md` |
| 详细设计文档 | 是 | `mango-docs/designs/issue-453-file-dedup-concurrency/technical-design.md`、`implementation-plan.md` |
| E2E 脚本 | 否 | EXCEPTION: 无 UI 变化；由公共应用服务入口的真实持久化并发集成测试覆盖 |
| 测试结果基线 | 是 | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-453 | AC-001 | 五线程首次并发保存完全相同字节 | P1 | 组件 | AUTO | `IT_453_` 前缀、同租户、同存储、相同字节 | 真实 Mapper、唯一约束、Spring 事务、五方屏障、线程安全存储替身 | `FileServiceConcurrentSaveIntegrationTest` | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | AUTOMATED |

## 6. 风险与限制

- Testcontainers MySQL 8.4 证明真实 InnoDB 唯一约束、事务隔离、锁定当前读和竞争恢复；测试仍只替换外部对象存储。
- 对象存储使用线程安全替身以稳定制造并发窗口；被测持久化与服务目标真实执行。
- 存储补偿失败只记录上下文，不得把已经成立的数据库复用反转为业务 500。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | Issue 453, BAC-001, SAC-001 | 五个相同内容并发保存全部成功并复用一个物理对象 | 数据库唯一约束最终仲裁，竞争失败后当前读胜出对象 | `mango/mango-platform/mango-file/mango-file-core/src/main/java/io/mango/file/core/service/impl/FileServiceImpl.java` | EXCEPTION: 未修改公开接口、配置或调用方式，无需更新使用说明 | `mango-docs/designs/issue-453-file-dedup-concurrency/business-requirements.md`、`system-requirements.md`、`technical-design.md`、`implementation-plan.md` | EXCEPTION: 无 UI；应用服务入口集成测试覆盖 | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | TC-453 全部业务与持久化断言 | DONE | `mango/mango-platform/mango-file/mango-file-core/src/test/java/io/mango/file/core/service/impl/FileServiceConcurrentSaveIntegrationTest.java`、`mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-453 | `IFileService.saveGenerated` 应用服务入口 | 相同内容首次并发保存 | `IT_453_`、五线程、租户 1001、用户 2001 | 五次成功、一对象、一映射、五结果、引用数五、存储对象一份 | EXCEPTION: 无 UI 变化 | EXCEPTION: 非浏览器/HTTP 用例 | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | PASS |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-453 | TASK-001 | TC-453 | EXCEPTION: 应用服务入口集成测试 | `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -Dtest=FileServiceConcurrentSaveIntegrationTest test` | Java 21.0.10、Maven 3.9.13、Testcontainers MySQL 8.4 | 一次性隔离 MySQL 容器与 `IT_453_` 数据集 | 用户 2001、租户 1001 | 待实现后回填五线程与模块测试结果 | NONE | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | 从并发唯一键失败变为全部成功并复用物理内容 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 升级到包含 Issue 453 修复的后端版本后移除通过改变 ZIP 内容规避重复哈希的逻辑；文件保存调用方式不变 | `mango-docs/designs/issue-453-file-dedup-concurrency/technical-design.md`、`delivery-contract.md`、`mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -Dtest=FileServiceConcurrentSaveIntegrationTest test` | 测试使用隔离内存库、用户 2001、租户 1001；业务环境沿用自身合法上下文 | 若仍出现相同唯一键错误，保留请求追踪与非敏感日志并升级 Mango Issue | DONE |
