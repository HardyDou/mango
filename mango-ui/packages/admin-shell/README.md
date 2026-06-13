# @mango/admin-shell

## 1. 能力定位

`@mango/admin-shell` 提供 Mango 管理端 Shell、布局、路由、菜单运行时、状态 store、开发调试页和应用启动封装。主要使用者是 Mango 管理端应用、业务前端包和 CLI 生成的后台项目。

## 2. 适用场景

- 创建标准 Mango 管理后台应用。
- 接入 authorization 菜单树和运行时应用配置。
- 复用 Shell 布局、标签页、面包屑、用户区和首页。
- 开发阶段注册组件示例页、上传页、工作流组件页等调试入口。

## 3. 不适用场景

- 不提供业务领域页面本身。
- 不负责后端认证、授权和菜单数据持久化。
- 不替代 `@mango/admin-pages` 的业务组件页注册能力。
- 不作为独立低代码平台运行。

## 4. 模块边界

本包负责前端 Shell 运行时。菜单来源、权限资源和租户应用绑定由后端 authorization 提供；业务页面由业务包或平台能力包注册。

## 5. 接入方式

依赖包：

```json
{
  "dependencies": {
    "@mango/admin-shell": "1.0.17"
  }
}
```

入口通常调用：

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
```

package exports 包括 `.`、`./runtime`、`./menu`、`./stores`、`./router`、`./home`、`./dev-pages`、`./dev-base-pages`、`./dev-upload-page`、`./dev-workflow-page`、`./style.css`。

## 6. 配置项

主要配置通过 `configureMangoAdminShell` 和 `getMangoAdminShellOptions` 管理。包依赖 `vue`、`vue-router`、`pinia`、`vue-i18n`，可选 peer 包包括 `@mango/file`、`@mango/notice`、`@mango/workflow`。

运行时菜单默认请求 `/authorization/menus/user?fmt=tree&appCode=internal-admin`。生产类环境 runtime config 默认 fail closed，并要求远程 entry allowlist。

## 7. 对外接口 / 扩展点

- `createMangoAdminApp`
- `MangoAdminShellApp`
- `MangoAdminShellView`
- `MangoAdminLayout`
- `MangoAdminParentView`
- `createMangoAdminRouter`
- `getShellPinia`
- `installShellApp`
- `configureMangoAdminShell`
- `getMangoAdminShellOptions`
- stores、menu host、runtime config、runtime host

## 8. 数据库 / 初始化数据

本包不包含数据库 migration。菜单、权限、应用入口和租户绑定数据由后端 authorization 维护。

## 9. 菜单 / 权限 / 租户

Shell 通过 authorization 菜单接口加载当前用户菜单，并根据运行时配置加载本地或远程页面。租户应用绑定、菜单包和权限资源由后端返回，前端不直接写入权限数据。

## 10. 验证方式

```bash
pnpm -F @mango/admin-shell test
pnpm -F @mango/admin-shell build
```

测试覆盖 router、menu host、feature registrars、tag navigation 和 shell 边界。

## 11. 业务接入最小闭环

业务后台通常优先消费 `@mango/admin` 聚合包；只有需要直接组装 Shell 时才接 `@mango/admin-shell`。最小入口需要在 `main.ts` 引入 `@mango/admin-shell/style.css`，调用 `createMangoAdminApp`，注册业务页面，并确保菜单返回的 component key 与页面注册 key 一致。

验收断言覆盖：runtime config 加载成功，`appCode` 与后端 authorization 菜单接口一致，登录后菜单能打开业务页面，远程 entry 在 allowlist 中，peer 依赖没有重复安装导致的 Vue/Pinia 实例冲突。

## 12. 常见问题

- 菜单不显示时检查 authorization 菜单接口、`appCode` 和页面注册 key。
- 远程页面加载失败时检查 runtime config 的 allowlist 和 entry 地址。
- peer 依赖缺失会导致宿主应用构建或运行时失败。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
