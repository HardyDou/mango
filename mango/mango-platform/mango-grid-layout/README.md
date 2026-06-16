# Grid Layout 自定义栅格布局使用说明

## 1. 概览

`mango-grid-layout` 提供当前登录用户的自定义栅格布局保存、查询和删除能力，配合前端 `@mango/grid-layout` 使用。它面向工作台、看板、业务驾驶舱、详情页自定义面板等需要按用户保存页面布局的场景。

模块主要服务 Mango 开发者、业务开发者和 AI Agent。后端只保存用户对某个 `pageCode` 的布局 JSON，不维护业务小组件数据、不维护默认布局、不负责前端拖拽算法，也不做小组件权限过滤。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 查询个人布局 | 获取当前登录人在某个页面保存的布局 | `GET /grid-layout/personal` |
| 保存个人布局 | 写入或覆盖当前登录人在某个页面的布局 | `PUT /grid-layout/personal` |
| 删除个人布局 | 清空个人布局，前端回到默认布局 | `DELETE /grid-layout/personal` |
| Java API | 供后端模块内或跨模块调用 | `GridLayoutPersonalApi` |
| 数据校验 | 校验 `pageCode`、布局 JSON、栅格宽度和坐标 | `GridLayoutPersonalService` |
| 租户用户隔离 | 按当前登录租户、用户和页面编码隔离布局 | `MangoContextHolder` |
| 自动装配 | 引入 starter 后注册服务和 Controller | `mango-grid-layout-starter` |

## 3. 接入方式

后端应用引入 starter：

```xml
<dependency>
  <groupId>io.mango.platform.gridlayout</groupId>
  <artifactId>mango-grid-layout-starter</artifactId>
</dependency>
```

前端页面配合 `@mango/grid-layout` 使用：

```ts
gridLayoutPersonalApi.getPersonal('admin-home-workbench');
gridLayoutPersonalApi.savePersonal({ pageCode, layoutJson });
gridLayoutPersonalApi.resetPersonal('admin-home-workbench');
```

业务接入时需要保持 `pageCode` 稳定。默认布局由前端业务页面维护；当查询个人布局为空时，前端使用默认布局展示。

## 4. 配置说明

当前版本随 starter 自动装配，不提供额外业务配置项。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| Maven dependency | `mango-grid-layout-starter` | 未引入 | 启用个人布局 HTTP 接口和服务 | 引入后注册 Controller、Service 和 Mapper | `mango-grid-layout-starter/pom.xml` |
| 请求参数 | `pageCode` | 无 | 页面唯一编码 | 决定当前用户保存或读取哪一份布局 | `GridLayoutPersonalQuery` |
| 请求体 | `layoutJson` | 无 | 前端布局 JSON 字符串 | 保存用户拖拽后的布局结果 | `SaveGridLayoutPersonalCommand` |

## 5. API 与扩展

HTTP 接口：

| 方法 | 地址 | 说明 |
|------|------|------|
| `GET` | `/grid-layout/personal?pageCode=...` | 查询当前登录人的页面布局 |
| `PUT` | `/grid-layout/personal` | 保存当前登录人的页面布局 |
| `DELETE` | `/grid-layout/personal?pageCode=...` | 删除当前登录人的页面布局 |

Java API：

| API | 说明 |
|-----|------|
| `GridLayoutPersonalApi#getPersonal` | 查询当前登录人的页面布局 |
| `GridLayoutPersonalApi#savePersonal` | 保存当前登录人的页面布局 |
| `GridLayoutPersonalApi#deletePersonal` | 删除当前登录人的页面布局 |

常用入参：

| 类型 | 字段 | 说明 |
|------|------|------|
| `GridLayoutPersonalQuery` | `pageCode` | 页面编码 |
| `SaveGridLayoutPersonalCommand` | `pageCode` | 页面编码 |
| `SaveGridLayoutPersonalCommand` | `layoutJson` | 布局 JSON 字符串 |

模块扩展优先通过前端小组件库和业务页面编排完成。后端模块不内置业务小组件注册表，不接管业务权限策略。

## 6. 数据与初始化

