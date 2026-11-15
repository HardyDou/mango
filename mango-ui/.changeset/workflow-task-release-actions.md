---
'@mango/workflow': minor
---

任务释放与改派可视能力补齐：任务视图新增 `assignSource`（承办人来源），释放按钮继续按 `unclaimable` 判断，不再需要解析 `claimTime`；设计器节点动作新增「释放」开关（默认开启），可按节点关停承办人自助释放。新增 `POST /workflow/tasks/reassign`（管理员改派到具体办理人，或把平台派单任务回收进候选池）与 `GET /workflow/tasks/assignee-options`（按节点配置返回当前候选成员）的前端类型与请求封装。本包新增公开字段、两个请求封装和设计器动作开关，属于可视能力变化，按版本判定表使用 minor；Maven 运行时代码变化随下一 Maven 发布批次交付。
