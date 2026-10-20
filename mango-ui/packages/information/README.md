# @mango/information

Mango 信息采集前端包，提供表单管理、采集记录、托管表单和类型化 API。后台页面通过 `@mango/admin-extension` 注册，公开托管表单不使用服务端 SK。

## 页面

| 页面     | 页面 key                        | 说明                                                                                                                                                          |
| -------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 表单管理 | `information/forms/index`       | 表单列表，行操作只保留详情、配置、采集记录、停用、删除                                                                                                        |
| 表单详情 | `information/forms/detail`      | 表单摘要；接入方式分左右两栏：左页面（地址 + 效果预览 + iframe 嵌入代码）、右接口（接口地址 + 专属 SK + cURL/Postman/Java/JavaScript/Python 示例 + 字段配置） |
| 表单设计 | `information/forms/config`      | 复用平台表单设计器 `form-create-designer`（与工作流表单设计器同源）                                                                                           |
| 采集记录 | `information/submissions/index` | 独立菜单入口，按表单筛选、动态列与处理抽屉                                                                                                                    |
| 托管表单 | `information/hosted/index`      | 匿名公开表单，无需登录                                                                                                                                        |

表单定义以 form-create 规则 JSON 存储，历史 `{fieldCode, fieldName, fieldType}` 形状会被自动升级。

## 1. 概览

`@mango/information` 是信息采集管理前端包，依赖宿主注入的 Mango `HttpClient`，提供后台表单维护、表单设计、采集记录处理、匿名托管表单和类型化 API。公开托管表单不携带服务端 SK；服务端代提交时才在服务端请求体中使用当前表单专属 `serverSecret`。

## 2. 功能清单

- `registerMangoInformationAdminPages()` 注册表单管理、详情、设计、采集记录和托管表单页面。
- `createInformationApi(httpClient)` 和 `useInformationApi()` 提供类型化后台与公开接口。
- `createInformationPublicFileClient(httpClient, formCode)` 为公开附件上传提供绑定表单编码的客户端。
- `style.css` 提供信息采集页面、表单控件和托管表单样式。

## 3. 接入方式

安装并注册页面：

```bash
pnpm add @mango/information
```

```ts
import { registerMangoInformationAdminPages } from '@mango/information/admin-pages';
import '@mango/information/style.css';

registerMangoInformationAdminPages();
```

宿主应用必须提供 `MANGO_HTTP_CLIENT_KEY` 对应的 Mango `HttpClient`。业务代码直接使用 `createInformationApi(httpClient)`；Vue 组件内可使用 `useInformationApi()`。公开托管页应让后端建立默认租户上下文，不要把服务端 SK 放入浏览器代码。

## 4. 配置说明

本包没有独立配置文件。后端 `mango.information.*` 配置、租户、文件存储和权限资源由 `mango-information-starter` 提供；前端只负责注册页面、注入 `HttpClient` 和加载样式。页面 key 与默认菜单、接口权限的对应关系以平台 Information README 为准。

## 5. API 与扩展

- `createInformationApi(httpClient)` 返回表单、公开表单、提交记录和处理 API。
- `createInformationPublicFileClient(httpClient, formCode)` 将公开上传请求绑定到指定表单编码；上传票据过期或表单不匹配时由服务端拒绝。
- `registerMangoInformationAdminPages()` 注册模块码 `mango-information`，页面 key 见上表；宿主可在统一页面注册表中覆盖页面实现，但不得绕过后端权限。
- 字段定义使用 form-create 规则 JSON；有 `field` 的规则参与提交校验和动态列，布局规则不作为采集值。

## 6. 数据与初始化

前端不创建数据库或初始化业务数据。菜单、公开 API、迁移表和资源同步由后端 `mango-information-starter` 负责；表单定义、提交值和字段快照由后端保存。宿主升级后应确认菜单资源同步、页面注册和后端 migration 均成功。

## 7. 管理入口

管理页面入口全部由后端 `AUTH_MENU` 配置驱动，包内不注册隐藏路由：

| 页面         | 路由                            | 菜单编码                       | 可见性       | 定位参数           |
| ------------ | ------------------------------- | ------------------------------ | ------------ | ------------------ |
| 表单管理     | `/information/forms`            | `information:forms`            | 可见         | 无                 |
| 采集记录     | `/information/submissions`      | `information:submissions`      | 可见         | 通过表单选择器筛选 |
| 表单详情     | `/information/form-detail`      | `information:form-detail`      | `visible: 0` | `query.id`         |
| 表单配置     | `/information/form-config`      | `information:form-config`      | `visible: 0` | `query.id`         |
| 采集记录处理 | `/information/form-submissions` | `information:form-submissions` | `visible: 0` | `query.id`         |

匿名托管页面为 `/information/hosted?formCode=<code>`。隐藏页面由列表行操作或详情页按钮以 `router.push({ path, query: { id } })` 打开，路由和组件由菜单配置决定；页面 key 必须与菜单 `component` 一致，管理 API 仍由服务端权限和租户边界最终校验。新增管理页面时，先在该模块的 `*-common-menu.json` 声明菜单（含 `visible: 0` 的隐藏页），再在 `registerMangoInformationAdminPages()` 注册同名页面 key，禁止在包内另写 `routes`。

## 8. 快速开始

1. 安装 `@mango/information`，注册 admin pages 并加载 `style.css`。
2. 在宿主应用提供 Mango `HttpClient`，确认后端 starter、迁移、菜单和权限资源已启用。
3. 创建并发布表单，在托管页面提交记录并在管理页处理。
4. 需要上传时先通过公开 API 领取票据，再使用 `createInformationPublicFileClient` 上传；不要把 `serverSecret` 放在前端。

## 9. 问题排查

- 页面 404：检查 registrar 是否执行、`@mango/admin-extension` 是否为同一发布批次版本，以及页面 key/菜单 component 是否一致。
- 请求未发送：检查宿主是否注入 `MANGO_HTTP_CLIENT_KEY`，不要在包内自行创建全局 Axios 实例。
- 公开提交失败：确认表单已发布、`formCode` 与租户匹配、幂等键未冲突；服务端代提交才检查 `serverSecret`。
- 文件上传失败：确认票据未过期且与 `formCode` 绑定，并检查后端文件存储和上传限制配置。

## 10. 相关文档

- [Information 后端模块](../../../mango/mango-platform/mango-information/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)
- [文件前端包](../file/README.md)
- [Admin 页面注册](../admin-extension/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
