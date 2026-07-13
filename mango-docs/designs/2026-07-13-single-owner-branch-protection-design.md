# 单 Owner 仓库分支保护设计

## 1. 背景

Mango 当前只有一个仓库 Owner，主要治理变更也由该 Owner 创建 PR。GitHub 禁止 PR 作者批准自己的 PR，因此“至少一人批准 + Code Owner approval + 对管理员生效”的组合会永久阻断 Owner 自己发起的发布，即使 Required Check 已全部通过。

本设计只解决单 Owner 仓库的可执行门禁，不降低机器质量检查，也不改变未来多人维护时恢复独立评审的选择。

## 2. 决策

采用“机器硬门禁 + Owner 合并”的单 Owner 模式：

- `pmo-doc-check` 继续作为 `main` 的 strict Required Check。
- Required approving review count 调整为 `0`，关闭远端 Code Owner approval。
- `CODEOWNERS` 保留，继续表达责任范围和评审路由，但不作为不可满足的远端阻断条件。
- Conversation resolution、enforce admins、禁止 force push、禁止 deletion 保持不变。
- PMO 规则按仓库治理模式区分单 Owner 与多人维护；多人维护且存在稳定独立评审人时，才要求远端审批门禁。

## 3. 备选方案

### 3.1 任一协作者强制审批

可以保留至少一人审批，但发布持续依赖协作者在线，无法满足 Owner 独立维护和紧急发布要求，因此不采用。

### 3.2 临时关闭审批后恢复

每次发布临时修改保护规则会制造高风险窗口和重复人工操作，也无法形成稳定可审计配置，因此不采用。

### 3.3 机器账号审批

机器账号不能提供独立业务或架构判断，只会把形式审批伪装成人工结论，因此不采用。

## 4. 实现范围

1. 修订 PMO 交付质量规则，定义单 Owner 模式和多人维护模式。
2. 修订治理意图检查，使其校验规则文本、CODEOWNERS 覆盖和稳定 Required Check，不错误要求单 Owner 仓库开启远端 Code Owner approval。
3. 增加单 Owner 正例和“不允许关闭 Required Check”的反例测试。
4. 将远端 `main` 配置为零强制审批、关闭 Code Owner approval，并保留其它保护项。
5. 更新分支保护证据，记录实际远端配置和采集时间。

## 5. 失败与恢复

- 远端修改前保存当前保护配置到忽略的 `.runtime/`。
- 只修改 review protection 字段；Required Check 和其它保护字段不参与更新。
- 修改后立即从 GitHub API 回读验证。任一保留项变化即停止发布并恢复原配置。
- 规则、检查器和远端配置不一致时，不得声明治理完成或继续发布。

## 6. 验证

- PMO 合同、Skill 和治理意图测试全部通过。
- `workspace-layout-check` 通过。
- 真实 PR 的 `pmo-doc-check` 仍为 Required Check 且 strict。
- GitHub API 显示 approvals 为 `0`、Code Owner approval 为 `false`。
- GitHub API 同时显示 conversation resolution 和 enforce admins 为 `true`，force push 和 deletion 为 `false`。
- Owner 可在 Required Check 通过后正常合并 PR，无需修改保护规则。

## 7. 非目标

- 不删除 `CODEOWNERS`。
- 不弱化或跳过 `pmo-doc-check`。
- 不允许直接推送绕过 PR。
- 不把当前单 Owner 决策永久套用于未来多人维护仓库。
