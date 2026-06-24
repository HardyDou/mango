# Mango Issue 登记 Runbook

## 1. 触发条件

业务开发使用 Mango 时，确认问题由 Mango 框架、starter、CLI、模板、前端包或发布物料引起，必须登记 Mango Issue。

非缺陷类建议也必须登记 Issue，并标记为建议。

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

## 8. 后续处理

- Issue 创建后，必须把 URL 写回当前业务任务记录。
- Mango 缺陷阻塞当前模块且没有同等质量替代方案时，暂停该模块开发。
- 当前任务结束时，必须提醒开发者根据 Issue 结论决定升级 Mango、更换方案或调整业务计划。
