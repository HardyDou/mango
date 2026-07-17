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
- 处理范围：`mango-file-preview-core` 的源地址生成，`mango-file-preview-engine` 的编码判定、转换文件资源映射和 PDF.js 加载路径，starter 安全属性兼容，模块说明、单元测试与真实浏览器联调。
- 不处理范围：修改 `mango-file` 存储实现、改变公开预览 API、数据库结构、权限或 token 生命周期。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `/file-preview/files/preview` | 文件 ID 对应名称为 `中文 (1).docx` | 引擎源 URL 的 `fullfilename` 使用 `file-<id>.docx` | 无法获得有效扩展名时使用不带扩展名的 ASCII 文件 ID 名，由引擎按不支持类型处理 | 解码后的引擎 URL不含原始中文名且包含稳定 fileId |
| SR-002 | 内嵌 kkFileView | 配置了内部源地址 | 引擎从内部地址请求 `/file-preview/sources` | 配置为空时回退现有请求 base URL | 配置值优先于外部请求 host |
| SR-003 | kkFileView URL 解析 | `%E4%B8%AD%E6%96%87%20(1).docx` | 识别为包含 URL 编码并仅解码一次 | 存在非法或残缺 `%` 时不得进入 `URLDecoder` | 混合编码、大小写十六进制和非法百分号用例通过 |
| SR-004 | `/file-preview/sources` | 有效 source token | 通过 `IFileContentProvider.downloadForService(fileId)` 返回原始文件流与原始文件名 | 无效、过期 token 保持现有稳定错误 | 现有上下文恢复和流读取测试通过 |
| SR-005 | PDF.js | LibreOffice 已生成本地 PDF | 通过同源 `/static/file-preview/file-<id>...pdf` 读取，不经 `/getCorsFile` 二次回源 | 资源不存在时返回 404，不能回退为任意 URL 代理 | PDF 响应 200、类型为 `application/pdf`，PDF.js 页和 canvas 均非空 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001 | 使用 `file-<fileId>` 作为稳定名，只从元数据文件名提取并规范化最后一个扩展名；不使用文件 hash，避免 hash 为空、跨记录复用和缓存身份不明确 | `FilePreviewServiceImpl` | 恢复原始 `fileName` 查询参数 |
| TD-002 | SR-002 | 新增可选 `mango.file-preview.source-base-url`；配置后只用于预览引擎访问源文件，不作为对外 URL 返回 | `FilePreviewProperties`、模块 README | 删除配置并恢复请求 base URL |
| TD-003 | SR-003 | `hasUrlEncoded` 完整扫描字符串：接受大小写 `%HH`，允许原始字符共存；任一非法 `%` 使结果为未编码；保留普通安全字符串的既有判断 | `UrlEncoderUtils` | 恢复旧扫描实现 |
| TD-004 | SR-001/SR-003 | 不把原始中文名写入引擎缓存键；原始名仍由 source token 解析后的下载结果返回到 `Content-Disposition` | core 与 engine 现有边界 | 恢复原始名作为 `fullfilename` |
| TD-005 | SR-005 | 为引擎文件目录增加 `/static/file-preview/**` 专用资源映射；本地转换 PDF 由 PDF.js 同源直读，复用已激活的 `/static/**` PUBLIC 策略 | `WebConfig`、`pdf.ftl` | 恢复根路径资源 URL 和 `getCorsFile` 代理逻辑 |
| TD-006 | SR-005 | 使用 `Path.toUri()` 生成文件资源 location，避免容器根工作目录下的 `//server/...` 被解释成带 authority 的 `file://server/...` | `WebConfig` | 恢复字符串拼接 `file:` |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | 1 | `mango-file-preview-core` | 生成 fileId 安全名并支持内部 source base URL |
| IMPL-002 | TD-003 | 2 | `mango-file-preview-engine` | 修正混合百分号编码判断且无非法解码 |
| IMPL-003 | TD-001/TD-002/TD-003 | 3 | 对应模块 `src/test/java` | 新增稳定回归测试，真实执行被测逻辑 |
| IMPL-004 | TD-002 | 4 | `mango-file-preview/README.md` | 配置字段和流量边界说明与实现一致 |
| IMPL-005 | TD-005/TD-006 | 5 | engine、starter 与 Playwright E2E | 转换 PDF 使用既有静态资源命名空间，容器路径可读，浏览器真实渲染成功 |
| IMPL-006 | 全部 | 6 | 直接修改 Maven 模块 | 定向测试、测试质量检查和真实联调通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M10 单元测试 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-core/pom.xml -Dtest=io.mango.file.preview.core.service.impl.FilePreviewServiceImplTest test` | PASS（9/9） | `mango-file-preview-core/target/surefire-reports` |
| SR-002 | M10 单元测试 | `FilePreviewServiceImplTest` 验证配置的内部 base URL 优先并规范化尾部斜杠 | PASS | 同上 |
| SR-003 | M10 单元测试 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml -Dtest=cn.keking.utils.UrlEncoderUtilsTest test` | PASS（6/6） | `mango-file-preview-engine/target/surefire-reports` |
| SR-004 | M10 回归测试 | `FilePreviewServiceImplTest` 的 source token、上下文和流读取用例 | PASS | `mango-file-preview-core/target/surefire-reports` |
| SR-005 | M10 单元测试 | `WebConfigTest`、`PdfTemplateTest`、`FilePreviewFrameOptionsFilterTest`、`FilePreviewEngineResourceRegistrarTest` | PASS | 文件 URI 无 authority；本地 PDF 使用 `/static/file-preview/**`；未注册新的根路径资源规则 |
| SR-001/SR-002/SR-004/SR-005 | M07 真实集成 + M11 E2E | Docker 启动 Java 21 + LibreOffice 24.2.7.2；Playwright 上传 `中文 (1).docx` 并实际打开 PDF.js | PASS（首次 1/1；单 worker 并发复跑 2/2） | `mango-ui/apps/mango-admin/e2e/.tmp/file-preview-types-live-results.json`、`file-preview-issue-563-docx.png` |
| 全部 | M09/M10 回归 | `mvn -f mango/pom.xml -pl :mango-file-preview-core,:mango-file-preview-engine,:mango-file-preview-starter test` | PASS（core 9；engine 40；starter 10；合计 59） | Maven 输出与各模块 `target/surefire-reports` |
| 全部 | 测试质量检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS（9 个测试文件；block/warn 0） | 命令输出 |

## 7. 例外与剩余风险

- 源文件仍由预览引擎通过 source token 发起一次内部 HTTP 下载；配置 `source-base-url` 后不会经过公网或外围网关。转换后的本地 PDF 不再通过 URL 代理二次回源。
- `mango workspace init` 未执行：当前环境没有可用的 `mango` 命令；不影响 Maven 定向验证，但属于本地工作区初始化例外。
- Apple Silicon 上运行 amd64 LibreOffice 镜像时，默认第二个 worker（2002）曾因 Rosetta 模拟失败；验证容器收敛为单 worker（2001）后，真实转换与两路并发浏览器用例均通过。该现象属于本地镜像架构兼容，不是业务链路失败。
