# 文件上传表单接入

## 1. 适用场景

业务表单需要上传合同、图片、附件或导入文件，并在详情页回显、下载或预览。

## 2. 阅读顺序

| 顺序 | 文档                                                                               | 关注点                                   |
| ---- | ---------------------------------------------------------------------------------- | ---------------------------------------- |
| 1    | [File 后端 README](../../../mango/mango-platform/mango-file/README.md)             | 存储配置、文件记录、下载接口、数据库资源 |
| 2    | [Fileproc README](../../../mango/mango-infra/mango-infra-fileproc/README.md)       | 文件渲染、转换、Aspose 配置              |
| 3    | [File Preview README](../../../mango/mango-platform/mango-file-preview/README.md)  | 预览 token、预览页面、下载边界           |
| 4    | [@mango/file README](../../../mango-ui/packages/file/README.md)                    | 前端组件、API 封装、页面 key             |
| 5    | [File Components README](../../../mango-ui/packages/file/src/components/README.md) | 上传、分类卡片、文件列表与预览组件用法   |
| 6    | [能力地图：文件上传到预览闭环](../../capabilities/README.md#3-组合接入入口)        | 组合验证入口                             |

## 3. 接入检查点

| 环节     | 检查点                                                                                                                                                 |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 后端依赖 | 业务后端引入 file 相关 starter，确认存储配置可用                                                                                                       |
| 业务表   | 业务表保存 fileId、fileIds 或业务附件关联表，不直接保存临时 URL                                                                                        |
| 上传接口 | 前端上传后拿到文件 ID，再随业务 Command/Request 一起提交                                                                                               |
| 回显     | 登录用户按文件 ID 查询元数据；业务页面仍自行控制附件入口，图片缩略图由 `MUpload` 按原始内容预览地址、存储公开访问地址或鉴权下载的临时 `blob:` 地址回显 |
| 预览     | 需要在线预览时确认 file-preview 能拿到源文件并生成预览 token                                                                                           |
| 删除     | 删除业务单据时区分业务解绑和物理文件清理                                                                                                               |

## 4. 最小业务样例

前端表单字段：

```vue
<script setup lang="ts">
import { ref } from "vue";
import { MUpload, FilePreviewPanel } from "@mango/file";
import "@mango/file/style.css";

const contractId = ref<string>();
const attachmentIds = ref<string[]>([]);
const previewFileId = ref<string>();
</script>

<template>
  <MUpload
    v-model="attachmentIds"
    :count="5"
    value-type="id"
    biz-type="contract"
    :biz-id="contractId"
  />
  <FilePreviewPanel v-if="previewFileId" :file-id="previewFileId" />
</template>
```

业务接口字段示例：

```java
public class CreateContractCommand {
    private String name;
    private List<String> attachmentIds;
}
```

业务表字段示例：

```sql
create table biz_contract_attachment (
  id bigint primary key,
  contract_id bigint not null,
  file_id bigint not null,
  purpose varchar(64),
  sort_no int
);
```

### 4.1 按资料分类上传

业务表单需要按资料类型分别展示卡片，并为每类资料配置必填、数量、格式或大小规则时，使用 `MangoAttachmentUploadGrid`。组件的 `v-model` 回写附件记录及 `categoryKey`，业务提交时仍按自身 Command 转换并保存文件 ID 或附件关联，不保存预览和下载地址。

组件的完整 Props、默认值、事件、插槽和 `validate()` 约定见 [`@mango/file` 组件 API](../../../mango-ui/packages/file/src/components/README.md)；详情页文件列表/预览弹框见 [`@mango/file` README](../../../mango-ui/packages/file/README.md)。本文只保留业务接入和验收口径，避免复制公共 API 表。

```vue
<script setup lang="ts">
import { ref } from "vue";
import {
  MangoAttachmentUploadGrid,
  type MangoAttachmentUploadCategory,
  type MangoAttachmentUploadFile,
} from "@mango/file";
import "@mango/file/style.css";

const files = ref<MangoAttachmentUploadFile[]>([]);
const categories: MangoAttachmentUploadCategory[] = [
  {
    key: "license",
    name: "营业执照",
    required: true,
    minFileCount: 1,
    maxFileCount: 1,
    formats: ["pdf"],
  },
];
</script>

<template>
  <MangoAttachmentUploadGrid
    v-model="files"
    :categories="categories"
    purpose="contract-material"
    access-level="PRIVATE"
  />
</template>
```

删除卡片中的文件只会解除当前 `v-model` 关系，不会删除文件中心中的物理文件。业务保存、解绑和物理清理由消费系统按自身生命周期处理。

## 5. 业务场景验收点

| 类别     | 检查项                                                                                                                                                                                 |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 存储配置 | 目标环境已配置可用存储，上传接口能写入文件记录；多个独立前端共用后端时优先使用 `PROXY`，每个前端通过同源 `/api` 访问文件接口                                                           |
| 权限基线 | 测试用户已登录；上传、回显、预览和下载无需额外角色权限，文件列表、归档、删除和管理配置仍按细粒度权限授权                                                                               |
| 租户数据 | 文件记录、业务单据和当前登录用户处于同一租户上下文                                                                                                                                     |
| 前端组件 | `MUpload` 返回 `fileId`、`fileIds` 或 token，详情页按文件 ID 回显；不要把 `previewUrl`、`downloadUrl` 或临时 `blob:` 地址提交给业务接口；`FilePreviewPanel` 不会把下载地址当作预览地址 |
| 菜单页面 | 使用文件中心管理页时，页面 key 和菜单 component 对齐                                                                                                                                   |
| 预览链路 | 启用预览时，file-preview 和 fileproc 依赖可用                                                                                                                                          |
| 业务语义 | 编辑、删除业务单据时，附件解绑或物理清理策略清晰                                                                                                                                       |

### 5.1 大文件和 HTTP IP 环境

文件管理员在“文件配置”页面维护当前租户的大文件策略：

| 配置                 | 说明                                                             |
| -------------------- | ---------------------------------------------------------------- |
| `multipartEnabled`   | 是否启用分片上传；关闭后文件走普通上传。                         |
| `multipartThreshold` | 进入分片上传的临界值，单位字节，默认 `20971520`（20 MiB）。      |
| `maxSize`            | 文件中心单文件上限；`MUpload` 与组件自身限制同时存在时取较小值。 |

业务页面不需要为 HTTP IP 单独配置 `MUpload`。浏览器没有 Web Crypto 时，组件会省略客户端 SHA-256，后端使用 `SERVER_CHUNK` 接收并在合并后计算哈希。本次上传不能在上传前命中秒传，完成后生成的哈希映射可供后续相同文件使用；HTTPS 或 localhost 环境仍保留客户端哈希、秒传和存储支持时的 `S3_MULTIPART`。

业务项目升级后至少验证：

1. HTTP IP 页面上传达到临界值的文件，不再出现 `crypto.subtle.digest` 异常。
2. 开启分片时上传会话按环境返回 `SERVER_CHUNK` 或 `S3_MULTIPART`，关闭分片时改走普通上传。
3. 临界值和 `maxSize` 修改后，新上传请求读取当前租户最新配置。
4. Spring/Tomcat、网关和反向代理的请求大小及超时足以承载普通上传或单个分片。

## 6. 最小闭环

1. 打开业务新增页。
2. 上传一个文件并保存业务单据。
3. 重新打开详情页，文件名称、大小、下载入口可见。
4. 点击预览或下载，直接使用文件查询返回的 `previewUrl`、`downloadUrl`。`PROXY` 模式下验证地址包含当前前端同源 `/api` 前缀，Nginx 转发时去掉该前缀；`DIRECT` 模式下验证 MinIO `publicEndpoint` 生成的跨域安全签名 URL 可访问，并在 24 小时过期后通过重新查询获得新链接。
5. 删除或编辑业务单据后，附件关系符合业务预期。

## 7. 后端打包附件

业务需要把表单附件、合同材料或归档材料按目录结构导出为一个 ZIP 时，业务后端依赖 `mango-file-api`，调用 `FileApi.packageFiles(FilePackageCommand)`，或通过文件服务 HTTP 入口 `POST /file/files/package` 发起打包。打包完成后文件中心会生成新的 ZIP 文件记录，业务表只保存返回的 ZIP `fileId` 或自己的归档记录。

`entries.path` 表示 ZIP 内部相对路径，可以使用 `${fileName}` 引用源文件记录的文件名，避免业务侧为了拼目录额外查询文件名。路径按安全相对文件路径填写，不传绝对路径、`..`、目录项、空路径或重复路径。

最小后端调用：

```java
FilePackageCommand command = new FilePackageCommand();
command.setFileName("contract-materials.zip");
command.setPurpose("contract-material-package");
command.setAccessLevel("PRIVATE");
command.setBizType("CONTRACT_MATERIAL_PACKAGE");
command.setBizId(contractId.toString());
command.setEntries(List.of(
        new FilePackageEntryCommand(fileId1, "01_签约资料/${fileName}"),
        new FilePackageEntryCommand(fileId2, "02_资料清单/配置的资料清单.xlsx")
));

FileRecordVO zipFile = fileApi.packageFiles(command).getData();
```

验收时除上传、回显、下载闭环外，还应确认 ZIP 中的目录结构、文件名、租户可见性和下载权限符合业务预期。

## 8. 后端合并 PDF 归档

业务需要把手机拍照上传的多张图片，或图片、PDF、Word 材料按顺序归档为一个 PDF 时，业务后端依赖 `mango-file-api`，调用 `FileApi.mergeToPdf(FileMergePdfCommand)`，或通过文件服务 HTTP 入口 `POST /file/files/merge-pdf` 发起合并。合并完成后文件中心会生成新的 PDF 文件记录，业务表只保存返回的 PDF `fileId` 或自己的归档记录。

首期输出目标格式固定为 `PDF`。文件服务会校验源文件属于当前租户可见且已完成状态；当前支持 PDF、JPG/JPEG、PNG、TIFF、DOC、DOCX，图片和 Word 会先转换为 PDF，再按 `entries` 顺序合并。

最小后端调用：

```java
FileMergePdfCommand command = new FileMergePdfCommand();
command.setFileName("contract-materials.pdf");
command.setPurpose("contract-material-pdf");
command.setAccessLevel("PRIVATE");
command.setBizType("CONTRACT_MATERIAL_PDF");
command.setBizId(contractId.toString());
command.setTargetFormat("PDF");
command.setEntries(List.of(
        new FileMergePdfEntryCommand(photoFileId, "现场照片"),
        new FileMergePdfEntryCommand(contractPdfFileId, "合同正文"),
        new FileMergePdfEntryCommand(wordFileId, "补充说明")
));

FileRecordVO pdfFile = fileApi.mergeToPdf(command).getData();
```

验收时除上传、回显、下载闭环外，还应确认 PDF 页面顺序、源文件租户可见性、生成 PDF 的预览/下载权限、以及不支持格式失败时不会生成半成品文件记录。

文件记录返回的 `previewUrl` 是原始文件内容预览地址，`downloadUrl` 是下载地址。前端文档预览组件、Office 转换和在线预览服务是另一条链路，业务页面需要时按文件 ID 使用 `FilePreviewPanel` 获取预览元数据，并由组件读取 `documentPreviewUrl`，不要把文档预览服务地址当作业务表字段保存。

## 9. 常见失败

| 现象                         | 优先检查                                                                                                                                                                |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 上传成功但业务保存后文件丢失 | 表单提交是否带 fileId，后端 Command/Request 是否接收并持久化                                                                                                            |
| 下载 404                     | fileId 是否存在，存储配置是否指向正确 bucket、目录或本地路径                                                                                                            |
| 预览失败                     | file-preview 依赖、转换配置、预览 token 和源文件读取权限                                                                                                                |
| 多租户下看不到文件           | 文件记录 tenantId、业务数据 tenantId、当前登录上下文是否一致                                                                                                            |
| 图片能下载但缩略图裂图       | 是否把受保护的 `previewUrl` 或 `downloadUrl` 直接交给 `<img>`；应升级并使用 `MUpload`，由组件按文件 ID 获取预览元数据或通过鉴权下载生成临时 `blob:` 地址                |
| 图片能下载但不能在线预览     | 前端是否使用预览入口，后端 MIME 类型和预览类型是否匹配                                                                                                                  |
| 点击预览触发下载             | 是否把 `/api/file/files/download` 或 `/file/files/download` 写入了 `previewUrl`；详情预览应使用有效 `previewUrl` 或文档预览服务链接，没有可用预览地址时展示下载查看提示 |
| ZIP 打包失败                 | `entries.path` 是否为空、重复、包含绝对路径或 `..`，源文件是否处于可下载的已完成状态                                                                                    |
| PDF 合并失败                 | `targetFormat` 是否为 `PDF`，源文件是否为 PDF、JPG/JPEG、PNG、TIFF、DOC、DOCX，fileproc 转换和 PDF 合并能力是否已配置，源文件是否处于可下载的已完成状态                 |

## 10. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-file -am test
mvn -f mango/pom.xml -pl mango-platform/mango-file-preview -am test
pnpm -F @mango/file build
pnpm -F @mango/file test
```

模块验证入口：

- [File 验证方式](../../../mango/mango-platform/mango-file/README.md)
- [File Preview 验证方式](../../../mango/mango-platform/mango-file-preview/README.md)
- [Frontend File 验证方式](../../../mango-ui/packages/file/README.md)
- [File Components 验证方式](../../../mango-ui/packages/file/src/components/README.md)

## 11. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [后端代码文件引用规则](../../../mango-pmo/rules/backend/01-code.md#51-文件引用规则)
- [后端 API 文件字段规则](../../../mango-pmo/rules/backend/03-api.md#22-文件字段规则)
- [前端文件上传与回显规则](../../../mango-pmo/rules/frontend/01-vue-code.md#41-文件上传与回显规则)

## 12. 历史变更

详细版本影响见[文件上传表单历史变更索引](../../changelog/business-integration/file-upload-form.md)。
