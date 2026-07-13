# Issue 453 相同内容并发保存测试结果基线

## 1. 基线信息

- 执行日期：2026-07-13
- 分支：`fix/issue-453-file-dedup-concurrency`
- 测试入口：`FileServiceConcurrentSaveIntegrationTest`
- 环境：OpenJDK 21.0.10、Apache Maven 3.9.13、worktree 专属本地 MySQL 8.4.8
- 数据边界：`mango workspace init` 分配的 `mango_dev_mango_issue_453_178`，租户 1001、用户 2001、`IT_453_` 文件前缀
- 持久化边界：真实 MyBatis Mapper、Spring 事务和数据库唯一约束；仅外部对象存储使用线程安全测试实现

## 2. 并发验收

执行命令：

```bash
set -a
source .mango/dev-workspace.env
set +a
mvn -f mango/pom.xml \
  -pl mango-platform/mango-file/mango-file-core \
  -Dtest=FileServiceConcurrentSaveIntegrationTest test
```

为检查稳定性，上述命令连续执行 5 轮，每轮均由五个线程同时越过首次查询并竞争保存完全相同的字节。

结果：5/5 轮通过。每轮均满足：

- 五次保存全部返回成功，且产生五个不同的文件记录 ID。
- `file_object` 仅一行，`file_hash_mapping` 仅一行，`file_record` 为五行。
- 五个文件记录指向同一对象，`ref_count` 等于 5。
- 哈希映射指向胜出的对象，测试存储最终仅保留一个物理对象。

## 3. 受影响模块回归

执行命令：

```bash
set -a
source .mango/dev-workspace.env
set +a
mvn -f mango/pom.xml \
  -pl mango-platform/mango-file/mango-file-core test
```

结果：通过。共执行 42 个测试，失败 0、错误 0、跳过 0。

## 4. 编译与静态质量

以下检查通过：

- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -am -DskipTests install`，含 `mango-file-api` 及目标模块依赖编译。
- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core checkstyle:check`
- 测试质量检查：1 个变更测试文件通过。
- 后端测试替身审计：阻断 0、警告 0。
- 文档集合、实施计划、生命周期 handoff 和交付台账检查通过。
- 仓库内不存在旧 `io.mango.file.api.FileCode` 引用；业务码常量、数值和消息原样迁移到 `io.mango.file.api.enums.FileCode`。
- 全 Reactor Mango architecture 与通用静态门禁通过：架构阻断 0，`newIssueCount=0`，工具失败 0，212 个模块构建成功。
- 顺带消除 `FileServiceImpl` 被重新识别的 Checkstyle 告警；Spring 容器管理依赖的 SpotBugs `EI_EXPOSE_REP2` 使用带理由的精准注解抑制，真实装箱告警已通过代码修复。

全 Reactor 含测试的 `verify` 会在上游 `mango-infra-persistence-starter` 的既有 Flyway 测试上下文提前中止，尚未运行到本次变更模块，因此不作为本次目标模块验收替代项；本次使用全 Reactor 跳过测试的完整架构与静态门禁，并单独执行目标模块全部 42 个测试。

## 5. 验收结论

`TC-453` 通过。数据库唯一约束作为并发最终仲裁，竞争失败线程通过当前读复用胜出对象；方案不依赖 Memory、Redis 或 JDBC KV 实现，因此三种运行模式无需新增锁、配置或分支逻辑。`FileCode` 仅迁移包路径，错误码数值、消息及原有业务特性保持不变。
