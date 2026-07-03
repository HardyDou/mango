# Issue #382 文件服务合并生成 PDF 详细设计

文档状态：实施中
日期：2026-07-03
关联 Issue：<https://github.com/HardyDou/mango/issues/382>

## 1. 设计目标与范围

本次为文件服务新增“多个 `fileId` 合并生成 PDF”能力。业务上传多张手机照片、PDF 或 Word 材料后，可调用文件服务生成一个新的 PDF 文件记录，并继续通过文件中心预览、下载和持久化 `fileId`。

范围内：

- 文件服务 API、HTTP 和 Feign 入口。
- 文件服务读取源文件、识别格式、调用 fileproc 转换/合并、保存新文件记录。
- 文件服务和业务接入文档。
- 后端单元测试。

范围外：

- 输出 Word 或其它目标格式。
- 异步任务、进度查询和后台队列。
- 前端页面、OCR、在线编辑和目录/书签高级编排。

## 2. 设计输入

| 输入 | 来源 | 结论 |
|---|---|---|
| 用户确认 | 2026-07-03 对话 | 第一版只交付 PDF，目标格式字段可预留。 |
| 现有 ZIP 打包 | `mango-file` Issue #316 实现 | 新能力与 `packageFiles` 平行，不复用 ZIP 语义。 |
| 文件加工边界 | `mango-infra-fileproc` README | fileproc 只处理转换和 PDF 操作，不处理 fileId、租户和存储。 |
| 文件服务边界 | `mango-file` README | 文件服务负责 fileId、权限、租户、存储和新文件记录。 |

## 3. 影响模块与改动边界

| 模块 | 路径 | 改动类型 | 是否公共能力 | 说明 |
|---|---|---|---|---|
| File API | `mango-platform/mango-file/mango-file-api` | 新增 | 是 | 新增命令、目标格式枚举和 API 方法。 |
| File Core | `mango-platform/mango-file/mango-file-core` | 新增 | 是 | 实现同步合并 PDF。 |
| File Starter | `mango-platform/mango-file/mango-file-starter` | 新增 | 是 | 暴露 HTTP 入口。 |
| File Remote Starter | `mango-platform/mango-file/mango-file-starter-remote` | 新增 | 是 | 暴露 Feign 入口。 |
| Fileproc | `mango-infra/mango-infra-fileproc` | 新增/复用 | 是 | 复用 `ConvertApi` 和 `RenderApi`，新增 ImageIO/PDFBox 图片转 PDF provider，避免 PNG/JPEG 合 PDF 依赖 Aspose.Imaging 的 Java 21 反射行为。 |
| 文档 | `mango-docs`、`mango-file/README.md` | 修改 | 是 | 补业务接入和能力说明。 |

## 4. 关键对象

| 对象 | 业务含义 | 归属 | 生命周期 | 关键约束 |
|---|---|---|---|---|
| PDF 合并命令 | 一次合并生成 PDF 的请求 | 调用方创建，文件服务执行 | 请求内一次性对象 | `entries` 不能为空，`targetFormat` 仅允许 `PDF`。 |
| PDF 合并条目 | 一个源文件引用 | 文件服务按顺序处理 | 请求内一次性对象 | `fileId` 必填，源文件必须可见且 `COMPLETED`。 |
| 源文件记录 | 已上传或已保存的文件 | 文件中心 | 既有文件生命周期 | 不读取归档、删除、未完成或跨租户文件。 |
| 输出 PDF 文件记录 | 合并结果 | 文件中心 | 普通文件记录 | `contentType=application/pdf`，返回新 `fileId`。 |

## 5. 数据流设计

```mermaid
flowchart LR
  B[业务后端] --> F[FileApi.mergeToPdf]
  F --> D[downloadForService]
  D --> C[ConvertApi 转 PDF]
  C --> R[RenderApi.mergePdf]
  R --> S[FileService.save]
  S --> O[新 PDF FileRecordVO]
```

