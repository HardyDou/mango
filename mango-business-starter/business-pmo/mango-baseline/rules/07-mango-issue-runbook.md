# Mango Issue 登记与处理 Runbook

## 1. 触发条件

业务开发使用 Mango 时，确认问题由 Mango 框架、starter、CLI、模板、前端包或发布物料引起，必须登记 Mango Issue。

非缺陷类建议也必须登记 Issue，并标记为建议。

处理已有 Mango Issue 时，必须先按本文件的处理流程确认事实和现状，再决定是否进入修复、验证、关闭或补充信息。

## 2. 仓库

Mango Issue 统一提交到：

```bash
https://github.com/HardyDou/mango/issues
```

## 3. 标题

标题格式：

```text
P0|P1|P2: <影响对象> <问题摘要>
```

示例：

```text
P0: Mango Flyway migration blocks upgraded business backend startup
P1: Generated mango.dev.json does not adapt to renamed business app folders
```

## 4. 标签

- 缺陷：`bug`
- 能力优化：`enhancement`
- 文档或流程：`documentation`
- 信息不足：`question`

没有合适标签时，先用最接近标签，不得因为标签不全而不登记 Issue。

## 5. 优先级

- `P0`：阻断服务启动、构建、发布、登录、核心验收或数据安全。
- `P1`：不阻断主流程，但导致业务升级、开发、验证明显不稳定或需要人工绕行。
- `P2`：体验优化、文档补充、低风险建议。

## 6. 证据

Issue body 必须包含：

- Mango 版本：CLI、npm 包、Maven 依赖或 Git commit。
- 消费项目场景：业务项目名称、启动方式、命令。
- 复现步骤：最少可复核步骤。
- 实际结果：错误日志、截图、命令输出或失败断言。
- 期望结果：Mango 应提供的行为。
- 影响范围：阻断、绕行成本、涉及模块。
- 业务任务记录：当前业务任务中登记 Issue 地址和阻塞状态。

日志和截图放在业务任务证据目录；Issue 中只贴关键摘要和证据路径，禁止上传密码、token、密钥、客户敏感数据。

## 7. 命令

```bash
gh issue create \
  --repo HardyDou/mango \
  --title "P1: <影响对象> <问题摘要>" \
  --label bug \
  --body-file /path/to/issue-body.md
```

紧急问题可直接写 body：

```bash
gh issue create \
  --repo HardyDou/mango \
  --title "P0: <影响对象> <问题摘要>" \
  --label bug \
  --body "## 现象
...

## 影响
P0

## 证据
..."
```

## 8. 处理已有 Issue

接到“处理 Issue / 修复 Issue / 关闭 Issue / Issue #xxx”任务时，必须先加载 Issue 信息，不得在未知 Issue 内容和影响范围时直接进入开发 preflight 或修改代码。

必须执行：

1. 读取 Issue 标题、正文、标签、状态、评论、关联 PR、最新相关提交和当前分支状态。
2. 判断 Issue 类型：缺陷、能力优化、文档流程、信息不足或已完成待关闭。
3. 判断问题是否仍存在：检查 `main`、相关发布物料、关联 PR 和本地代码；不能仅凭记忆判断。
4. 判断是否可复现：能复现时记录复现命令、环境、数据、日志或截图；不能复现时记录缺失条件和需要补充的信息。
5. 判断是否已被修复：如已修复，必须给出修复来源、版本或提交依据，并执行可行的回归验证后再建议关闭。
6. 判断影响范围：明确涉及路径、模块、接口、数据库、前端页面、测试、文档或发布物料。
7. 制定处理方案：关闭、补充信息、复现验证、修复、回归、发布或转为新 Issue。
8. 只有确定需要修改受版本控制文件、形成验证结论、发布、提交或 PR 时，才按明确的 role、phase、task 和 paths 执行 PMO preflight，并读取输出的 Must read。
9. preflight 输出 `worktree-required` 时，必须先创建或复用任务 worktree 后再改服务代码、接口、数据库、测试、前端页面或构建配置。

禁止：

- 未读取 Issue 内容就直接按猜测修复。
- 未判断问题是否仍存在就创建分支或改代码。
- 未复现或未说明不可复现原因就声明修复完成。
- 已被修复的问题重复改代码。
- 用登记 Issue 流程替代处理已有 Issue 流程。

## 9. 后续处理

- Issue 创建后，必须把 URL 写回当前业务任务记录。
- Mango 缺陷阻塞当前模块且没有同等质量替代方案时，暂停该模块开发。
- 当前任务结束时，必须提醒开发者根据 Issue 结论决定升级 Mango、更换方案或调整业务计划。
