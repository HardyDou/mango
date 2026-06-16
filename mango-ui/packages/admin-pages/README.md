# @mango/admin-pages

## 1. 概览
`@mango/admin-pages` 是 Mango 管理后台的页面注册表。后端菜单返回 `moduleCode` 和 `component` 后，Shell 或单体入口通过本包找到对应 Vue 页面 loader。

本包属于 `admin-shell` 配套插件，不是官网、营销站或普通前台站点组件库。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 平台能力包向管理后台登记页面 | 前端注册 / 组件 / API 封装 |
| 业务页面包把自己的 component key 注册给 Shell | 前端注册 / 组件 / API 封装 |
| 单体入口和微前端 Shell 复用同一套页面解析逻辑 | 前端注册 / 组件 / API 封装 |
| 开发中心页面、隐藏路由、账号页面和默认平台页面需要统一注册 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 平台能力包向管理后台登记页面。
- 业务页面包把自己的 component key 注册给 Shell。
- 单体入口和微前端 Shell 复用同一套页面解析逻辑。
- 开发中心页面、隐藏路由、账号页面和默认平台页面需要统一注册。

## 4. 边界说明
- 不负责页面布局、登录态、菜单拉取和路由渲染。
- 不负责后端菜单、权限、租户和资源入库。
- 不提供通用网站页面组件。
- 不替代 `@mango/admin-shell` 或 `@mango/admin`。

## 5. 模块组成
本包只维护前端页面 key 到 loader 的映射。

边界：

- `registerModulePages` 写入页面注册表。
- `getPageLoader` 按 `moduleCode` 和 `component` 查找 loader。
- `getRegisteredPageRoutes` 输出包内登记的隐藏路由。
- 默认平台页面来自 `registerDefaultAdminPages`，依赖 `@mango/auth`、`@mango/rbac`、`@mango/system`。
- 权限判断和租户隔离必须由后端接口兜底。

## 6. 接入方式
安装依赖：

```bash
pnpm add @mango/admin-pages
```

业务页面包注册：

```ts
import { registerModulePages } from '@mango/admin-pages';

export function registerRbacPages() {
  registerModulePages({
    moduleCode: 'mango-authorization',
    pages: {
      'system/user/index': () => import('@mango/rbac').then(m => m.UserView),
    },
    routes: [
      {
        path: '/system/user',
        component: 'system/user/index',
        menuName: '用户管理',
        menuCode: 'system:user',
        visible: 0,
        keepAlive: 1,
      },
    ],
  });
}
```

后台入口调用：

```ts
import { registerRbacPages } from '@mango/rbac';

registerRbacPages();
```

后端菜单中的字段要对齐：

| 后端菜单字段 | 前端注册值 |
|--------------|------------|
| `moduleCode` | `mango-authorization` |
| `component` | `system/user/index` |
| `path` | `/system/user` |

## 7. 配置说明
本包没有运行时配置文件。行为由注册函数入参决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `MangoPageRegistry` | `moduleCode` | 无 | 模块编码 | 和后端菜单 `moduleCode` 匹配 | `registerModulePages` |
| `MangoPageRegistry` | `pages` | 无 | component 到 loader 映射 | 决定菜单能否加载页面 | `registerModulePages` |
| `MangoPageRegistry` | `routes` | 空 | 隐藏路由列表 | Shell 生成未入菜单的可跳转路由 | `getRegisteredPageRoutes` |
| `MangoPageRoute` | `path` | 无 | 前端路由路径 | 自动补齐前导斜线 | `normalizePageRoute` |
| `MangoPageRoute` | `component` | 无 | 页面 component key | 会去掉 `@/`、`src/`、`views/` 和 `.vue` | `normalizeComponentPath` |
| `MangoPageRoute` | `visible` | `0` | 是否在菜单显示 | 隐藏路由默认不显示 | `normalizePageRoute` |
| `MangoPageRoute` | `keepAlive` | `0` | 是否缓存 | Shell 生成 route meta | `normalizePageRoute` |
| `registerDefaultAdminPages` | `features` | 全量启用 | 平台能力开关 | 控制 authorization、system 等默认页是否注册 | `features.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `registerModulePages(registry)` | 批量注册模块页面和隐藏路由 |
| `registerPage(moduleCode, component, loader)` | 注册单个页面 |
| `registerShellPages(loaders)` | 注册首页和 404 |
| `getPageLoader(moduleCode, component)` | Shell 根据菜单取页面 loader |
| `resolvePageModuleCode(component, path)` | 根据 component 或 path 反推 moduleCode |
| `getRegisteredPageRoutes(moduleCodes)` | 取已注册隐藏路由 |
| `registerDefaultAdminPages(options)` | 注册默认平台页面 |
| `features` | 管理端能力开关解析 |
| `notice` | 通知能力页面注册入口 |
| `dev-pages`、`dev-component-pages` | 开发中心页面注册入口 |

`package.json` 还导出 `./core`、`./defaults`、`./features`、`./notice`、`./dev-pages` 和 `./dev-component-pages`。

## 9. 数据与初始化
本包不包含数据库 migration。菜单、权限、租户和资源入库由后端模块处理。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 页面注册 | 前端包注册函数 | component loader | `moduleCode + component` | 前端入口执行注册函数 | `getPageLoader` |
| 隐藏路由 | `routes` 参数 | 非菜单路由 | `moduleCode + path` | Shell 加载菜单时追加 | `getRegisteredPageRoutes` |
| 默认平台页 | `registerDefaultAdminPages` | auth、rbac、system 页面 | moduleCode + component | Shell 或 admin 入口初始化 | 菜单路由和页面加载 |

## 10. 管理入口
前端页面注册必须和后端菜单一致：

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 个人中心 | `profile/index` | 用户登录态 | Shell 隐藏路由 | 当前登录用户 | auth 接口 |
| 修改密码 | `password/index` | 用户登录态 | Shell 隐藏路由 | 当前登录用户 | auth 接口 |
| RBAC 页面 | `system/user/index` 等 | 后端定义 | authorization 初始化 | 角色授权 | authorization 接口 |
| 业务页面 | 业务模块自己的 component key | 业务定义 | 业务 resource manifest | 角色授权 | 业务 Controller / Service |

本包不做按钮权限或租户过滤。按钮显示可以读前端权限上下文，但后端接口必须再次校验。

## 11. 快速开始
1. 在业务页面包中实现页面组件。
2. 导出 `register<Module>Pages()`。
3. 调用 `registerModulePages` 登记 `moduleCode`、`pages` 和必要的 `routes`。
4. 在后台入口调用业务注册函数。
5. 后端 resource manifest 或菜单初始化写入同一个 component key。
6. 登录后台打开菜单，验证页面加载、接口鉴权和租户数据。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 菜单打开空白 | component key 没注册或 moduleCode 不一致 | 对比后端菜单和 `registerModulePages` |
| 本地能打开，Shell 不能打开 | 入口没有调用业务注册函数 | 在 admin app 初始化前注册业务页面 |
| 隐藏详情页 404 | 没传 `routes` | 在 `MangoPageRegistry.routes` 中登记隐藏路由 |
| 权限绕过 | 只做了前端按钮隐藏 | 后端接口补权限和租户校验 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端组件规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
