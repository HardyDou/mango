---
'@mango/admin-shell': patch
'@mango/cms': patch
'@mango/rbac': patch
---

恢复全量前端静态质量门禁后修正各包的严格类型与可空性缺陷：`@mango/admin-shell` 收窄 `ShellRouteMenu` 契约并修正 `routesList`、`columnsAside` 与 pinia 类型声明；`@mango/cms` 在 `saveRow` 中显式收窄 `create`/`update` 配置并移除多余的 `normalizeRow`；`@mango/rbac` 补齐 `AppModuleRuntimeStrategy` 与 `ApiResourceVO` 字段声明，修正模块码为空时的运行时默认值。对外导出与大屏布局行为不变。
