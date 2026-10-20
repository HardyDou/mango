---
'@mango/cli': patch
---

随 Maven `1.0.61` 发布 `@mango/cli@1.3.1`，把 CLI 的 Maven 版本矩阵锁定到 `1.0.61`。本批次无其它 npm 包内容变化。

- `@mango/cli`：`release-versions.json` 的 `maven.mangoBackend` 更新为 `1.0.61`，生成项目和模板随之锁定该 Maven 版本。

业务项目升级 `@mango/cli@1.3.1` 后，配套升级 Maven `1.0.61`；如业务项目此前手工配置了 `/cms/open/**`、`/information/open/**` 或 `/link/open/**` 的 `mango.auth.security.permit-paths`，应删除这些条目并依赖 Resource Registry 同步 `API_RESOURCE` 声明。
