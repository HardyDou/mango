# Issue #382 文件服务合并生成 PDF 交付台账

日期：2026-07-03

| 项 | 状态 | 说明 |
|---|---|---|
| 需求澄清 | 完成 | 第一版只支持输出 PDF；`targetFormat` 可预留但仅允许 `PDF`。 |
| API 契约 | 完成 | 新增 `FileMergePdfCommand`、`FileMergePdfEntryCommand`、`FileMergeTargetFormat`、`FileApi.mergeToPdf`。 |
| 服务实现 | 完成 | 源文件读取、格式识别、转 PDF、合并和保存新 PDF。 |
| HTTP 入口 | 完成 | `POST /file/files/merge-pdf`。 |
| 远程调用 | 完成 | `FileFeignClient.mergeToPdf`。 |
| 状态隔离 | 完成 | 源文件沿用 `downloadForService`，只允许当前租户可见且已完成文件。 |
| 图片转 PDF | 完成 | 新增 `ImageToPdfConvertProvider`，PNG/JPEG 默认走 ImageIO/PDFBox，避开 Java 21 下 Aspose.Imaging 反射限制。 |
| 文档 | 完成 | 更新 `mango-file/README.md`、`mango-infra-fileproc/README.md`、业务接入指南和根 `CHANGELOG.md`。 |
| 测试 | 完成 | 覆盖接口层、服务层、真实 PDF/PNG/JPEG/DOCX 样本转换合并、provider 和自动配置。 |
