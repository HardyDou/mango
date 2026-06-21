# @mango/template

## 1. 概览

`@mango/template` 是 Mango 模板中心的前端包，配套后端 `mango-template` 使用。

它提供：

- 模板管理页面。
- 模板分类页面。
- 模板版本发布、历史版本查看和当前版本切换。
- 模板变量维护和变量提取。
- 同步/异步渲染和渲染记录页面。
- 模板能力说明页。
- 前端 API 封装和 TypeScript 类型。

本包属于管理后台 `admin-pages` 插件。文件型模板上传依赖 `@mango/file`，后端渲染和格式转换依赖 `mango-template`、`mango-file`、`mango-infra-fileproc`。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 模板分类管理 | 维护分类编码、名称、排序和启停状态。 |
| 模板管理 | 维护 `templateCode`、名称、业务域、分类、草稿和状态。 |
| 文件型模板 | 通过文件上传组件保存 DOCX、XLSX 模板源文件。 |
| 版本发布 | 发布草稿为版本，切换当前发布版本。 |
| 变量维护 | 维护变量名、标签、类型、是否必填、示例、描述和子变量。 |
| 变量提取 | 从文本内容或源文件提取变量建议。 |
| 模板渲染 | 发起同步/异步渲染，支持 TEXT、HTML、DOCX、XLSX、PDF、OFD 输出。 |
| 渲染记录 | 查询渲染状态、输出文件、输出内容和失败原因。 |

## 3. 集成形态

| 形态 | 是否支持 | 说明 |
|------|----------|------|
| `admin-shell` | 否 | 不提供后台壳、登录态或导航能力。 |
| `admin-pages` | 是 | 通过 `registerMangoTemplateAdminPages` 注册模板页面。 |
| `business-component` | 否 | 不提供官网或普通业务页通用组件。 |
| `api-client` | 是 | 导出 `templateApi`、`templateCategoryApi` 和类型。 |

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/template
```

注册管理页面：

```ts
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';

registerMangoTemplateAdminPages();
```

业务前端直接渲染示例：

```ts
import { templateApi } from '@mango/template';

