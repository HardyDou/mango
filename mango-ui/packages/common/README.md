# @mango/common

## 1. 概览

`@mango/common` 是 Mango 前端公共基础包，提供请求封装、session、菜单和权限工具、主题工具、实时通信、公共 API、Vue hooks 和管理端通用组件。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-shell` | request、session、菜单树、TagsView、权限函数、主题、实时通信等后台底座工具。 |
| `business-component` | 分页、字典、组织、用户、区域、验证码、编辑器、图表等后台业务页面组件。 |
| `api-client` | 上传、验证码、组织、地区、字典等公共接口封装。 |

这个包不是无依赖的官网组件库。它依赖 Vue、Element Plus、Pinia、Vue Router、Mango 请求响应格式、登录 token、租户头和若干后端平台能力。官网、营销页或 C 端站点要复用时，优先只引入明确需要的工具或组件，不建议全量接入。

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 统一请求 | `request`、`get`、`post`、`put`、`del` | baseURL、token、租户头、refresh token、错误提示、原始响应。 |
| Session | `Session` | 保存 token、refresh token、过期时间、用户信息和租户。 |
| API 加密 | `wrapRequest`、`sm2Encrypt`、`sm2Decrypt` | 按环境变量启用 SM2 或 BFF 透传。 |
| 菜单和权限 | `buildMenuTree`、权限函数、TagsView 工具 | 给管理后台菜单、按钮权限和标签页使用。 |
| 公共 API | `uploadFile`、captcha、org、area、dict API | 连接 file、captcha、org、system 后端。 |
| 通用组件 | `MangoDialog`、`Pagination`、`DictSelect`、`OrgSelector`、`UserSelector` 等 | 后台页面复用组件。 |
| hooks | `useTitle`、`useDict`、`useECharts`、`useLocale` | 页面标题、字典、图表和语言相关能力。 |
| 实时通信 | `useRealtime`、`SSE`、`Websocket` | SSE/WebSocket client 和组件。 |
| 主题和消息 | `mangoMessage`、theme 工具、主题 CSS | 管理端统一提示和主题样式。 |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/common
```

宿主应用需要提供 peer 依赖：

```bash
pnpm add vue vue-router pinia element-plus vue-i18n
```

引入样式：

```ts
import '@mango/common/style.css';
import '@mango/common/theme/index.css';
```

配置请求：

```ts
import { get, registerUnauthorizedHandler, setRequestBaseUrl } from '@mango/common';

setRequestBaseUrl('/api');
registerUnauthorizedHandler(() => {
  window.location.hash = '/login';
});

const rows = await get('/system/dict/data/options', {
  params: { typeCode: 'order_status' },
});
```

使用组件：

```vue
<script setup lang="ts">
import { DictSelect, OrgSelector, Pagination } from '@mango/common';
import '@mango/common/style.css';
</script>
```

使用通用弹框：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoDialog } from '@mango/common';
import '@mango/common/style.css';

const visible = ref(false);
</script>

<template>
  <MangoDialog
    v-model="visible"
    title="新增应用"
    width="720px"
    footer-align="right"
  >
    <template #default>
      弹框内容
    </template>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary">确定</el-button>
    </template>
  </MangoDialog>
