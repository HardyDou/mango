# Issue #972 Office 预览稳定性实施计划

## 目标

在业务单体和独立预览引擎部署下，避免同一文件重复转换；让 Office/UNO 断连、进程退出和转换超时进入可恢复终态；缓存按文件内容版本隔离；读取大文件保持流式，不把整个文件载入堆内存。

## 实施边界

- Mango core：在预览源 URL 中携带稳定的源版本键；继续由 `FilePreviewFileGateway` 执行 `fileId`、租户和服务权限校验。
- Preview engine：增加稳定缓存键、并发任务协调器、任务超时和结果原子提交；保持独立部署的 URL/token 兼容协议。
- 不改变已有非 Office 转换器的业务行为；先覆盖 Office 图片/PDF 两条路径。

## 文件与职责

- `FilePreviewServiceImpl`：生成基于文件 hash/大小/更新时间的稳定 source version key。
- `FileAttribute`、`FileHandlerService`：解析并携带 source version key，生成版本隔离的转换缓存名。
- `ConversionCoordinator`：按任务键合并并发请求，记录完成/失败/超时状态并释放任务。
- `OfficeFilePreviewImpl`：通过协调器运行转换，设置整体超时，保证临时输出原子提交。
- `FileConvertStatusManager`：补充失联/超时状态清理，避免永久 RUNNING。
- Redis/JDK/RocksDB cache implementations：统一版本缓存读写和有限生命周期。

## 测试顺序

1. 先为稳定版本键、流式大文件写入、任务去重、超时和失败清理编写失败单测。
2. 实现最小代码使单测通过。
3. 增加 OfficeFilePreviewImpl 的并发与缓存命中回归测试。
4. 在可用 LibreOffice 环境执行真实小 DOCX、复杂 DOCX 和生成 200MB DOCX 的集成验证；不可用时保留明确未验证记录。
5. 执行模块测试、全仓质量门禁、test-quality-check，并回读 diff。

## 验收

- 相同版本并发请求只执行一次转换。
- 文件 hash/大小/更新时间变化后缓存失效。
- 断连、进程退出、超时后不会永久停留在转换中，后续任务可以执行。
- 结果文件写入失败或不完整时不写成功缓存。
- 单体使用 fileId 网关读取，独立部署使用短期 token URL，均不绕过权限。
- 200MB 级 DOCX 测试不因一次性读入导致堆内存异常。
