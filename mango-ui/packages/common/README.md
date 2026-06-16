# @mango/common

## 1. 概览
`@mango/common` 是 Mango 前端公共基础包，提供请求封装、session、消息、菜单树、权限函数、主题、实时通信、公共 API、hooks 和通用 Vue 组件。

本包是公共能力包，但很多组件依赖 Element Plus、Pinia、Vue Router、Mango 后端接口和管理端上下文。官网或普通网站复用前必须逐项确认依赖。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理后台页面需要统一请求、token、租户头和错误处理 | 前端注册 / 组件 / API 封装 |
| 业务页面需要字典、组织、区域、上传、验证码等公共 API | 前端注册 / 组件 / API 封装 |
| 管理端需要分页、字典展示、树选择、组织选择、用户选择、图表、编辑器等通用组件 | 前端注册 / 组件 / API 封装 |
| Shell、页面包和业务包需要共享菜单树、TagsView、权限判断和主题工具 | 前端注册 / 组件 / API 封装 |
| 实时消息、SSE 或 WebSocket 需要统一 client 和 Vue hook | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理后台页面需要统一请求、token、租户头和错误处理。
- 业务页面需要字典、组织、区域、上传、验证码等公共 API。
- 管理端需要分页、字典展示、树选择、组织选择、用户选择、图表、编辑器等通用组件。
- Shell、页面包和业务包需要共享菜单树、TagsView、权限判断和主题工具。
- 实时消息、SSE 或 WebSocket 需要统一 client 和 Vue hook。

## 4. 边界说明
- 不负责后端接口实现。
- 不负责页面注册；页面 key 归 `@mango/admin-pages` 管。
- 不负责菜单、权限、租户和字典数据初始化。
- 不适合在不引入 Element Plus 和 Mango 请求上下文的站点里直接全量使用。

## 5. 模块组成
本包包含：

| 类别 | 入口 | 说明 |
|------|------|------|
| 请求 | `utils/request` | axios 实例、base URL、token、租户头、refresh token、错误处理 |
| Session | `utils/storage` | token、refresh token、过期时间、用户信息 |
| 安全 | `utils/apiCrypto` | BFF 模式透传、非 BFF 模式 SM2 加密 |
| 菜单/权限 | `utils/menuTree`、`utils/authFunction`、`utils/tagsView` | 菜单树、权限函数、标签页工具 |
| UI 工具 | `utils/message`、`utils/theme`、`utils/iconConfig` | 消息、主题、图标 |
| 公共 API | `api/upload`、`api/captcha`、`api/org`、`api/area`、`api/dict` | 后端公共接口封装 |
| hooks | `useTitle`、`useDict`、`useECharts`、`useLocale` | Vue hooks |
| 组件 | `Pagination`、`DictSelect`、`OrgSelector`、`UserSelector` 等 | 管理端通用组件 |
| 实时 | `utils/realtime`、`SSE`、`Websocket` | SSE/WebSocket client 和组件 |

## 6. 接入方式
安装：

```bash
pnpm add @mango/common
```

样式：

```ts
import '@mango/common/style.css';
import '@mango/common/theme/index.css';
```

请求：

```ts
import { get, post, setRequestBaseUrl, registerUnauthorizedHandler } from '@mango/common';

setRequestBaseUrl('/api');
registerUnauthorizedHandler(() => {
  window.location.hash = '/login';
});

const users = await get('/authorization/users', {
  params: { page: 1, size: 20 },
});
```

组件：

```ts
import { Pagination, DictSelect, OrgSelector } from '@mango/common';
```

## 7. 配置说明
### 6.1 RequestConfig

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| request | `baseURL` | `/api` 或 Wujie runtime `apiBaseUrl` | API 前缀 | 所有请求默认前缀 | `resolveApiBaseUrl` |
| `setRequestBaseUrl` | `baseURL` | 无 | 手动设置 API 前缀 | 修改 axios defaults | `setRequestBaseUrl` |
| `registerUnauthorizedHandler` | handler | hash 到 login | 401 处理 | Shell 统一退出登录 | `registerUnauthorizedHandler` |
| `RequestConfig` | `loading` | 可选 | 是否显示全局 loading | 当前只计数，未显示 UI | `showLoading` |
| `RequestConfig` | `ignoreToken` | `false` | 是否不带 token | 登录、刷新 token 可用 | `handleToken` |
| `RequestConfig` | `retry` | 可选 | 请求重试次数 | 当前未实现通用重试 | 类型保留 |
| `RequestConfig` | `rawResponse` | `false` | 返回 AxiosResponse | 文件下载等场景 | 响应拦截器 |
| `RequestConfig` | `skipRefreshToken` | `false` | 跳过刷新 token | 避免刷新接口递归 | `shouldRefreshToken` |
| `RequestConfig` | `silentError` | `false` | 静默错误提示 | 不弹全局错误消息 | 响应拦截器 |

请求头：

