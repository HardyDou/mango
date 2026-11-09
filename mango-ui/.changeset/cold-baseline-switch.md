---
'@mango/cli': patch
---

统一 full 后端模板的构建期 cold baseline 开关：父 POM 默认关闭 `mango.baseline.enabled`，最终 API 应用预置生成 execution，关闭时清理旧 BSQL 和 manifest；生成项目通过 `MANGO_COLD_BASELINE_ENABLED` 控制运行时是否使用制品内基线。模板同时补齐 `ai`、`mango-information` 的 Flyway 模块声明，移除没有 migration 的 `captcha`、`notification` 声明，并启用 KV `cache`、`rate-limiter` 能力和单一安全过滤链。
