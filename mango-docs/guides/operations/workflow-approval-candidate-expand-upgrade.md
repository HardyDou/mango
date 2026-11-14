# Workflow 审批人候选组展开升级指南

本文面向已接入 Mango Workflow 的业务项目，说明升级到“角色、岗位、组织审批人按多人审批方式展开”版本时，哪些节点会改变任务生成方式、业务代码要改什么、怎么发布和回滚。

适用版本：Maven `1.9.0` 起（Issue #1263）。模块运行时事实源见 [Workflow 模块 README](../../../mango/mango-platform/mango-workflow/README.md) 的“审批人来源与多人审批方式”和“升级与兼容”小节。

## 1. 一句话结论

升级本身不会改变任何已发布流程的行为。**只有重新发布流程定义后**，`assigneeType=SPECIFIED_ROLE|SPECIFIED_POST|SPECIFIED_ORG|ORG_LEADER` 的节点才会从“一条共享待领取任务”变成“每个成员一条任务”，并且按节点 `approvalMode` 处理（或签一人办结、会签全员/比例、依次按顺序）。

## 2. 是否受影响

满足以下任一条即受影响：

1. 流程定义的节点 `approvalConfig.assigneeType` 是 `SPECIFIED_ROLE`、`SPECIFIED_POST`、`SPECIFIED_ORG` 或 `ORG_LEADER`。
2. 业务代码或页面依赖这些节点的“待领取”状态：读取 `claimStatus`、比较 `assigneeDisplayName=="待领取"`、调用 `/workflow/tasks/claim` 或 `unclaim`、统计 `todoType=CLAIMABLE`。
3. 业务代码在这些节点上执行“自动领取”或“指定上一轮办理人处理”。

不受影响：`SPECIFIED_USER`（显式用户）、`INITIATOR`、`INITIATOR_SELECT`、`FORM_USER` + 表单用户字段、`EXPRESSION`，以及 `assignmentMode=AUTO` 的自动派单节点（仍是一条任务、由平台派单人）。

自查命令（导出业务仓的 designer JSON 后执行）：

```bash
grep -rn "SPECIFIED_ROLE\|SPECIFIED_POST\|SPECIFIED_ORG\|ORG_LEADER" path/to/workflow/definition/*.java
grep -rn "待领取\|CLAIMABLE\|claimTask\|/workflow/tasks/claim" path/to/backend path/to/frontend
```

## 3. 改前后对比

| 节点配置 | 改前（旧行为） | 改后（重新发布后） |
|---|---|---|
| 角色 + `OR_SIGN` | 1 条共享任务，停在待领取 | 每名成员 1 条待办，任一人办结，其余 `MI_END` 取消 |
| 角色 + `COUNTERSIGN`（节点默认值） | 1 条共享任务，一人领取办结 | 每名成员 1 条待办，按 `passRatio` 通过，默认 100% 即全部成员通过 |
| 角色 + `SEQUENTIAL` | 1 条共享任务，无序 | 每名成员按 `userId` 升序依次到达 |
| 角色 + `assignmentMode=AUTO` | 1 条任务，平台派给一人 | 不变 |

成员解析在流程进入节点时执行，角色、岗位、组织成员变化对下一次提交生效，不需要重新发布定义。成员为空时按节点 `emptyAssigneeStrategy` 处理（`AUTO_REJECT` 仍会驳回，`TO_ADMIN` 仍转管理员）。

## 4. 选择迁移路径

按节点原本的业务语义选一种，不要混用。

### 4.1 路径 A：角色内任一人处理（推荐）

适用：原来靠“领取”把任务分给某个人，业务上只要求一个人签。

1. 把节点 `approvalConfig.approvalMode` 改为 `OR_SIGN`。
2. 删除该节点 `actions.claim`（展开后没有可领取任务）。
3. 业务代码把“待领取”判断改为“任务已派单给我，尚未处理”。

### 4.2 路径 B：由平台自动指派一个人

适用：业务希望“任务到达即到人”，例如“沿用上一轮办理人”。

1. 节点设 `assignmentMode=AUTO`。
2. 选择策略：`autoAssignmentStrategy=ROUND_ROBIN`（轮询）、`LEAST_TASKS`（任务量最少）、`AFFINITY`（优先同一流程实例最近完成任务且仍在候选集的人）、`NODE_AFFINITY`（优先同一节点最早处理过的人，可配 `nodeAffinityScope=BUSINESS_KEY|AFFINITY_KEY` 跨轮次复用）。
3. 删除业务侧的自动领取代码；任务从创建起就带 `assignee`。
4. 删除 `actions.claim`。

### 4.3 路径 C：真正需要全员会签

适用：业务确实要求角色内每名成员都通过。

