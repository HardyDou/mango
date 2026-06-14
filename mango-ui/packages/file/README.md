# 文件前端包

## 1. 能力定位

提供文件 API、上传组件和预览面板。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务页面需要上传、选择或预览 Mango 文件时使用。

## 3. 不适用场景

不负责后端存储策略和文件权限判定。

## 4. 模块边界

包名：`@mango/file`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

安装并引入：

```ts
import '@mango/file/style.css';
import { FilePreviewPanel, MUpload, fileApi } from '@mango/file';
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
```

后端需要接入 [File](../../../mango/mango-platform/mango-file/README.md)；预览需要接入 [File Preview](../../../mango/mango-platform/mango-file-preview/README.md) 和 [Fileproc](../../../mango/mango-infra/mango-infra-fileproc/README.md)。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口：

- 页面：`FileView`、`FileStorageView`、`FileSettingsView`。
- 组件：`MUpload`、`FilePreviewPanel`。
- API：`fileApi`、`fileStorageApi`、`fileSettingsApi`；存储配置页面和后端接口使用 `storage-configs` 命名。
- 页面注册：`registerMangoFileAdminPages()`，页面 key 包括 `file/files/index`、`file/storage-configs/index`、`file/settings/index`。

主要 API 前缀：`/file`、`/file-preview`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/file build
```

## 11. 业务接入最小闭环

业务页面使用 `MUpload` 上传文件，只保存后端返回的 fileId，再用 `FilePreviewPanel` 打开预览链接，确认下载和预览都通过后端权限校验。

## 12. 常见问题

上传失败优先检查 `/file/files`、存储配置和文件大小限制；预览失败优先检查 `/file-preview`、fileproc 渲染/转换和 Aspose/Office 环境；403 优先检查文件归属和权限。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
