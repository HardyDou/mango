# mango-file

`mango-file` 是平台统一文件能力模块，负责文件上传、下载、记录查询、预览元数据和归档。旧项目 `mango-file` 只作为能力参考，本模块按当前 `io.mango` 分层、权限模型、机构隔离和 Flyway 规范重新实现。

## 模块结构

- `mango-file-api`：文件业务码、枚举、Command、Query、VO、模块 API。
- `mango-file-core`：文件记录持久化、文件服务、本地存储实现、Flyway 迁移。
- `mango-file-starter`：自动配置、Web Controller、Swagger/Knife4j 模块声明。

## P0 能力

- `POST /file/files`：单文件上传。
- `POST /file/files/batch`：多文件上传。
- `GET /file/files/page`：文件记录分页。
- `GET /file/files/detail?id=1`：文件详情。
- `GET /file/files/preview?id=1`：预览元数据。
- `GET /file/files/download?id=1`：文件下载。
- `DELETE /file/files?id=1`：归档文件记录，不物理删除对象。
- `GET /file/storage-configs/page`：文件存储配置分页。
- `GET /file/storage-configs/detail?id=1`：文件存储配置详情。
- `POST /file/storage-configs`：新增文件存储配置。
- `PUT /file/storage-configs`：修改文件存储配置。
- `DELETE /file/storage-configs?id=1`：删除文件存储配置。
- `PUT /file/storage-configs/active?id=1`：设为默认文件存储配置。
- `POST /file/storage-configs/test`：测试文件存储配置。
- 默认本地存储，支持 S3 兼容、MinIO、AWS S3、阿里云 OSS、腾讯云 COS、七牛云 Kodo。
- 文件记录按当前登录机构隔离，技术字段为 `tenant_id`，用户可见语义为“机构”。
- 存储配置是平台级基础设施配置，不按机构隔离；文件对象路径和文件记录继续按机构隔离。
- Knife4j/Swagger 分组路径：`/file`。

## 机构隔离

文件记录表为 `sys_file_record`，包含 `tenant_id`。上传时从当前登录上下文写入机构 ID，查询、详情、下载、归档均按当前机构范围访问。

文件对象路径包含机构前缀：

```text
tenant-{tenantId}/yyyy/MM/dd/{uuid}.{ext}
```

当前 P0 实现不提供匿名公开下载，也不绕过机构隔离做平台全量查看。`PUBLIC_READ` 访问级别先保留为元数据能力，真正匿名访问、签名 URL、跨机构授权在 P1/P2 完成。

## 配置

配置前缀：`mango.file`。

```yaml
mango:
  file:
    enabled: true
    storage-type: LOCAL
    default-bucket: local
    local:
      root-path: ./data/files
    upload:
      max-size: 104857600
      allowed-extensions: []
      blocked-extensions:
        - exe
        - bat
        - cmd
        - sh
        - jar
```

本地存储路径格式：

```text
{root-path}/{bucket}/tenant-{tenantId}/yyyy/MM/dd/{uuid}.{ext}
```

## 云存储配置

第三方存储配置保存在 `sys_file_storage_config`，后台页面为“系统管理 / 文件存储配置”。配置中最多一个启用为默认，后续上传使用默认配置；文件记录会写入 `storage_config_id`、`storage_type`、`bucket_name`、`object_name`，因此切换默认存储后历史文件仍按原配置读取。

支持类型：

- `LOCAL`：本地磁盘。
- `S3`：S3 兼容对象存储。
- `MINIO`：MinIO，建议开启 Path Style。
- `AWS_S3`：AWS S3。
- `ALIYUN_OSS`：阿里云 OSS。
- `TENCENT_COS`：腾讯云 COS。
- `QINIU_KODO`：七牛云 Kodo。

本地 MinIO 联调：

```bash
docker run --name mango-minio -d \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

后台新增配置：

- 存储类型：`MINIO`
- 接入地址：`http://127.0.0.1:9000`
- 存储桶：`mango-file`
- AccessKey：`minioadmin`
- SecretKey：`minioadmin`
- Path Style：开启
- 设为默认：开启

SecretKey 不会在详情接口明文返回；编辑时留空表示不修改。

## 前端接入

前端通用上传组件默认接入 `/api/file/files`：

- `ImageUpload`
- `FileUpload`
- `ExcelUpload`
- `Upload`

组件默认返回 `mango-file:{id}` 文件标识，方便已有表单字段继续用字符串保存。需要完整文件记录时，`ImageUpload` 和 `FileUpload` 可设置 `value-type="record"`，返回值包含 `id`、`fileName`、`fileSize`、`contentType`、`objectName`、`url`。

`ExcelUpload` 会同时完成两件事：

- 上传原始 Excel 文件到统一文件模块。
- 前端本地解析 Excel 内容并通过 `v-model` 返回预览数据。

Excel 原始文件记录通过 `upload-success` 事件返回。

文件管理页面路径：`packages/system/src/views/file/index.vue`。

下载必须通过统一 request 发起 blob 请求，不能使用 `window.open('/api/file/files/download?id=1')`。原因是当前认证过滤器读取 `Authorization` 头，浏览器直接打开地址不会自动追加该头。

## 后续计划

P1：

- 签名 URL。
- MIME、大小、扩展名白名单。
- `PUBLIC_READ` 匿名读取。
- `INTERNAL` 内部访问边界。
- 文件下载审计。

P2：

- 分块上传。
- 断点续传。
- 云存储直传。
- 批量 ZIP 下载。
- 生命周期清理。
- 内容安全/病毒扫描扩展点。
