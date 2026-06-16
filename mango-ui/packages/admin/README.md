# @mango/admin

## 1. 概览
`@mango/admin` 是 Mango 管理后台前端聚合包。它重新导出 Shell、页面注册表、认证页面和常用平台页面注册函数，并生成管理端样式入口。

业务项目优先使用本包创建标准 Mango 后台；只有要深度定制壳层时才直接使用 `@mango/admin-shell`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务后台快速接入 Mango 管理壳 | 前端注册 / 组件 / API 封装 |
| 单体管理端需要一次性引入默认平台页面和样式 | 前端注册 / 组件 / API 封装 |
| CLI 生成的业务前端入口需要稳定的聚合 API | 前端注册 / 组件 / API 封装 |
| 业务项目需要选择性注册 file、job、notice、numgen、template、workflow 等管理页面包 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 业务后台快速接入 Mango 管理壳。
- 单体管理端需要一次性引入默认平台页面和样式。
- CLI 生成的业务前端入口需要稳定的聚合 API。
- 业务项目需要选择性注册 file、job、notice、numgen、template、workflow 等管理页面包。

## 4. 边界说明
- 不适合官网、营销页、普通内容站。
- 不负责业务页面实现。
- 不负责后端菜单、权限、租户和资源入库。
- 不替代 `@mango/admin-pages` 的页面 key 注册。

## 5. 模块组成
本包是聚合入口，不实现新的业务页面。

当前导出来源：

| 来源 | 内容 |
|------|------|
| `@mango/admin-shell` | `createMangoAdminApp`、Shell 配置、router、stores、runtime |
| `@mango/admin-pages` | 页面注册表和默认页面注册 |
| `@mango/auth` | 登录、个人中心、修改密码相关导出 |
| 平台页面包 | calendar、file、job、notice、numgen、template、workflow 页面注册函数 |

`./full` 额外导出 `mangoFullAdminFeatureRegistrars`，用于一次性注册完整平台能力。

## 6. 接入方式
安装：

```bash
pnpm add @mango/admin
```

基础入口：

```ts
import { createMangoAdminApp, registerDefaultAdminPages } from '@mango/admin';
import '@mango/admin/style.css';

registerDefaultAdminPages();

const { mount } = createMangoAdminApp({
  apiBaseUrl: '/api',
  title: 'Mango Admin',
});

mount('#app');
```

完整能力入口：

```ts
import { createMangoAdminApp, registerDefaultAdminPages } from '@mango/admin';
import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';
import '@mango/admin/style-full.css';

registerDefaultAdminPages();

const { mount } = createMangoAdminApp({
  featureRegistrars: mangoFullAdminFeatureRegistrars,
});

mount('#app');
```

## 7. 配置说明
本包自身没有独立运行时配置，配置透传给 `@mango/admin-shell`。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `createMangoAdminApp` | `apiBaseUrl` | `/api` | 后端 API base URL | 请求前缀 | `@mango/admin-shell` |
| `createMangoAdminApp` | `title` | `Mango Admin` | 后台标题 | Shell 展示 | `@mango/admin-shell` |
| `createMangoAdminApp` | `features` | 全量启用 | 管理能力开关 | 菜单和默认页过滤 | `@mango/admin-pages` |
| `createMangoAdminApp` | `featureRegistrars` | 空 | 扩展页面注册函数 | 启动时注册平台能力 | `@mango/admin-shell` |
| `./full` | `mangoFullAdminFeatureRegistrars` | 9 个注册函数 | 完整平台能力注册列表 | 注册 file、job、notice、workflow 等页面 | `src/full.ts` |
| `style.css` | 生成样式 | common、admin-shell、auth、rbac、system、job | 基础后台样式 | 宿主需引入 | `admin-packages.json` |
| `style-full.css` | 完整样式 | 基础样式加完整能力样式 | 完整后台样式 | 宿主需引入 | package style build |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `createMangoAdminApp` | 创建 Mango 后台 |
| `MangoAdminShellOptions` | Shell 配置类型 |
| `MangoAdminAppInstance` | app、router、mount 返回类型 |
| `registerDefaultAdminPages` | 注册默认 auth、rbac、system 页面 |
| `registerMangoFileAdminPages` | 注册文件管理页面 |
| `registerMangoJobAdminPages` | 注册任务调度页面 |
| `registerMangoNoticeAdminPages`、`registerMangoNoticeAdminShell` | 注册通知页面和 Shell 扩展 |
| `registerMangoNumgenAdminPages` | 注册编号生成页面 |
| `registerMangoTemplateAdminPages` | 注册模板页面 |
| `registerMangoWorkflowAdminPages` | 注册工作流页面 |
| `registerMangoWorkflowBusinessExampleAdminPages` | 注册工作流业务示范页面 |
| `mangoFullAdminFeatureRegistrars` | 完整能力注册函数数组 |

## 9. 数据与初始化
本包不包含数据库 migration。菜单、权限、租户和资源入库由后端平台模块和业务模块负责。

| 类型 | 后端来源 | 前端消费方式 | 排查入口 |
|------|----------|--------------|----------|
| 默认平台菜单 | authorization、system 等后端模块 | `registerDefaultAdminPages` 对应页面 key | 菜单可打开 |
| 扩展能力菜单 | file、job、notice、workflow 等后端模块 | 对应 `registerMango*AdminPages` | 页面可打开 |
| 业务菜单 | 业务 resource manifest | 业务包自己的注册函数 | 菜单和页面一致 |

## 10. 管理入口
`@mango/admin` 只让前端具备页面加载能力，不给用户授权。

接入时必须同时满足：

- 后端菜单在 `internal-admin` 应用下存在。
- 菜单 `moduleCode` 和 `component` 能在页面注册表中找到。
- 用户角色拥有对应菜单和按钮权限。
- 后端接口按当前用户、权限和租户上下文校验。

## 11. 快速开始
1. 安装 `@mango/admin` 和 peer 依赖。
2. 引入 `@mango/admin/style.css` 或 `@mango/admin/style-full.css`。
3. 调用 `registerDefaultAdminPages`。
4. 按需传入 `featureRegistrars` 或业务页面注册函数。
5. 设置 `apiBaseUrl`。
6. 后端初始化菜单、权限和角色授权。
7. 登录后台验证菜单、页面、接口权限和租户数据。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 页面无样式 | 没引入样式入口 | 引入 `style.css` 或 `style-full.css` |
| file/workflow 页面打不开 | 只用了基础入口，没注册扩展能力 | 使用对应注册函数或 `mangoFullAdminFeatureRegistrars` |
| 菜单显示但空白 | 后端 component key 没有注册 | 查 `@mango/admin-pages` |
| 构建 peer 依赖报错 | 宿主缺 Vue、Pinia、Element Plus 等 peer | 按 package peerDependencies 安装 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端组件规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [@mango/admin-shell](../admin-shell/README.md)
- [@mango/admin-pages](../admin-pages/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
