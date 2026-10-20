---
'@mango/cli': patch
---

随 Maven `1.0.63` 发布 `@mango/cli@1.3.3`，把 CLI 的 Maven 版本矩阵锁定到 `1.0.63`。

本批次只包含后端修复：`mango-information-starter` 的 `InformationFormResourceHandler` 声明 `ResourceBaselinePolicy.ENVIRONMENT_REQUIRED`，内置表单不再进入可移植数据库基线，修复 `MANGO_BSQL_ENABLED=true` 构建的 `MANGO-BASELINE-040`。

业务项目升级后需重新生成数据库基线（`mango:baseline-generate`），并在部署期由 bootstrap 恢复正常创建内置表单。