本模块使用 Flyway migration 初始化数据表。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 |
|------|------|------------|-----------------|----------|
| Flyway migration | `mango-grid-layout-core/src/main/resources/db/migration/grid-layout/V1__init_grid_layout.sql` | 创建 `mango_user_grid_layout` 数据表 | `tenant_id + user_id + page_code` | 应用启动时由 Flyway 执行 |

数据表：

| 表名 | 说明 |
|------|------|
| `mango_user_grid_layout` | 保存当前租户、当前用户、指定页面编码下的个人布局 JSON |

关键字段：

| 字段 | 说明 |
|------|------|
| `tenant_id` | 当前登录租户 |
| `user_id` | 当前登录用户 |
| `page_code` | 页面编码 |
| `layout_json` | 前端布局 JSON 字符串 |

模块不提供默认布局初始化数据。恢复默认布局时删除个人配置，由前端业务页面重新使用默认布局。

## 7. 管理入口

本模块不新增后台菜单页面，不新增按钮权限码，也不新增默认套餐或角色授权数据。

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 无独立管理页面 | 无 | 无新增 | 无 | 无新增 | `GridLayoutPersonalController` / `GridLayoutPersonalService` |

接口使用登录访问模式。租户和用户来自 `MangoContextHolder`，前端不传 `tenantId` 和 `userId`。业务侧的小组件权限、菜单权限和数据权限在调用布局组件前完成过滤，本模块只保存当前登录人的布局结果。

## 8. 快速开始

业务接入最小闭环：

1. 后端应用引入 `mango-grid-layout-starter`。
2. 数据库执行 `grid-layout` Flyway migration。
3. 前端页面定义稳定的 `pageCode`。
4. 前端页面传入默认布局和可用小组件列表。
5. 页面加载时查询个人布局；没有个人布局时使用前端默认布局。
6. 用户手动保存后写入个人布局。
7. 用户恢复默认时删除个人布局。

后端核心测试：

```bash
mvn -f mango/mango-platform/mango-grid-layout/mango-grid-layout-core/pom.xml test
```

后端模块编译安装：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-starter -am install -DskipTests
```

## 9. 返回字段

`GridLayoutPersonalVO` 常用字段：

| 字段 | 说明 | 是否建议业务入库 |
|------|------|------------------|
| `id` | 个人布局记录 ID | 后端维护 |
| `tenantId` | 当前租户 ID | 后端维护 |
| `userId` | 当前用户 ID | 后端维护 |
| `pageCode` | 页面编码 | 业务页面传入 |
| `layoutJson` | 布局 JSON 字符串 | 后端保存 |
| `createdAt` | 创建时间 | 后端维护 |
| `updatedAt` | 更新时间 | 后端维护 |

业务模块通常不需要另建布局表；按页面编码使用本模块保存当前登录人的个人布局即可。

## 10. 问题排查

| 问题 | 排查方向 |
|------|----------|
| 查询返回为空 | 当前用户在该 `pageCode` 下没有保存过个人布局，前端应使用默认布局 |
| 保存失败且提示页面编码不一致 | 检查请求 `pageCode` 与 `layoutJson.pageCode` 是否一致 |
| 保存失败且提示宽度超出 12 栅格 | 检查布局项 `x + w` 是否大于 `12` |
| 恢复默认后仍显示旧布局 | 检查删除接口是否成功，前端是否清空个人布局并重新使用默认布局 |
| 不同用户看到同一布局 | 检查登录上下文中的 `tenantId`、`userId` 是否正确，以及唯一键是否生效 |

## 11. 相关文档

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端开发流程](../../../mango-pmo/rules/backend/10-dev-flow.md)
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [@mango/grid-layout 前端包 README](../../../mango-ui/packages/grid-layout/README.md)
- [@mango/grid-layout 独立包定位 ADR](../../../mango-docs/designs/mango-grid-layout-package-adr.md)
- [工作台自定义布局设计方案](../../../mango-docs/designs/mango-grid-layout-workbench-design.md)
- [工作台自定义布局交付台账](../../../mango-docs/plans/2026-06-15-grid-layout-workbench-delivery-ledger.md)
