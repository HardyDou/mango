# Grid Layout 自定义栅格布局

## 1. 能力定位

提供当前登录用户的自定义栅格布局保存、查询和删除能力，配合前端 `@mango/grid-layout` 使用。

主要使用者：Mango 开发者、业务开发者和 AI Agent。

## 2. 适用场景

适用于工作台、数据看板、业务驾驶舱、详情页自定义面板等需要按用户保存页面布局的场景。

## 3. 不适用场景

不负责小组件业务数据查询，不负责小组件权限过滤，不负责默认布局维护，不负责前端拖拽算法，也不负责复杂多端冲突合并。

## 4. 模块边界

模块包含 `api/core/starter`：

- `api` 提供 `GridLayoutPersonalApi`、`SaveGridLayoutPersonalCommand`、`GridLayoutPersonalQuery` 和 `GridLayoutPersonalVO`。
- `core` 负责个人布局保存、查询、删除、JSON 校验和数据库访问。
- `starter` 提供自动装配和 HTTP Controller。

数据按当前登录租户、当前登录用户和 `pageCode` 隔离。

## 5. 接入方式

后端应用引入：

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

## 6. 配置项

随 starter 自动装配。第一版不提供额外业务配置项。

## 7. 对外接口 / 扩展点

HTTP 接口：

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/grid-layout/personal?pageCode=...` | 查询当前登录人的页面布局 |
| PUT | `/grid-layout/personal` | 保存当前登录人的页面布局 |
| DELETE | `/grid-layout/personal?pageCode=...` | 删除当前登录人的页面布局 |

Java API：

- `GridLayoutPersonalApi#getPersonal`
- `GridLayoutPersonalApi#savePersonal`
- `GridLayoutPersonalApi#deletePersonal`

## 8. 数据库 / 初始化数据

migration 路径：

```txt
mango-grid-layout-core/src/main/resources/db/migration/grid-layout/V1__init_grid_layout.sql
```

新增表：

```txt
mango_user_grid_layout
```

唯一约束：

```txt
tenant_id + user_id + page_code
```

## 9. 菜单 / 权限 / 租户

接口使用登录访问模式，不新增菜单权限点。

租户和用户来自 `MangoContextHolder`，前端不传 `tenantId` 和 `userId`。

## 10. 验证方式

```bash
mvn -f mango/mango-platform/mango-grid-layout/mango-grid-layout-core/pom.xml test
```

后端模块编译安装：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-starter -am install -DskipTests
```

## 11. 业务接入最小闭环

1. 后端应用引入 `mango-grid-layout-starter`。
2. 数据库执行 `grid-layout` migration。
3. 前端页面定义稳定 `pageCode`。
4. 前端页面传入默认布局和可用小组件列表。
5. 页面加载时查询个人布局；无个人布局时使用前端默认布局。
6. 用户手动保存后写入个人布局。
7. 用户恢复默认时删除个人布局。

## 12. 常见问题

- 查询返回空：说明当前用户在当前 `pageCode` 下没有保存过个人布局，前端应使用默认布局。
- 保存失败且提示页面编码不一致：检查请求 `pageCode` 和 `layoutJson.pageCode` 是否一致。
- 保存失败且提示宽度超出 12 栅格：检查布局项 `x + w` 是否大于 12。
- 恢复默认后仍显示旧布局：检查前端是否删除成功后重新使用默认布局或刷新了本地状态。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端开发流程](../../../mango-pmo/rules/backend/10-dev-flow.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [@mango/grid-layout 独立包定位 ADR](../../../mango-docs/designs/mango-grid-layout-package-adr.md)
- [工作台自定义布局设计方案](../../../mango-docs/designs/mango-grid-layout-workbench-design.md)
- [工作台自定义布局交付台账](../../../mango-docs/plans/2026-06-15-grid-layout-workbench-delivery-ledger.md)
