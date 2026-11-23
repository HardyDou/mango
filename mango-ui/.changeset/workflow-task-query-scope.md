---
'@mango/workflow': minor
---

任务分页查询（`POST /workflow/tasks/page-by-business-key`）的状态参数改为「分组值 + 细状态值」双入口：`statuses` 接受 `TODO` / `DONE` / `CANCELED` 三个分组，也接受 `COMPLETED` / `REJECTED` / `RETURNED` / `WITHDRAWN` / `TERMINATED` 五个细状态，多值取并集；`DONE` 现在包含本人驳回或退回、随后流程实例被删除的任务（依据任务级处理记录），被他人驳回牵连作废的任务仍归 `CANCELED`。部门档改由平台数据范围配置（资源码 `workflow:task:list`）按登录人下发，`deptIds` 废弃且传入会被拒绝，`scope=ORG` 直接报错并提示改用数据范围；数据范围配置为「全部」但用户缺少 `workflow:task:all` 时退化成本人范围并记 WARN。不传 `statuses` 与 `scope` 时生成的 SQL 与 `1.15.0` 逐字节一致。
