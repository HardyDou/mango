# Issue 453 相同内容并发保存测试结果基线

## 1. 基线信息

- 执行日期：2026-07-13
- 分支：`fix/issue-453-file-dedup-concurrency`
- 测试入口：`FileServiceConcurrentSaveIntegrationTest`
- 环境：OpenJDK 21.0.10、Apache Maven 3.9.13、H2 MySQL 模式
- 数据边界：隔离内存数据库，租户 1001、用户 2001、`IT_453_` 文件前缀
- 持久化边界：真实 MyBatis Mapper、Spring 事务和数据库唯一约束；仅外部对象存储使用线程安全测试实现

## 2. 并发验收

执行命令：

```bash
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
mvn -f mango/pom.xml \
  -pl mango-platform/mango-file/mango-file-core test
```

结果：通过。共执行 42 个测试，失败 0、错误 0、跳过 0。

## 4. 编译与静态质量

以下检查通过：

- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -am -DskipTests verify`
- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core checkstyle:check`
- 测试质量检查：1 个变更测试文件通过。
- 后端测试替身审计：阻断 0、警告 0。

全 Reactor 含测试的 `verify` 在上游 `mango-infra-persistence-starter` 提前中止，原因为主分支已有的 `db/migration/link/V20260711001__link_high_version_seed.sql` 未在该测试上下文声明；尚未运行到本次变更模块。全量 Mango 专项检查也被同一高版本号整数解析问题和历史 Path 参数问题阻断。两项均不在 Issue 453 范围内，本次受影响模块测试、并发验收和变更文件质量门禁均已通过。

## 5. 验收结论

`TC-453` 通过。数据库唯一约束作为并发最终仲裁，竞争失败线程通过当前读复用胜出对象；方案不依赖 Memory、Redis 或 JDBC KV 实现，因此三种运行模式无需新增锁、配置或分支逻辑。
