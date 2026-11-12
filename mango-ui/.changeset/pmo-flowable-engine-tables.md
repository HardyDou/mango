---
'@mango/pmo': minor
---

持久化规范新增 Flowable 引擎表边界：`act_*`、`flw_*` 由 Mango 直接集成，平台按前缀硬编码跳过租户拦截与 Schema 校验，不写入 `excluded-tables`，也不接受业务配置扩大该前缀范围；同时同步业务基线投影与规则索引。既有普通租户表、单数据源与多数据源配置语义不变。
