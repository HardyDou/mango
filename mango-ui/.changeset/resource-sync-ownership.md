---
'@mango/site-shell': major
'@mango/admin': major
'@mango/admin-shell': patch
'@mango/rbac': patch
'@mango/cms': patch
'@mango/cli': patch
---

发布资源同步模式收敛、资源所有权记录、CMS banner 能力下线与信息采集提交风控。

- `@mango/site-shell`（major）：移除 `SiteBanner`/`CmsBanner` 类型与 `listBanners` 调用，跟随 CMS banner 能力下线。
- `@mango/admin`（major）：聚合 site-shell 2.0.0 与新的 admin-shell、rbac、cms 版本。
- `@mango/admin-shell`（patch）：菜单树父级高亮修复、内容区滚动复位与页签搜索状态保留。
- `@mango/rbac`（patch）：菜单管理页补充 keep-alive 运行时配置。
- `@mango/cms`（patch）：内容运营页适配 banner 下线后的资源视图。
- `@mango/cli`（patch）：锁定 Maven `1.1.0`。

业务项目升级时必须整体升级 Maven `1.1.0` 与 npm 闭包；移除 banner 相关调用与菜单，执行 `V3`/`V4` 资源迁移，并按 README 校验 `SYNC_ALWAYS`/`SYNC_ONCE` 声明。
