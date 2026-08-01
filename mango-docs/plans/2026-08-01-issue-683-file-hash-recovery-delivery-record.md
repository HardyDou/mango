# 标准交付记录

> Issue #683 Mango File LOCAL hash dedup reuses missing physical object

## 1. 元数据

- 任务 ID：GitHub Issue #683
- 交付模式：STANDARD
- 需求影响：L2 - 共享文件服务可为已丢失的物理对象创建新的完成文件记录，直到下载才失败，阻断依赖内部生成文件的业务流程。
- 方案风险：L2 - 修复修改秒传复用和失效对象恢复路径，覆盖所有已实现存储后端；不改变公开 API、数据库 schema、权限或租户隔离。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-683-file-hash-recovery` 上的 `fix/issue-683-file-hash-recovery`

## 2. 目标与范围

- 目标：秒传只能复用可读取且长度匹配的物理对象；发现哈希映射指向失效对象时，使用当前上传内容恢复该对象和映射。
- 成功条件：`save`、`saveGenerated` 和上传会话初始化不会将失效对象作为秒传结果返回；恢复后新旧 `file_record` 均能引用同一可下载的对象行。
- 处理范围：`mango-file-core` 的哈希复用、失效对象恢复和核心回归测试。
- 不处理范围：不增加存储协议、公开 API、配置项、Flyway migration 或自动批量修复历史对象；不发布 Maven 制品。Issue 由修复 PR 合并后自动关闭。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-683-01 | `IFileContentProvider.save` / `saveGenerated` | 活跃哈希映射引用的对象不存在、不可读或长度不匹配 | 不复用失效对象；成功上传当前内容后恢复对象和映射 | 上传失败仍返回既有 `FILE_STORE_FAILED`，不创建完成文件记录 | 返回的文件记录下载成功，映射引用恢复后的对象 |
| SR-683-02 | `createUploadSession` | 客户端哈希命中失效映射 | 不返回 `instant=true`，改走正常上传会话 | 现有会话校验和存储失败语义不变 | 返回非秒传会话，后续完成上传可恢复对象 |
| SR-683-03 | 所有 `FileStorage` 实现 | 任一受支持存储后端返回对象 | 通过现有统一读取抽象探测对象，立即关闭探测流 | 无法探测的对象不得作为秒传结果 | 有效对象继续秒传；失效对象不会返回坏元数据 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-683-01 | SR-683-01, SR-683-03 | 使用现有 `FileStorageRouter.getObject` 探测且立即关闭流，同时校验对象长度；不新增存储 SPI 或各云厂商 Head API | `FileService` | 删除探测即可恢复原有数据库秒传行为 |
| TD-683-02 | SR-683-01, SR-683-02 | 将失效映射置为 inactive、关联对象置为非完成；上传成功后复用同一唯一对象行并替换其定位和状态，从而保留历史 `file_record` 引用 | `FileService`、`file_object`、`file_hash_mapping` 现有数据 | 回退服务逻辑；无需数据回滚或 schema migration |
| TD-683-03 | SR-683-01 | `saveGenerated` 的非秒传后备复用同样探测，防止绕开映射后再次取得坏对象 | `FileService` | 删除统一可读性校验 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-683-01 | TD-683-01, TD-683-02, TD-683-03 | 1 | `mango-file-core/.../FileService.java` | 有效秒传对象经探测后复用；失效对象可由当前上传恢复 |
| IMP-683-02 | TD-683-01, TD-683-02 | 2 | `mango-file-core/.../FileService*Test.java` | 覆盖有效复用、对象缺失恢复和上传会话拒绝坏秒传 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-683-01, SR-683-02, SR-683-03 | M10/M11 核心文件服务回归 | `mvn -B -ntp -f mango/pom.xml -pl :mango-file-core -Dtest=FileServiceUploadSessionTest,FileServiceConcurrentSaveIntegrationTest test`（worktree 专用 MySQL） | PASS：7 项，失败 0、错误 0、跳过 0；`IFileContentProvider.save` 的活跃/失活映射恢复、有效并发去重和上传会话坏映射降级均通过 | `mango-file-core/target/surefire-reports/` |
| SR-683-01, SR-683-02, SR-683-03 | M09 直接模块质量门禁 | `MANGO_DB_NAME=<workspace>_concurrency mvn -B -ntp -f mango/pom.xml -pl :mango-file-core verify` | PASS：67 项，失败 0、错误 0、跳过 0 | Maven 输出与 Surefire 报告 |
| 全部 | 测试资产质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main`；`git diff --check`；`mvn ... mango:check -Drule=naming` | PASS：测试质量 2 文件；mock block/warn 0；diff clean；Mango naming 0 issue | 命令输出 |

## 7. 例外与剩余风险

- 只在同内容再次保存或再次发起上传时修复失效对象；既有但从未重新写入的坏记录不会自动扫描或补写。
- 探测使用现有读取接口，云存储短暂不可读时也会拒绝秒传并尝试当前上传；若上传失败，事务会回滚数据库失效标记。
- PMD 6.42 在 Java 21 下对当前模块全部 56 个源文件报告解析错误，未提供可用规则结果；Checkstyle 0 violations、Mango naming 0 issues，属于工具兼容性遗留风险。
