# 文件预览下载与 Token 持久化交付契约

## 1. 目标

修正文件预览读取源文件时复用外部下载语义的问题，并让预览 token 的 1 天有效期在单体服务重启后仍然有效。

## 2. 范围

- `mango-file-api` 增加服务内文件读取契约。
- `mango-file-core` 和 `mango-file-starter` 提供服务内读取实现。
- `mango-file-preview-core` 打开源文件时使用服务内读取契约。
- 单体应用启用 JDBC KV token-store。
- 补充服务内读取相关回归测试。

## 3. 不做什么

- 不新增 `mango-file-preview` 到 `mango-file-core` 的跨模块依赖。
- 不恢复或新增 `FileApiAdapter`。
- 不改动文件预览引擎格式支持范围。
- 不变更数据库表结构；复用已有 `infra_kv_entry` migration。

## 4. 设计输入

- 用户要求新增 `downloadForService` 或重载方法。
- 用户要求 token 安全先放宽，默认 1 天有效期。
- 用户指出内部下载不能按外部下载响应处理。

## 5. 设计说明

### 5.1 影响模块

- `mango-platform/mango-file/mango-file-api`
- `mango-platform/mango-file/mango-file-core`
- `mango-platform/mango-file/mango-file-starter`
- `mango-platform/mango-file/mango-file-starter-remote`
- `mango-platform/mango-file-preview/mango-file-preview-core`
- `mango-platform/mango-file-preview/mango-file-preview-starter`
- `mango-app/monolith/mango-monolith-app`

### 5.2 接口变化

- `FileApi` 新增 `downloadForService(Long id)` 默认方法，表达服务内读取文件内容。
- `IFileService` 新增 `downloadForService(Long id)`。
- 原 `download(Long id)` 保持兼容。

### 5.3 数据变化

- 无新增 DDL。
- 单体应用将 `mango.kv.store.type` 从 `memory` 调整为 `jdbc`，并启用 `mango.kv.capability.token-store`，复用 `infra_kv_entry`。

### 5.4 菜单/页面/权限变化

- 无菜单、页面、权限变更。

### 5.5 测试范围

- `FileApiTest` 验证 `downloadTo` 使用服务内读取。
- `FilePreviewServiceImplTest` 验证打开源文件使用 `downloadForService`。
- `mango:check` 覆盖受影响模块的边界与质量规则。

## 6. 风险与限制

- 远程 Feign 场景仍通过 HTTP 下载端点兜底，避免本次引入新的跨服务二进制传输协议。
- 单体 token 持久化依赖 `infra_kv_entry` 表和 JDBC 数据源可用。
- 历史无关测试失败不在本次修复范围。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 新增服务内下载方法 | 在 `FileApi`/`IFileService` 增加 `downloadForService`，原 `download` 保持兼容 | `FileApi.java`、`IFileService.java`、`FileServiceImpl.java`、`FileController.java` | 编译与 `FileApiTest` | DONE | `mvn ... -Dtest=FileApiTest,FilePreviewServiceImplTest ... test` |
| TASK-002 | 用户要求 | 文件预览不能按外部下载响应读取源文件 | `FilePreviewServiceImpl.openSource` 改为调用 `fileApi.downloadForService` | `FilePreviewServiceImpl.java` | `FilePreviewServiceImplTest.openSource_使用服务内下载读取源文件` | DONE | `mvn ... -Dtest=FileApiTest,FilePreviewServiceImplTest ... test` |
| TASK-003 | 用户要求 | token 默认 1 天有效，刷新/重启后不应提前失效 | 单体启用 JDBC KV token-store，并让文件预览自动配置排在 KV capability 后 | `application.yml`、`FilePreviewAutoConfiguration.java` | 配置走查与启动验证 | DONE | `mango.kv.store.type=jdbc`、`mango.kv.capability.token-store=true` |
