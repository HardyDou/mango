# Mango File

## 1. 能力定位

`mango-file` 提供平台统一文件资产能力，负责上传、下载、预览元数据、文件记录、逻辑目录、存储配置、运行时策略、秒传和分片上传。主要使用者是业务模块、管理端文件中心和需要统一对象存储接入的 Mango 开发者。

代码事实：

- 聚合模块 `io.mango.platform.file:mango-file`。
- 子模块包括 `mango-file-api`、`mango-file-core`、`mango-file-starter`、`mango-file-starter-remote`。
- HTTP 路径覆盖 `/file/files`、`/file/storage-configs`、`/file/settings`、`/file/directories`、`/file/local-objects`。
- 远程 Feign Client 服务名为 `mango-file`，路径为 `/file/files`。

## 2. 适用场景

- 业务附件、图片、导入文件、审批附件等统一上传和下载。
- 文件记录按租户隔离，底层对象按存储配置写入本地或对象存储。
- 管理文件存储配置、默认存储、文件中心运行时策略和逻辑目录。
- 大文件秒传、分片上传、预签名直传或后端接收分片。

## 3. 不适用场景

- 不负责 Office 转 PDF、OCR、模板渲染、在线编辑等文档处理能力。
- 受控文件记录下载不绕过登录态和租户边界；`/file/local-objects` 是本地对象公开读取路径，需要按存储策略单独评估暴露范围。
- 不替代业务模块的附件关联表和业务状态流转。
- 不把完整文件读入内存作为通用处理方式。

## 4. 模块边界

`mango-file` 管理文件资产元数据、底层对象、存储配置和访问入口。业务模块负责保存文件与业务单据的关系；文档处理模块负责转换、合并、OCR 等加工能力，加工结果可回写文件中心。

## 5. 接入方式

本地文件服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter</artifactId>
</dependency>
```

远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter-remote</artifactId>
</dependency>
```

只使用契约模型时依赖 `mango-file-api`。

`mango-file-starter-remote` 的 Feign 入口主要覆盖 `/file/files` 对应的 `FileApi` 文件契约，不等同于远程开放 storage-configs、settings、directories 等管理接口；这些管理能力应在文件服务本地侧验收。

## 6. 配置项

已发现配置前缀：

- `mango.file`：文件模块配置，来源 `FileProperties`。
- `mango.file.enabled`：`FileAutoConfiguration` 条件开关，默认匹配启用。

存储配置也可通过文件中心运行时表维护，代码支持本地、S3 兼容、Aliyun OSS、Tencent COS、Qiniu Kodo 等实现。

关键配置族包括 local、upload、access、preview、publicBaseUrl 等文件访问、上传和预览策略。

## 7. 对外接口 / 扩展点

- `FileApi`：文件上传、下载、查询、预览和删除契约。
- Controller：`FileController`、`FileStorageConfigController`、`FileSettingsController`、`FileDirectoryController`、`LocalFileObjectController`。
- Feign：`FileFeignClient`。
- 分片上传、秒传、上传会话、分片签名、完成和取消由 HTTP Controller / `IFileService` 运行时能力提供。
- 枚举覆盖访问模式、上传模式、存储类型、命名策略等文件中心运行时策略。

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-file-core/src/main/resources/db/migration/file`。

核心表：

- `file_record`
- `file_storage_config`
- `file_settings`
- `file_directory`
- `file_object`
- `file_hash_mapping`
- `file_upload_session`
- `file_upload_part`

仓库内存在 `mango-file/docker-compose.yml`，用于文件能力相关本地依赖启动。

## 9. 菜单 / 权限 / 租户

文件记录带租户边界，存储配置是平台级基础设施配置。文件中心菜单和按钮权限属于 file 能力资产；历史 SQL 或 authorization 迁移只是历史事实，新资产应按模块菜单规范通过本模块资源清单或 migration 维护。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-file -am test
```

代表性接口验收：

- `POST /file/files` 上传后可查询详情。
- 批量上传、预览、归档、删除、目录树、settings 和存储配置 test 接口应覆盖。
- `GET /file/files/download` 可下载当前租户有权访问的文件。
- `POST /file/files/uploads` 初始化分片上传。
- 分片上传应覆盖初始化、签名、上传分片、完成和取消闭环。
- `PUT /file/storage-configs/active` 切换默认存储配置。

## 11. 业务接入最小闭环

业务模块保存附件关系时只保存业务单据与 `fileId`、用途、排序等关系，不复制文件中心对象路径或存储凭据。普通上传走 `POST /file/files`，下载走 `/file/files/download`，大文件按初始化上传会话、上传或直传分片、完成会话的顺序处理。

接入前先配置默认存储和上传限制，必要时通过存储配置 test 验证 endpoint、bucket 和凭据。验收断言覆盖：当前租户上传后可下载，跨租户不能访问，归档后下载被拒，分片上传完成后生成文件记录，业务删除附件关系不等于删除底层文件资产。

## 12. 常见问题

- 下载失败先检查文件记录是否归档、租户是否匹配、底层对象是否存在。
- 大文件优先使用分片上传或对象存储直传，不应通过业务后端完整中转。
- 存储配置测试失败时分别检查 endpoint、bucket、凭据、路径前缀和网络连通性。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
