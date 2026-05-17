# 文档处理与模板中心能力规划

更新时间：2026-05-17

## 背景

文件中心已经收敛为文件资产服务，只管理文件记录、存储、访问、版本、派生关系和预览入口。Word 转 PDF、PDF 工具、OCR、在线 Office 编辑、业务模板渲染不再放入 `mango-file`，避免文件服务边界膨胀。

后续新增两个能力域：

- `mango-document`：文档处理中心。
- `mango-template`：模板中心。

## 模块边界

### mango-file

职责：

- 文件上传、下载、预览入口。
- 存储配置和对象存储适配。
- 文件记录、逻辑目录、权限、归档、审计。
- 文件版本和派生文件关系。
- 分片上传、秒传、直传。

不负责：

- Office 转 PDF。
- PDF 合并、拆分、压缩、水印。
- OCR。
- 在线 Office 编辑。
- 模板变量、模板渲染、业务出文档。

### mango-document

职责：

- Office / WPS / ODF 转 PDF。
- PDF 合并、拆分、压缩、水印、线性化。
- 图片转 PDF、PDF 转图片、缩略图生成。
- OCR 文本识别。
- 在线 Office provider 适配，例如 ONLYOFFICE、Collabora、WPS WebOffice。
- 文档处理任务、任务状态、失败重试、处理日志。

推荐技术：

- Office 转 PDF：LibreOffice headless + JODConverter。
- PDF 合并/拆分/水印：Apache PDFBox。
- PDF 无损优化：qpdf。
- 图片型 PDF 强压缩：Ghostscript，许可证需单独评估。
- OCR：后续按部署条件选择 Tesseract 或商业 OCR provider。
- 在线编辑：优先调研 ONLYOFFICE Community；如果客户明确要求 WPS 体验，再评估 WPS WebOffice 授权。

### mango-template

职责：

- 模板分类、模板编码、模板版本。
- 模板变量定义、变量来源、变量校验。
- 模板文件管理，模板原文件由 `mango-file` 保存。
- 模板预览。
- 根据业务数据渲染生成 Word/PDF。
- 模板发布、停用、版本回滚。
- 模板使用记录和生成记录。

推荐技术：

- Word 模板渲染：优先 `poi-tl` 或 `docx4j`，按模板复杂度选择。
- PDF 输出：模板先生成 Word，再通过 `mango-document` 转 PDF；正式 PDF 作为文件中心新文件或派生文件保存。
- 简单文本模板：可使用 Velocity，但只用于变量文本渲染，不作为 Word 预览或高保真转换方案。

## 调用关系

```text
业务系统 -> mango-file
业务系统 -> mango-template -> mango-document -> mango-file
mango-file -> mango-document 生成预览派生文件
```

## 典型流程

### Word 预览

1. 用户上传 Word 到 `mango-file`。
2. 用户点击预览。
3. `mango-file` 校验文件权限和归档状态。
4. `mango-file` 查询是否已有 PDF 预览派生文件。
5. 如果没有，调用 `mango-document` 转 PDF。
6. `mango-document` 从 `mango-file` 读取原文件，转换后写回 `mango-file`。
7. `mango-file` 建立原文件与 PDF 派生文件关系。
8. 前端预览 PDF。

### PDF 合并

1. 业务系统提交多个 `fileId` 到 `mango-document`。
2. `mango-document` 调用 `mango-file` 校验并读取文件。
3. 使用 PDFBox 合并。
4. 合并结果写入 `mango-file`。
5. 返回新 `fileId` 给业务系统。

### 模板生成文档

1. 业务系统调用 `mango-template`，传入 `templateCode`、业务 ID 和变量上下文。
2. `mango-template` 加载已发布模板版本和变量定义。
3. `mango-template` 渲染生成 Word。
4. 生成结果写入 `mango-file`。
5. 如需 PDF，`mango-template` 调用 `mango-document` 转换。
6. 生成记录保存模板版本、变量快照、输出文件 ID。

## P0 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P0 | 文件中心派生文件关系模型 | 可记录原文件、预览 PDF、压缩版、缩略图等关系 |
| P0 | `mango-document` 模块骨架 | API/Core/Starter 边界清晰，README 说明职责和非职责 |
| P0 | Office 转 PDF 任务接口设计 | 支持提交转换、查询状态、获取结果文件 ID |
| P0 | `mango-template` 模块骨架 | API/Core/Starter 边界清晰，README 说明模板职责 |
| P0 | 模板元数据模型设计 | 支持模板编码、分类、版本、状态、模板文件 ID |

## P1 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P1 | LibreOffice + JODConverter 转 PDF | doc/docx/xls/xlsx/ppt/pptx 可转换为 PDF |
| P1 | PDF 合并 | 多个 PDF 合并成新文件记录 |
| P1 | 模板变量定义与渲染记录 | 生成记录可追溯模板版本和变量快照 |
| P1 | Word 模板生成 Word | 使用模板和变量生成 docx，并保存到文件中心 |

## P2 计划

| 优先级 | 任务 | 验收标准 |
|---|---|---|
| P2 | PDF 压缩 | 支持图片型 PDF 压缩档位，保留原文件 |
| P2 | OCR | 支持扫描件文本识别，识别结果可检索 |
| P2 | 在线 Office 编辑 provider | 支持 ONLYOFFICE / Collabora / WPS WebOffice provider 插拔 |
| P2 | 模板生成 PDF | 模板生成 Word 后可自动转 PDF 并建立关联 |

## 设计原则

- 文件中心不感知模板变量和文档处理细节。
- 文档处理不保存业务模板语义，只处理文件转换和加工任务。
- 模板中心不直接管理底层对象存储，只持有 `fileId`。
- 所有处理结果都进入文件中心，形成可审计、可追溯的文件记录。
- 正式件不覆盖原文件，生成新版本或派生文件。
