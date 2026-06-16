# @mango/template

## 1. 概览
`@mango/template` 提供模板中心前端页面和 API：模板分类、模板列表、版本发布、变量提取、同步/异步渲染、渲染记录和能力说明页。

本包属于 `admin-pages` 配套能力，依赖后端 `mango-template`，文件型模板还依赖 file/fileproc 能力。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 维护 TEXT、HTML、DOCX、XLSX 模板 | 前端注册 / 组件 / API 封装 |
| 发布模板版本并切换当前版本 | 前端注册 / 组件 / API 封装 |
| 从模板内容或文件中提取变量 | 前端注册 / 组件 / API 封装 |
| 渲染 TEXT、HTML、DOCX、XLSX、PDF、OFD 输出 | 前端注册 / 组件 / API 封装 |
| 查看模板渲染记录和失败原因 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 维护 TEXT、HTML、DOCX、XLSX 模板。
- 发布模板版本并切换当前版本。
- 从模板内容或文件中提取变量。
- 渲染 TEXT、HTML、DOCX、XLSX、PDF、OFD 输出。
- 查看模板渲染记录和失败原因。

## 4. 边界说明
- 不实现后端模板渲染引擎。
- 不处理文件预览、文件转换和文件存储。
- 不替代业务领域模板审批流程。
- 不初始化菜单、权限、模板分类或模板数据。

## 5. 模块组成
本包包含：

- `TemplateListView`：模板管理。
- `TemplateCategoryView`：模板分类。
- `TemplateRenderRecordsView`：渲染记录。
- `TemplateServiceGuideView`：模板能力说明页。
- `registerMangoTemplateAdminPages`：页面注册。
- `templateApi`、`templateCategoryApi`：模板和分类 API。

后端负责模板版本、渲染执行、输出文件、权限和租户隔离。

## 6. 接入方式
安装：

```bash
pnpm add @mango/template
```

注册管理页面：

```ts
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';

registerMangoTemplateAdminPages();
```

调用渲染：

```ts
import { templateApi } from '@mango/template';

const result = await templateApi.render({
  templateCode: 'ORDER_PRINT',
  outputFormat: 'PDF',
  variables: { orderNo: 'SO202606150001' },
});
```

## 7. 配置说明
本包没有运行时配置文件。行为由模板数据、API 参数和后端渲染配置决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `registerMangoTemplateAdminPages` | `moduleCode` | `mango-template` | 页面归属模块 | 和后端菜单匹配 | `admin-pages.ts` |
| 页面注册 | `component` | 多个 template key | 菜单打开页面 | `admin-pages.ts` |
| `TemplateItem` | `templateCode` | 无 | 模板编码 | 业务渲染主键 | `api/template.ts` |
| `TemplateItem` | `sourceFormat` | 可选 | 源格式 | TEXT、HTML、DOCX、XLSX | `api/template.ts` |
| `TemplateVersion` | `versionNo` | 后端生成 | 模板版本 | 发布和激活版本 | `api/template.ts` |
| `TemplateVersion` | `sourceFileId` | 可选 | 源文件 ID | 文件型模板渲染 | `api/template.ts` |
| `TemplateRenderPayload` | `outputFormat` | 无 | 输出格式 | TEXT、HTML、DOCX、XLSX、PDF、OFD | `api/template.ts` |
| `TemplateRenderPayload` | `variables` | 无 | 渲染变量 | 模板变量替换 | `api/template.ts` |
| `TemplateRenderPayload` | `async` | 可选 | 是否异步 | 生成渲染记录 | `api/template.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `TemplateListView` | 模板管理页 |
| `TemplateCategoryView` | 模板分类页 |
| `TemplateRenderRecordsView` | 渲染记录页 |
| `TemplateServiceGuideView` | 能力说明页 |
| `registerMangoTemplateAdminPages` | 注册页面 |
| `templateApi.page`、`detail`、`create`、`update`、`delete` | 模板 CRUD |
| `templateApi.publishVersion`、`activateVersion` | 版本发布和激活 |
| `templateApi.extractVariables` | 变量提取 |
| `templateApi.render`、`renderAsync` | 同步和异步渲染 |
| `templateApi.renderRecordPage`、`renderRecord` | 渲染记录 |
| `templateCategoryApi` | 模板分类 CRUD |

页面 key：

```text
system/template/index
template/templates/index
template/categories/index
template/render-records/index
debug/capabilities/template
```

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端初始化：

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 模板分类 | mango-template | 分类页和模板下拉 | 分类列表有数据 |
| 模板 | mango-template | 模板列表和渲染 | 创建后可查询 |
| 模板版本 | mango-template | 发布、激活、渲染 | 发布后可渲染 |
| 渲染记录 | mango-template | 记录页 | 渲染后有记录 |
| 源文件/输出文件 | mango-file / fileproc | 文件型模板和输出 | 文件可下载或预览 |
| 菜单权限 | authorization / template resource | 页面入口和按钮权限 | 菜单可见、接口可用 |

## 10. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 模板管理 | `template/templates/index` 或 `system/template/index` | 后端 template 模块定义 | 后端 resource / migration | 角色授权 | template admin API |
| 模板分类 | `template/categories/index` | 后端 template 模块定义 | 后端 resource / migration | 角色授权 | template admin API |
| 渲染记录 | `template/render-records/index` | 后端 template 模块定义 | 后端 resource / migration | 角色授权 | template admin API |
| 能力说明 | `debug/capabilities/template` | 开发/说明入口 | 前端注册 | 开发态或授权配置 | 无业务写操作 |

模板数据必须按租户隔离。渲染接口也要按后端权限和租户上下文校验。

## 11. 快速开始
1. 后端启用 `mango-template`、file 和 fileproc。
2. 前端注册 `@mango/template/admin-pages`。
3. 后端初始化菜单权限并授权。
4. 创建分类和模板。
5. 发布模板版本。
6. 业务页面或后端按 `templateCode` 调用渲染。
7. 验证输出、记录、权限和租户隔离。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 渲染失败 | 模板未发布、变量缺失或格式不支持 | 查模板版本和变量 |
| DOCX/PDF 输出失败 | fileproc 或转换依赖不可用 | 查后端 fileproc 配置 |
| 页面打不开 | component key 未注册 | 调用 `registerMangoTemplateAdminPages` |
| 记录为空 | 同步渲染未落记录或后端未启用记录 | 查后端 render record |
| 跨租户可见 | 后端模板查询未隔离 | 补后端租户校验 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Template](../../../mango/mango-platform/mango-template/README.md)
- [后端 FileProc](../../../mango/mango-infra/mango-infra-fileproc/README.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
