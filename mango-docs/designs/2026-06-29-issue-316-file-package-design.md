# Issue #316 文件服务 ZIP 打包详细设计

文档状态：已实施
日期：2026-06-29

## 1. 接口契约

新增命令：

- `FilePackageCommand`
  - `fileName`：输出 ZIP 文件名，缺少 `.zip` 后缀时自动补齐。
  - `purpose`、`accessLevel`、`bizType`、`bizId`、`bizMeta`、`directoryId`：透传给新文件记录。
  - `entries`：打包清单，不能为空。
- `FilePackageEntryCommand`
  - `fileId`：源文件记录 ID。
  - `path`：ZIP 内相对文件路径，支持 `${fileName}` 表示源文件记录名。

新增入口：

- Java API：`FileApi.packageFiles(FilePackageCommand command)`。
- Service：`IFileService.packageFiles(FilePackageCommand command)`。
- HTTP：`POST /file/files/package`。
- Feign：`FileFeignClient.packageFiles(FilePackageCommand command)`。

## 2. 核心流程

1. 校验命令和打包清单非空。
2. 规范输出文件名，确保以 `.zip` 结尾。
3. 逐个处理 `entries`：
   - 校验 `fileId` 非空。
   - 通过 `downloadForService(fileId)` 读取源文件。
   - 使用源文件记录名替换 `entries.path` 中的 `${fileName}`。
   - 校验替换后的 ZIP 内路径合法且不重复。
   - 写入 `ZipOutputStream`，entry name 使用业务传入的相对路径。
4. ZIP 字节生成后构造 `SaveFileCommand`：
   - `contentType = application/zip`
   - `fileSize = zipBytes.length`
   - 业务字段和访问级别来自原命令。
5. 调用现有 `save` 方法写入存储层、创建对象记录和文件记录。
6. 返回 `save` 的 `FileRecordVO`。

## 3. 路径与安全规则

`entries.path` 必须满足：

- 非空。
- 可包含 `${fileName}` 变量，替换值来自源文件记录的 `fileName`，不会要求业务额外查询文件名。
- 不能以 `/` 开头。
- 不能是 Windows 盘符绝对路径，例如 `C:/a.pdf`。
- 不能以 `/` 结尾，避免目录 entry。
- 不能包含 `//`。
- 路径段不能为空、`.`、`..`。
- 路径段不能包含 NUL 字符。
- 同一个 ZIP 内路径不能重复。

源文件读取继续走当前租户可见文件查询，且只允许 `COMPLETED` 状态文件参与打包。这样后续异步方案预创建 `UPLOADING` 记录时，不会被下载和再次打包读取。

## 4. 同步与异步取舍

首期同步方案的返回值是完成态新记录，适合中小文件和后端业务立即返回结果：

```text
业务后端 -> packageFiles -> 文件服务生成 ZIP -> 写入存储 -> 返回 FileRecordVO
```

异步扩展保留方向：

```text
业务后端 -> createPackageTask -> 预创建 FileRecord(UPLOADING) -> 返回 id/下载地址
前端轮询 get/preview -> 后台生成 ZIP -> 写入存储 -> 更新 COMPLETED
```

当前实现已经收紧下载链路，只允许完成态文件下载，为后续预创建记录提供状态隔离。异步落地时需要新增任务状态、失败态回写和后台执行器，不在本次范围内。

## 5. 测试策略

采用完整特性单元测试，不单独测试私有路径工具函数：

- 通过 `FileServiceImpl.packageFiles` 发起完整打包。
- 使用内存模拟 mapper 和 storage，验证新文件记录和真实 ZIP 内容。
- 覆盖非法路径拒绝后不生成新记录。
- 覆盖 ZIP 内重复路径拒绝后不生成新记录。
