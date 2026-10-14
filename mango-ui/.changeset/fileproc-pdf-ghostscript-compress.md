---
'@mango/file': patch
---

为 `fileApi.downloadUrl(id, options)` 与 `fileApi.download(id, options)` 增加可选 `compression`（`none`/`low`/`standard`/`medium`/`high`/`extreme`）与 `perFileTargetSizeBytes` 参数，下载时可由文件服务直接返回压缩后的内容。

同一批次的 Maven 物料 `mango-infra-fileproc-api`、`mango-infra-fileproc-core`、`mango-infra-fileproc-starter`：

- `FileCompression` 新增 `STANDARD`、`EXTREME`，形成 5 档压缩强度 `LOW < STANDARD < MEDIUM < HIGH < EXTREME`；既有档位语义不变。
- 用流式 Ghostscript 实现替换 PDFBox 逐页栅格实现：不把源文件常驻内存，纯文字页面保持矢量文字，支持目标体积，输出不变小时保留原文件。
- 新增配置项 `mango.fileproc.compress.ghostscript-path` 与 `mango.fileproc.compress.timeout-seconds`；未安装 Ghostscript 时明确失败。

业务适配：文件服务下载压缩链路需在运行镜像中安装 Ghostscript，并按需配置可执行文件路径。
