# Issue #316 文件服务 ZIP 打包交付台账

日期：2026-06-29

| 项 | 状态 | 说明 |
|---|---|---|
| 需求澄清 | 完成 | 同步生成 ZIP，写回存储层，返回新的 `FileRecordVO`；异步预创建 ID 后续扩展。 |
| API 契约 | 完成 | 新增 `FilePackageCommand`、`FilePackageEntryCommand`、`FileApi.packageFiles`。 |
| 服务实现 | 完成 | `FileServiceImpl.packageFiles` 生成 ZIP 并复用 `save` 保存。 |
| HTTP 入口 | 完成 | `POST /file/files/package`。 |
| 远程调用 | 完成 | `FileFeignClient.packageFiles`。 |
| 路径安全 | 完成 | 拒绝绝对路径、路径穿越、目录项、空段和重复路径。 |
| 状态隔离 | 完成 | 服务端下载只允许 `COMPLETED` 文件。 |
| 文档 | 完成 | 更新 `mango-file/README.md`，新增计划和详细设计。 |
| 测试 | 完成 | 新增完整特性测试覆盖成功生成、非法路径和重复路径。 |
