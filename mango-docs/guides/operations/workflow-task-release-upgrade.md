# Workflow 任务释放与改派升级指南

## 1. 适用范围

- 升级对象：`mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter`、`@mango/workflow`。
- 触发场景：业务页面出现「平台派单任务无法释放」（Mango Issue #1275），或需要为承办人请假、调岗、离职、派错人提供合法处置手段。
- 不适用范围：流程定义重写、数据库结构迁移、引擎升级。

## 2. 行为变化

| 项 | 升级前 | 升级后 |
|---|---|---|
| 释放判据 | 只认 `mangoClaimedFromCandidate` 任务级标记，等价于「只有本人从候选池认领的任务能释放」 | 按承办人来源判定：`SELF_CLAIM`、`AUTO_ASSIGN` 可释放；其余来源明确拒绝并说明原因 |
| 平台派单任务 | `claim` 报「任务已被认领」，`unclaim` 报「只能释放通过认领获得的任务」，两条路都被拒绝 | `unclaim` 成功，任务回到节点候选池，承办人清空 |
| 释放后自动派单 | 无此场景 | 释放的任务写入 `mangoReleasedToPool`，流程推进不再对它自动派单；节点再次进入生成的新任务正常自动派单 |
| 转办目标 | 接受任意字符串，可转给不存在或已停用的账号 | 限定为当前租户的启用成员，按用户 ID 或用户名都能解析 |
| 引擎 `ASSIGNEE_` 取值 | 平台派单写用户 ID，认领和候选展开写用户名 | 统一写登录用户名，解析不出用户名时回落用户 ID；存量数据读取侧保持双形态兼容 |
| 新增能力 | 无 | `POST /workflow/tasks/reassign`（改派到具体人或回收进候选池）、`GET /workflow/tasks/assignee-options`（可选办理人）、`WorkflowTaskVO.assignSource` |
| 节点动作 | 释放不受节点配置控制 | 节点动作新增 `unclaim`，默认开启；需要「仅限本人办理」的节点可以关停 |

## 3. 业务侧需要配合的动作

1. 释放按钮继续按 `WorkflowTaskVO.unclaimable` 显示，不要再用 `claimStatus` 推断。升级后 `unclaimable` 与 `unclaim` 共用同一判据，不会再出现「页面显示可释放、接口拒绝」。
2. 原来拦截的错误码 `417013`（「仅可释放本人领取的任务」）应改为按 `unclaimable` 判断；`mango-baohan-risk` 恢复「释放」动作即可，无需平台额外配置。
3. 需要「承办人不在（请假、离职、调岗）」的场景改用管理员改派 `POST /workflow/tasks/reassign`，权限码 `workflow:task:reassign`；改派目标从 `GET /workflow/tasks/assignee-options` 取。`targetUserId` 改派给具体人；`pool=true` 把任务回收进候选池——回收不要求调用者是承办人，但来源白名单与承办人自助释放一致（仅本人认领与平台派单可回收），按人展开和指定类任务仍只能改派给具体人。
4. 需要区分「本人认领」和「平台派单」时读 `WorkflowTaskVO.assignSource`，不要再解析 `claimTime`：Flowable 的 `CLAIM_TIME_` 只在认领时写入，平台派单不会写。
5. 按 `ASSIGNEE_` 精确匹配的自研 SQL 或报表要同时兼容用户 ID 和用户名：升级后新数据统一为用户名，存量数据仍是两种形态混合。

## 4. 不支持释放的来源与处理方式

| 来源 `assignSource` | 典型节点 | 处理方式 |
|---|---|---|
| `CANDIDATE_EXPAND` | 角色、岗位、组织、组织主管审批人按人展开（会签、或签、依次） | 转办（本人）或管理员改派 |
| `DEFINITION` | 流程定义直接指定办理人 | 转办或管理员改派 |
| `INITIATOR_SELECT` / `FORM_USER` | 发起人选择、表单字段指定 | 转办或管理员改派 |
| `TRANSFER` | 转办接手 | 再次转办或管理员改派 |
| `REASSIGN` | 管理员改派接手 | 管理员改派 |
| `RETURN_ASSIGN` | 退回时上游指定复核人 | 转办或管理员改派 |

失败提示会给出具体来源原因；节点启用了转办时追加「如需移交请使用转办」，未启用转办时只说明原因，不提示做不到的动作。

## 5. 已知行为与运维口径

- 释放后的任务是「候选池任务」：任何当前候选人都可以领取，包括原承办人。释放不等于「我不办了」，弃权语义不在本版本范围内。
- 会签节点释放后可能出现「任务停在池中、无人领取」：原成员各自已有待办，只能等新增候选人，或由管理员改派。平台不做超时回收。
- 需要人工兜底时，工作流管理员用 `todoType=ALL` 查看未指派任务并直接办理；管理员对已有承办人的任务没有代办权限，只能走改派。
- 存量任务在升级后第一次释放/查询时按 `workflow_task_record` 中该任务最近一条 `CLAIM` 或 `AUTO_ASSIGN` 记录推导来源；推导不出按 `DEFINITION` 处理，承办人无法自助释放。

## 6. 回滚方式

回退 `mango-workflow-{api,core,starter}` 与 `@mango/workflow` 到升级前版本即可。已释放的任务在旧版本里因为缺少 `mangoClaimedFromCandidate` 标记无法释放（与升级前行为一致），任务本身仍在正常待办中；`mangoAssignSource`、`mangoReleasedToPool` 只是任务级变量，旧版本忽略它们，不需要清理数据。
