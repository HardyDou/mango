# 标准交付记录

任务：Issue #759 FILE_ASSET 外部资产根目录标准交付。

## 1. 元数据

- 任务 ID：Mango Issue #759
- 交付模式：STANDARD
- 需求影响：L2 - 新增业务项目可消费的 FILE_ASSET 位置协议和 starter 配置，改变 Bootstrap 资产来源契约
- 方案风险：L2 - 文件系统路径解析涉及目录穿越和符号链接安全边界，失败会阻断 Resource Bootstrap
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-759`，`fix/issue-759`）
- 启用能力：M01、M08、M09、M10、M11

## 2. 目标与范围

- 目标：允许业务声明使用环境无关的 `asset:<relative-path>`，由 `mango.file.asset-root` 在开发或容器环境映射到外部二进制资产目录。
- 成功条件：外部资产经安全路径解析、可读性与 SHA-256 校验后进入既有 FILE_ASSET 发布链路；现有 `classpath:META-INF/mango/assets/` 声明继续工作。
- 处理范围：Mango File 配置、FILE_ASSET Handler、单元测试、模块 README、业务接入指南和能力地图。
- 不处理范围：开放任意 `file:` URI、修改 Resource Declaration JSON 结构、改变对象存储协议、Maven 发布或业务项目升级。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | Resource Bootstrap | `asset:documents/example.pdf` 且配置有效根目录 | 从根目录读取文件，校验摘要并按既有对象发布链路落盘 | 根目录、文件或摘要无效时明确失败且不上传 | 外部资产成功及失败单测通过 |
| REQ-002 | Resource Bootstrap | 绝对路径、`..`、反斜杠或越根符号链接 | 拒绝不安全路径 | 不得读取根目录之外内容 | 路径安全单测通过 |
| REQ-003 | 现有业务项目 | 使用 `classpath:META-INF/mango/assets/...` | 行为与升级前一致 | classpath 资产不得因新协议失效 | classpath 兼容单测通过 |
| REQ-004 | 开发与容器部署 | 同一声明在不同环境配置不同根目录 | 只切换 `mango.file.asset-root`，不改声明相对路径 | 缺少配置时 Bootstrap 明确失败 | README 提供开发和 Docker 示例 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-004 | 新增 `mango.file.asset-root` 和 `asset:` 协议，不支持任意 `file:` URI | `FileProperties`、`FileAssetResourceHandler` | 删除新配置和协议分支，classpath 路径仍独立可用 |
| DEC-002 | REQ-002 | 根目录与目标均解析真实路径，目标必须是根内普通可读文件；声明必须是无 `..` 的相对路径 | `FileAssetResourceHandler` | 回滚会恢复仅 classpath 的较窄攻击面 |
| DEC-003 | REQ-003 | 保留原 classpath 前缀和读取方式，外部资产复用现有摘要、暂存、发布和幂等逻辑 | Handler 与回归测试 | classpath 无需迁移 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-001 | DEC-001、DEC-002 | 1 | File 配置与 Handler | 安全解析并加载 `asset:` 外部资产 |
| IMP-002 | DEC-003 | 2 | Handler 单元测试 | 成功、失败、安全和兼容分支均有有效断言 |
| IMP-003 | DEC-001、DEC-003 | 3 | File README、业务指南、能力地图 | 配置、迁移和开发/容器示例可发现 |
| IMP-004 | 全部 | 4 | 定向 Maven 与 PMO 检查 | M09、M10 和测试质量门禁通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-002、REQ-003 | M10 单元测试 | `mvn -pl mango-platform/mango-file/mango-file-core -Dtest='*Test,!*IntegrationTest' test` | PASS（68 tests） | Maven Surefire 输出；`FileAssetResourceHandlerTest` 14 tests |
| REQ-001、REQ-002、REQ-003 | M11 真实模块集成 | `mvn -pl mango-platform/mango-file/mango-file-core -Dtest=FileAssetResourceHandlerIntegrationTest test` | PASS（2 tests） | Spring Boot + H2 + MyBatis Mapper + LocalFileStorage 输出 |
| REQ-001、REQ-002、REQ-003 | M09 定向模块验证 | `mvn -pl mango-platform/mango-file/mango-file-core -Dtest='FilePropertiesTest,FileAssetResourceHandlerTest,FileAssetResourceHandlerIntegrationTest' verify` | PASS（17 tests） | Maven verify 输出 |
| REQ-001、REQ-002、REQ-003 | M09/M10/M11 模块回归 | `mvn -pl mango-platform/mango-file/mango-file-core -Dtest='*Test,!FileServiceConcurrentSaveIntegrationTest' verify` | PASS（76 tests） | Maven verify 输出；仅排除需外部 MySQL 的既有并发测试 |
| REQ-001、REQ-002、REQ-003 | M09 测试质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS（3 changed test files） | Checker 输出 |
| REQ-004 | M08 能力说明 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、标准记录检查 | PASS | README/能力地图审计与标准记录 checker 输出 |

## 7. 例外与剩余风险

- 全模块默认 `mvn -pl mango-platform/mango-file/mango-file-core test` 还会执行既有 `FileServiceConcurrentSaveIntegrationTest`，因本 workspace 数据库尚未创建且未通过 `mango dev start` 注入运行环境而阻塞（`Access denied for user '${MANGO_DB_USERNAME}'`）；本任务新增的 H2/MyBatis/本地存储真实集成测试已通过，不将该既有外部 MySQL 环境阻塞归因于本次改动。
- 本机没有 `docker` 命令，未构建并启动业务消费项目 Docker 镜像，因此不声明真实容器发布已验收；README 中的 Docker 配置属于接入说明，运行时能力由 Spring 配置绑定测试和真实本地文件/数据库集成测试覆盖。
- `checkstyle:check` 通过；仓库当前 PMD 6.42 对 Java 21 模块产生全量 processing errors 且命令仍返回成功，因此不把该 PMD 输出计为有效质量证据。
- 本任务不发布 Maven 新版本；业务项目需在后续发布包含该变更的 Mango Maven 版本后升级使用。
