# 标准交付记录

任务：Issue #563 文件预览源地址修复。

## 1. 元数据

- 任务 ID：MANGO-ISSUE-563
- 交付模式：STANDARD
- 需求影响：L2 - 影响共享文件预览能力的 Office 文件识别、源文件下载链路和转换缓存命名。
- 方案风险：L2 - 同时调整预览源 URL、内部回环地址和 vendored 引擎的编码判定，但不改变公开 API、数据或权限契约。
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/mango-issue-563`，`fix/issue-563-mixed-url-filenames`）

## 2. 目标与范围

- 目标：修复 URL 编码与原始字符混合的 Office 文件名无法预览，并避免 Mango 内嵌预览场景把原始中文文件名作为 kkFileView 缓存名。
- 成功条件：
  - Mango 按 `fileId` 生成仅含 ASCII 的引擎文件名，并保留原始扩展名供文件类型识别。
  - 源文件 token 仍绑定 `fileId` 与签发上下文，原始文件名只用于文件流响应展示。
  - 源文件地址支持显式配置内部 base URL；未配置时保持现有入口地址兼容行为。
  - kkFileView 能识别包含合法 `%HH` 与原始 RFC 安全字符的混合文件名，且不解码非法百分号序列。
  - 现有普通文件名、完整百分号编码文件名和源 token 行为不回归。
- 处理范围：`mango-file-preview-core` 的源地址生成、`mango-file-preview-engine` 的编码判定、模块配置说明和针对性测试。
- 不处理范围：重写 kkFileView 的 URL 下载模型、修改 `mango-file` 存储实现、改变公开预览 API、数据库结构、权限或 token 生命周期。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `/file-preview/files/preview` | 文件 ID 对应名称为 `中文 (1).docx` | 引擎源 URL 的 `fullfilename` 使用 `file-<id>.docx` | 无法获得有效扩展名时使用不带扩展名的 ASCII 文件 ID 名，由引擎按不支持类型处理 | 解码后的引擎 URL不含原始中文名且包含稳定 fileId |
| SR-002 | 内嵌 kkFileView | 配置了内部源地址 | 引擎从内部地址请求 `/file-preview/sources` | 配置为空时回退现有请求 base URL | 配置值优先于外部请求 host |
| SR-003 | kkFileView URL 解析 | `%E4%B8%AD%E6%96%87%20(1).docx` | 识别为包含 URL 编码并仅解码一次 | 存在非法或残缺 `%` 时不得进入 `URLDecoder` | 混合编码、大小写十六进制和非法百分号用例通过 |
| SR-004 | `/file-preview/sources` | 有效 source token | 通过 `IFileContentProvider.downloadForService(fileId)` 返回原始文件流与原始文件名 | 无效、过期 token 保持现有稳定错误 | 现有上下文恢复和流读取测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001 | 使用 `file-<fileId>` 作为稳定名，只从元数据文件名提取并规范化最后一个扩展名；不使用文件 hash，避免 hash 为空、跨记录复用和缓存身份不明确 | `FilePreviewServiceImpl` | 恢复原始 `fileName` 查询参数 |
| TD-002 | SR-002 | 新增可选 `mango.file-preview.source-base-url`；配置后只用于预览引擎访问源文件，不作为对外 URL 返回 | `FilePreviewProperties`、模块 README | 删除配置并恢复请求 base URL |
| TD-003 | SR-003 | `hasUrlEncoded` 完整扫描字符串：接受大小写 `%HH`，允许原始字符共存；任一非法 `%` 使结果为未编码；保留普通安全字符串的既有判断 | `UrlEncoderUtils` | 恢复旧扫描实现 |
| TD-004 | SR-001/SR-003 | 不把原始中文名写入引擎缓存键；原始名仍由 source token 解析后的下载结果返回到 `Content-Disposition` | core 与 engine 现有边界 | 恢复原始名作为 `fullfilename` |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | 1 | `mango-file-preview-core` | 生成 fileId 安全名并支持内部 source base URL |
| IMPL-002 | TD-003 | 2 | `mango-file-preview-engine` | 修正混合百分号编码判断且无非法解码 |
| IMPL-003 | TD-001/TD-002/TD-003 | 3 | 对应模块 `src/test/java` | 新增稳定回归测试，真实执行被测逻辑 |
| IMPL-004 | TD-002 | 4 | `mango-file-preview/README.md` | 配置字段和流量边界说明与实现一致 |
| IMPL-005 | 全部 | 5 | 直接修改 Maven 模块 | 定向测试、测试质量检查和 `verify` 通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M10 单元测试 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-core/pom.xml -Dtest=io.mango.file.preview.core.service.impl.FilePreviewServiceImplTest test` | PASS（9/9） | `mango-file-preview-core/target/surefire-reports` |
| SR-002 | M10 单元测试 | `FilePreviewServiceImplTest` 验证配置的内部 base URL 优先并规范化尾部斜杠 | PASS | 同上 |
| SR-003 | M10 单元测试 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml -Dtest=cn.keking.utils.UrlEncoderUtilsTest test` | PASS（6/6） | `mango-file-preview-engine/target/surefire-reports` |
| SR-004 | M10 回归测试 | `FilePreviewServiceImplTest` 的 source token、上下文和流读取用例 | PASS | `mango-file-preview-core/target/surefire-reports` |
| 全部 | M09 静态验证 | 合并最新 `origin/main` 后，两个直接修改模块分别执行 `mvn verify` | PASS（core 9/9；engine 39/39） | Maven 输出与各模块 `target/surefire-reports` |
| 全部 | 测试质量检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS（2 个变更测试文件；block/warn 0） | 命令输出 |

## 7. 例外与剩余风险

- 本次不移除 kkFileView 的 HTTP 下载模型；配置 `source-base-url` 后可以使用集群内部地址避免公网 hairpin，但仍存在一次内部 HTTP 往返。
- `mango workspace init` 未执行：当前环境没有可用的 `mango` 命令；不影响 Maven 定向验证，但属于本地工作区初始化例外。
- 不执行 LibreOffice 浏览器级转换验收；自动化验证覆盖源 URL、文件名判定、token 和文件流边界，真实 LibreOffice 环境仍需发布前回归。
