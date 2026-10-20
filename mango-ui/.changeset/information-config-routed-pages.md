---
'@mango/admin-shell': patch
'@mango/information': patch
'@mango/admin': patch
'@mango/cli': patch
---

修复信息采集「详情 / 配置 / 采集记录」打不开的问题，并把管理子页面改为配置菜单驱动的路由。

- `@mango/admin-shell`：包内隐藏路由按模块自身 feature 判定，未声明 feature 的模块默认启用；删除重复的硬编码 feature 映射表。
- `@mango/information`：删除包内 `routes` 注册，表单详情、表单设计、采集记录处理改为 `AUTH_MENU` 中 `visible: 0` 的隐藏菜单（`/information/form-detail`、`/information/form-config`、`/information/form-submissions`），用 `query.id` 定位表单。
- `@mango/admin`：聚合新的 admin-shell 与 information 版本。
- `@mango/cli`：锁定 Maven `1.0.62`。

业务项目升级后需同步 Maven `1.0.62`、执行 Resource Registry 菜单同步（`AUTH_MENU` 版本 6），并确认 `mango-information-starter` 已升级，否则三个隐藏页面不会出现在用户菜单中。
