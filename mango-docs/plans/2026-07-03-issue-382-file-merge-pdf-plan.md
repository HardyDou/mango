# Issue #382 文件服务合并生成 PDF 实施计划

文档状态：实施中
关联 Issue：<https://github.com/HardyDou/mango/issues/382>
日期：2026-07-03

## 1. 目标

文件服务支持业务后端提交多个已有 `fileId`，按清单顺序合并生成一个 PDF，PDF 写回文件中心当前存储层，并返回新的 `FileRecordVO`。

首期采用同步生成：

- 入参包含输出文件名、业务归属、访问级别、逻辑目录和 `entries[{fileId,title}]`。
- 首期目标格式固定为 `PDF`，命令保留 `targetFormat` 字段但只允许 `PDF`。
- 源文件通过文件中心当前租户可见性和服务端下载链路读取。
- PDF、图片、Word 通过 `mango-infra-fileproc` 转换为 PDF 后合并。
- 合并结果复用 `save` 写入文件中心，业务继续只保存返回的 `fileId`。

## 2. 不处理范围

- 首期不支持输出 Word、Excel、PPT、OFD 或 ZIP。
- 首期不做异步任务、进度表和队列消费。
- 首期不预创建 `UPLOADING` 文件记录。
- 首期不支持跨租户读取、归档文件读取或未完成文件读取。
- 首期不新增前端页面和前端组件。

## 3. 设计结论

| 主题 | 结论 |
|---|---|
| API | 新增 `FileApi.mergeToPdf(FileMergePdfCommand)` 和 `POST /file/files/merge-pdf`。 |
| 返回 | 同步生成完成后返回新 PDF 的 `FileRecordVO`。 |
| 目标格式 | `targetFormat` 默认 `PDF`，首期仅允许 `PDF`，为后续独立扩展保留兼容入口。 |
| 存储 | PDF 内容作为普通文件复用 `save` 写入存储层，遵守大小、扩展名、访问级别、业务归属和目录规则。 |
| 源文件 | 源文件必须是当前租户可见且状态为 `COMPLETED` 的文件。 |
| 格式 | 首期支持 PDF、JPG/JPEG、PNG、TIFF、DOC、DOCX；其它格式明确失败且不保存半成品。 |
| 异步扩展 | 后续可扩展为“预创建记录并返回 id，后台完成后更新状态”，当前接口模型保留业务归属和文件记录返回语义。 |

## 4. 交付项

| 层 | 交付物 |
|---|---|
| API | `FileMergePdfCommand`、`FileMergePdfEntryCommand`、`FileMergeTargetFormat`、`FileApi.mergeToPdf`。 |
| Core | `IFileService.mergeToPdf`、`FileServiceImpl.mergeToPdf`、源文件格式识别、转换和 PDF 合并。 |
| Starter | `FileController.mergeToPdf`。 |
| Remote | `FileFeignClient.mergeToPdf`。 |
| 文档 | 更新 `mango-file/README.md`、业务接入指南、文档索引和本计划/设计/台账。 |
| 测试 | 新增完整特性单元测试，覆盖 PDF 合并、图片/Word 转换后合并、非法目标格式、空清单、不支持格式和失败不生成记录。 |
