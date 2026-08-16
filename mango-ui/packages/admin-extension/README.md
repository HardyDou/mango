# @mango/admin-extension

`@mango/admin-extension` 是 Mango 管理端的 FE1 扩展契约包，提供页面注册表、功能集合和通知铃铛 provider 合同。它不包含具体业务页面、宿主布局或路由装配。

## 1. 概览

该包承接原来位于 `@mango/admin-pages/core`、`features` 和 `notice` 的中立运行时契约，使 FE2 领域包可以依赖 FE1 扩展层，而不需要反向依赖 FE3 `@mango/admin-pages` 组装包。

## 2. 功能清单

| 能力               | 入口                              | 用途                                             |
| ------------------ | --------------------------------- | ------------------------------------------------ |
| 页面和隐藏路由注册 | `@mango/admin-extension/core`     | 注册、查询和诊断 component key 对应的页面 loader |
| 管理端功能集合     | `@mango/admin-extension/features` | 解析 core/full preset 和模块功能映射             |
| 通知铃铛 provider  | `@mango/admin-extension/notice`   | 注册通知组件和提醒配置读取函数                   |
| 聚合导出           | `@mango/admin-extension`          | 从一个入口导入上述契约                           |

## 3. 接入方式

新建或已迁移的 FE2 领域包直接依赖本包：

```ts
import { registerModulePages } from '@mango/admin-extension/core';

registerModulePages({
  moduleCode: 'mango-example',
  pages: {
    'example/index': () => import('./views/ExampleView.vue'),
  },
});
```

`@mango/admin-pages/core`、`@mango/admin-pages/features` 和 `@mango/admin-pages/notice` 继续 re-export 同一份实现，因此已发布消费者不会得到第二个注册表实例。这三个兼容子路径保留到下一个主版本；在仓外消费者完成迁移并经独立发布说明确认后才会删除。

## 4. 配置说明

本包没有环境变量或 YAML 配置。页面注册使用 `moduleCode`、`pages`、可选 `routes` 和可选制品元数据；功能集合使用 `core`、`full`、功能数组或布尔映射。字段语义与 [Admin Pages README](../admin-pages/README.md) 的配置说明保持一致。

## 5. API 与扩展

- `registerModulePages()`、`registerPage()`、`registerShellPages()`：写入唯一页面注册表。
- `getPageLoader()`、`getRegisteredPageRoutes()`、`resolvePageModuleCode()`：供 Shell 和路由装配读取注册结果。
- `getRegisteredModulePagesSnapshot()`、`probeRegisteredPage()`：提供不可变快照和页面 loader 运行诊断。
- `resolveMangoAdminFeatures()`、`resolveMangoAdminModuleFeature()`：解析功能 preset 和模块归属。
- `registerMangoNoticeBellProvider()`、`getMangoNoticeBellProvider()`：写入和读取通知铃铛 provider。

## 6. 数据与初始化

本包只在前端进程内保存注册表和 provider 引用，不访问数据库，也不生成菜单或权限数据。各领域包在 Admin 启动时调用注册函数；菜单数据仍来自后端资源声明或 migration。

## 7. 管理入口

本包没有独立管理页面。用户打开后端授权菜单时，Admin Shell 通过本包的页面注册表解析 `moduleCode + component` 并加载对应领域页面。

## 8. 快速开始

1. 在领域前端包中添加对 `@mango/admin-extension` 的精确版本依赖。
2. 从 `@mango/admin-extension/core` 导入 `registerModulePages()`。
3. 用与后端菜单一致的 `moduleCode` 和 component key 注册页面。
4. 在 Admin 启动的 feature registrar 中调用领域注册函数。

## 9. 问题排查

**菜单打开后显示 404**

检查菜单 `moduleCode` 和归一化后的 component key，再用 `getRegisteredModulePagesSnapshot()` 或 `probeRegisteredPage()` 确认 registrar 是否写入当前进程的注册表。

**新旧导入路径的注册结果不一致**

检查应用是否安装了同一发布 tuple 中的 `@mango/admin-extension` 和 `@mango/admin-pages`，以及打包配置是否保留了 `@mango/admin-extension/*` 外部导入。

## 10. 相关文档

- [Admin Pages README](../admin-pages/README.md)
- [File README](../file/README.md)
- [前端 Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 11. 验证入口

```bash
pnpm -C mango-ui --filter @mango/admin-extension test
pnpm -C mango-ui --filter @mango/admin-extension build
pnpm -C mango-ui architecture:check
```