| 流程 | 数据来源 | 处理方 | 写入对象 | 失败处理 | 用户可见结果 |
|---|---|---|---|---|---|
| 合并生成 PDF | `entries.fileId` | `FileServiceImpl` | 新 `FileRecord` 和存储对象 | 任一源文件不可读、格式不支持、转换失败或合并失败时抛业务异常，不保存半成品 | 成功返回新 PDF，失败返回明确错误 |

## 6. 接口设计

| 接口 | 入口 | 请求 | 响应 | 权限/租户 | 兼容策略 |
|---|---|---|---|---|---|
| Java API | `FileApi.mergeToPdf(FileMergePdfCommand)` | 输出文件信息和 `entries` | `R<FileRecordVO>` | 复用文件服务当前租户可见性 | 新增 default 方法，旧实现不受影响。 |
| Service | `IFileService.mergeToPdf(FileMergePdfCommand)` | 同 Java API | `R<FileRecordVO>` | 同上 | 新增内部服务方法。 |
| HTTP | `POST /file/files/merge-pdf` | JSON body | `R<FileRecordVO>` | `file:files:upload` | 与 `/package` 同级能力。 |
| Feign | `FileFeignClient.mergeToPdf` | JSON body | `R<FileRecordVO>` | 服务端校验 | 与 `/package` 一致。 |

## 7. 格式处理

| 源格式 | 处理方式 |
|---|---|
| PDF | 直接作为 PDF source 参与合并。 |
| JPG/JPEG/PNG/TIFF | `ConvertApi` 转 PDF 后参与合并。 |
| DOC/DOCX | `ConvertApi` 转 PDF 后参与合并。 |
| 其它格式 | 抛出 `FILE_EXTENSION_NOT_ALLOWED`，不保存输出文件。 |

PNG/JPEG 默认由 `ImageToPdfConvertProvider` 转 PDF；TIFF 继续由 `TiffToPdfConvertProvider` 转 PDF；Word 继续由现有 Word/Office 转换 provider 转 PDF。

## 8. 异常与边界

| 场景 | 处理 |
|---|---|
| 命令为空或条目为空 | `FILE_EMPTY`。 |
| `targetFormat` 不是 `PDF` | `FILE_EXTENSION_NOT_ALLOWED`。 |
| 源文件不存在、跨租户、归档、删除或未完成 | 复用 `downloadForService` 的文件错误。 |
| 源格式无法识别或不支持 | `FILE_EXTENSION_NOT_ALLOWED`。 |
| 转换或合并失败 | `FILE_READ_FAILED`。 |
| 保存输出 PDF 失败 | 复用 `save` 的文件保存错误。 |

## 9. 验收映射

| 验收项 | 测试用例 | 优先级 | 层级 | 自动化判断 |
|---|---|---|---|---|
| 多个 PDF 按顺序合并并保存为新 PDF | TC-382-001 | P1 | 单元 | AUTO |
| 图片和 Word 转 PDF 后参与合并 | TC-382-002 | P1 | 单元 | AUTO |
| 非 PDF 目标格式被拒绝 | TC-382-003 | P1 | 单元 | AUTO |
| 不支持源格式被拒绝且不生成记录 | TC-382-004 | P1 | 单元 | AUTO |
| 源文件状态隔离沿用文件服务下载链路 | TC-382-005 | P1 | 单元 | AUTO |
| 真实 PDF/PNG/JPEG/DOCX 样本可转换并合并为可打开 PDF | TC-382-006 | P1 | 集成单元 | AUTO |

## 10. 风险与取舍

- 首期同步处理适合中小文件合并；大批量扫描件后续应扩展异步任务。
- Word 转 PDF 依赖当前运行时 fileproc 转换配置；未配置转换能力时应明确失败。
- `targetFormat` 只作为兼容预留字段，首期不承诺输出 Word。

## 11. 自检

| 检查项 | 结果 | 说明 |
|---|---|---|
| 目标、范围、不处理范围清楚 | PASS | 已明确 PDF-only。 |
| 接口、数据、权限、租户、文件边界清楚 | PASS | 不新增表，不改变现有租户规则。 |
| 验收项有测试映射 | PASS | 覆盖正常和异常。 |
| 不存在无法追溯的新范围 | PASS | 均来自 Issue、用户确认或现有模块边界。 |
