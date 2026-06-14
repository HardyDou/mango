# 管理端单体应用包

## 1. 能力定位

提供单体管理端应用入口和完整样式构建。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务项目需要直接使用单体管理端壳层时使用。

## 3. 不适用场景

不负责微前端 Shell 编排和具体业务页面开发。

## 4. 模块边界

包名：`@mango/admin`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

在业务前端或 Mango 前端包中引入 `@mango/admin`；如包导出 `./admin-pages`，需要同步注册到统一页面注册表。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口以 `package.json` exports 和 `src/index.ts` 为准；页面包通常额外导出 `src/admin-pages.ts`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/admin build
```

## 11. 业务接入最小闭环

前端应用引入 `@mango/admin` 或 `@mango/admin/full`，加载样式文件，验证登录、菜单和页面注册。

## 12. 常见问题

如果页面打不开，先检查包是否构建、样式是否引入、菜单 component 是否注册、后端 API 是否返回真实数据。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