| Header | 来源 | 含义 |
|--------|------|------|
| `Authorization` | `Session.getToken()` | Bearer token |
| `X-Mango-Tenant-Id` | userInfo 或 Session tenantId | 当前租户 |
| `TENANT-ID` | userInfo 或 Session tenantId | 兼容租户头 |

### 6.2 API Crypto

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| 环境变量 | `VITE_API_ENC_ENABLED` | `false` | 是否启用 API 加密 | `wrapRequest`、SM2 函数 | `apiCrypto.ts` |
| 环境变量 | `VITE_IS_BFF` | `true` | 是否 BFF 模式 | BFF 模式透传 | `isBffMode` |
| 环境变量 | `VITE_SM2_PUBLIC_KEY` | 空 | SM2 公钥 | 非 BFF 模式加密 | `sm2Encrypt` |

BFF 模式下前端透传 JSON，由 BFF 层处理加密。非 BFF 模式且配置公钥时，前端用 SM2 加密。

## 8. API 与扩展
### 7.1 请求和工具

| 导出 | 用途 |
|------|------|
| `request` | axios 实例 |
| `get`、`post`、`put`、`del` | 常用请求方法 |
| `setRequestBaseUrl` | 设置 API base URL |
| `registerUnauthorizedHandler` | 注册 401 处理 |
| `normalizeApiPayload` | 把明确 ID 数值兜底转字符串 |
| `Session` | token、refresh token、用户信息存储 |
| `mangoMessage` | Element Plus 消息封装 |
| `wrapRequest`、`sm2Encrypt`、`sm2Decrypt` | API 加密工具 |
| `useRealtime` | 实时连接 hook |

### 7.2 公共组件

| 组件 | 能力 |
|------|------|
| `Pagination` | 分页器 |
| `IconSelector` | 图标选择 |
| `DictTag`、`DictSelect` | 字典展示和选择 |
| `RightToolbar` | 列表工具栏 |
| `FormCreate` | 表单创建器封装 |
| `Sign` | 签名组件 |
| `OrgSelector`、`UserSelector` | 组织和用户选择 |
| `TreeSelect` | 树选择 |
| `CaptchaSelector` 及各 Captcha 组件 | 验证码 |
| `Chat` | 聊天 UI |
| `ChinaArea` | 行政区域选择 |
| `CodeEditor`、`Editor` | 代码和富文本编辑 |
| `ECharts` | 图表 |
| `SSE`、`Websocket` | 实时通信组件 |

### 7.3 公共 API

| API 包 | 用途 |
|--------|------|
| `api/upload` | 上传相关接口 |
| `api/captcha` | 验证码接口 |
| `api/org` | 组织接口 |
| `api/area` | 区域接口 |
| `api/dict` | 字典接口 |

## 9. 数据与初始化
本包不包含数据库 migration。它依赖后端已经初始化的公共数据。

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 字典 | system 字典表 | `DictSelect`、`DictTag`、`useDict` | 字典下拉有数据 |
| 组织 | org / identity | `OrgSelector`、组织 API | 组织树可加载 |
| 用户 | identity | `UserSelector` | 用户选择可搜索 |
| 区域 | system area | `ChinaArea`、区域 API | 省市区可选择 |
| 文件 | file 模块 | 上传 API | 上传成功并返回文件 ID |
| 验证码 | captcha 模块 | Captcha 组件 | 校验通过 |

## 10. 管理入口
本包不写菜单和权限数据，但请求层会自动带 token 和租户头。业务接入要验证：

- 当前用户 session 中有 token。
- 当前租户在 `userInfo.tenantId` 或 Session `tenantId` 中。
- 后端接口识别 `X-Mango-Tenant-Id` 或 `TENANT-ID`。
- 前端权限函数只用于展示，后端接口必须再次校验。
- 公共选择器的数据范围不能跨租户。

## 11. 快速开始
1. 安装 `@mango/common` 和 peer 依赖。
2. 引入样式和主题。
3. 设置 API base URL。
4. 注册 401 处理。
5. 页面按需使用公共组件、公共 API 和 hooks。
6. 后端准备字典、组织、用户、区域、文件、验证码等数据。
7. 执行构建、单测和真实接口联调。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 请求没带 token | 未登录或 `ignoreToken=true` | 检查 Session 和请求配置 |
| 租户头缺失 | userInfo 和 Session 没有 tenantId | 登录后写入租户上下文 |
| 大 ID 精度丢失 | 页面或 API 包把 ID 当 number | 使用 `ApiId` 和字符串 |
| 字典下拉为空 | 后端字典未初始化或接口权限不足 | 查 system 字典和权限 |
| 官网引入后构建变重 | 全量 common 依赖管理端库 | 只抽取必要工具或拆更小公共包 |
| 加密没生效 | BFF 模式默认透传或未配置公钥 | 检查 `VITE_API_ENC_ENABLED`、`VITE_IS_BFF`、`VITE_SM2_PUBLIC_KEY` |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [Element Plus UI 规范](../../../mango-pmo/rules/frontend/02-element-plus-ui.md)
- [前端组件规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [@mango/api-schema](../api-schema/README.md)
