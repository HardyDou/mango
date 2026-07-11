# 文件访问权限基线详细设计

## 1. 目标

解决已登录用户因缺少 `file:files:query/upload/download` 角色权限而无法上传、预览或下载的问题，同时保持文件查询直接返回可使用的预览和下载链接。

## 2. 设计约束

- 不新增文件访问 token、grant 或重复的访问接口。
- 复用 `mango-file` 现有 `PROXY/DIRECT` 访问模式和各存储适配器的安全签名能力。
- 文件分页、详情、上传结果和预览查询继续返回 `previewUrl`、`downloadUrl`。
- 默认使用 `DIRECT` 模式，并强制签名访问；预览和下载签名有效期统一为 `86400` 秒。
- MinIO/S3、OSS、COS、七牛等适配器返回浏览器可直接访问的跨域绝对 URL。
- 业务表和 `MUpload` 的 `v-model` 只保存 `fileId`/`fileIds`，不保存运行时 URL。

## 3. 权限矩阵

| 能力 | 模式 |
|---|---|
| 详情、上传、批量、秒传、分片、预览元数据、下载、打包、合并、运行时设置读取 | `LOGIN` |
| 文件列表、归档、删除、目录和管理配置 | `PERMISSION` |
| DIRECT 签名 URL 读取 | 由对象存储验证签名和有效期，不经过 Mango 登录鉴权 |

`LOGIN` 只控制签名链接的获取。文件服务仍在签名前按当前租户查询文件，不能跨租户获取其他文件的地址。

## 4. 查询与链接处理

`FileServiceImpl.toVO()` 为分页、详情、上传及衍生文件结果调用 `fillDirectAccess(FileRecordVO, ...)`：

1. DIRECT 模式解析文件对象与存储配置。
2. `accessTokenEnabled=true` 或文件访问级别要求签名时，调用存储适配器的 `presignedGetUrl`、`presignedDownloadUrl`。
3. `previewUrl` 返回可直接内联读取的签名地址。
4. `downloadUrl` 返回带下载语义的签名地址。
5. `directPreviewExpireSeconds`、`directDownloadExpireSeconds` 记录 86400 秒有效期。
6. 地址过期后，调用方重新查询文件记录或预览元数据获取新地址。

PROXY 地址仅作为未启用 DIRECT 或存储无法生成直连地址时的兼容回退，不作为跨域组件的首选链接。

## 5. 前端组件

- `MUpload` 上传成功或加载已有记录后，直接使用返回的 `previewUrl` 做图片回显，使用 `downloadUrl` 做下载。
- `FilePreviewPanel` 对图片、PDF、音视频直接使用 `previewUrl`；Office 等格式继续使用 `documentPreviewUrl`。
- 前端不拼接签名参数、不保存签名 URL、不把下载 URL 当预览 URL。
- 去除上传、预览、下载按钮的旧 `v-auth`，归档和删除按钮继续鉴权。

## 6. 配置与升级

默认文件设置资源升级版本，统一声明：

| 配置 | 默认值 |
|---|---|
| `accessMode` | `DIRECT` |
| `accessTokenEnabled` | `true` |
| `publicReadRequiresToken` | `true` |
| `accessTokenExpireSeconds` | `86400` |
| `previewExpireSeconds` | `86400` |

## 7. 验证

- 无文件角色权限的登录用户可上传并获得文件记录。
- 分页、详情、上传结果和预览查询均返回非空、可直接访问的 `previewUrl`、`downloadUrl`。
- DIRECT 地址为存储域跨域签名 URL，浏览器直接预览和下载不返回 401。
- 签名有效期为 24 小时，过期地址失败，重新查询可获得新地址。
- 文件列表、归档、删除和管理配置仍要求原权限。
