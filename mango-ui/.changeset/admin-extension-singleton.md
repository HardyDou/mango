---
'@mango/admin': patch
'@mango/admin-extension': minor
'@mango/admin-pages': minor
'@mango/ai': minor
'@mango/file': minor
'@mango/cli': patch
---

修复 `@mango/admin-extension` 全局单例被劈裂导致的模块菜单 404。

- `@mango/admin-extension`：页面注册表改挂到 `globalThis` 全局单例，同版本多副本共用一份注册表；检测到两个不同版本同时加载时抛出明确错误，替代静默 404。
- `@mango/admin-extension` 对 `@mango/admin-pages`、`@mango/ai`、`@mango/file` 和业务模块包改为 `peerDependencies`，由宿主应用提供唯一版本，pnpm 不再装出两份。
- `@mango/cli`：生成项目的 `frontend/package.json` 显式声明单一 `@mango/admin-extension`；业务模块模板改为 `peerDependencies`。

业务项目必须把 `@mango/admin-extension` 与整套 Mango 包升级到同一发布批次，禁止只升单包。