const result = await templateApi.render({
  templateCode: 'CONTRACT_NOTICE',
  outputFormat: 'PDF',
  variables: { contractNo: 'CT202606160001' },
  bizType: 'CONTRACT',
  bizId: '10001',
});
```

生产业务更推荐由业务后端调用模板 API，前端只触发业务动作并读取业务结果。

## 5. 快速开始

1. 后端启用 `mango-template`、`mango-file` 和 `mango-infra-fileproc`。
2. 前端安装并注册 `@mango/template/admin-pages`。
3. 角色拥有模板相关菜单和接口权限。
4. 创建模板分类。
5. 创建模板并填写 `templateCode`、`templateName`、`domainCode`。
6. TEXT/HTML 模板填写内容；DOCX/XLSX 模板上传源文件。
7. 维护变量定义并发布版本。
8. 发起渲染，确认返回文本内容或文件 ID。
9. 同步渲染失败时，页面会显示失败状态和后端错误信息。
10. 在渲染记录页查看状态和失败原因。

## 6. 配置说明

本包没有独立运行时配置文件。页面行为由页面注册、后端模板数据、后端文件能力和后端渲染能力决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 |
|----------|------------|--------|------|
| `registerMangoTemplateAdminPages` | `moduleCode` | `mango-template` | 页面归属模块。 |
| 页面注册 | component key | 多个 template key | 菜单打开具体模板页面。 |
| 模板源格式 | `sourceFormat` | 无 | `TEXT`、`HTML`、`DOCX`、`XLSX`。 |
| 模板输出格式 | `outputFormat` | 无 | `TEXT`、`HTML`、`DOCX`、`XLSX`、`PDF`、`OFD`。 |
| 文件型模板 | `sourceFileId` | 无 | DOCX、XLSX 源文件 ID。 |
| 渲染变量 | `variables` | `{}` | 模板变量替换数据。 |

依赖：

| 类型 | 依赖 |
|------|------|
| dependencies | `@mango/admin-pages`、`@mango/common`、`@mango/file`、`@element-plus/icons-vue` |
| peerDependencies | `vue`、`vue-router`、`element-plus` |

## 7. API 与扩展

### 7.1 页面 key

| 页面 | component key |
|------|---------------|
| 模板管理 | `system/template/index`、`template/templates/index` |
| 模板分类 | `template/categories/index` |
| 渲染记录 | `template/render-records/index` |
| 能力说明 | `debug/capabilities/template` |

### 7.2 导出对象

| 导出 | 用途 |
|------|------|
| `TemplateListView` | 模板管理页。 |
| `TemplateCategoryView` | 模板分类页。 |
| `TemplateRenderRecordsView` | 渲染记录页。 |
| `TemplateServiceGuideView` | 模板能力说明页。 |
| `registerMangoTemplateAdminPages` | 注册模板页面。 |
| `templateApi` | 模板 CRUD、版本、变量、渲染和记录 API。 |
| `templateCategoryApi` | 模板分类 API。 |

### 7.3 常用 API

| API | 后端路径 | 用途 |
|-----|----------|------|
| `templateApi.page` | `/template/templates/page` | 查询模板分页。 |
| `templateApi.detail` | `/template/templates/detail` | 查询模板详情、草稿和版本。 |
| `templateApi.create` | `/template/templates` | 新增模板。 |
| `templateApi.update` | `/template/templates` | 修改模板和草稿。 |
| `templateApi.delete` | `/template/templates` | 删除模板。 |
| `templateApi.updateStatus` | `/template/templates/status` | 启停模板。 |
| `templateApi.publishVersion` | `/template/templates/versions` | 发布模板版本。 |
| `templateApi.activateVersion` | `/template/templates/versions/current` | 启用历史版本。 |
| `templateApi.extractVariables` | `/template/templates/variables/extract` | 提取变量建议。 |
| `templateApi.render` | `/template/templates/render` | 同步渲染。 |
| `templateApi.renderAsync` | `/template/templates/render/async` | 异步渲染。 |
| `templateApi.renderRecord` | `/template/templates/render-records/detail` | 查询渲染记录详情。 |
| `templateApi.renderRecordPage` | `/template/templates/render-records/page` | 查询渲染记录分页。 |
| `templateCategoryApi.page` | `/template/categories/page` | 查询分类分页。 |
| `templateCategoryApi.list` | `/template/categories/list` | 查询分类列表。 |
| `templateCategoryApi.create` | `/template/categories` | 新增分类。 |
| `templateCategoryApi.update` | `/template/categories` | 修改分类。 |
| `templateCategoryApi.updateStatus` | `/template/categories/status` | 启停分类。 |
| `templateCategoryApi.delete` | `/template/categories` | 删除分类。 |

### 7.4 常用类型

| 类型 | 关键字段 |
|------|----------|
| `TemplateItem` | `templateCode`、`templateName`、`categoryCode`、`domainCode`、`sourceFormat`、`status`、`currentVersionNo`、`hasUnpublishedChanges` |
| `TemplateCategory` | `categoryCode`、`categoryName`、`sort`、`status` |
| `TemplateVariableDefinition` | `name`、`label`、`type`、`required`、`example`、`description`、`children` |
| `TemplateVersion` | `templateId`、`versionNo`、`sourceFormat`、`content`、`sourceFileId`、`variables`、`currentPublished` |
| `TemplateRenderPayload` | `templateCode`、`versionNo`、`outputFormat`、`variables`、`async`、`bizType`、`bizId` |
| `TemplateRenderResult` | `recordId`、`status`、`content`、`fileId`、`fileName`、`contentType`、`errorMessage` |
| `TemplateRenderRecord` | `templateCode`、`versionNo`、`outputFormat`、`status`、`outputFileId`、`outputContent`、`errorMessage`、`bizType`、`bizId` |

## 8. 数据与初始化

本包不创建数据库表，也不初始化菜单权限。它依赖后端完成以下初始化：

| 数据 | 后端来源 | 前端用途 |
|------|----------|----------|
| 模板分类 | `mango-template` | 分类页和模板下拉。 |
| 模板 | `mango-template` | 模板列表、草稿、发布和渲染。 |
| 模板版本 | `mango-template` | 历史版本、当前版本切换。 |
| 渲染记录 | `mango-template` | 渲染记录页。 |
| 源文件和输出文件 | `mango-file` | 文件型模板上传、文档类输出下载或预览。 |
| 渲染和转换能力 | `mango-infra-fileproc` | 文本/文档渲染、PDF/OFD 转换。 |
| 菜单权限 | authorization | 页面入口和按钮权限。 |

## 9. 管理入口

| 入口 | 页面 key | 说明 |
|------|----------|------|
| 模板管理 | `system/template/index`、`template/templates/index` | 模板列表、草稿、发布、渲染。 |
| 模板分类 | `template/categories/index` | 分类维护。 |
| 渲染记录 | `template/render-records/index` | 渲染状态、输出文件、错误信息。 |
| 能力说明 | `debug/capabilities/template` | 模板能力说明页。 |

页面可见但打不开时，检查 `registerMangoTemplateAdminPages()` 是否执行，以及菜单 component 是否能映射到上面的页面 key。

## 10. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 页面打不开 | 是否注册 `@mango/template/admin-pages`，菜单 component 是否正确。 |
| 文件上传不可用 | 是否安装 `@mango/file`，后端文件服务是否可用。 |
| 渲染失败 | 模板是否发布、变量是否完整、输出格式是否受支持。 |
| 预览区显示 FAILED | 查看错误信息，按后端返回的模板变量、文件处理或输出格式错误修正。 |
| DOCX/PDF/OFD 输出失败 | 后端 fileproc 和转换 license 是否可用。 |
| 渲染返回无文件 ID | 输出格式是否是文档类，后端 `mango-file` 是否保存成功。 |
| 渲染记录查不到 | 查询条件 `templateCode`、`bizType`、`bizId`、状态是否正确。 |
| 页面请求 403 | 当前角色是否拥有模板菜单和接口权限。 |

## 11. 相关文档

- [后端模板模块](../../../mango/mango-platform/mango-template/README.md)
- [文件前端包](../file/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