1. 保持 `approvalMode=COUNTERSIGN`，按需设置 `passRatio`（例如 `50` 表示过半通过）。
2. 删除 `actions.claim` 和所有待领取/自动领取逻辑。
3. 页面提示改为“会签”，并说明需要哪些人处理。

## 5. 需要同步调整的业务代码

| 位置 | 改法 |
|---|---|
| `assigneeDisplayName=="待领取"`、`claimStatus=NONE` 判断 | 改为按 `claimStatus=ASSIGNED`（已派单）和 `assigneeId` 是否当前用户判断；“未处理”用任务是否仍存在于待办表示 |
| `todoType=CLAIMABLE` 统计 | 这类节点不再产生待领取任务；保留统计时只服务 `assignmentMode=AUTO` + `NODE_AFFINITY` + `nodeAffinityFallback=CLAIM` 的例外节点 |
| `/workflow/tasks/claim`、`unclaim` 调用 | 展开节点没有可认领任务，调用会返回业务错误；按第 4 节路径删除或改用 `/workflow/tasks/transfer`（转办） |
| 自动领取上一轮办理人 | 用路径 B 的 `AFFINITY`/`NODE_AFFINITY` 交给平台；或对已派单任务调用 `/workflow/tasks/transfer` 转给上一轮办理人 |
| 业务快照里的“待领取”状态（如 `INITIAL_PENDING_CLAIM`） | 改为“已派单待处理”，并在快照更新时保存实际办理人 |
| 页面“领取”按钮 | 展开节点移除；保留节点动作配置与页面按钮一致 |

业务快照和页面文案属于业务事实，不要靠平台字段兼容；平台只保证任务、候选人和办理人字段本身可读。

## 6. 发布与验证步骤

1. 升级 Maven/npm 闭包到本批次版本，按批次升级指南执行（`mango-docs/guides/operations/mango-<旧>-to-<新>-upgrade.md`）。
2. 先在预发布环境按第 4 节改完节点配置和业务代码，**再重新发布流程定义**。
3. 发起一条测试流程，按节点类型断言：

```sql
-- 期望：角色 3 人 + 或签 → 3 条任务，办理人分别为 3 名成员
select assignee_, count(*) from ACT_RU_TASK where proc_inst_id_ = '<pid>' group by assignee_;
-- 期望：0（展开节点不再挂候选组）
select count(*) from ACT_RU_IDENTITYLINK where proc_inst_id_ = '<pid>' and group_id_ is not null;
```

```bash
# 每名成员各自应命中 1 条待办，待领取 0
curl -s "$BASE/workflow/tasks/todo?todoType=ASSIGNED&page=1&size=10" -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/workflow/tasks/todo?todoType=CLAIMABLE&page=1&size=10" -H "Authorization: Bearer $TOKEN"
```

4. 由一名成员调 `/workflow/tasks/complete-result`，断言 `ended=true`、节点剩余任务 0、其余历史任务 `delete_reason_=MI_END`。
5. 回归业务页面：待办、已办、我的发起、审批进度、通知、加签、退回、驳回、转办。

可重复的本地验证脚本参考 [Issue #1263 本地场景脚本](../../evidence/issue-1263/run-local-scenario.sh)。

## 7. 回滚

- 行为回滚：把节点改回不需要展开的配置（`assignmentMode=AUTO`）后重新发布，或回退到升级前的发布版本并重新发布定义。已生成的运行时任务不受历史版本影响。
- 代码回滚不改变已发布定义；需要同时回退定义，否则新定义仍按展开语义执行。
- 已展开节点的历史任务保留 `MI_END` 记录，回滚不回写历史。

## 8. 业务升级日志（可直接写入发布说明）

```text
Changed: 角色、岗位、组织、组织负责人审批人按节点 approvalMode 展开为每人一条任务。
Affected: 重新发布后，这类节点不再生成待领取共享任务；会签默认值下要求全体成员通过。
Business adaptation: 按节点语义选择路径 A（approvalMode=OR_SIGN）、路径 B（assignmentMode=AUTO + AFFINITY/NODE_AFFINITY）或路径 C（保留 COUNTERSIGN 并删除领取逻辑）；把“待领取”判断改为按办理人判断，删除 claim/unclaim 调用或改用 transfer；升级但不重新发布定义时行为不变。
Verification: 发起一条流程，断言运行时任务数量与办理人、待办 1／待领取 0、一人办结后其余 MI_END。
Rollback: 回退节点配置或版本并重新发布定义；历史任务不回写。
```

## 9. 关联文档

- [Workflow 模块 README](../../../mango/mango-platform/mango-workflow/README.md)
- [业务审批接入 · 审批人来源与多人审批方式](../business-integration/workflow-business-approval.md)
- [能力地图 · Workflow](../../capabilities/README.md)
- [Agent 文本输出规范](../../../mango-pmo/rules/13-agent-text-output.md)
