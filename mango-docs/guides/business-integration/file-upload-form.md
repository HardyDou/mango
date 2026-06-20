# 文件上传表单接入

## 1. 适用场景

业务表单需要上传合同、图片、附件或导入文件，并在详情页回显、下载或预览。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [File 后端 README](../../../mango/mango-platform/mango-file/README.md) | 存储配置、文件记录、下载接口、数据库资源 |
| 2 | [Fileproc README](../../../mango/mango-infra/mango-infra-fileproc/README.md) | 文件渲染、转换、Aspose 配置 |
| 3 | [File Preview README](../../../mango/mango-platform/mango-file-preview/README.md) | 预览 token、预览页面、下载边界 |
| 4 | [@mango/file README](../../../mango-ui/packages/file/README.md) | 前端组件、API 封装、页面 key |
| 5 | [File Components README](../../../mango-ui/packages/file/src/components/README.md) | `MUpload`、`FilePreviewPanel` 用法和 props |
| 6 | [能力地图：文件上传到预览闭环](../../capabilities/README.md#3-组合接入入口) | 组合验证入口 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 后端依赖 | 业务后端引入 file 相关 starter，确认存储配置可用 |
| 业务表 | 业务表保存 fileId、fileIds 或业务附件关联表，不直接保存临时 URL |
| 上传接口 | 前端上传后拿到文件 ID，再随业务 Command/Request 一起提交 |
| 回显 | 详情页用文件 ID 查询元数据，按业务权限决定下载和预览入口 |
| 预览 | 需要在线预览时确认 file-preview 能拿到源文件并生成预览 token |
| 删除 | 删除业务单据时区分业务解绑和物理文件清理 |

## 4. 最小业务样例

前端表单字段：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MUpload, FilePreviewPanel } from '@mango/file';
import '@mango/file/style.css';

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

| 类别 | 检查项 |
|------|--------|
| 存储配置 | 目标环境已配置可用存储，上传接口能写入文件记录 |
| 权限资源 | 下载和预览相关权限已授权给测试角色 |
| 租户数据 | 文件记录、业务单据和当前登录用户处于同一租户上下文 |
| 前端组件 | `MUpload` 返回 `fileId`、`fileIds` 或 token，详情页按文件 ID 回显 |
| 菜单页面 | 使用文件中心管理页时，页面 key 和菜单 component 对齐 |
| 预览链路 | 启用预览时，file-preview 和 fileproc 依赖可用 |
| 业务语义 | 编辑、删除业务单据时，附件解绑或物理清理策略清晰 |

## 6. 最小闭环

1. 打开业务新增页。
2. 上传一个文件并保存业务单据。
3. 重新打开详情页，文件名称、大小、下载入口可见。
4. 点击预览或下载，后端鉴权和文件读取返回正常结果。
5. 删除或编辑业务单据后，附件关系符合业务预期。

## 7. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 上传成功但业务保存后文件丢失 | 表单提交是否带 fileId，后端 Command/Request 是否接收并持久化 |
| 下载 404 | fileId 是否存在，存储配置是否指向正确 bucket、目录或本地路径 |
| 预览失败 | file-preview 依赖、转换配置、预览 token 和源文件读取权限 |
| 多租户下看不到文件 | 文件记录 tenantId、业务数据 tenantId、当前登录上下文是否一致 |
| 图片能下载但不能预览 | 前端是否使用预览入口，后端 MIME 类型和预览类型是否匹配 |

## 8. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-file -am test
mvn -f mango/pom.xml -pl mango-platform/mango-file-preview -am test
pnpm -F @mango/file build
```

模块验证入口：

- [File 验证方式](../../../mango/mango-platform/mango-file/README.md#10-验证方式)
- [File Preview 验证方式](../../../mango/mango-platform/mango-file-preview/README.md#10-验证方式)
- [Frontend File 验证方式](../../../mango-ui/packages/file/README.md#10-验证方式)
- [File Components 验证方式](../../../mango-ui/packages/file/src/components/README.md#8-验证方式)

## 9. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [后端代码文件引用规则](../../../mango-pmo/rules/backend/01-code.md#51-文件引用规则)
- [后端 API 文件字段规则](../../../mango-pmo/rules/backend/03-api.md#22-文件字段规则)
- [前端文件上传与回显规则](../../../mango-pmo/rules/frontend/01-vue-code.md#41-文件上传与回显规则)

## 10. 变更影响记录

- PR #195 加固前端 `@mango/*` 包的 `exports`、`types` 和生成声明文件，使业务项目通过发布后的 `dist` 产物独立消费；不改变文件上传、下载、预览的公开 API、前端组件、配置、权限、租户、页面、启动方式和表单验收步骤。业务项目应继续使用公开 package 入口和 `./style.css`，不要依赖包内 `src` 路径。
- PR #194 发布资源注册中心版本并升级 `@mango/file@1.0.11`、`@mango/admin@1.0.23`、`@mango/common@1.0.10`、`@mango/cli@1.0.34` 等前端包；不改变文件上传、下载、预览的公开 API、前端组件、权限、租户、页面、启动方式和表单验收步骤。业务升级时应成组升级前端 `@mango/*` 包并刷新后端 Mango `1.0.0-SNAPSHOT` 依赖。
- PR #193 新增 `mango-resource` 注册中心并将文件存储配置、文件设置默认数据迁移为资源声明同步；不改变文件上传、下载、预览的公开 API、前端组件、权限、租户、页面和表单验收步骤。排查默认存储配置缺失时，需要同时确认 `FILE_STORAGE_CONFIG` 和 `FILE_SETTINGS` 声明是否已同步。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变文件上传、下载、预览的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变文件上传、下载、预览的公开 API、配置、权限、租户、页面、启动和运行时行为。
