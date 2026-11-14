---
'@mango/workflow': patch
---

待办任务分页支持按业务申请的可筛属性过滤：Flyway `V5` 新增 `workflow_business_apply_attribute` 子表，创建申请可携带 `filterAttributes`，新增 `PUT /workflow/business-applies/filter-attributes` 整体覆盖可筛属性，任务分页新增 `applyAttributeFilters`（条件间且、条件内或）、`applyTimeBegin`/`applyTimeEnd` 与 `sortBy=applyTime`，事件载荷与默认通知模板变量同步支持可筛属性。本包只更新文档说明，运行时代码变化在 Maven `1.9.0` 中发布。
