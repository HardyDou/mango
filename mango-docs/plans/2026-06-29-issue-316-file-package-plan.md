# Issue #316 文件服务 ZIP 打包实施计划

文档状态：已实施
关联 Issue：<https://github.com/HardyDou/mango/issues/316>
日期：2026-06-29

## 1. 目标

文件服务支持业务后端提交一个文件清单，按清单中的 ZIP 内相对路径生成目录结构化压缩包，压缩包写回文件中心当前存储层，并返回新的 `FileRecordVO`。

首期采用同步打包：

- 入参包含输出文件名、业务归属、访问级别和 `entries[{fileId,path}]`。
- 每个源文件通过文件中心现有可见性和下载链路读取。
- ZIP 生成后复用 `save` 写入存储层，生成新的文件记录。
- 返回值是新压缩包的 `FileRecordVO`，业务可直接把 `id` 或下载入口反馈给前端。

## 2. 不处理范围

- 首期不做异步任务、进度表和队列消费。
- 首期不预创建 `UPLOADING` 文件记录。
- 首期不支持文件夹递归打包，业务必须显式传入文件清单。
- 首期不做跨租户读取、归档文件读取或未完成文件读取。
- 首期不新增前端页面。

## 3. 设计结论

| 主题 | 结论 |
|---|---|
| API | 新增 `FileApi.packageFiles(FilePackageCommand)` 和 `POST /file/files/package`。 |
| 返回 | 同步生成完成后返回新 ZIP 的 `FileRecordVO`。 |
| 存储 | ZIP 内容作为普通文件复用 `save` 写入存储层，遵守大小、扩展名、访问级别、业务归属和目录规则。 |
| 路径 | `entries.path` 只允许 ZIP 内相对文件路径，支持 `${fileName}` 引用源文件记录名，拒绝空路径、目录项、绝对路径、`..` 和重复路径。 |
| 源文件 | 源文件必须是当前租户可见且状态为 `COMPLETED` 的文件。 |
| 异步扩展 | 后续可扩展为“预创建记录并返回 id，后台完成后更新状态”，当前接口模型保留业务归属和文件记录返回语义。 |

## 4. 交付项

| 层 | 交付物 |
|---|---|
| API | `FilePackageCommand`、`FilePackageEntryCommand`、`FileApi.packageFiles`。 |
| Core | `IFileService.packageFiles`、`FileServiceImpl.packageFiles`、ZIP 生成和路径校验。 |
| Starter | `FileController.packageFiles`。 |
| Remote | `FileFeignClient.packageFiles`。 |
| 文档 | 更新 `mango-file/README.md`，新增本实施计划和详细设计。 |
| 测试 | 新增完整特性单元测试，覆盖生成 ZIP、保存新记录、路径非法和重复路径拒绝。 |
