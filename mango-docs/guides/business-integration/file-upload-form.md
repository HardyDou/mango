# 文件上传表单接入

## 1. 适用场景

业务表单需要上传合同、图片、附件或导入文件，并在详情页回显、下载或预览。

## 2. 阅读顺序

| 顺序 | 文档                                                                               | 关注点                                     |
| ---- | ---------------------------------------------------------------------------------- | ------------------------------------------ |
| 1    | [File 后端 README](../../../mango/mango-platform/mango-file/README.md)             | 存储配置、文件记录、下载接口、数据库资源   |
| 2    | [Fileproc README](../../../mango/mango-infra/mango-infra-fileproc/README.md)       | 文件渲染、转换、Aspose 配置                |
| 3    | [File Preview README](../../../mango/mango-platform/mango-file-preview/README.md)  | 预览 token、预览页面、下载边界             |
| 4    | [@mango/file README](../../../mango-ui/packages/file/README.md)                    | 前端组件、API 封装、页面 key               |
| 5    | [File Components README](../../../mango-ui/packages/file/src/components/README.md) | `MUpload`、`FilePreviewPanel` 用法和 props |
| 6    | [能力地图：文件上传到预览闭环](../../capabilities/README.md#3-组合接入入口)        | 组合验证入口                               |

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

## 8. 变更影响记录

- 2026-08-02 Issue #690：文件上传、回显、预览、下载、ZIP/PDF 接口、fileId 持久化和租户权限合同不变。本次影响是应用初始化与制品消费边界：使用 Maven `1.0.30`/`1.0.3x` 的业务仓升级时按新 release tuple 成组更新，并验证 Boot JAR、Bootstrap receipt、File 资源声明和预览引擎注册结果；已有文件记录、业务附件关系和文件内容不重建。

- 2026-07-30：`FilePreviewPanel` 新增默认关闭的 `fit-container` 容器填充模式。固定高度容器、`ElDialog` 或
  `MangoDialog` 正文可以直接传入该 prop，使 PDF、Office、图片和音视频随容器缩放，无需业务页面监听 resize
  或组合预览高度变量。未传入时继续使用自然高度；文件 ID 持久化、上传/预览/下载 API、权限、租户、文档转换
  接口和本场景验收步骤不变。

- 2026-07-30：`FilePreviewPanel` 的图片预览默认直接展示无蒙层的内嵌 Element Plus Image Viewer，缩放、旋转、
  适应模式、原始尺寸和拖拽工具栏进入预览区后即可使用，不再点击图片打开第二层覆盖层。业务接入不需要增加 prop；
  文件 ID 持久化、上传/预览/下载 API、权限、租户和文档转换接口不变。

- v2026.07.08-admin-page-layout-release 只发布后台统一页面骨架组件、运营列表页 CLI/starter 模板和前端 npm 版本锁；不改变文件上传、回显、下载、预览、文件权限、租户隔离、业务表保存 fileId/fileIds 的接入方式和本场景验收步骤。业务项目升级时按发布说明成组升级前端 `@mango/*` 包和 `@mango/cli`。

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

- [File 验证方式](../../../mango/mango-platform/mango-file/README.md#10-验证方式)
- [File Preview 验证方式](../../../mango/mango-platform/mango-file-preview/README.md#10-验证方式)
- [Frontend File 验证方式](../../../mango-ui/packages/file/README.md#10-验证方式)
- [File Components 验证方式](../../../mango-ui/packages/file/src/components/README.md#8-验证方式)

## 11. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [后端代码文件引用规则](../../../mango-pmo/rules/backend/01-code.md#51-文件引用规则)
- [后端 API 文件字段规则](../../../mango-pmo/rules/backend/03-api.md#22-文件字段规则)
- [前端文件上传与回显规则](../../../mango-pmo/rules/frontend/01-vue-code.md#41-文件上传与回显规则)

## 12. 变更影响记录

- Issue #639 修复 HTTP IP、非安全上下文中大文件上传因 Web Crypto 不可用而中断的问题。文件管理员可配置 `multipartEnabled` 和 `multipartThreshold`；`MUpload` 自动读取当前租户策略和 `maxSize`。无客户端哈希时固定走 `SERVER_CHUNK`，服务端完成后补算 SHA-256；业务表单仍只保存 `fileId`/`fileIds`，无需增加组件属性。业务项目需在后续发布批次整体升级 `@mango/file`、固定版本前端消费者和对应后端 Maven 物料，并执行本节 HTTP IP 验收步骤。

- v2026.07.21-maven-1.0.25-cli-1.0.89-branding-workflow-bom-release 将 `mango-file-preview-engine` 的既有依赖版本交由同版本 `mango-bom` 统一管理，不改变文件上传、回显、预览、下载的公开 API、配置、权限、租户、页面入口、启动方式或本场景验收步骤。继承 `mango-parent` 的业务项目只需整体升级 Mango 版本；使用自有 parent 的项目导入 `io.mango:mango-bom:1.0.25`。

- Issue #553 将 `/file-preview/files/preview-link` 和 `/file-preview/files/preview` 与现有文件上传、详情、预览内容和下载基线对齐为登录可用，业务表单无需再为统一预览链接配置 `file:files:download` 角色权限。链接生成仍先按当前租户查找文件，匿名请求不能生成预览 token；文件列表、归档、删除、目录和管理配置权限不变。前端 `/api` 仍只是代理路径标识，后端实际路径不带 `/api`。

- Issue #563 修复 Office 文件名同时包含 URL 编码字节和括号等原始字符时的 PDF 预览失败。File Preview 内部改用基于 `fileId` 的 ASCII 转换名，并通过与源文件 token 绑定的同源接口读取转换 PDF。业务表单仍只保存 `fileId`/`fileIds`，上传、回显、预览入口、下载、权限、租户和本指南的验收步骤不变；涉及中文、空格或括号的 Word 附件时，确认 PDF.js 能渲染实际页面且网络请求不出现 `%25` 二次编码。

- PR #565 补充多个独立前端共用同一后端时的文件访问约定：推荐将运行时文件访问模式设为 `PROXY`，各前端只消费当前 Origin 下带 `/api` 的 `previewUrl`、`downloadUrl`，由各自 Nginx 去掉 `/api` 后转发到后端，避免 8081、8082、8083 之间跨域访问。确需 `DIRECT` 时，MinIO 配置稳定的 `publicEndpoint`，签名地址中的 host、port、path 和 query 保持原样，并为实际前端 Origin 配置 bucket CORS。业务表仍只保存 fileId/fileIds；已有租户配置不会被默认资源覆盖。

- PR #454 将后端错误码类型的 Java 导入路径从 `io.mango.file.api.FileCode` 调整为
  `io.mango.file.api.enums.FileCode`；业务后端如直接引用错误码需更新 import。错误码数值和消息、文件上传与去重
  行为、HTTP/API 契约、fileId/fileIds 持久化、权限、租户、页面入口、启动方式及本场景验收步骤不变，不提供旧包
  兼容类型。

- PR #451 清理 file-preview engine 未被源码使用且目标仓库不可解析的旧 `javax.media:jai_core`、`javax.media:jai_codec` 构建依赖；现有源码编译和模块测试继续通过。该内部依赖清理不改变文件上传、回显、预览、下载的公开 API 与配置，不改变菜单、权限、租户、页面入口、启动方式、运行时行为或本场景验收步骤。

- v2026.07.11-maven-1.0.14-cli-release 仅将当前后端实现向前发布为 Maven `1.0.14` 并更新 CLI 后端版本锁；不改变文件上传、回显、预览、下载 API、fileId/fileIds 持久化、权限、租户或本场景验收步骤。

- v2026.07.11-npm-readme-forward-release 以 `@mango/file@1.0.21` 向前发布已更正的 README 和精确依赖，不回退 `1.0.20` 的运行时变更。`FilePreviewPanel` 不再接受 `downloadPermission` prop，业务页面升级时需要删除该 prop；上传、预览和下载入口由登录态与后端文件访问校验决定，不再使用 `file:files:upload/query/download` 前端按钮权限隐藏。列表、归档、删除、目录和管理配置的细粒度权限不变。

- PR #433 统一文件访问基线：详情、上传、批量、秒传、分片、预览、下载、打包、合并和运行时设置读取改为登录即可，不再依赖默认角色注入或 `file:files:query/upload/download` 权限码。文件分页、详情、上传结果和预览查询继续直接返回可使用的 `previewUrl`、`downloadUrl`；DIRECT 模式沿用存储适配器的跨域安全签名策略，有效期统一为 24 小时。文件列表、归档、删除、目录和管理配置继续使用细粒度权限，业务表单仍只保存 fileId/fileIds。

- PR #439 优化 `FilePreviewPanel` 弹性高度和文件管理页预览弹框布局，并补充组件入口 README 中 `MUpload` 与 `FilePreviewPanel` 的选型边界；文件上传表单的 fileId/fileIds 保存方式、上传/下载/预览 API、权限、租户、页面入口、启动方式和本场景验收步骤不变。业务页面已使用 `FilePreviewPanel` 时可沿用文件 ID 接入方式；需要弹框承载时，由外层弹框负责标题和尺寸，预览内容继续交给 `FilePreviewPanel`。

- Issue #431 新增 Excel 导入失败工作簿保存桥接。`BaseCrudController` 存在行级导入错误且装配 `mango-file-starter` 时，会以 `PRIVATE`、`EXCEL_IMPORT` 用途保存失败 `.xlsx`，并在 `ImportResult.failureFileId` 返回文件 ID；下载继续经过既有文件权限和租户隔离。普通文件上传表单的 fileId/fileIds 持久化、回显、预览和下载接口不变。

- PR #386 简化 `@mango/file` 前端 `FileRecord` 公共类型，只保留业务可见字段并移除存储层和直连访问字段；文件上传表单仍按 `fileId`、`fileIds` 或文件 token 保存业务值，上传、回显、预览、下载 API、权限、租户、页面入口、启动方式和本场景验收步骤不变。业务前端如曾读取 `storageType`、`bucketName`、`objectName`、`url`、`directPreviewUrl` 或 `directDownloadUrl`，升级后应改为使用 `previewUrl`、`downloadUrl` 或按文件 ID 调用预览/下载能力。

- Issue #382 新增 `FileApi.mergeToPdf` 和 `POST /file/files/merge-pdf`，业务后端可以把多个已存在图片、PDF、Word 文件按顺序生成 PDF 并保存为新的文件记录；输出目标格式首期仅支持 `PDF`。文件上传、预览、下载、前端组件、菜单、权限和租户基础规则不变。业务验收需要额外确认 PDF 页面顺序、源文件状态隔离、生成 PDF 的预览/下载权限，以及不支持格式失败时不会生成半成品。

- v2026.07.02-maven-1.0.6-home-widgets-cli-release 仅发布首页小组件归属拆分、CLI 版本锁和 generated backend baseline 修复；`@mango/file@1.0.15` 只是随批次对齐依赖版本，不改变文件上传、下载、预览 API、组件用法、业务表保存方式、权限、租户、页面入口、启动方式和本场景验收步骤。业务项目升级时按发布说明成组升级后端 `<mango.version>`、前端 `@mango/*` 包和 `@mango/cli`。

- v2026.06.30-maven-1.0.1-admin-branding-cli-release 发布固定后端 Maven `1.0.1` 和 `@mango/file@1.0.14` 前端批次，仅对齐文件组件回显修复、npm 物料和 CLI/starter 版本锁；不改变文件上传、下载、预览 API、业务表保存方式、权限、租户、页面入口、启动方式和表单验收步骤。业务项目应成组升级本发布批次的后端 `<mango.version>` 和前端 `@mango/*` 包。

- Issue #337 修复 `FilePreviewPanel` 预览地址与下载地址混用问题：详情预览区域只使用有效 `previewUrl`、预览元数据中的存储公开预览地址或文档预览服务链接，不再使用 `downloadUrl` 或 `/api/file/files/download` 作为内联预览兜底；下载入口和上传回显策略不变。业务验收需要额外确认图片/PDF/音视频可正常预览，只有下载地址时页面展示下载查看提示，点击预览不再触发浏览器自动下载。

- Issue #332 修复文件下载响应头文件名二次编码问题，中文、`+` 等字符在浏览器下载保存时应显示为原始文件名；不改变上传、fileId 持久化、详情回显、预览/下载 API、权限、租户、页面入口、启动方式和表单验收步骤。业务验收仍按本指南最小闭环执行，涉及中文附件名或 ZIP 文件名时确认下载后的本地文件名可读。

- PR 本次后台品牌配置修复同步调整 `MUpload` 图片缩略图回显策略：业务值仍只保存文件 ID、文件 token 或文件记录；组件优先使用文件记录的 `previewUrl`，必要时按文件 ID 获取预览元数据，没有可直接展示地址时通过鉴权下载生成临时 `blob:` 地址显示缩略图。文件上传、下载、在线预览 API、业务表保存方式、权限资源、租户隔离和接入代码不变。业务验收需要额外确认上传后立即回显、刷新后按文件 ID 回显、无存储公开访问地址时图片缩略图不裂图。

- PR #329 新增文件下载压缩参数，业务调用文件服务下载或 ZIP 打包时可以为图片和 PDF 设置压缩档位，并可用 `perFileTargetSizeBytes` 或 entry 级 `targetSizeBytes` 指定单文件目标大小；该目标不表示 ZIP 总大小。文件上传、fileId 持久化、详情回显、预览入口、权限资源、租户隔离和前端 `MUpload` 接入方式不变。业务验收需要额外确认压缩后的图片/PDF 可打开、未支持格式在 ZIP 中保持原内容、每个 entry 的压缩参数只影响对应源文件。

- PR #319 新增 `FileApi.packageFiles` 和 `POST /file/files/package`，业务后端可以把多个已存在文件按 `entries.path` 生成 ZIP 并保存为新的文件记录；文件上传、预览、下载、前端组件、菜单、权限和租户基础规则不变。业务验收需要额外确认 ZIP 内部目录结构、`${fileName}` 替换结果、路径安全校验和生成 ZIP 的下载权限。

- PR #280 将文件详情、下载、预览和设置读取等已登录用户可用接口标记为 `LOGIN` 资源，业务表单不需要再为这些通用文件读取接口配置角色或用户授权；业务页面入口、业务数据可见性、上传、归档、删除和设置保存仍按原有业务权限、租户与数据权限控制。文件上传表单的 fileId 持久化、详情回显、预览和下载验收步骤不变。

- v2026.06.27-system-component-release 同步发布 `@mango/file@1.0.13` 及其前端依赖批次，仅对齐 npm 物料和 CLI/starter 版本锁；不改变文件上传组件用法、上传/下载/预览 API、存储配置、业务表保存方式、权限边界、页面入口和本场景验收步骤。业务项目排查上传表单异常时，仍先确认前端包批次一致、后端 file starter 已引入、存储配置可用。

- Issue #264 发布 `@mango/file@1.0.12` 并随前端发布批次对齐 `@mango/admin-pages@1.0.11`；不改变文件上传、下载、预览的公开 API、前端组件、配置、权限、租户、页面、启动方式和表单验收步骤。本次仅同步发布锁和 package 边界，业务项目继续通过 `@mango/file` 公开入口和 `@mango/file/style.css` 接入。
- PR #216 加固前端 `@mango/*` npm 包发布边界，非 CLI 包不再发布 `src` 等源码目录，并补充发布包 tarball 和业务消费 typecheck 基线；不改变文件上传、下载、预览的公开 API、前端组件、配置、权限、租户、页面、启动方式和表单验收步骤。业务项目应继续使用公开 package 入口和样式入口，升级到后续发布的新包版本后重新运行前端 typecheck。

- PR #199 加固 Resource Registry runtime、远程上报和能力 app 注入链路，并保持 file/file-preview 能力通过声明方式初始化菜单、权限和默认资源；不改变文件上传、下载、预览的公开 API、前端组件、存储配置、租户隔离和表单验收步骤。清库重建或 1.0 rebase 升级后，排查文件中心菜单、下载/预览权限或默认文件配置缺失时，需要同时确认 `AUTH_MENU`、`API_RESOURCE`、`FILE_STORAGE_CONFIG` 和 `FILE_SETTINGS` 声明同步成功。
- PR #195 加固前端 `@mango/*` 包的 `exports`、`types` 和生成声明文件，使业务项目通过发布后的 `dist` 产物独立消费；不改变文件上传、下载、预览的公开 API、前端组件、配置、权限、租户、页面、启动方式和表单验收步骤。业务项目应继续使用公开 package 入口和 `./style.css`，不要依赖包内 `src` 路径。
- PR #194 发布资源注册中心版本并升级 `@mango/file@1.0.11`、`@mango/admin@1.0.23`、`@mango/common@1.0.10`、`@mango/cli@1.0.34` 等前端包；不改变文件上传、下载、预览的公开 API、前端组件、权限、租户、页面、启动方式和表单验收步骤。业务升级时应成组升级前端 `@mango/*` 包并刷新后端 Mango `1.0.0-SNAPSHOT` 依赖。
- PR #193 新增 `mango-resource` 注册中心并将文件存储配置、文件设置默认数据迁移为资源声明同步；不改变文件上传、下载、预览的公开 API、前端组件、权限、租户、页面和表单验收步骤。排查默认存储配置缺失时，需要同时确认 `FILE_STORAGE_CONFIG` 和 `FILE_SETTINGS` 声明是否已同步。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变文件上传、下载、预览的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变文件上传、下载、预览的公开 API、配置、权限、租户、页面、启动和运行时行为。

- Issue #322 仅放宽 Mango 前端包在当前已认证主版本内的 `peerDependencies` 范围，并明确 `pinia@3`、`vue-i18n@10+`、`vue-router@5` 暂未纳入当前认证范围；不改变文件上传、下载、预览的公开 API、前端组件、fileId 持久化、权限、租户、页面、启动方式和表单验收步骤。业务项目安装依赖时如出现 peer warning，应先按 `mango-ui/README.md` 的认证范围对齐前端包批次，文件上传表单异常仍按文件服务、组件接入和业务字段持久化链路排查。

- v2026.07.04-maven-1.0.8-platform-release 精简文件记录返回字段，业务表单仍只保存文件 ID、文件 token 或文件记录；详情回显、预览和下载应读取文件服务返回的 `previewUrl` 与 `downloadUrl`，不要依赖 `url`、`directPreviewUrl`、`directDownloadUrl`、bucket 或 objectName 等存储层字段。文件上传组件接入方式、权限资源、租户隔离、页面入口和本场景验收步骤不变；文档预览类页面继续按文件 ID 获取预览元数据，`FileRecordVO.previewUrl` 仅表示文件服务预览地址。

- v2026.07.07-maven-1.0.13-menu-api-codes-release 仅发布 menuCode/apiCodes 权限模型的 Maven、npm 和 CLI 版本批次；不改变文件上传、下载、预览的公开 API、前端组件、fileId 持久化、权限、租户、页面入口、启动方式和表单验收步骤。文件基础接口仍由默认角色权限承载，业务单据的查看、编辑、归档和删除仍由业务菜单权限、数据权限和租户隔离控制。

- v2026.07.11-npm-lock-sync-release 发布 `@mango/file@1.0.20`：文件管理页上传、预览和下载操作不再依赖
  `file:files:upload`、`file:files:query` 或 `file:files:download` 前端角色权限码，改为由登录态和后端文件访问
  校验决定；列表、归档、删除、目录、存储配置和文件设置仍使用既有细粒度权限。`FilePreviewPanel` 不再支持
  `downloadPermission` prop，业务页面应移除该 prop。文件 ID 持久化、上传/下载/预览 API、租户边界、页面入口和
  启动方式不变。

- Resource 历史债务治理仅迁移 File 资源 Handler 的本地 SPI 依赖，不改变上传、下载、预览 API、文件 ID 持久化、访问权限、租户边界、页面入口、启动方式和本场景验收步骤。

- File Preview 历史债务治理将预览页读取源文件的内部地址调整为 `/file-preview/sources?token=...`，并分离页面与二进制流适配器；业务表单仍只保存文件 ID，上传、详情回显、预览入口、下载、租户边界和本场景验收步骤不变。

## 2026-07-19 前端规范候选影响

- 本次前端规范候选统一公开包合同、显式 `style.css` 入口和 Host 请求客户端注入；不改变文件上传、下载、预览 API、文件 ID 持久化、权限、租户或业务表单验收步骤。业务项目主动升级完整前端包矩阵后，需要确认 Host 已注入请求客户端并显式引入文件组件样式，再执行本指南的上传、回显、预览和下载闭环。

## 2026-07-22 富文本托管资源影响

- `MangoEditor` 显式启用 `imageValueType="token"` 与 `pasteImageMode="upload"` 后，工具栏图片、剪贴板 File/Data URI 和远程 HTML 图片统一进入文件服务；对外 HTML 只保留 `mango-file:<id>` 与 `data-file-id`。远程图片通过登录态 `POST /file/files/import-image` 受控导入并固定为 `PRIVATE` 图片文件，普通附件继续复用 `MUpload`，业务表单仍只保存文件 ID、文件 ID 列表或文件 token。未启用新属性的 Editor 以及现有上传、回显、预览、下载接口保持兼容。

## 2026-07-27 Bootstrap 生命周期影响

- 普通业务表单上传、回显、预览和下载 API、文件 ID 持久化、权限及租户边界保持不变。需要随业务制品预置的固定文件改用 `FILE_ASSET` Resource，由 Bootstrap 把 classpath 二进制发布到配置的 LOCAL/S3/OSS/COS/Qiniu 存储并写入稳定的 `file_record`、`file_object`；重入时按对象长度和 SHA-256 校验，内容漂移会重新发布。业务升级顺序调整为先完成 `bootstrap apply` 与 `bootstrap verify`，不再在 Runtime 启动代码中上传预置文件。
## 2026-07-31 文件预览容器适配影响

- `v2026.07.31-maven-1.0.29-pmo-1.3.7-cli-1.0.93-notice-file-dialog-release` 为 `FilePreviewPanel` 增加可选的 `fitContainer`，用于让预览面板填满宿主容器；图片预览默认在预览舞台内使用嵌入式 Element Plus Viewer，不再依赖全屏遮罩交互。上传、文件 ID 持久化、回显、预览元数据、下载 API、权限和租户边界均不变。业务页面升级后应同时验证普通尺寸与 `fitContainer` 布局、图片缩放/切换/关闭，以及自定义 Viewer CSS 是否仍适配。

## Issue #661 单 ZIP 大小控制影响

- 文件模块新增可选的 `FileApi.packageFilesWithSizeControl` 和 `POST /file/files/package-size-control`，供业务后端以 AUTO 总目标或 MANUAL 逐文件目标生成单个 ZIP。普通上传表单的 fileId/fileIds 持久化、上传、回显、预览、下载、权限、租户和前端组件接入均不变。业务使用新入口时应验收 PDF/图片压缩、`compression=NONE`、Office 原样保留，以及目标不可达时仍返回实际 ZIP 和结构化未达标结果。

## 2026-08-03 Mango 1.0.31 发布影响

- Mango `1.0.31` 不改变文件上传、回显、预览、下载、ZIP/PDF API、fileId 持久化、权限或租户边界。使用 `1.0.30` 或其它 `1.0.3x` 版本的业务项目应成组升级 Maven、`@mango/file` 及其固定依赖、CLI 和 PMO，并在独立 Bootstrap 成功后复验上传到下载的完整闭环；已有文件记录、对象内容和业务附件关系无需重建，也不迁移为 URL 字段。
