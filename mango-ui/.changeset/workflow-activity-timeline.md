---
'@mango/workflow': minor
---

新增活动时间轴接口 `POST /workflow/tasks/activity-timeline-by-business-key`：按业务键返回流程实例的全部活动（含网关与事件节点），`kind` 区分任务/网关/事件，用于还原完整审批路径；与既有流转记录接口互补，后者语义不变。