</template>
```

部署时没有单独的 `@mango/common` 后端 starter。它调用的接口来自业务已经启用的后端模块，例如 file、captcha、org、system、auth。

## 4. 配置说明

请求配置：

| 配置位置 | 字段 | 默认值 | 含义 |
|----------|------|--------|------|
| `setRequestBaseUrl(baseURL)` | `baseURL` | `/api` 或 Wujie runtime `apiBaseUrl` | 设置 axios 默认 API 前缀。 |
| `registerUnauthorizedHandler(handler)` | handler | hash 跳转 `/login` | 统一 401 处理。 |
| `RequestConfig` | `ignoreToken` | `false` | 不附加 Authorization，登录和公开接口可用。 |
| `RequestConfig` | `rawResponse` | `false` | 返回 AxiosResponse，文件下载等场景使用。 |
| `RequestConfig` | `skipRefreshToken` | `false` | 禁止当前请求触发 refresh token。 |
| `RequestConfig` | `silentError` | `false` | 失败时不弹全局错误提示。 |
| `RequestConfig` | `loading` | 可选 | 参与内部 loading 计数，当前不直接展示 UI。 |
| `RequestConfig` | `retry` | 可选 | 类型保留，当前没有通用自动重试。 |

请求头：

| Header | 来源 | 含义 |
|--------|------|------|
| `Authorization` | `Session.getToken()` | Bearer token。 |
| `X-Mango-Tenant-Id` | `userInfo.tenantId` 或 Session `tenantId` | 当前租户。 |
| `TENANT-ID` | `userInfo.tenantId` 或 Session `tenantId` | 兼容租户头。 |

API 加密环境变量：

| 变量 | 默认值 | 含义 |
|------|--------|------|
| `VITE_API_ENC_ENABLED` | `false` | 是否启用 API 加密包装。 |
| `VITE_IS_BFF` | `true` | BFF 模式下前端透传，非 BFF 模式可用 SM2。 |
| `VITE_SM2_PUBLIC_KEY` | 空 | 非 BFF 模式 SM2 加密公钥。 |

开发 mock：

| 变量 | 影响 |
|------|------|
| `VITE_USE_MOCK=true` | `getAreaTree()`、`getOrgTree()` 在 dev 环境返回本地 mock 树。 |

## 5. API 与扩展

请求和工具导出：

| 导出 | 用途 |
|------|------|
| `request` | axios 实例。 |
| `get`、`post`、`put`、`del` | 常用请求方法，默认返回后端包裹体中的 `data`。 |
| `setRequestBaseUrl` | 设置 API baseURL。 |
| `registerUnauthorizedHandler` | 注册未授权处理。 |
| `normalizeApiPayload` | 把明确 ID 字段中的 number 兜底转成字符串。 |
| `Session` | token、refresh token、用户信息和租户存储。 |
| `mangoMessage` | Element Plus 消息封装。 |
| `useRealtime` | 实时连接 hook。 |

公共 API：

| 函数 | HTTP 接口 | 说明 |
|------|-----------|------|
| `uploadFile(file)` | `POST /file/files` | 上传普通附件，`purpose=attachment`。 |
| `uploadImage(file)` | `POST /file/files` | 上传图片，`purpose=image`。 |
| `uploadExcel(file)` | `POST /file/files` | 上传 Excel，`purpose=excel`。 |
| `uploadMultiple(files)` | `POST /file/files/batch` | 批量上传。 |
| `getUploadedFileDetail(id)` | `GET /file/files/detail` | 查询文件详情。 |
| `downloadUploadedFile(id)` | `GET /file/files/download` | 下载文件，返回原始 blob response。 |
| `getCaptchaTypes()` | `GET /captcha/types` | 查询验证码类型和存储。 |
| `generateArithmetic()` | `GET /captcha/arithmetic` | 生成算术验证码。 |
| `generateBlockPuzzle()` | `GET /captcha/block-puzzle` | 生成拼图验证码。 |
| `generateClickWord()` | `GET /captcha/click-word` | 生成点选验证码。 |
| `generateBehavior()` | `GET /captcha/behavior` | 生成行为验证码。 |
| `verifyCaptcha(request)` | `POST /captcha/verify` | 校验验证码。 |
| `sendSms(mobile)` | `POST /auth/captcha/send` | 发送登录短信验证码。 |
| `sendEmail(email)` | `POST /auth/captcha/send` | 发送登录邮箱验证码。 |
| `getOrgTree(params)` | `GET /org/tree` | 组织树。 |
| `getAreaTree(params)` | `GET /system/area/tree` 或 `GET /system/area/children` | 地区树或子节点。 |
| `listDictOptions(typeCode)` | `GET /system/dict/data/options` | 字典选项。 |

组件导出：

| 组件 | 能力 |
|------|------|
| `Pagination` | 分页器。 |
| `MangoDialog` | 管理端通用弹框外壳，统一标题区、关闭按钮、内容滚动区和底部按钮区。 |
| `DictSelect`、`DictTag` | 字典选择和展示。 |
| `OrgSelector`、`UserSelector` | 组织和用户选择。 |
| `CaptchaSelector`、各验证码组件 | 验证码展示和交互。 |
| `ChinaArea` | 行政区域选择。 |
| `IconSelector`、`TreeSelect`、`RightToolbar` | 管理端通用选择和工具栏。 |
| `FormCreate`、`Sign`、`CodeEditor`、`Editor`、`ECharts` | 表单、签名、代码、富文本和图表。 |
| `SSE`、`Websocket`、`Chat` | 实时通信和聊天 UI。 |

`MangoDialog` 组件：

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `modelValue` | 弹框显示状态，支持 `v-model`。 |
| props | `title` | 标题文本，也可通过 `title` slot 覆盖。 |
| props | `width` | 弹框宽度，语义与 Element Plus Dialog 一致，默认 `50%`。 |
| props | `showHeader` | 是否展示完整顶部标题区，默认 `true`。 |
| props | `showClose` | 是否展示关闭按钮，默认 `true`。 |
| props | `footerAlign` | 底部插槽对齐方式，支持 `left`、`center`、`right`，默认 `right`。 |
| props | `destroyOnClose` | 关闭后是否销毁内容，默认 `false`。 |
| emits | `update:modelValue`、`open`、`opened`、`close`、`closed` | 透出弹框显示状态和 Element Plus Dialog 生命周期事件。 |
| slots | `default` | 内容区域。内容区独立滚动，弹框最大高度为视口高度的 90%。 |
| slots | `title` | 自定义标题内容。 |
| slots | `headerExtra` | 标题右侧扩展区域。 |
| slots | `footer` | 底部按钮区域。未传入时不渲染底部。 |

## 6. 数据与初始化

`@mango/common` 不创建后端数据。组件和 API 能否返回数据，取决于后端模块是否已初始化：

| 数据 | 后端来源 | 前端消费 |
|------|----------|----------|
| token、用户、租户 | auth、identity、system | request 请求头、Session、权限函数。 |
| 字典 | system 字典 | `DictSelect`、`DictTag`、`useDict`、`listDictOptions()`。 |
| 组织 | org | `OrgSelector`、`getOrgTree()`。 |
| 地区 | system area | `ChinaArea`、`getAreaTree()`。 |
| 文件 | file | 上传、下载、文件详情 API。 |
| 验证码 | captcha、auth | 登录验证码和二次校验。 |
| 实时连接 | realtime 后端或业务服务 | `useRealtime`、`SSE`、`Websocket`。 |

## 7. 管理入口

本包不注册菜单，也不写权限资源。它提供的权限函数只用于前端展示控制，接口访问必须由后端再次校验。

接入管理后台时至少确认：

| 检查项 | 说明 |
|--------|------|
| token | 登录后 `Session` 中存在 access token。 |
| 租户 | `userInfo.tenantId` 或 Session `tenantId` 能写入租户头。 |
| baseURL | `/api` 或运行时 `apiBaseUrl` 能转发到后端。 |
| 数据权限 | 公共选择器返回的数据已经由后端按租户和权限过滤。 |

## 8. 快速开始

1. 安装 `@mango/common` 和 peer 依赖。
2. 引入 `@mango/common/style.css` 和主题 CSS。
3. 调用 `setRequestBaseUrl()` 设置 API 前缀。
4. 调用 `registerUnauthorizedHandler()` 接入登录页跳转。
5. 登录成功后写入 token、refresh token、用户和租户信息。
6. 在页面中按需使用公共 API、hooks 和组件。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 请求没有 token | 未登录、Session 未写入或 `ignoreToken=true` | 检查登录保存逻辑和请求配置。 |
| 租户头缺失 | userInfo 和 Session 都没有 tenantId | 登录后写入租户上下文。 |
| 401 后没有跳登录 | 没注册 unauthorized handler | 调用 `registerUnauthorizedHandler()`。 |
| 文件下载拿到 JSON | 后端返回业务错误而不是 blob | 看 JSON 中 `message` 或 `msg`，排查文件权限。 |
| 字典、组织、地区为空 | 后端数据未初始化或权限不足 | 分别检查 system、org 和接口权限。 |
| 官网引入后体积过大 | 全量 common 带管理端组件和依赖 | 改为按子路径引入，或拆出站点专用轻量组件。 |
| 大 ID 精度问题 | 业务把 id 当 number 继续运算 | ID 字段按字符串处理，使用 `ApiId`。 |

## 10. 相关文档

- [@mango/api-schema](../api-schema/README.md)
- [后端 File](../../../mango/mango-platform/mango-file/README.md)
- [后端 Captcha](../../../mango/mango-platform/mango-captcha/README.md)
- [后端 System](../../../mango/mango-platform/mango-system/README.md)
- [后端 Org](../../../mango/mango-platform/mango-org/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 11. 变更影响记录

- 本次新增 `PasswordPolicyHint` 组件和 `passwordPolicy` 工具，用于统一展示密码规则和强弱判断。它们只影响前端密码提示和表单校验，不改变 request、Session、菜单、权限、租户头、公开 API 路径或后端存储结构。包的公开入口已通过 `@mango/common` 主入口和 `./components/PasswordPolicyHint/index.vue` 子路径导出。
